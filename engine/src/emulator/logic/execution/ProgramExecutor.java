package emulator.logic.execution;

import emulator.logic.variable.Variable;

import java.util.Map;

public interface ProgramExecutor {
    long run(Long... input);
    Map<Variable, Long> variableState();
    int getLastExecutionCycles();
    int getLastDynamicCycles();
    void setStepListener(StepListener listener);

    interface StepListener {
        void onStep(int pcAfter, int cycles, Map<String, String> vars, boolean finished);
    }
}
