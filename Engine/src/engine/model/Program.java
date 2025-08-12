package engine.model;

import java.util.List;

public class Program {
    private final String name;
    private final List<String> inputs;
    private final List<String> labels;
    private final List<Instruction> instructions;

    public Program(String name, List<String> inputs, List<String> labels, List<Instruction> instructions) {
        this.name = name;
        this.inputs = List.copyOf(inputs);
        this.labels = List.copyOf(labels);
        this.instructions = List.copyOf(instructions);
    }

    public String getName() { return name; }
    public List<String> getInputs() { return inputs; }
    public List<String> getLabels() { return labels; }
    public List<Instruction> getInstructions() { return instructions; }
}
