package engine.model;

import java.nio.file.Path;
import java.util.List;

public interface ExecutionEngine {
    LoadResult loadProgram(Path xmlPath);
    ProgramView showProgram();
    int getMaxDegree();
    ProgramView expandTo(int degree);
    RunResult run(int degree, List<Long> inputs);
    List<RunSummary> history();
}
