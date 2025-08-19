package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class JumpEqualVariableInstruction extends AbstractInstruction {

    private final Label jeVariableLabel;
    private final Variable compareVariable;

    public JumpEqualVariableInstruction(Variable variable,
                                        Variable compareVariable,
                                        Label jeVariableLabel) {
        super(InstructionData.JUMP_EQUAL_VARIABLE, Objects.requireNonNull(variable, "variable"));
        this.compareVariable = Objects.requireNonNull(compareVariable, "compareVariable");
        this.jeVariableLabel = Objects.requireNonNull(jeVariableLabel, "jeVariableLabel");
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
}
