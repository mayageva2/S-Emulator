package emulator.exception;

import java.util.Map;

public class ProgramException extends RuntimeException {
    private final String code;
    private final Map<String, Object> context;

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

    public String getCode() { return code; }
    public Map<String, Object> getContext() { return context; }
}
