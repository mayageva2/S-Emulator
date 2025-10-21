package emulator.api.dto;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record RunRecord(
        String username,
        String programName,
        int runNumber,
        int degree,
        List<Long> inputs,
        long y,
        int cycles
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static RunRecord of(String username, String programName, int runNumber, int degree, long[] inputs, long y, int cycles) {
        List<Long> in = (inputs == null) ? List.of() : Arrays.stream(inputs).boxed().toList();
        return new RunRecord(username, programName, runNumber, degree, in, y, cycles);
    }
    public String inputsCsv() {
        return inputs == null ? "" : inputs.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}