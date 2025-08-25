package emulator.api;

import emulator.api.dto.LoadResult;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunRecord;
import emulator.api.dto.RunResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface EmulatorEngine {
    ProgramView programView();
    LoadResult loadProgram(Path xmlPath);
    RunResult run(Long... input);
    RunResult run(int degree, Long... input);
    int lastCycles();
    List<RunRecord> history();
    void clearHistory();
    boolean hasProgramLoaded();
    Path saveOrReplaceVersion(Path original, int version) throws IOException;
}
