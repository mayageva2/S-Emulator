package emulator.logic.instruction;

public enum InstructionData {

    INCREASE("INCREASE", 1, true, 0),
    DECREASE("DECREASE", 1, true, 0),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2, true, 0),
    NEUTRAL("NEUTRAL", 0, true, 0),

    ZERO_VARIABLE("ZERO_VARIABLE", 1, false, 1),
    GOTO_LABEL("GOTO_LABEL", 1, false, 1),
    JUMP_ZERO("JUMP_ZERO", 2, false, 1),

    ASSIGNMENT("ASSIGNMENT", 4, false, 2),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2, false, 2),

    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2, false, 3),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2, false, 3),
    QUOTATION("QUOTE", 5, false, 3),

    ;

    private final String name;
    private final int cycles;
    private final boolean basic;
    private final int degree;

    InstructionData(String name, int cycles, boolean basic, int degree) {
        this.name = name;
        this.cycles = cycles;
        this.basic = basic;
        this.degree = degree;
    }

    public String getName() {
        return name;
    }
    public int getCycles() { return cycles; }
    public boolean isBasic() { return basic; }
    public int getDegree() { return degree; }
}
