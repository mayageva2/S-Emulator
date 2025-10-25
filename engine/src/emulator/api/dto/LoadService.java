package emulator.api.dto;

import emulator.api.EmulatorEngine;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class LoadService {

    private final ProgramService programService = ProgramService.getInstance();
    private final FunctionService functionService = FunctionService.getInstance();
    private final UserService userService = new UserService();
    private static final Map<String, String> programTypes = new HashMap<>();
    public static String getTypeForProgram(String name) {
        if (name == null) return "PROGRAM";
        return programTypes.getOrDefault(name.toUpperCase(Locale.ROOT), "PROGRAM");
    }

    public static void registerProgramType(String name, String type) {
        if (name != null && type != null) {
            programTypes.put(name.toUpperCase(Locale.ROOT), type);
        }
    }

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

    private void validateCrossReferences(LoadResult result) throws Exception {
        if (result == null)
            throw new IllegalArgumentException("Invalid load result (null).");

        String programName = result.programName();
        if (programService.programExists(programName)) {
            throw new IllegalStateException("Program '" + programName + "' already exists in the system.");
        }

        Set<String> existingFunctions = new HashSet<>();
        for (FunctionInfo f : functionService.getAllFunctions()) {
            existingFunctions.add(f.name().toUpperCase(Locale.ROOT));
        }

        for (String f : result.functions()) {
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

    public static void debugPrintTypes() {
        System.out.println("=== PROGRAM TYPES MAP ===");
        for (var e : programTypes.entrySet()) {
            System.out.println("  " + e.getKey() + " → " + e.getValue());
        }
        System.out.println("=========================");
    }

}
