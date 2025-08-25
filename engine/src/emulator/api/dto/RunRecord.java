package emulator.api.dto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record RunRecord(
        int runNumber,
        int degree,
        List<Long> inputs,
        long y,
        int cycles
){
    public static RunRecord of(int runNumber, int degree, long[] inputs, long y, int cycles) {
        List<Long> in = (inputs == null) ? List.of() : Arrays.stream(inputs).boxed().toList();
        return new RunRecord(runNumber, degree, in, y, cycles);
    }
    public String inputsCsv() {
        return inputs == null ? "" : inputs.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}