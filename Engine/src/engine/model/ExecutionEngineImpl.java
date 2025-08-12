package engine.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExecutionEngineImpl implements ExecutionEngine {
    private Program currentProgram;
    private final List<RunSummary> history = new ArrayList<>();

    @Override
    public LoadResult loadProgram(Path xmlPath) {
        return null;
    }

    @Override
    public ProgramView showProgram() {
        return null;
    }

    @Override
    public int getMaxDegree() {
        return 0;
    }

    @Override
    public ProgramView expandTo(int degree) {
        return null;
    }

    @Override
    public RunResult run(int degree, List<Long> inputs) {
        return null;
    }

    @Override
    public List<RunSummary> history() {
        return List.copyOf(history);
    }
}
