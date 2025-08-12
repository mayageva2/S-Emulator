package engine.model;

import java.util.List;
import java.util.Map;

public class RunResult {
    private final long yValue;
    private final Map<String, Long> variables;
    private final long cycles;
    private final ProgramView executedProgram;

    public RunResult(long yValue, Map<String, Long> variables, long cycles, ProgramView executedProgram) {
        this.yValue = yValue;
        this.variables = Map.copyOf(variables);
        this.cycles = cycles;
        this.executedProgram = executedProgram;
    }

    public long getyValue() {
        return yValue;
    }

    public Map<String, Long> getVariables() {
        return variables;
    }

    public long getCycles() {
        return cycles;
    }

    public ProgramView getExecutedProgram() {
        return executedProgram;
    }
}
