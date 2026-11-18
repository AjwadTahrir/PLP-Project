package mplab.javarunner;

/** Status values proposed for the shared MPLab execution API (see docs/week1/04-api-proposal.md). */
public enum ExecutionStatus {
    SUCCESS("success"),
    COMPILE_ERROR("compile_error"),
    RUNTIME_ERROR("runtime_error"),
    TIMEOUT("timeout"),
    MEMORY_LIMIT_EXCEEDED("memory_limit_exceeded"),
    UNSUPPORTED_LANGUAGE("unsupported_language"),
    INVALID_REQUEST("invalid_request"),
    INTERNAL_ERROR("internal_error");

    private final String wire;

    ExecutionStatus(String wire) {
        this.wire = wire;
    }

    /** The lowercase value used in JSON. */
    public String wire() {
        return wire;
    }
}
