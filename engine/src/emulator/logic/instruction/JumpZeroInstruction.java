package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JumpZeroInstruction extends AbstractInstruction implements Expandable {

    private final Label jzLabel;

    public JumpZeroInstruction(Variable variable, Label jzLabel) {
        super(InstructionData.JUMP_ZERO, Objects.requireNonNull(variable, "variable"));
        this.jzLabel = Objects.requireNonNull(jzLabel, "jzLabel");
        setArgument("gotoLabel", jzLabel.getLabelRepresentation());
    }

    public JumpZeroInstruction(Variable variable, Label jzLabel, Label myLabel) {
        super(InstructionData.JUMP_ZERO,
                Objects.requireNonNull(variable, "variable"),
                Objects.requireNonNull(myLabel, "label"));
        this.jzLabel = Objects.requireNonNull(jzLabel, "jzLabel");
        setArgument("gotoLabel", jzLabel.getLabelRepresentation());
    }

    //This func executes the instruction
    @Override
    public Label execute(ExecutionContext context) {
        long v = context.getVariableValue(getVariable());
        return (v == 0L) ? jzLabel : FixedLabel.EMPTY;
    }

    //This func expands an JUMP_ZERO instruction
    @Override
    public List<Instruction> expand(ExpansionHelper helper) {

        List<Instruction> out = new ArrayList<>();
        Variable var =  getVariable();
        if (var == null) {
            throw new IllegalStateException("JUMP_ZERO missing variable");
        }

        Label L1 = helper.freshLabel();
        Label L = getJzLabel();
        Label firstLabel = getLabel();
        if (firstLabel == null || FixedLabel.EMPTY.equals(firstLabel)) {
            out.add(new JumpNotZeroInstruction(var, L1));
        } else {
            out.add(new JumpNotZeroInstruction(var, L1, firstLabel));
        }

        out.add(new GoToLabelInstruction(L));
        out.add(new NeutralInstruction(var, L1));
        return out;
    }

    //This func returns target label
    public Label getJzLabel() { return jzLabel; }

}
