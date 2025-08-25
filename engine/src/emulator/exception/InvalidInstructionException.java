package emulator.exception;

import java.util.Map;

public class InvalidInstructionException extends ProgramException {

    public InvalidInstructionException(String opcode, String reason, int index) {
        super("BAD_OPCODE", "Invalid instruction: " + opcode + " (" + reason + ")", Map.of("opcode", opcode, "index", index));
    }

    public InvalidInstructionException(String name, String reason) {
        super("BAD_INSTRUCTION", "Invalid instruction: " + name + " (" + reason + ")",
                Map.of("instruction", name));
    }
}
