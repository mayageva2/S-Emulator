package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractInstruction implements Instruction {

    private final InstructionData instructionData;
    private final Label label;
    private final Variable variable;
    private int degree;
    private Instruction createdFrom;

    public AbstractInstruction(InstructionData instructionData, Variable variable) {
        this(instructionData, variable, FixedLabel.EMPTY);
        this.degree = 0;
    }

    public AbstractInstruction(InstructionData instructionData, Variable variable, Label label) {
        this.instructionData = java.util.Objects.requireNonNull(instructionData, "instructionData");
        this.label = (label == null) ? FixedLabel.EMPTY : label;
        this.variable = variable;
        this.degree = 0;
    }

    @Override
    public String getName() {
        return instructionData.getName();
    }

    @Override
    public int cycles() {
        return instructionData.getCycles();
    }

    @Override
    public Label getLabel() {
        return label;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }

    @Override
    public Collection<Variable> referencedVariables() {
        return getVariable() == null ? Collections.emptyList() : List.of(getVariable());
    }

    public int getDegree() { return degree; }
    public void setDegree(int degree) { this.degree = degree; }

    public abstract Label execute(ExecutionContext context);

    public Instruction getCreatedFrom() { return createdFrom; }
    public void setCreatedFrom(Instruction origin) { this.createdFrom = origin; }
}
