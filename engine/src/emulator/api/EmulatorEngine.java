package emulator.api;

import emulator.api.debug.DebugService;
import emulator.api.dto.*;
import emulator.exception.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface EmulatorEngine {
    ProgramView programView();
    ProgramView programView(int degree);
    ProgramView programView(String programName, int degree);
    RunResult run(Long... input);
    RunResult run(int degree, Long... input);
    RunResult run(String programName, int degree, Long... inputs);
    Map<String, Long> lastRunVars();
    List<Long> lastRunInputs();
    int lastRunDegree();
    String lastRunProgramName();
    List<RunRecord> history();
    List<RunRecord> history(String programName);
    List<String> availablePrograms();
    boolean hasProgramLoaded();
    List<String> extractInputVars(ProgramView pv);
    LoadResult loadProgram(Path xmlPath)
            throws XmlWrongExtensionException,   // wrong extension
            XmlNotFoundException,          // file does not exist
            XmlReadException,              // malformed XML / I/O during parse
            XmlInvalidContentException,    // schema/content invalid
            InvalidInstructionException,   // bad opcode/args
            MissingLabelException,         // label refs without definition
            ProgramException,              // other domain errors
            IOException;                   // reader surfaces raw IO
    LoadResult loadProgram(Path xmlPath, ProgressListener listener) throws Exception;
    public void clearHistory();
    void saveState(Path fileWithoutExt) throws Exception;
    void loadState(Path fileWithoutExt) throws Exception;
    DebugService debugger();
    void debugResume();
    boolean debugIsFinished();
    Map<String,String> debugVarsSnapshot();
    int debugCurrentPC();
    int debugCycles();
}
