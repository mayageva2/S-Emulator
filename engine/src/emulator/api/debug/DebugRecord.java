package emulator.api.debug;

import java.util.Map;

public record DebugRecord(
        int pcAfter,                 // Program counter after this step
        int cycles,                  // Total cycles
        Map<String,String> vars,     // Variable values
        boolean finished,            // If program finished = True
        String event                 //Description of debug event
) {}
