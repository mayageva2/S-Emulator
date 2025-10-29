package emulator.api.dto;

import emulator.logic.program.Program;

import java.util.List;

public record LoadResult(
        String programName,               // Name of the loaded program
        int instructionCount,             // Total number of instructions
        int maxDegree,                    // Maximum expansion degree
        List<String> functions,           // Functions defined in the program
        List<String> referencedFunctions, // Functions used from other programs
        Program program                   // Full loaded Program object
) {
    public LoadResult(String programName, int instructionCount, int maxDegree,
                      List<String> functions, List<String> referencedFunctions, Program program) {
        this.programName = programName;
        this.instructionCount = instructionCount;
        this.maxDegree = maxDegree;
        this.functions = functions;
        this.referencedFunctions = referencedFunctions;
        this.program = program;
    }

    public LoadResult(String programName, int instructionCount, int maxDegree,
                      List<String> functions, Program program) {
        this(programName, instructionCount, maxDegree, functions, List.of(), program);
    }
}