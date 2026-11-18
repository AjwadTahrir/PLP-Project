package mplab.javarunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a logical tool invocation ({@code javac ...} / {@code java ...}) into the real command line.
 *
 * <ul>
 *   <li>{@link Local} — runs the JDK directly as a child process. For development only.</li>
 *   <li>{@link Docker} — runs inside a throw-away container with no network, capped memory/CPU/PIDs,
 *       read-only root filesystem and all capabilities dropped. This is a stand-in until the class-wide
 *       sandbox infrastructure (approved by FSKTM IT) is agreed; the interface stays the same.</li>
 * </ul>
 */
public interface Sandbox {

    String name();

    /**
     * @param tool       "javac" or "java"
     * @param args       arguments to the tool
     * @param workDir    host directory containing the source / class files
     * @param writable   whether the program may write to workDir (true for compilation only)
     * @param timeoutMs  limit enforced inside the sandbox as well as by the host
     */
    List<String> command(String tool, List<String> args, Path workDir, boolean writable, long timeoutMs);

    /** Exit code meaning "killed by sandbox timeout", or null if the sandbox has none. */
    default Integer timeoutExitCode() {
        return null;
    }

    /** Exit code meaning "killed for exceeding memory", or null if the sandbox has none. */
    default Integer memoryKilledExitCode() {
        return null;
    }

    static Sandbox fromConfig(ExecutorConfig config) {
        if ("docker".equalsIgnoreCase(config.sandbox)) {
            return new Docker(config.dockerImage, config.memoryMb);
        }
        return new Local(config.javaHome);
    }

    /** Development mode: run the host JDK directly. */
    final class Local implements Sandbox {
        private final String javaHome;

        public Local(String javaHome) {
            this.javaHome = javaHome;
        }

        @Override
        public String name() {
            return "local";
        }

        @Override
        public List<String> command(String tool, List<String> args, Path workDir, boolean writable, long timeoutMs) {
            List<String> cmd = new ArrayList<>();
            cmd.add(resolve(tool));
            cmd.addAll(args);
            return cmd;
        }

        private String resolve(String tool) {
            if (javaHome != null && !javaHome.isBlank()) {
                Path bin = Path.of(javaHome, "bin");
                for (String candidate : new String[] {tool, tool + ".exe"}) {
                    Path p = bin.resolve(candidate);
                    if (Files.isRegularFile(p)) {
                        return p.toString();
                    }
                }
            }
            return tool; // fall back to PATH lookup
        }
    }

    /** Container mode: one short-lived container per compile/run step. */
    final class Docker implements Sandbox {
        private final String image;
        private final int memoryMb;

        public Docker(String image, int memoryMb) {
            this.image = image;
            this.memoryMb = memoryMb;
        }

        @Override
        public String name() {
            return "docker";
        }

        @Override
        public List<String> command(String tool, List<String> args, Path workDir, boolean writable, long timeoutMs) {
            long seconds = Math.max(1, (timeoutMs + 999) / 1000);
            List<String> cmd = new ArrayList<>(List.of(
                    "docker", "run", "--rm", "-i",
                    "--network", "none",
                    "--memory", (memoryMb * 2) + "m",       // heap (-Xmx) + JVM overhead
                    "--memory-swap", (memoryMb * 2) + "m",
                    "--cpus", "1",
                    "--pids-limit", "128",
                    "--read-only",
                    "--tmpfs", "/tmp:rw,size=64m",
                    "--cap-drop", "ALL",
                    "--security-opt", "no-new-privileges",
                    "--user", "65534:65534",               // nobody
                    "-v", workDir.toAbsolutePath() + ":/code" + (writable ? "" : ":ro"),
                    "-w", "/code",
                    image,
                    "timeout", "--kill-after=1", String.valueOf(seconds),
                    tool));
            cmd.addAll(args);
            return cmd;
        }

        @Override
        public Integer timeoutExitCode() {
            return 124; // GNU coreutils `timeout`
        }

        @Override
        public Integer memoryKilledExitCode() {
            return 137; // SIGKILL from the cgroup OOM killer
        }
    }
}
