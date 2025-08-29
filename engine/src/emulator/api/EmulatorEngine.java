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
    ProgramView programView(int degree);
    LoadResult loadProgram(Path xmlPath);
    RunResult run(Long... input);
    RunResult run(int degree, Long... input);
    List<RunRecord> history();
    boolean hasProgramLoaded();
    List<String> extractInputVars(ProgramView pv);
    Path saveOrReplaceVersion(Path original, int version) throws IOException;
}
