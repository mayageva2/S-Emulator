package emulator.api.debug;

import java.util.Collections;
import java.util.Map;

public record DebugSnapshot(
        int currentInstructionIndex,
        Map<String, String> vars,
        int cycles,
        boolean finished
) {
    public DebugSnapshot {
        vars = (vars == null) ? Collections.emptyMap()
                : Collections.unmodifiableMap(vars);
    }
}
