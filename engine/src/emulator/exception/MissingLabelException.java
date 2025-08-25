package emulator.exception;

import java.util.Map;

public class MissingLabelException extends ProgramException {
    public MissingLabelException(String label) {
        super("MISSING_LABEL", "Label is referenced but not defined: " + label,
                Map.of("label", label));
    }
}