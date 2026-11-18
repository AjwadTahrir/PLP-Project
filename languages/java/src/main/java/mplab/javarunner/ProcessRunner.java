package mplab.javarunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs one external process (javac, java, or a sandbox wrapper) with stdin, a wall-clock timeout and
 * bounded output capture. Submitted code is never run inside the web server's own JVM.
 */
public final class ProcessRunner {

    /** Outcome of a process run. {@code exitCode} is null when the process was killed by the timeout. */
    public static final class Outcome {
        public final Integer exitCode;
        public final String stdout;
        public final String stderr;
        public final boolean timedOut;
        public final boolean truncated;
        public final long elapsedMs;

        Outcome(Integer exitCode, String stdout, String stderr, boolean timedOut, boolean truncated, long elapsedMs) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.timedOut = timedOut;
            this.truncated = truncated;
            this.elapsedMs = elapsedMs;
        }
    }

    private ProcessRunner() {
    }

    public static Outcome run(List<String> command, Path workDir, String stdin, long timeoutMs, int maxOutputBytes)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command).directory(workDir.toFile());
        Map<String, String> env = pb.environment();
        // These variables make the JVM print "Picked up ..." banners that would pollute stderr.
        env.remove("JAVA_TOOL_OPTIONS");
        env.remove("_JAVA_OPTIONS");
        env.remove("JDK_JAVA_OPTIONS");
        env.remove("CLASSPATH");

        long start = System.nanoTime();
        Process p = pb.start();

        Collector out = new Collector(p.getInputStream(), maxOutputBytes);
        Collector err = new Collector(p.getErrorStream(), maxOutputBytes);
        out.start();
        err.start();

        Thread feeder = new Thread(() -> {
            try (OutputStream os = p.getOutputStream()) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // program exited without reading all input - not an error
            }
        }, "mplab-stdin");
        feeder.setDaemon(true);
        feeder.start();

        boolean finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
            p.waitFor(2, TimeUnit.SECONDS);
        }
        out.join(2000);
        err.join(2000);
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        return new Outcome(finished ? p.exitValue() : null, out.text(), err.text(), !finished,
                out.truncated || err.truncated, elapsed);
    }

    /** Drains a stream on its own thread, keeping at most {@code limit} bytes. */
    private static final class Collector extends Thread {
        private final InputStream in;
        private final int limit;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private volatile boolean truncated;

        Collector(InputStream in, int limit) {
            super("mplab-output");
            setDaemon(true);
            this.in = in;
            this.limit = limit;
        }

        @Override
        public void run() {
            byte[] chunk = new byte[8192];
            try (InputStream s = in) {
                int n;
                while ((n = s.read(chunk)) != -1) {
                    int room = limit - buf.size();
                    if (room > 0) {
                        buf.write(chunk, 0, Math.min(room, n));
                    }
                    if (n > room) {
                        truncated = true; // keep draining so the child never blocks on a full pipe
                    }
                }
            } catch (IOException ignored) {
                // stream closed when the process was killed
            }
        }

        synchronized String text() {
            return buf.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
