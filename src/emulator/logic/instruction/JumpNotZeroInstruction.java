package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

public class JumpNotZeroInstruction extends AbstractInstruction{

    private final Label jnzLabel;   //Target label

    public JumpNotZeroInstruction(Variable variable, Label jnzLabel) {
        this(variable, jnzLabel, FixedLabel.EMPTY);
    }

    public JumpNotZeroInstruction(Variable variable, Label jnzLabel, Label label) {
        super(InstructionData.JUMP_NOT_ZERO, variable, label);
        this.jnzLabel = jnzLabel;
    }

    @Override
    public int degree() { return 0; }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(getVariable());

        if (variableValue != 0) {
            return jnzLabel;
        }
        return FixedLabel.EMPTY;
    }

    public Label getJnzLabel() {return jnzLabel; }
}
