package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.Objects;

public class JumpZeroInstruction extends AbstractInstruction {

    private final Label jzLabel;

    public JumpZeroInstruction(Variable variable, Label jzLabel) {
        super(InstructionData.JUMP_ZERO, Objects.requireNonNull(variable, "variable"));
        this.jzLabel = Objects.requireNonNull(jzLabel, "jzLabel");
    }

    public JumpZeroInstruction(Variable variable, Label jzLabel, Label myLabel) {
        super(InstructionData.JUMP_ZERO,
                Objects.requireNonNull(variable, "variable"),
                Objects.requireNonNull(myLabel, "label"));
        this.jzLabel = Objects.requireNonNull(jzLabel, "jzLabel");
    }

    @Override
    public int degree() { return 0; }

    @Override
    public Label execute(ExecutionContext context) {
        long v = context.getVariableValue(getVariable());
        return (v == 0L) ? jzLabel : FixedLabel.EMPTY;
    }

    public Label getJzLabel() { return jzLabel; }

}
