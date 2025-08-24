package emulator.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface EmulatorEngine {
    LoadResult loadProgram(Path xmlPath);
    RunResult run(Long... input);
    List<String> programSummary();
    Map<String, Long> variableState();
    int lastCycles();
    List<RunRecord> history();
    boolean hasProgramLoaded();
}
