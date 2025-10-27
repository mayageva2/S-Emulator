package emulator.exception;

public final class DebugAbortException extends RuntimeException {
    public DebugAbortException() { super("Debug aborted"); }
}
