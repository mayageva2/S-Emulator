package emulator.logic.program;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.*;

import static emulator.logic.label.FixedLabel.EMPTY;

public class ProgramImpl implements Program {

    private final String name;
    private final List<Instruction> instructions;
    private final Set<Variable> variables;
    private final Map<String, Integer> labelToIndex;

    public ProgramImpl(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.variables = new LinkedHashSet<>();
        this. labelToIndex = new HashMap<>();
    }

    //This func returns program name
    @Override
    public String getName() { return name; }

    //This func adds an instruction to program
    @Override
    public void addInstruction(Instruction instruction) {
        Objects.requireNonNull(instruction, "instruction must not be null");
        int idx = instructions.size();
        instructions.add(instruction);

        // Updates all variables this instruction uses
        for (Variable var : instruction.referencedVariables()) {
            if (var != null) variables.add(var);
        }

        // Creates an Index label
        Label lbl = instruction.getLabel();
        if (lbl != null && lbl != FixedLabel.EMPTY) {
            String key = lbl.getLabelRepresentation();
            Integer prev = labelToIndex.putIfAbsent(key, idx);
            if (prev != null) {
                throw new IllegalStateException(
                        "Duplicate label '" + key + "' at indices " + prev + " and " + idx
                );
            }
        }
    }

    //This func returns all instructions in program
    @Override
    public List<Instruction> getInstructions() { return Collections.unmodifiableList(instructions); }

    //This func returns all variables in program
    @Override
    public Set<Variable> getVariables() {
        return  Collections.unmodifiableSet(variables);
    }

    //This func returns the instructions at specific label
    @Override
    public Instruction instructionAt(Label label) {
        String key = label.getLabelRepresentation();
        Integer idx = labelToIndex.get(key);
        if (idx == null) {
            throw new IllegalArgumentException("Unknown label: " + key);
        }
        return instructions.get(idx);
    }

    //This func returns program's max degree
    @Override
    public int calculateMaxDegree() {
        int max = 0;
        for (Instruction ins : instructions) {
            if (ins != null) {
                max = Math.max(max, ins.degree());
            }
        }
        return max;
    }
}
