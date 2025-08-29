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

public class JumpEqualConstantInstruction extends AbstractInstruction implements Expandable {

    private final Label jeConstantLabel;
    private final long constantValue;

    public JumpEqualConstantInstruction(Variable variable, long constantValue, Label jeConstantLabel) {
        super(InstructionData.JUMP_EQUAL_CONSTANT, Objects.requireNonNull(variable, "variable"));
        if (constantValue < 0) throw new IllegalArgumentException("constantValue (K) must be non-negative");
        this.constantValue = constantValue;
        this.jeConstantLabel = Objects.requireNonNull(jeConstantLabel, "jeConstantLabel");
        setArgument("gotoLabel", jeConstantLabel.getLabelRepresentation());
        setArgument("constantValue", String.valueOf(constantValue));
    }

    public JumpEqualConstantInstruction(Variable variable, long constantValue, Label jeConstantLabel, Label myLabel) {
        super(InstructionData.JUMP_EQUAL_CONSTANT,
                Objects.requireNonNull(variable, "variable"),
                Objects.requireNonNull(myLabel, "label"));
        if (constantValue < 0) throw new IllegalArgumentException("constantValue (K) must be non-negative");
        this.constantValue = constantValue;
        this.jeConstantLabel = Objects.requireNonNull(jeConstantLabel, "jeConstantLabel");
        setArgument("gotoLabel", jeConstantLabel.getLabelRepresentation());
        setArgument("constantValue", String.valueOf(constantValue));
    }

    @Override
    public Label execute(ExecutionContext context) {
        long v = context.getVariableValue(getVariable());
        return (v == constantValue) ? jeConstantLabel : FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {

        Variable var = getVariable();
        if (var == null) {
            throw new IllegalStateException("JUMP_EQUAL_VARIABLE missing variable");
        }

        Variable z1 = helper.freshVar();
        List<Instruction> out = new ArrayList<>();
        Label L1 = helper.freshLabel();
        out.add(new AssignmentInstruction(z1, var));

        for(long i = 0; i < constantValue; i++) {
            out.add(new JumpZeroInstruction(z1, L1));
            out.add(new DecreaseInstruction(z1));
        }

        out.add(new JumpNotZeroInstruction(z1, L1));
        out.add(new GoToLabelInstruction(jeConstantLabel));
        out.add(new NeutralInstruction(var, L1));
        return out;
    }

    public Label getJeConstantLabel() { return jeConstantLabel; }
    public long getConstantValue() { return constantValue; }
}
