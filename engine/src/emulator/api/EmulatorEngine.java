package emulator.api;

import emulator.api.debug.DebugService;
import emulator.api.dto.*;
import emulator.exception.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface EmulatorEngine {
    void setSessionUser(UserDTO user);
    // Returns the current program view
    ProgramView programView();
    ProgramView programView(int degree);
    ProgramView programView(String programName, int degree);

    // Runs program
    RunResult run(Long... input);
    RunResult run(int degree, Long... input);
    RunResult run(String programName, int degree, Long... inputs);

    // Returns objects of program
    Map<String, Long> lastRunVars();
    List<Long> lastRunInputs();
    int lastRunDegree();
    String lastRunProgramName();
    List<String> extractInputVars(ProgramView pv);

    //History funcs
    List<RunRecord> history();
    List<RunRecord> history(String programName);
    public void clearHistory();

    // Checks if there's a loaded program
    boolean hasProgramLoaded();

    // Load program
    LoadResult loadProgram(Path xmlPath)
            throws XmlWrongExtensionException,   // wrong extension
            XmlNotFoundException,          // file does not exist
            XmlReadException,              // malformed XML / I/O during parse
            XmlInvalidContentException,    // schema/content invalid
            InvalidInstructionException,   // bad opcode/args
            MissingLabelException,         // label refs without definition
            ProgramException,              // other domain errors
            IOException;                   // reader surfaces raw IO
    public LoadResult loadProgramFromStream(InputStream xmlStream)
            throws XmlWrongExtensionException,
            XmlNotFoundException,
            XmlReadException,
            XmlInvalidContentException,
            InvalidInstructionException,
            MissingLabelException,
            ProgramException,
           IOException;
    LoadResult loadProgram(Path xmlPath, ProgressListener listener) throws Exception;
    public FunctionService getFunctionService();
    public ProgramService getProgramService();

    //saves/ loads state
    void saveState(Path fileWithoutExt) throws Exception;
    void loadState(Path fileWithoutExt) throws Exception;

    //Debug handle
    DebugService debugger();
    void debugResume();
    boolean debugIsFinished();
    Map<String,String> debugVarsSnapshot();
    int debugCurrentPC();
    int debugCycles();
}
