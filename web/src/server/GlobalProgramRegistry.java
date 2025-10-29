package server;

import emulator.logic.program.Program;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class GlobalProgramRegistry {
    private static final Map<String, Program> programs = new ConcurrentHashMap<>();

    public static void add(String name, Program p) {
        if (name == null || p == null) return;
        programs.put(name, p);
        System.out.println("[GlobalProgramRegistry] Added program: " + name);
    }

    public static Program get(String name) {
        if (name == null) return null;
        return programs.get(name);
    }

    public static void clear() { programs.clear(); }
}
