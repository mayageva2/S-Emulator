package emulator.api.dto;

import java.util.List;

public record LoadResult(
        String programName,
        int instructionCount,
        int maxDegree,
        List<String> functions
) {}
