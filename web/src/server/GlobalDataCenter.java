package server;

import emulator.api.dto.FunctionInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalDataCenter {

    // === Inner class for program info ===
    public static class ProgramEntry {
        public final String programName;
        public final String username;
        public final int instructionCount;
        public final int maxDegree;
        public final int runCount;
        public final double avgCreditCost;

        public ProgramEntry(String programName, String username, int instructionCount,
                            int maxDegree, int runCount, double avgCreditCost) {
            this.programName = programName;
            this.username = username;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
            this.avgCreditCost = avgCreditCost;
            this.runCount = runCount;
        }
    }

    // === Shared global collections ===
    private static final List<ProgramEntry> programs =
            Collections.synchronizedList(new ArrayList<>());

    private static final List<FunctionInfo> functions =
            Collections.synchronizedList(new ArrayList<>());

    // === Raw XML files for reloading (instead of Program objects!) ===
    private static final Map<String, byte[]> programFiles = new ConcurrentHashMap<>();

    public static void storeProgramFile(String name, byte[] data) {
        if (name == null || data == null) return;
        programFiles.put(name, data);
        System.out.println("[GlobalDataCenter] Stored raw XML for: " + name);
    }

    public static byte[] getProgramFile(String name) {
        if (name == null) return null;
        return programFiles.get(name);
    }

    // === Programs ===
    public static void addProgram(String programName, String username, int instructionCount, int maxDegree) {
        synchronized (programs) {
            boolean exists = programs.stream().anyMatch(p -> p.programName.equals(programName));
            if (!exists) {
                programs.add(new ProgramEntry(programName, username, instructionCount, maxDegree, 0, 0));
                System.out.println("[GlobalDataCenter] Added program: " + programName + " by " + username);
            }
        }
    }

    public static List<ProgramEntry> getPrograms() {
        synchronized (programs) {
            return new ArrayList<>(programs);
        }
    }

    // === Functions ===
    public static void addFunction(FunctionInfo f) {
        synchronized (functions) {
            functions.removeIf(existing ->
                    existing.functionName().equalsIgnoreCase(f.functionName())
                            && existing.programName().equalsIgnoreCase(f.programName()));

            functions.add(f);
        }
    }

    public static List<FunctionInfo> getFunctions() {
        synchronized (functions) {
            return new ArrayList<>(functions);
        }
    }

    public static void clearAll() {
        synchronized (programs) { programs.clear(); }
        synchronized (functions) { functions.clear(); }
        programFiles.clear();
    }
}
