package emulator.api.dto;

public record FunctionInfo(
        String functionName,
        String programName,
        String username,
        int instructionCount,
        int maxDegree
) {}
