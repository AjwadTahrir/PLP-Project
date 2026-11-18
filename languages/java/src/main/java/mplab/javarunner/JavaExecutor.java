package mplab.javarunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

/**
 * MPLab language module for Java (OpenJDK).
 *
 * <p>Pipeline for each request:
 * <ol>
 *   <li>validate the request (language, size limits);</li>
 *   <li>analyse the source to choose the file name and main class;</li>
 *   <li>write {@code Name.java} into a fresh temporary directory;</li>
 *   <li>compile with {@code javac} (stage "compile") — non-zero exit = {@code compile_error};</li>
 *   <li>run with {@code java} under heap/stack/time limits (stage "run");</li>
 *   <li>classify the outcome and delete the directory.</li>
 * </ol>
 */
public final class JavaExecutor implements LanguageExecutor {

    private final ExecutorConfig config;
    private final Sandbox sandbox;
    private final Semaphore slots;
    private volatile String cachedVersion;

    public JavaExecutor(ExecutorConfig config) {
        this.config = config;
        this.sandbox = Sandbox.fromConfig(config);
        this.slots = new Semaphore(Math.max(1, config.maxConcurrent));
    }

    @Override
    public String language() {
        return "java";
    }

    @Override
    public String version() {
        String v = cachedVersion;
        if (v == null) {
            v = detectVersion();
            cachedVersion = v;
        }
        return v;
    }

    public String sandboxName() {
        return sandbox.name();
    }

    @Override
    public ExecutionResult execute(ExecutionRequest req) {
        ExecutionResult invalid = validate(req);
        if (invalid != null) {
            return invalid.version(version());
        }
        boolean acquired = false;
        Path dir = null;
        try {
            slots.acquire();
            acquired = true;
            dir = Files.createTempDirectory("mplab-java-");
            if ("docker".equals(sandbox.name())) {
                makeWorldWritable(dir); // container runs as 'nobody'
            }
            return compileAndRun(req, dir).version(version());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.of(ExecutionStatus.INTERNAL_ERROR, "Execution interrupted").version(version());
        } catch (IOException e) {
            return ExecutionResult.of(ExecutionStatus.INTERNAL_ERROR,
                    "Could not start the Java toolchain: " + e.getMessage()).version(version());
        } finally {
            if (acquired) {
                slots.release();
            }
            deleteQuietly(dir);
        }
    }

    private ExecutionResult validate(ExecutionRequest req) {
        if (req == null) {
            return ExecutionResult.of(ExecutionStatus.INVALID_REQUEST, "Missing request");
        }
        if (req.language() == null || !"java".equalsIgnoreCase(req.language().trim())) {
            return ExecutionResult.of(ExecutionStatus.UNSUPPORTED_LANGUAGE,
                    "The Java module only accepts language \"java\"");
        }
        if (req.sourceCode() == null || req.sourceCode().isBlank()) {
            return ExecutionResult.of(ExecutionStatus.INVALID_REQUEST, "source_code must not be empty");
        }
        if (req.sourceCode().getBytes(StandardCharsets.UTF_8).length > config.maxSourceBytes) {
            return ExecutionResult.of(ExecutionStatus.INVALID_REQUEST,
                    "source_code exceeds " + config.maxSourceBytes + " bytes");
        }
        if (req.stdin().getBytes(StandardCharsets.UTF_8).length > config.maxStdinBytes) {
            return ExecutionResult.of(ExecutionStatus.INVALID_REQUEST,
                    "stdin exceeds " + config.maxStdinBytes + " bytes");
        }
        return null;
    }

