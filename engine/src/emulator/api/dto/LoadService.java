package emulator.api.dto;

import emulator.api.EmulatorEngine;
import emulator.logic.user.UserService;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class LoadService {

    private final ProgramService programService;
    private final FunctionService functionService;
    private final ProgramStatsRepository programStats;
    private final UserService userService = new UserService();
    private final Map<String, String> programTypes = new HashMap<>();

    public LoadService(ProgramService programService, FunctionService functionService, ProgramStatsRepository stats) {
        this.programService = programService;
        this.functionService = functionService;
        this.programStats = stats;
    }

    //This func returns the type of the file
    public String getTypeForProgram(String name) {
        if (name == null) return "PROGRAM";
        return programTypes.getOrDefault(name.toUpperCase(Locale.ROOT), "PROGRAM");
    }

    //This func registers a program or function type
    public void registerProgramType(String name, String type) {
        if (name != null && type != null) {
            programTypes.put(name.toUpperCase(Locale.ROOT), type);
        }
    }

    //This func loads a program from a file path
    public LoadResult load(EmulatorEngine engine, Path xmlPath, String username) throws Exception {
        LoadResult result = engine.loadProgram(xmlPath);
        validateCrossReferences(result);

        int functionCount = (result.functions() != null) ? result.functions().size() : 0;
        userService.incrementMainProgramsAndFunctions(functionCount);
        programService.addProgram(
                result.programName(),
                username,
                result.instructionCount(),
                result.maxDegree()
        );
        registerProgramType(result.programName(), "PROGRAM");

        for (String func : result.functions()) {
            if (!func.equalsIgnoreCase(result.programName())) {
                functionService.addFunction(
                        func,
                        result.programName(),
                        username,
                        result.instructionCount(),
                        result.maxDegree(),
                        0.0
                );
                registerProgramType(func, "FUNCTION");
            }
        }

        return result;
    }

    //This func loads a program from an InputStream
    public LoadResult loadFromStream(EmulatorEngine engine, InputStream xmlStream, String username) throws Exception {
        LoadResult result = engine.loadProgramFromStream(xmlStream);
        validateCrossReferences(result);
        int functionCount = (result.functions() != null) ? result.functions().size() : 0;
        userService.incrementMainProgramsAndFunctions(functionCount);

        programService.addProgram(
                result.programName(),
                username,
                result.instructionCount(),
                result.maxDegree()
        );

        for (String func : result.functions()) {
            functionService.addFunction(
                    func,
                    result.programName(),
                    username,
                    result.instructionCount(),
                    result.maxDegree(),
                    0.0
            );
        }
        return result;
    }

    //This func validates that no duplicate or missing function references exist in the loaded program
    private void validateCrossReferences(LoadResult result) throws Exception {
        if (result == null)
            throw new IllegalArgumentException("Invalid load result (null).");

        String programName = result.programName();
        if (programService.programExists(programName)) {   // Check if the program already exists
            throw new IllegalStateException("Program '" + programName + "' already exists in the system.");
        }

        Set<String> existingFunctions = new HashSet<>();
        for (FunctionInfo f : functionService.getAllFunctions()) {  // Collect all existing functions
            existingFunctions.add(f.name().toUpperCase(Locale.ROOT));
        }

        for (String f : result.functions()) {   // Ensure new functions don't conflict with existing ones
            if (existingFunctions.contains(f.toUpperCase(Locale.ROOT))) {
                throw new IllegalStateException("Function '" + f + "' already exists in the system.");
            }
        }

        if (result.referencedFunctions() != null) {
            for (String ref : result.referencedFunctions()) {
                if (!existingFunctions.contains(ref.toUpperCase(Locale.ROOT))
                        && !result.functions().contains(ref)) {
                    throw new IllegalStateException(
                            "Unknown function '" + ref + "' referenced in program '" + programName + "'."
                    );
                }
            }
        }
    }
}
