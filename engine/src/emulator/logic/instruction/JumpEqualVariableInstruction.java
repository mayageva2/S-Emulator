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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class JumpEqualVariableInstruction extends AbstractInstruction implements Expandable {

    private final Label jeVariableLabel;
    private final Variable compareVariable;

    public JumpEqualVariableInstruction(Variable variable,
                                        Variable compareVariable,
                                        Label jeVariableLabel) {
        super(InstructionData.JUMP_EQUAL_VARIABLE, Objects.requireNonNull(variable, "variable"));
        this.compareVariable = Objects.requireNonNull(compareVariable, "compareVariable");
        this.jeVariableLabel = Objects.requireNonNull(jeVariableLabel, "jeVariableLabel");
        setArgument("gotoLabel", jeVariableLabel.getLabelRepresentation());
    }

    public JumpEqualVariableInstruction(Variable variable,
                                        Variable compareVariable,
                                        Label jeVariableLabel,
                                        Label myLabel) {
        super(InstructionData.JUMP_EQUAL_VARIABLE,
                Objects.requireNonNull(variable, "variable"),
                Objects.requireNonNull(myLabel, "label"));
        this.compareVariable = Objects.requireNonNull(compareVariable, "compareVariable");
        this.jeVariableLabel = Objects.requireNonNull(jeVariableLabel, "jeVariableLabel");
        setArgument("gotoLabel", jeVariableLabel.getLabelRepresentation());
    }

    @Override
    public Label execute(ExecutionContext context) {
        long v  = context.getVariableValue(getVariable());
        long v2 = context.getVariableValue(compareVariable);
        return (v == v2) ? jeVariableLabel : FixedLabel.EMPTY;
    }

    @Override
    public Collection<Variable> referencedVariables() {
        return List.of(getVariable(), compareVariable);
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {

        Variable var = getVariable();
        Variable compareVar = compareVariable;
        if (var == null) {
            throw new IllegalStateException("JUMP_EQUAL_VARIABLE missing variable");
        }
        if (compareVariable == null) {
            throw new IllegalStateException("JUMP_EQUAL_VARIABLE missing compare variable");
        }

        Variable z1 = helper.freshVar();
        Variable z2 = helper.freshVar();

        List<Instruction> out = new ArrayList<>();

        Label L1 = helper.freshLabel();
        Label L2 = helper.freshLabel();
        Label L3 = helper.freshLabel();
        Label L = jeVariableLabel;

        Label firstLabel = getLabel();
        if (firstLabel == null || FixedLabel.EMPTY.equals(firstLabel)) {
            out.add(new AssignmentInstruction(z1, var));
        } else {
            out.add(new AssignmentInstruction(z1, var, firstLabel));
        }

        out.add(new AssignmentInstruction(z2, compareVar));
        out.add(new JumpZeroInstruction(z1, L3, L2));
        out.add(new JumpZeroInstruction(z2, L1));
        out.add(new DecreaseInstruction(z1));
        out.add(new DecreaseInstruction(z2));
        out.add(new GoToLabelInstruction(L2));
        out.add(new JumpZeroInstruction(z2, L, L3));
        out.add(new NeutralInstruction(var, L1));

        return out;
    }

    public Variable getCompareVariable() { return compareVariable; }
    public Label getJeVariableLabel() { return jeVariableLabel; }
}
