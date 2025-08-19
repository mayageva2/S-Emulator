package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.Objects;

public class ConstantAssignmentInstruction extends AbstractInstruction {
    private final long constantValue;

    public ConstantAssignmentInstruction(Variable target, long constantValue) {
        super(InstructionData.CONSTANT_ASSIGNMENT, Objects.requireNonNull(target, "target"));
        if (constantValue < 0) {
            throw new IllegalArgumentException("constantValue (K) must be non-negative");
        }
        this.constantValue = constantValue;
    }

    public ConstantAssignmentInstruction(Variable target, long constantValue, Label label) {
        super(InstructionData.CONSTANT_ASSIGNMENT,
                Objects.requireNonNull(target, "target"),
                Objects.requireNonNull(label, "label"));
        if (constantValue < 0) {
            throw new IllegalArgumentException("constantValue (K) must be non-negative");
        }
        this.constantValue = constantValue;
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), constantValue);
        return FixedLabel.EMPTY;
    }

    public long getConstantValue() { return constantValue; }
}


