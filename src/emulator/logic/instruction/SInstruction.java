package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

public interface SInstruction {

    String getName();
    Label execute(ExecutionContext context);
    int cycles();
    Label getLabel();
    Variable getVariable();
}
