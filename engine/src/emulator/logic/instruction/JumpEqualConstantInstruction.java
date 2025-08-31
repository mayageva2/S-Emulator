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

public class JumpEqualConstantInstruction extends AbstractInstruction implements Expandable {

    private final Label jeConstantLabel;
    private final long constantValue;

    private JumpEqualConstantInstruction(Builder builder) {
        super(InstructionData.JUMP_EQUAL_CONSTANT, builder.variable, builder.myLabel);
        if (builder.constantValue < 0) {
            throw new IllegalArgumentException("constantValue (K) must be non-negative");
        }
        this.constantValue = builder.constantValue;
        this.jeConstantLabel = Objects.requireNonNull(builder.jeConstantLabel, "jeConstantLabel");

        setArgument("gotoLabel", jeConstantLabel.getLabelRepresentation());
        setArgument("constantValue", String.valueOf(constantValue));
    }

    //This func builds instruction
    public static class Builder {
        private Variable variable;
        private long constantValue;
        private Label jeConstantLabel;
        private Label myLabel;

        public Builder variable(Variable variable) {
            this.variable = variable;
            return this;
        }

        public Builder constantValue(long value) {
            this.constantValue = value;
            return this;
        }

        public Builder jeConstantLabel(Label label) {
            this.jeConstantLabel = label;
            return this;
        }

        public Builder myLabel(Label label) {
            this.myLabel = label;
            return this;
        }

        public JumpEqualConstantInstruction build() {
            return new JumpEqualConstantInstruction(this);
        }
    }

    //This func executes the instruction
    @Override
    public Label execute(ExecutionContext context) {
        long v = context.getVariableValue(getVariable());
        return (v == constantValue) ? jeConstantLabel : FixedLabel.EMPTY;
    }

    //This func expands an JUMP_EQUAL_CONSTANT instruction
    @Override
    public List<Instruction> expand(ExpansionHelper helper) {

        Variable var = getVariable();
        if (var == null) {
            throw new IllegalStateException("JUMP_EQUAL_CONSTANT missing variable");
        }

        Variable z1 = helper.freshVar();
        List<Instruction> out = new ArrayList<>();
        Label L1 = helper.freshLabel();

        Label firstLabel = getLabel();
        if (firstLabel == null || FixedLabel.EMPTY.equals(firstLabel)) {
            out.add(new AssignmentInstruction(z1, var));
        } else {
            out.add(new AssignmentInstruction(z1, var, firstLabel));
        }

        for(long i = 0; i < constantValue; i++) {
            out.add(new JumpZeroInstruction(z1, L1));
            out.add(new DecreaseInstruction(z1));
        }

        out.add(new JumpNotZeroInstruction(z1, L1));
        out.add(new GoToLabelInstruction(jeConstantLabel));
        out.add(new NeutralInstruction(var, L1));
        return out;
    }

    //These funcs return label and const value
    public Label getJeConstantLabel() { return jeConstantLabel; }
    public long getConstantValue() { return constantValue; }
}
