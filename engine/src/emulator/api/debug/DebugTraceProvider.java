package emulator.api.debug;

import java.util.List;

public interface DebugTraceProvider {
    List<DebugRecord> getDebugTrace();
}
