package emulator.api.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProgramRegistry {
    private static final Map<String, ProgramStats> programs = new ConcurrentHashMap<>();
    private static volatile ProgramStats lastLoaded = null;

    public static void register(ProgramStats stats) {
        if (stats == null || stats.getProgramName() == null) return;
        programs.put(stats.getProgramName(), stats);
        lastLoaded = stats;
    }

    public static ProgramStats get(String programName) {
        return programs.get(programName);
    }

    public static ProgramStats getLastLoaded() {
        return lastLoaded;
    }

    public static Map<String, ProgramStats> getAll() {
        return programs;
    }

    public static void clear() {
        programs.clear();
        lastLoaded = null;
    }
}
