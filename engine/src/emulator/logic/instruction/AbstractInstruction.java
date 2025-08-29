package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.*;

public abstract class AbstractInstruction implements Instruction {

    private final InstructionData instructionData;
    private final Label label;
    private final Variable variable;
    private final Map<String, String> arguments = new HashMap<>();
    private int degree;
    private Instruction createdFrom;

    public AbstractInstruction(InstructionData instructionData) {
        this(instructionData,null, FixedLabel.EMPTY);
        this.degree = 0;
    }

    public AbstractInstruction(InstructionData instructionData, Variable variable) {
        this(instructionData, variable, FixedLabel.EMPTY);
        this.degree = 0;
    }

    public AbstractInstruction(InstructionData instructionData, Label label) {
        this.instructionData = Objects.requireNonNull(instructionData, "instructionData");
        this.variable = null;
        this.label = (label == null) ? FixedLabel.EMPTY : label;
        this.degree = 0;
    }

    public AbstractInstruction(InstructionData instructionData, Variable variable, Label label) {
        this.instructionData = Objects.requireNonNull(instructionData, "instructionData");
        this.label = (label == null) ? FixedLabel.EMPTY : label;
        this.variable = variable;
        this.degree = 0;
    }

    //This func returns instructionData
    @Override
    public InstructionData getInstructionData() { return instructionData; }

    //This func returns name of instruction
    @Override
    public String getName() { return instructionData.getName(); }

    //This func
    @Override
    public int cycles() { return instructionData.getCycles(); }

    //This func returns cycles amount
    @Override
    public Label getLabel() { return label; }

    //This func returns the main variable
    @Override
    public Variable getVariable() { return variable; }

    //This func returns the variables referenced in instruction
    @Override
    public Collection<Variable> referencedVariables() { return getVariable() == null ? Collections.emptyList() : List.of(getVariable()); }

    //This func returns the instruction’s arguments
    @Override
    public Map<String, String> getArguments() { return Collections.unmodifiableMap(arguments); }

    //This func
    @Override
    public int degree() {
        return getInstructionData().getDegree();
    }

    //This func returns the original instruction
    @Override
    public Instruction getCreatedFrom() { return createdFrom; }

    //This func updates an argument
    public void setArgument(String key, String value) {
        arguments.put(key, value);
    }

    //This func executes the instruction logic
    public abstract Label execute(ExecutionContext context);

    //This func sets which instruction created the given instruction
    public void setCreatedFrom(Instruction origin) { this.createdFrom = origin; }
}
