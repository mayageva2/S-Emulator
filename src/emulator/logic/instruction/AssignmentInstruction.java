package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.Objects;

public class AssignmentInstruction extends AbstractInstruction {

    private final Variable assignedVariable;

    public AssignmentInstruction(Variable target, Variable assignedVariable) {
        super(InstructionData.ASSIGNMENT, Objects.requireNonNull(target, "target"));
        this.assignedVariable = Objects.requireNonNull(assignedVariable, "assignedVariable");
    }

    public AssignmentInstruction(Variable target, Variable assignedVariable, Label label) {
        super(InstructionData.ASSIGNMENT, Objects.requireNonNull(target, "target"),
                Objects.requireNonNull(label, "label"));
        this.assignedVariable = Objects.requireNonNull(assignedVariable, "assignedVariable");
    }

    @Override
    public Label execute(ExecutionContext context) {
        long value = context.getVariableValue(assignedVariable);
        context.updateVariable(getVariable(), value);
        return FixedLabel.EMPTY;
    }
}
