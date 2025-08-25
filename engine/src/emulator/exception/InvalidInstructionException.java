package emulator.exception;

import java.util.Map;

public class InvalidInstructionException extends ProgramException {
    public InvalidInstructionException(String name, String reason) {
        super("BAD_INSTRUCTION", "Invalid instruction: " + name + " (" + reason + ")",
                Map.of("instruction", name));
    }
}
