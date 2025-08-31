package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

public class JumpNotZeroInstruction extends AbstractInstruction{

    private final Label jnzLabel;   //Target label

    public JumpNotZeroInstruction(Variable variable, Label jnzLabel) {
        this(variable, jnzLabel, FixedLabel.EMPTY);
        setArgument("gotoLabel", jnzLabel.getLabelRepresentation());
    }

    public JumpNotZeroInstruction(Variable variable, Label jnzLabel, Label label) {
        super(InstructionData.JUMP_NOT_ZERO, variable, label);
        this.jnzLabel = jnzLabel;
        setArgument("gotoLabel", jnzLabel.getLabelRepresentation());
    }

    //This func executes the instruction
    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(getVariable());

        if (variableValue != 0) {
            return jnzLabel;
        }
        return FixedLabel.EMPTY;
    }

    //This func returns target label
    public Label getJnzLabel() {return jnzLabel; }
}
