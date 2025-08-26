package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.List;

public class GoToLabelInstruction extends AbstractInstruction implements Expandable {

    private final Label gtlLabel;
    public GoToLabelInstruction(Label gtlLabel) {
        super(InstructionData.GOTO_LABEL);
        this.gtlLabel = gtlLabel;
    }

    public GoToLabelInstruction(Label label, Label gtlLabel) {
        super(InstructionData.GOTO_LABEL, label);
        this.gtlLabel = gtlLabel;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return gtlLabel;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        if (gtlLabel == null || gtlLabel.getLabelRepresentation().trim().isEmpty()) {
            throw new IllegalStateException("GOTO_LABEL missing target label");
        }

        Variable var = helper.freshVar();

        IncreaseInstruction inc = new IncreaseInstruction(var, this.getLabel());
        inc.setCreatedFrom(this);

        JumpNotZeroInstruction jnz = new JumpNotZeroInstruction(var, gtlLabel);
        jnz.setCreatedFrom(this);

        return List.of(inc, jnz);
    }

    public Label getgtlLabel() { return gtlLabel; }


}
