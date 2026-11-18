package mplab.javarunner;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result returned to the execution controller / web interface.
 *
 * <p>The first four fields ({@code status, stdout, stderr, exit_code}) are the minimum required by the
 * project brief; the rest are the optional extensions proposed by Group 3.
 */
public final class ExecutionResult {

    private ExecutionStatus status;
    private String stdout = "";
    private String stderr = "";
    private Integer exitCode;
    private String stage;
    private String message;
    private long executionTimeMs;
    private long compileTimeMs;
    private boolean truncated;
    private String language = "java";
    private String version;

    public static ExecutionResult of(ExecutionStatus status, String message) {
        ExecutionResult r = new ExecutionResult();
        r.status = status;
        r.message = message;
        return r;
    }

    public ExecutionStatus status() { return status; }
    public String stdout() { return stdout; }
    public String stderr() { return stderr; }
    public Integer exitCode() { return exitCode; }
    public String stage() { return stage; }
    public String message() { return message; }
    public boolean truncated() { return truncated; }
    public long executionTimeMs() { return executionTimeMs; }

    public ExecutionResult status(ExecutionStatus s) { this.status = s; return this; }
    public ExecutionResult stdout(String s) { this.stdout = s == null ? "" : s; return this; }
    public ExecutionResult stderr(String s) { this.stderr = s == null ? "" : s; return this; }
    public ExecutionResult exitCode(Integer c) { this.exitCode = c; return this; }
    public ExecutionResult stage(String s) { this.stage = s; return this; }
    public ExecutionResult message(String m) { this.message = m; return this; }
    public ExecutionResult executionTimeMs(long t) { this.executionTimeMs = t; return this; }
    public ExecutionResult compileTimeMs(long t) { this.compileTimeMs = t; return this; }
    public ExecutionResult truncated(boolean t) { this.truncated = t; return this; }
    public ExecutionResult language(String l) { this.language = l; return this; }
    public ExecutionResult version(String v) { this.version = v; return this; }

    /** JSON-ready map with snake_case keys, matching the shared API. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status.wire());
        m.put("stdout", stdout);
        m.put("stderr", stderr);
        m.put("exit_code", exitCode);
        m.put("language", language);
        m.put("version", version);
        m.put("stage", stage);
        m.put("message", message);
        m.put("compile_time_ms", compileTimeMs);
        m.put("execution_time_ms", executionTimeMs);
        m.put("truncated", truncated);
        return m;
    }

    public String toJson() {
        return Json.write(toMap());
    }
}
