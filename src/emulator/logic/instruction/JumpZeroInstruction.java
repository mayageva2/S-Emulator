package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JumpZeroInstruction extends AbstractInstruction implements Expandable {

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
    public Label execute(ExecutionContext context) {
        long v = context.getVariableValue(getVariable());
        return (v == 0L) ? jzLabel : FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {

        List<Instruction> out = new ArrayList<>();
        Variable var =  getVariable();
        Variable y = new VariableImpl(VariableType.RESULT, 0, "y");

        Label firstLabel = getLabel();
        if (firstLabel == null || FixedLabel.EMPTY.equals(firstLabel)) {
            firstLabel = helper.freshLabel();
        }

        Label L1 = helper.freshLabel();
        Label L = jzLabel;

        JumpNotZeroInstruction jnzToL1 = new JumpNotZeroInstruction(var, L1, firstLabel);
        jnzToL1.setCreatedFrom(this);
        out.add(jnzToL1);

        for (Instruction zi : new GoToLabelInstruction(var, L).expand(helper)) {
            if (zi instanceof AbstractInstruction ai) {
                ai.setCreatedFrom(this);
            }
            out.add(zi);
        }

        NeutralInstruction neutral = new NeutralInstruction(y, L1);
        neutral.setCreatedFrom(this);
        out.add(neutral);

        return out;
    }

    public Label getJzLabel() { return jzLabel; }

}
