package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

public class GoToLabelInstruction extends AbstractInstruction {

    private final Label gtlLabel;
    public GoToLabelInstruction(Variable variable, Label gtlLabel) {
        super(InstructionData.GOTO_LABEL, variable);
        this.gtlLabel = gtlLabel;
    }

    public GoToLabelInstruction(Variable variable, Label label, Label gtlLabel) {
        super(InstructionData.GOTO_LABEL, variable, label);
        this.gtlLabel = gtlLabel;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return gtlLabel;
    }

    public Label getgtlLabel() { return gtlLabel; }
}
