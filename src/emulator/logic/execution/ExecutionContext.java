package emulator.logic.execution;

import emulator.logic.variable.Variable;

public interface ExecutionContext {

    long getVariableValue(Variable v);
    void updateVariable(Variable v, long value);
}
