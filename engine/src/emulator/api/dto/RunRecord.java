package emulator.api.dto;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class RunRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String programName;
    private final int runNumber;
    private final int degree;
    private final List<Long> inputs;
    private final long y;
    private final int cycles;
    private Map<String, Long> varsSnapshot = new LinkedHashMap<>();

    public RunRecord(String username, String programName, int runNumber, int degree,
                     List<Long> inputs, long y, int cycles, Map<String, Long> varsSnapshot) {
        this.username = username;
        this.programName = programName;
        this.runNumber = runNumber;
        this.degree = degree;
        this.inputs = (inputs != null) ? List.copyOf(inputs) : List.of();
        this.y = y;
        this.cycles = cycles;
        this.varsSnapshot = varsSnapshot;
    }

    public String username() { return username; }
    public String programName() { return programName; }
    public int runNumber() { return runNumber; }
    public int degree() { return degree; }
    public List<Long> inputs() { return inputs; }
    public long y() { return y; }
    public int cycles() { return cycles; }

    public static RunRecord of(String username, String programName, int runNumber, int degree, long[] inputs, long y, int cycles, Map<String, Long> varsSnapshot) {
        List<Long> in = (inputs == null) ? List.of() : Arrays.stream(inputs).boxed().toList();
        return new RunRecord(username, programName, runNumber, degree, in, y, cycles, varsSnapshot);
    }

    public String inputsCsv() {
        return inputs == null ? "" : inputs.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public void setVarsSnapshot(Map<String, Long> vars) {
        this.varsSnapshot = (vars != null)
                ? new LinkedHashMap<>(vars)
                : new LinkedHashMap<>();
    }

    public Map<String, Long> getVarsSnapshot() {
        return Collections.unmodifiableMap(varsSnapshot);
    }
}
