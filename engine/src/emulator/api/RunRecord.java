package emulator.api;

import java.util.List;

public record RunRecord(List<Long> inputs, long y, int cycles) { }