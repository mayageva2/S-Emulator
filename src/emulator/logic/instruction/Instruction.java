package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.Collection;
import java.util.Collections;

public interface Instruction {

    String getName();
    Label execute(ExecutionContext context);
    int cycles();
    Label getLabel();
    Variable getVariable();
    default Collection<Variable> referencedVariables() { return Collections.emptyList(); }
}
