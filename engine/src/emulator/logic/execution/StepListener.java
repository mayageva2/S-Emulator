package emulator.logic.execution;

import java.util.Map;

@FunctionalInterface
public interface StepListener {
    void onStep(int pcAfter, int cycles, Map<String,String> vars, boolean finished);
}
