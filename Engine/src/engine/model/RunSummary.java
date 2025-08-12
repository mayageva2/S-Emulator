package engine.model;

import java.util.List;

public class RunSummary {
    private final int runNumber;
    private final int degree;
    private final List<Long> inputs;
    private final long yValue;
    private final long cycles;

    public RunSummary(int runNumber, int degree, List<Long> inputs, long yValue, long cycles) {
        this.runNumber = runNumber;
        this.degree = degree;
        this.inputs = List.copyOf(inputs);
        this.yValue = yValue;
        this.cycles = cycles;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public int getDegree() {
        return degree;
    }

    public List<Long> getInputs() {
        return inputs;
    }

    public long getyValue() {
        return yValue;
    }

    public long getCycles() {
        return cycles;
    }
}
