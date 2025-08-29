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

public class ConstantAssignmentInstruction extends AbstractInstruction implements Expandable {
    private final long constantValue;

    public ConstantAssignmentInstruction(Variable target, long constantValue) {
        super(InstructionData.CONSTANT_ASSIGNMENT, Objects.requireNonNull(target, "target"));
        if (constantValue < 0) {
            throw new IllegalArgumentException("constantValue (K) must be non-negative");
        }
        this.constantValue = constantValue;
    }

    public ConstantAssignmentInstruction(Variable target, long constantValue, Label label) {
        super(InstructionData.CONSTANT_ASSIGNMENT,
                Objects.requireNonNull(target, "target"),
                Objects.requireNonNull(label, "label"));
        if (constantValue < 0) {
            throw new IllegalArgumentException("constantValue (K) must be non-negative");
        }
        this.constantValue = constantValue;
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), constantValue);
        return FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        List<Instruction> out = new ArrayList<>();
        Variable var = getVariable();
        if (var == null) {
            throw new IllegalStateException("CONSTANT_ASSIGNMENT missing variable");
        }

        Label firstLabel = getLabel();
        if (firstLabel == null || FixedLabel.EMPTY.equals(firstLabel)) {
            out.add(new ZeroVariableInstruction(var));
        } else {
            out.add(new ZeroVariableInstruction(var, firstLabel));
        }

        for(int i = 0; i < constantValue; i++) {
            out.add(new IncreaseInstruction(var));
        }

        return out;
    }

    public long getConstantValue() { return constantValue; }
}


