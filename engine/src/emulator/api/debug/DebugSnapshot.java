package emulator.api.debug;

import java.util.Collections;
import java.util.Map;

public record DebugSnapshot(
        int currentInstructionIndex,     // Index of the current instruction
        Map<String, String> vars,        // Variable values
        int cycles,                      // Total cycle
        boolean finished                 // True if program finished running
) {
    public DebugSnapshot {  //Ensure vars map is never null or modifiable
        vars = (vars == null) ? Collections.emptyMap()
                : Collections.unmodifiableMap(vars);
    }
}
