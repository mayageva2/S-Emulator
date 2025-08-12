package engine.model;

public class Instruction {
    public enum Kind { BASIC, SYNTHETIC }

    private final int id;
    private final Kind kind;
    private final String label;
    private final String command;
    private final int cycles;

    public Instruction(int id, Kind kind, String label, String command, int cycles) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.command = command;
        this.cycles = cycles;
    }

    public int getId() { return id; }
    public Kind getKind() { return kind; }
    public String getLabel() { return label; }
    public String getCommand() { return command; }
    public int getCycles() { return cycles; }
}
