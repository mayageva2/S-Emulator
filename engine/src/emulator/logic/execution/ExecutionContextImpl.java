package emulator.logic.execution;

import emulator.logic.variable.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExecutionContextImpl implements ExecutionContext {
    private final Map<Variable, Long> vars = new HashMap<>();
    private QuoteEvaluator quoteEvaluator;
    private int dynamicCycles = 0;

    @Override public long getVariableValue(Variable v) {return vars.getOrDefault(v, 0L);}
    @Override public void updateVariable(Variable v, long value) {vars.put(v, value);}
    @Override public Map<Variable, Long> getAllVariables() {
        return Collections.unmodifiableMap(vars);
    }
    @Override public void setQuoteEvaluator(QuoteEvaluator evaluator) { this.quoteEvaluator = evaluator; }
    @Override public QuoteEvaluator getQuoteEvaluator() { return quoteEvaluator; }
    public void addDynamicCycles(int n) {
        dynamicCycles += n;
    }
    public int getDynamicCycles() {
        return dynamicCycles;
    }
    public int drainDynamicCycles() {
        int current = dynamicCycles;
        dynamicCycles = 0;
        return current;
    }
}
