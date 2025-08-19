package emulator.logic.instruction;

public enum InstructionData {

    INCREASE("INCREASE", 1),
    DECREASE("DECREASE", 1),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 3),
    NEUTRAL("NEUTRAL", 0),
    ZERO_VARIABLE("ZERO_VAR", 1),
    GOTO_LABEL("GOTO_LABEL", 1),
    ASSIGNMENT("ASSIGNMENT", 4),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2),
    JUMP_ZERO("JUMP_ZERO", 2),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2),

    ;

    private final String name;
    private final int cycles;

    InstructionData(String name, int cycles) {
        this.name = name;
        this.cycles = cycles;
    }

    public String getName() {
        return name;
    }

    public int getCycles() {
        return cycles;
    }
}
