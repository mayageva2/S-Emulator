package server;

import java.util.*;

public class GlobalDataCenter {

    public static class ProgramEntry {
        public final String programName;
        public final String username;
        public final int instructionCount;
        public final int maxDegree;

        public ProgramEntry(String programName, String username, int instructionCount, int maxDegree) {
            this.programName = programName;
            this.username = username;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
        }
    }

    private static final List<ProgramEntry> programs = Collections.synchronizedList(new ArrayList<>());

    public static void addProgram(String programName, String username, int instructionCount, int maxDegree) {
        synchronized (programs) {
            boolean exists = programs.stream().anyMatch(p -> p.programName.equals(programName));
            if (!exists) {
                programs.add(new ProgramEntry(programName, username, instructionCount, maxDegree));
            }
        }
    }

    public static List<ProgramEntry> getPrograms() {
        synchronized (programs) {
            return new ArrayList<>(programs);
        }
    }

    public static void clear() {
        synchronized (programs) {
            programs.clear();
        }
    }
}
