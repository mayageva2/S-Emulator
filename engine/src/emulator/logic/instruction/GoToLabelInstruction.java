package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class GoToLabelInstruction extends AbstractInstruction implements Expandable {

    private final Label gtlLabel;
    public GoToLabelInstruction(Label gtlLabel) {
        super(InstructionData.GOTO_LABEL);
        this.gtlLabel = gtlLabel;
        setArgument("gotoLabel", gtlLabel.getLabelRepresentation());
    }

    public GoToLabelInstruction(Label label, Label gtlLabel) {
        super(InstructionData.GOTO_LABEL, label);
        this.gtlLabel = gtlLabel;
        setArgument("gotoLabel", gtlLabel.getLabelRepresentation());
    }

    @Override
    public Label execute(ExecutionContext context) {
        return gtlLabel;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        List<Instruction> out = new ArrayList<>();
        if (gtlLabel == null || gtlLabel.getLabelRepresentation().trim().isEmpty()) {
            throw new IllegalStateException("GOTO_LABEL missing target label");
        }

        Variable var = helper.freshVar();
        out.add(new IncreaseInstruction(var));
        out.add(new JumpNotZeroInstruction(var, gtlLabel));

        return out;
    }

    public Label getgtlLabel() { return gtlLabel; }


}
