package InstructionsTable;

public record InstructionRow(
        int index,
        String type, // "B" or "S"
        String instruction,
        int cycles
) {}
