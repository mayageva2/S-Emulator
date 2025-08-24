package emulator.api;

import java.util.Map;

public record RunResult(long y, int cycles, Map<String, Long> vars) { }
