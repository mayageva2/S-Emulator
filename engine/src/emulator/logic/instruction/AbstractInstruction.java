package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.label.LabelImpl;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

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

    @Override
    public InstructionData getInstructionData() { return instructionData; }

    @Override
    public String getName() {
        return instructionData.getName();
    }

    @Override
    public int cycles() { return instructionData.getCycles(); }

    @Override
    public Label getLabel() { return label; }

    @Override
    public Variable getVariable() {
        return variable;
    }

    @Override
    public Collection<Variable> referencedVariables() {
        return getVariable() == null ? Collections.emptyList() : List.of(getVariable());
    }

    @Override
    public Map<String, String> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }

    public void setArgument(String key, String value) {
        arguments.put(key, value);
    }

    @Override
    public int degree() {
        return getInstructionData().getDegree();
    }

    protected ExpansionHelper degreeHelper() {
        return ExpansionHelper.fromUsedSets(
                Set.of(), Set.of(),
                name -> new VariableImpl(VariableType.WORK, 0),
                name -> new LabelImpl(0)
        );
    }

    @Override
    public Instruction getCreatedFrom() { return createdFrom; }

    public int getDegree() { return degree; }
    public void setDegree(int degree) { this.degree = degree; }

    public abstract Label execute(ExecutionContext context);
    public void setCreatedFrom(Instruction origin) { this.createdFrom = origin; }
}
