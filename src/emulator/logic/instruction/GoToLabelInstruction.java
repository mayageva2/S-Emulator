package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.label.LabelImpl;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

import java.util.ArrayList;
import java.util.List;

public class GoToLabelInstruction extends AbstractInstruction implements Expandable {

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

    @Override
    public int degree() {
        ExpansionHelper helper = ExpansionHelper.fromUsedSets(
                java.util.Set.of(), java.util.Set.of(),
                name -> new VariableImpl(VariableType.WORK, 0, name),
                name -> new LabelImpl(0)
        );

        return 1 + expand(helper).stream()
                .mapToInt(Instruction::degree)
                .max()
                .orElse(0);
    }

    public Label getgtlLabel() { return gtlLabel; }


}
