package emulator.api.dto;

import java.util.List;

public record RunResult(long y, int cycles, List<VariableView> vars) { }

