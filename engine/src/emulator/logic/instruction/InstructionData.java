package emulator.logic.instruction;

public enum InstructionData {

    INCREASE("INCREASE", 1, true),
    DECREASE("DECREASE", 1, true),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2, true),
    NEUTRAL("NEUTRAL", 0, true),
    ZERO_VARIABLE("ZERO_VARIABLE", 1, false),
    GOTO_LABEL("GOTO_LABEL", 1, false),
    ASSIGNMENT("ASSIGNMENT", 4, false),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2, false),
    JUMP_ZERO("JUMP_ZERO", 2, false),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2, false),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2, false),
    QUOTATION("QUOTE", 5, false),

    ;

    private final String name;
    private final int cycles;
    private final boolean basic;

    InstructionData(String name, int cycles, boolean basic) {
        this.name = name;
        this.cycles = cycles;
        this.basic = basic;
    }

    public String getName() {
        return name;
    }
    public int getCycles() { return cycles; }
    public boolean isBasic() { return basic; }
}
