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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AssignmentInstruction extends AbstractInstruction implements Expandable {

    private final Variable assignedVariable;

    public AssignmentInstruction(Variable target, Variable assignedVariable) {
        super(InstructionData.ASSIGNMENT, Objects.requireNonNull(target, "target"));
        this.assignedVariable = Objects.requireNonNull(assignedVariable, "assignedVariable");
    }

    public AssignmentInstruction(Variable target, Variable assignedVariable, Label label) {
        super(InstructionData.ASSIGNMENT, Objects.requireNonNull(target, "target"),
                Objects.requireNonNull(label, "label"));
        this.assignedVariable = Objects.requireNonNull(assignedVariable, "assignedVariable");
    }

    @Override
    public Label execute(ExecutionContext context) {
        long value = context.getVariableValue(assignedVariable);
        context.updateVariable(getVariable(), value);
        return FixedLabel.EMPTY;
    }

    @Override
    public Collection<Variable> referencedVariables() {
        return List.of(getVariable(), assignedVariable);
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        Variable dst = getVariable();
        Variable src = assignedVariable;

        if (dst == null || src == null) {
            throw new IllegalStateException("ASSIGNMENT missing src/dst variable");
        }

        if (dst == src || dst.equals(src)) {
            return List.of();
        }

        List<Instruction> out = new ArrayList<>();

        Label firstLabel = getLabel();
        if (firstLabel == null || FixedLabel.EMPTY.equals(firstLabel)) {
            firstLabel = helper.freshLabel();
        }

        Variable tmp = helper.freshVar();
        Label L1 = helper.freshLabel();
        Label L2 = helper.freshLabel();
        Label L3 = helper.freshLabel();

        for (Instruction zi : new ZeroVariableInstruction(dst, firstLabel).expand(helper)) {
            if (zi instanceof AbstractInstruction ai) {
                ai.setCreatedFrom(this);
            }
            out.add(zi);
        }

        JumpNotZeroInstruction jnzToL1 = new JumpNotZeroInstruction(src, L1);
        jnzToL1.setCreatedFrom(this);
        out.add(jnzToL1);

        for (Instruction zi : new GoToLabelInstruction(dst, L3).expand(helper)) {
            if (zi instanceof AbstractInstruction ai) {
                ai.setCreatedFrom(this);
            }
            out.add(zi);
        }

        DecreaseInstruction decSrc = new DecreaseInstruction(src, L1);
        decSrc.setCreatedFrom(this);
        out.add(decSrc);

        IncreaseInstruction incTmp = new IncreaseInstruction(tmp);
        incTmp.setCreatedFrom(this);
        out.add(incTmp);

        JumpNotZeroInstruction jnzSrc = new JumpNotZeroInstruction(src, L1);
        jnzSrc.setCreatedFrom(this);
        out.add(jnzSrc);

        DecreaseInstruction decTmpRestore = new DecreaseInstruction(tmp, L2);
        decTmpRestore.setCreatedFrom(this);
        out.add(decTmpRestore);

        IncreaseInstruction incDst = new IncreaseInstruction(dst);
        incDst.setCreatedFrom(this);
        out.add(incDst);

        IncreaseInstruction incSrc = new IncreaseInstruction(src);
        incSrc.setCreatedFrom(this);
        out.add(incSrc);

        JumpNotZeroInstruction jnzTmpRestore = new JumpNotZeroInstruction(tmp, L2);
        jnzTmpRestore.setCreatedFrom(this);
        out.add(jnzTmpRestore);

        NeutralInstruction neutral = new NeutralInstruction(dst, L3);
        neutral.setCreatedFrom(this);
        out.add(neutral);

        return out;
    }

    public Variable getAssignedVariable() { return assignedVariable; }
}
