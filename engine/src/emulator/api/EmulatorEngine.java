package emulator.api;

import emulator.api.dto.*;
import emulator.exception.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface EmulatorEngine {
    ProgramView programView();
    ProgramView programView(int degree);
    RunResult run(Long... input);
    RunResult run(int degree, Long... input);
    List<RunRecord> history();
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
    void saveState(Path fileWithoutExt) throws Exception;
    void loadState(Path fileWithoutExt) throws Exception;
}
