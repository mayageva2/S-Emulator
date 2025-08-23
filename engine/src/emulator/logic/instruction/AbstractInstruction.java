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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractInstruction implements Instruction {

    private final InstructionData instructionData;
    private final Label label;
    private final Variable variable;
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

    @Override
    public int degree() {

        if (!(this instanceof Expandable expandable)) {
            return 0;
        }

        List<Instruction> children = expandable.expand(degreeHelper());
        if (children == null || children.isEmpty()) {
            return 0;
        }
        return 1 + children.stream()
                .mapToInt(Instruction::degree)
                .max()
                .orElse(0);
    }

    protected ExpansionHelper degreeHelper() {
        return ExpansionHelper.fromUsedSets(
                Set.of(), Set.of(),
                name -> new VariableImpl(VariableType.WORK, 0),
                name -> new LabelImpl(0)
        );
    }

    public int getDegree() { return degree; }
    public void setDegree(int degree) { this.degree = degree; }

    public abstract Label execute(ExecutionContext context);

    public Instruction getCreatedFrom() { return createdFrom; }
    public void setCreatedFrom(Instruction origin) { this.createdFrom = origin; }
}
