package emulator.api.debug;

import java.util.Map;

public record DebugRecord(
        int pcAfter,
        int cycles,
        Map<String,String> vars,
        boolean finished,
        String event
) {}
