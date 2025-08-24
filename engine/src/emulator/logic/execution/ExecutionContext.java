package emulator.logic.execution;

import emulator.logic.variable.Variable;

import java.util.Map;

public interface ExecutionContext {

    long getVariableValue(Variable v);
    void updateVariable(Variable v, long value);
    Map<Variable, Long> getAllVariables();
}
