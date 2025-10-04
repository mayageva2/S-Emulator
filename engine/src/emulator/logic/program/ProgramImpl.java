package emulator.logic.program;

import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.quote.QuotationRegistry;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.io.Serializable;
import java.util.*;

public class ProgramImpl implements Program, Serializable {

    private final String name;
    private final List<Instruction> instructions;
    private final Set<Variable> variables;
    private final Map<String, Integer> labelToIndex;
    private static final long serialVersionUID = 1L;

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

    @Override
    public List<String> getInputVariableNames() {
        Set<Integer> nums = new TreeSet<>();
        for (var instr : getInstructions()) {
            if (instr.getVariable() != null) {
                String name = instr.getVariable().getRepresentation().toLowerCase(Locale.ROOT);
                if (name.startsWith("x")) {
                    try {
                        int n = Integer.parseInt(name.substring(1));
                        nums.add(n);
                    } catch (NumberFormatException ignore) {}
                }
            }
            if (instr.getArguments() != null) {
                for (String v : instr.getArguments().values()) {
                    if (v != null && v.toLowerCase(Locale.ROOT).matches("x\\d+")) {
                        try {
                            int n = Integer.parseInt(v.substring(1));
                            nums.add(n);
                        } catch (NumberFormatException ignore) {}
                    }
                }
            }
        }

        List<String> out = new ArrayList<>();
        for (int n : nums) out.add("x" + n);
        return out;
    }


    public int calculateCyclesAtDegree(int degree, QuotationRegistry registry) {
        return new ProgramCost(registry).cyclesAtDegree(this, degree);
    }
    public int calculateCyclesFullyExpanded(QuotationRegistry registry) {
        return new ProgramCost(registry).cyclesFullyExpanded(this);
    }
}
