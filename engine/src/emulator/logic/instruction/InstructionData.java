package emulator.logic.instruction;

public enum InstructionData {

    INCREASE("INCREASE", 1, true, 0, 5),
    DECREASE("DECREASE", 1, true, 0, 5),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2, true, 0, 5),
    NEUTRAL("NEUTRAL", 0, true, 0, 5),
    ZERO_VARIABLE("ZERO_VARIABLE", 1, false, 1, 100),
    GOTO_LABEL("GOTO_LABEL", 1, false, 1, 100),
    ASSIGNMENT("ASSIGNMENT", 4, false, 2, 500),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2, false, 2, 100),
    JUMP_ZERO("JUMP_ZERO", 2, false, 2, 500),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2, false, 3, 500),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2, false, 3, 500),
    QUOTATION("QUOTE", 5, false, 0, 1000),                               //max degree changes dynamically
    JUMP_EQUAL_FUNCTION("JUMP_EQUAL_FUNCTION", 6, false, 0, 1000),       //max degree and cycle changes dynamically
    ;

    private final String name;
    private final int cycles;
    private final boolean basic;
    private final int degree;
    private final int baseCreditCost;

    InstructionData(String name, int cycles, boolean basic, int degree, int baseCreditCost) {
        this.name = name;
        this.cycles = cycles;
        this.basic = basic;
        this.degree = degree;
        this.baseCreditCost = baseCreditCost;
    }

    public String getName() { return name; }
    public int getCycles() { return cycles; }
    public boolean isBasic() { return basic; }
    public int getDegree() { return degree; }
    public int getBaseCreditCost() { return baseCreditCost; }
}
