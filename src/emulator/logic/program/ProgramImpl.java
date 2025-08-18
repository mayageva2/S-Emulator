package emulator.logic.program;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.*;

import static emulator.logic.label.FixedLabel.EMPTY;

public class ProgramImpl implements Program {

    private final String name;
    private final List<Instruction> instructions;
    private final Set<Variable> variables;
    private final Map<Label, Integer> labelToIndex;

    public ProgramImpl(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.variables = new LinkedHashSet<>();
        this. labelToIndex = new HashMap<>();
    }

    @Override
    public String getName() { return name; }

    @Override
    public void addInstruction(Instruction instruction) {
        Objects.requireNonNull(instruction, "instruction must not be null");
        int idx = instructions.size();
        instructions.add(instruction);

        Variable v = instruction.getVariable();
        if (v != null) variables.add(v);

        // Creates an Index label
        Label lbl = instruction.getLabel();
        if (lbl != EMPTY) {
            Integer prev = labelToIndex.put(lbl, idx);
            if (prev != null) { throw new IllegalStateException( "Duplicate label '" + lbl + "' at indices " + prev + " and " + idx); }
        }
    }

    @Override
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    @Override
    public Set<Variable> getVariables() {
        return  Collections.unmodifiableSet(variables);
    }

    @Override
    public Instruction instructionAt(Label label) {
        Integer idx = labelToIndex.get(label);
        if (idx == null) {
            throw new IllegalArgumentException("Unknown label: " + label);
        }
        return instructions.get(idx);
    }

    @Override
    public boolean validate() {
        return false;
    }

    @Override
    public int calculateMaxDegree() {
        // traverse all commands and find maximum degree
        return 0;
    }

    @Override
    public int calculateCycles() {
        // traverse all commands and calculate cycles
        return 0;
    }
}
