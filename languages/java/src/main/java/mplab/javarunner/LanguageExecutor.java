package mplab.javarunner;

/**
 * Contract every MPLab language module implements. The execution controller (Group 2) selects an
 * executor by {@link #language()} and delegates the request to it.
 */
public interface LanguageExecutor {

    /** Language id used in the API, e.g. {@code "java"}. */
    String language();

    /** Toolchain version string, e.g. {@code "javac 21.0.4"}. */
    String version();

    /** Compiles/interprets and runs the program. Never throws; failures are reported in the result. */
    ExecutionResult execute(ExecutionRequest request);
}
