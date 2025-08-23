package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

public class NeutralInstruction extends AbstractInstruction {

    public NeutralInstruction(Variable variable) {
        super(InstructionData.NEUTRAL, variable);
    }

    public NeutralInstruction(Variable variable, Label label) { super(InstructionData.NEUTRAL, variable, label); }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;
    }
}
