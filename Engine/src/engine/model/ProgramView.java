package engine.model;

import java.util.List;

public class ProgramView {
    private final String name;
    private final List<String> inputs;
    private final List<String> labels;
    private final List<String> formattedInstructions;

    public ProgramView(String name, List<String> inputs, List<String> labels, List<String> formattedInstructions) {
        this.name = name;
        this.inputs = List.copyOf(inputs);
        this.labels = List.copyOf(labels);
        this.formattedInstructions = List.copyOf(formattedInstructions);
    }

    public String getName() {
        return name;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<String> getFormattedInstructions() {
        return formattedInstructions;
    }
}