    private ExecutionResult compileAndRun(ExecutionRequest req, Path dir) throws IOException, InterruptedException {
        SourceAnalyzer.Analysis a = SourceAnalyzer.analyze(req.sourceCode());
        String fileName = a.fileBaseName + ".java";
        Files.writeString(dir.resolve(fileName), req.sourceCode(), StandardCharsets.UTF_8);

        // ---- Stage 1: compile -------------------------------------------------------------------
        List<String> javacArgs = List.of(
                "-J-Xmx256m", "-J-Duser.language=en", "-J-Duser.country=US",
                "-encoding", "UTF-8", "-Xmaxerrs", "50", "-d", ".", fileName);
        ProcessRunner.Outcome c = ProcessRunner.run(
                sandbox.command("javac", javacArgs, dir, true, config.compileTimeLimitMs),
                dir, "", config.compileTimeLimitMs + hostGraceMs(), config.maxOutputBytes);

        if (c.timedOut || isSandboxTimeout(c)) {
            return ExecutionResult.of(ExecutionStatus.TIMEOUT,
                            "Compilation exceeded " + config.compileTimeLimitMs + " ms")
                    .stage("compile").stderr(c.stderr).compileTimeMs(c.elapsedMs);
        }
        if (c.exitCode == null || c.exitCode != 0) {
            String diagnostics = (c.stderr + c.stdout).trim();
            return ExecutionResult.of(ExecutionStatus.COMPILE_ERROR, summariseCompileErrors(diagnostics))
                    .stage("compile").stderr(diagnostics + "\n").exitCode(c.exitCode)
                    .compileTimeMs(c.elapsedMs).truncated(c.truncated);
        }

        // ---- Stage 2: run -----------------------------------------------------------------------
        List<String> javaArgs = List.of(
                "-Xmx" + config.memoryMb + "m", "-Xss8m",
                "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-XX:-UsePerfData",
                "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-Duser.language=en", "-Duser.country=US",
                "-cp", ".", a.mainClass);
        ProcessRunner.Outcome r = ProcessRunner.run(
                sandbox.command("java", javaArgs, dir, false, config.runTimeLimitMs),
                dir, req.stdin(), config.runTimeLimitMs + hostGraceMs(), config.maxOutputBytes);

        ExecutionResult res = new ExecutionResult()
                .stage("run").stdout(r.stdout).stderr(r.stderr).exitCode(r.exitCode)
                .compileTimeMs(c.elapsedMs).executionTimeMs(r.elapsedMs).truncated(r.truncated);

        if (r.timedOut || isSandboxTimeout(r) || r.elapsedMs > config.runTimeLimitMs + 1000) {
            return res.status(ExecutionStatus.TIMEOUT).exitCode(null)
                    .message("Program exceeded the time limit of " + config.runTimeLimitMs + " ms");
        }
        if (r.stderr.contains("java.lang.OutOfMemoryError")
                || (sandbox.memoryKilledExitCode() != null && sandbox.memoryKilledExitCode().equals(r.exitCode))) {
            return res.status(ExecutionStatus.MEMORY_LIMIT_EXCEEDED)
                    .message("Program exceeded the memory limit of " + config.memoryMb + " MB");
        }
        if (r.exitCode != null && r.exitCode == 0) {
            return res.status(ExecutionStatus.SUCCESS)
                    .message(r.truncated ? "Program finished (output truncated)" : "Program finished successfully");
        }
        return res.status(ExecutionStatus.RUNTIME_ERROR).message(summariseRuntimeError(r));
    }

    /** Extra host-side wait when the sandbox enforces its own limit (container start-up time). */
    private long hostGraceMs() {
        return sandbox.timeoutExitCode() != null ? 3000 : 0;
    }

    private boolean isSandboxTimeout(ProcessRunner.Outcome o) {
        return sandbox.timeoutExitCode() != null && sandbox.timeoutExitCode().equals(o.exitCode);
    }

    /** "2 compilation errors; first at line 3: ';' expected". */
    static String summariseCompileErrors(String diagnostics) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^[^\\s:]+\\.java:(\\d+): error: (.+)$", java.util.regex.Pattern.MULTILINE)
                .matcher(diagnostics);
        int count = 0;
        String first = null;
        while (m.find()) {
            count++;
            if (first == null) {
                first = "line " + m.group(1) + ": " + m.group(2).trim();
            }
        }
        if (count == 0) {
            return "Compilation failed";
        }
        return count + " compilation error" + (count == 1 ? "" : "s") + "; first at " + first;
    }

    /** "Uncaught java.lang.ArithmeticException: / by zero" or "Program exited with code 3". */
    static String summariseRuntimeError(ProcessRunner.Outcome r) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Exception in thread \"[^\"]*\" (\\S+?)(?::\\s*(.*))?$", java.util.regex.Pattern.MULTILINE)
                .matcher(r.stderr);
        if (m.find()) {
            return "Uncaught " + m.group(1) + (m.group(2) != null ? ": " + m.group(2).trim() : "");
        }
        if (r.stderr.contains("Could not find or load main class") || r.stderr.contains("main method not found")
                || r.stderr.contains("Main method not found")) {
            return "No runnable main method found - declare public static void main(String[] args)";
        }
        return "Program exited with code " + r.exitCode;
    }

    private String detectVersion() {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("mplab-java-ver-");
            ProcessRunner.Outcome o = ProcessRunner.run(
                    sandbox.command("javac", List.of("-version"), dir, false, 60_000),
                    dir, "", 60_000, 4096);
            String v = (o.stdout + o.stderr).trim();
            return v.isEmpty() ? "javac (unknown version)" : v.lines().findFirst().orElse(v);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "javac (not found)";
        } finally {
            deleteQuietly(dir);
        }
    }

    private static void makeWorldWritable(Path dir) {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            try {
                Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxrwxrwx"));
            } catch (IOException ignored) {
                // compilation will report a clear error if the container cannot write
            }
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }
}
