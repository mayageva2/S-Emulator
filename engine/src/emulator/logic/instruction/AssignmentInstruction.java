package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AssignmentInstruction extends AbstractInstruction implements Expandable {

    private final Variable assignedVariable;

    public AssignmentInstruction(Variable target, Variable assignedVariable) {
        super(InstructionData.ASSIGNMENT, Objects.requireNonNull(target, "target"));
        this.assignedVariable = Objects.requireNonNull(assignedVariable, "assignedVariable");
        setArgument("assignedVariable", assignedVariable.getRepresentation());
    }

    public AssignmentInstruction(Variable target, Variable assignedVariable, Label label) {
        super(InstructionData.ASSIGNMENT, Objects.requireNonNull(target, "target"),
                Objects.requireNonNull(label, "label"));
        this.assignedVariable = Objects.requireNonNull(assignedVariable, "assignedVariable");
        setArgument("assignedVariable", assignedVariable.getRepresentation());
    }

    //This func executes the instruction
    @Override
    public Label execute(ExecutionContext context) {
        long value = context.getVariableValue(getAssignedVariable());
        context.updateVariable(getVariable(), value);
        return FixedLabel.EMPTY;
    }

    //This func returns referenced variables
    @Override
    public Collection<Variable> referencedVariables() {
        return List.of(getVariable(), assignedVariable);
    }

    //This func expands an ASSIGNMENT instruction
    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        Variable dst = getVariable();
        Variable src = assignedVariable;

        // Safety checks
        if (dst == null || src == null) {
            throw new IllegalStateException("ASSIGNMENT missing src/dst variable");
        }
        if (dst == src || dst.equals(src)) {
            return List.of();
        }

        List<Instruction> out = new ArrayList<>();
        Variable tmp = helper.freshVar();
        Label L1 = helper.freshLabel();
        Label L2 = helper.freshLabel();
        Label L3 = helper.freshLabel();

        Label firstLabel = getLabel();
        if (firstLabel == null || FixedLabel.EMPTY.equals(firstLabel)) {
            out.add(new ZeroVariableInstruction(dst));
        } else {
            out.add(new ZeroVariableInstruction(dst, firstLabel));
        }

        out.add(new JumpNotZeroInstruction(src, L1));
        out.add(new GoToLabelInstruction(L3));
        out.add(new DecreaseInstruction(src, L1));
        out.add(new IncreaseInstruction(tmp));
        out.add(new JumpNotZeroInstruction(src, L1));
        out.add(new DecreaseInstruction(tmp, L2));
        out.add(new IncreaseInstruction(dst));
        out.add(new IncreaseInstruction(src));
        out.add(new JumpNotZeroInstruction(tmp, L2));
        out.add(new NeutralInstruction(dst, L3));

        return out;
    }

    //This func returns the assigned variable
    public Variable getAssignedVariable() { return assignedVariable; }
}
