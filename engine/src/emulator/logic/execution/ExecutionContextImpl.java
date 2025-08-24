package emulator.logic.execution;

import emulator.logic.variable.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExecutionContextImpl implements ExecutionContext {
    private final Map<Variable, Long> vars = new HashMap<>();

    @Override
    public long getVariableValue(Variable v) {
        return vars.getOrDefault(v, 0L);
    }

    @Override
    public void updateVariable(Variable v, long value) {
        vars.put(v, value);
    }

    @Override
    public Map<Variable, Long> getAllVariables() {
        return Collections.unmodifiableMap(vars);
    }
}
