package emulator.api.dto;

public record LoadResult(
        String programName,
        int instructionCount,
        int maxDegree
) {}
