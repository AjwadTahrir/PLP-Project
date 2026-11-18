package mplab.javarunner;

/**
 * Limits and settings for the Java module. Defaults can be overridden with environment variables so the
 * same build runs on a laptop and on the faculty VM.
 *
 * <table>
 *   <tr><td>MPLAB_JAVA_TIME_LIMIT_MS</td><td>run-time limit (default 5000)</td></tr>
 *   <tr><td>MPLAB_JAVA_COMPILE_LIMIT_MS</td><td>javac time limit (default 20000)</td></tr>
 *   <tr><td>MPLAB_JAVA_MEMORY_MB</td><td>max JVM heap for the program (default 256)</td></tr>
 *   <tr><td>MPLAB_MAX_OUTPUT_BYTES</td><td>stdout/stderr cap each (default 65536)</td></tr>
 *   <tr><td>MPLAB_MAX_CONCURRENT</td><td>parallel executions (default 4)</td></tr>
 *   <tr><td>MPLAB_SANDBOX</td><td>{@code local} (dev) or {@code docker}</td></tr>
 *   <tr><td>MPLAB_DOCKER_IMAGE</td><td>image for docker sandbox (default eclipse-temurin:21-jdk)</td></tr>
 *   <tr><td>MPLAB_JAVA_HOME</td><td>JDK used in local mode (default: JAVA_HOME, then PATH)</td></tr>
 * </table>
 */
public final class ExecutorConfig {

    public long runTimeLimitMs = 5_000;
    public long compileTimeLimitMs = 20_000;
    public int memoryMb = 256;
    public int maxOutputBytes = 64 * 1024;
    public int maxSourceBytes = 64 * 1024;
    public int maxStdinBytes = 64 * 1024;
    public int maxConcurrent = 4;
    public String sandbox = "local";
    public String dockerImage = "eclipse-temurin:21-jdk";
    public String javaHome;

    public static ExecutorConfig fromEnvironment() {
        ExecutorConfig c = new ExecutorConfig();
        c.runTimeLimitMs = envLong("MPLAB_JAVA_TIME_LIMIT_MS", c.runTimeLimitMs);
        c.compileTimeLimitMs = envLong("MPLAB_JAVA_COMPILE_LIMIT_MS", c.compileTimeLimitMs);
        c.memoryMb = (int) envLong("MPLAB_JAVA_MEMORY_MB", c.memoryMb);
        c.maxOutputBytes = (int) envLong("MPLAB_MAX_OUTPUT_BYTES", c.maxOutputBytes);
        c.maxConcurrent = (int) envLong("MPLAB_MAX_CONCURRENT", c.maxConcurrent);
        c.sandbox = env("MPLAB_SANDBOX", c.sandbox);
        c.dockerImage = env("MPLAB_DOCKER_IMAGE", c.dockerImage);
        c.javaHome = env("MPLAB_JAVA_HOME", System.getenv("JAVA_HOME"));
        return c;
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v.trim();
    }

    private static long envLong(String key, long def) {
        try {
            String v = System.getenv(key);
            return v == null || v.isBlank() ? def : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
