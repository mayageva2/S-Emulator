package server;

import emulator.api.dto.FunctionInfo;
import emulator.logic.program.Program;

import java.util.*;

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
            this.runCount = runCount;
            this.avgCreditCost = avgCreditCost;
        }
    }

    // === Shared global collections ===
    private static final List<ProgramEntry> programs = Collections.synchronizedList(new ArrayList<>());
    private static final List<FunctionInfo> functions = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Program> programObjects = Collections.synchronizedMap(new HashMap<>());

    // === Programs ===
    public static void addProgram(String programName, String username, int instructionCount, int maxDegree) {
        synchronized (programs) {
            boolean exists = programs.stream().anyMatch(p -> p.programName.equals(programName));
            if (!exists) {
                programs.add(new ProgramEntry(programName, username, instructionCount, maxDegree, 0, 0));
                System.out.println("[GlobalDataCenter] Added program: " + programName + " by " + username);
            } else {
                System.out.println("[GlobalDataCenter] Program already exists: " + programName);
            }
        }
    }

    public static void addProgramObject(String programName, Program program) {
        if (program == null || programName == null) return;
        synchronized (programObjects) {
            programObjects.put(programName, program);
            System.out.println("[GlobalDataCenter] Stored Program object: " + programName);
        }
    }

    public static Program getProgramAsObject(String programName) {
        synchronized (programObjects) {
            return programObjects.get(programName);
        }
    }

    public static boolean hasProgramObject(String programName) {
        synchronized (programObjects) {
            return programObjects.containsKey(programName);
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
        synchronized (programs) {
            programs.clear();
        }
        synchronized (functions) {
            functions.clear();
        }
        synchronized (programObjects) {
            programObjects.clear();
        }
    }
}
