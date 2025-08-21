package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.List;
import java.util.Objects;

public class JumpEqualConstantInstruction extends AbstractInstruction implements Expandable {

    private final Label jeConstantLabel;
    private final long constantValue;

    public JumpEqualConstantInstruction(Variable variable, long constantValue, Label jeConstantLabel) {
        super(InstructionData.JUMP_EQUAL_CONSTANT, Objects.requireNonNull(variable, "variable"));
        this.jeConstantLabel = Objects.requireNonNull(jeConstantLabel, "jeConstantLabel");
        if (constantValue < 0) {
            throw new IllegalArgumentException("constantValue (K) must be non-negative");
        }
        this.constantValue = constantValue;
    }

    public JumpEqualConstantInstruction(Variable variable, long constantValue, Label jeConstantLabel, Label myLabel) {
        super(InstructionData.JUMP_EQUAL_CONSTANT,
                Objects.requireNonNull(variable, "variable"),
                Objects.requireNonNull(myLabel, "label"));
        this.jeConstantLabel = Objects.requireNonNull(jeConstantLabel, "jeConstantLabel");
        if (constantValue < 0) {
            throw new IllegalArgumentException("constantValue (K) must be non-negative");
        }
        this.constantValue = constantValue;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long v = context.getVariableValue(getVariable());
        return (v == constantValue) ? jeConstantLabel : FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {

    }

    public Label getJeConstantLabel() { return jeConstantLabel; }
    public long getConstantValue() { return constantValue; }
}
