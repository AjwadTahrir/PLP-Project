package mplab.javarunner;

import java.util.Map;

/**
 * An execution request as defined by the shared MPLab API.
 *
 * <pre>{ "language": "java", "source_code": "...", "stdin": "" }</pre>
 */
public final class ExecutionRequest {

    private final String language;
    private final String sourceCode;
    private final String stdin;

    public ExecutionRequest(String language, String sourceCode, String stdin) {
        this.language = language;
        this.sourceCode = sourceCode;
        this.stdin = stdin == null ? "" : stdin;
    }

    public String language() {
        return language;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public String stdin() {
        return stdin;
    }

    /** Builds a request from a parsed JSON object; throws IllegalArgumentException on bad shape. */
    public static ExecutionRequest fromJson(Object parsed) {
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Request body must be a JSON object");
        }
        Map<?, ?> m = (Map<?, ?>) parsed;
        return new ExecutionRequest(
                optString(m, "language"),
                optString(m, "source_code"),
                optString(m, "stdin"));
    }

    private static String optString(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            return null;
        }
        if (!(v instanceof String)) {
            throw new IllegalArgumentException("Field '" + key + "' must be a string");
        }
        return (String) v;
    }
}
