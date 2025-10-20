package emulator.api.dto;

import emulator.api.EmulatorEngine;
import java.io.InputStream;
import java.nio.file.Path;

public class LoadService {

    private final ProgramService programService = ProgramService.getInstance();
    private final UserService userService = new UserService();

    public LoadResult load(EmulatorEngine engine, Path xmlPath, String username) throws Exception {
        LoadResult result = engine.loadProgram(xmlPath);

        int functionCount = (result.functions() != null) ? result.functions().size() : 0;
        userService.incrementMainProgramsAndFunctions(functionCount);

        programService.addProgram(
                result.programName(),
                username,
                result.instructionCount(),
                result.maxDegree()
        );

        return result;
    }

    public LoadResult loadFromStream(EmulatorEngine engine, InputStream xmlStream, String username) throws Exception {
        LoadResult result = engine.loadProgramFromStream(xmlStream);

        int functionCount = (result.functions() != null) ? result.functions().size() : 0;
        userService.incrementMainProgramsAndFunctions(functionCount);

        programService.addProgram(
                result.programName(),
                username,
                result.instructionCount(),
                result.maxDegree()
        );

        return result;
    }
}
