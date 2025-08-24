package emulator.api;

import emulator.api.dto.LoadResult;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunRecord;
import emulator.api.dto.RunResult;
import emulator.logic.variable.Variable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface EmulatorEngine {
    ProgramView programView();
    LoadResult loadProgram(Path xmlPath);
    RunResult run(Long... input);
    Map<Variable, Long> variableState();
    int lastCycles();
    List<RunRecord> history();
    boolean hasProgramLoaded();
}
