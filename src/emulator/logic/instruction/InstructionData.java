package emulator.logic.instruction;

public enum InstructionData {

    INCREASE("INCREASE", 1),
    DECREASE("DECREASE", 1),
    JUMP_NOT_ZERO("JNZ", 3),
    NEUTRAL("NEUTRAL", 0),
    ZERO_VARIABLE("ZERO_VAR", 1),
    GOTO_LABEL("GOTO_LABEL", 1),

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
