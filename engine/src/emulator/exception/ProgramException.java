package emulator.exception;

import java.util.Map;

public class ProgramException extends RuntimeException {
    private final String code;                       //error code
    private final Map<String, Object> context;       //additional context information about the error

    public ProgramException(String code, String message) {
        super(message);
        this.code = code;
        this.context = Map.of();
    }

    public ProgramException(String code, String message, Map<String, Object> context) {
        super(message);
        this.code = code;
        this.context = Map.copyOf(context);
    }

    //GETTERS
    public String getCode() { return code; }
    public Map<String, Object> getContext() { return context; }
}
