package emulator.api.dto;

import java.util.List;

public record LoadResult(
        String programName,               // Name of the loaded program
        int instructionCount,             // Total number of instructions
        int maxDegree,                    // Maximum expansion degree
        List<String> functions,           // Functions defined in the program
        List<String> referencedFunctions  // Functions used from other programs
) {
    public LoadResult(String programName, int instructionCount, int maxDegree, List<String> functions) {
        this(programName, instructionCount, maxDegree, functions, List.of());
    }
}