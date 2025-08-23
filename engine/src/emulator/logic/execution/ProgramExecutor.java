package emulator.logic.execution;

import emulator.logic.variable.Variable;

import java.util.Map;

public interface ProgramExecutor {

    long run(Long... input);
    Map<String, Long> variableState();
    int getLastExecutionCycles();
}
