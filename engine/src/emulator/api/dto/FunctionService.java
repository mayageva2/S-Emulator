package emulator.api.dto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionService {
    private final Map<String, FunctionInfo> functions = new ConcurrentHashMap<>();
    private final ProgramStatsRepository programStats;
    private static final Map<String, Set<String>> programToFunctions = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> funcToFuncs       = new ConcurrentHashMap<>();

    public FunctionService(ProgramStatsRepository programStats) {
        this.programStats = programStats;
    }

    //This function adds a new function entry and links it to its parent program
    public void addFunction(String functionName, String programName, String username,
                            int instructionCount, int maxDegree, double score) {
        functions.put(functionName, new FunctionInfo(
                functionName,
                programName,
                username,
                instructionCount,
                maxDegree,
                score
        ));

        programToFunctions
                .computeIfAbsent(programName, k -> ConcurrentHashMap.newKeySet())
                .add(functionName);
    }

    //This function returns a list of all functions
    public List<FunctionInfo> getAllFunctions() {
        List<FunctionInfo> result = new ArrayList<>();
        for (FunctionInfo f : functions.values()) {
            double avg = programStats.getAverage(f.functionName());
            result.add(new FunctionInfo(
                    f.functionName(),
                    f.programName(),
                    f.username(),
                    f.instructionCount(),
                    f.maxDegree(),
                    avg
            ));
        }
        return result;
    }

    //This function clears the maps
    public void clear() {
        functions.clear();
        programToFunctions.clear();
        funcToFuncs.clear();
    }

    //This func returns all functions directly belonging to the given program
    public Set<String> getFunctionsByProgram(String programName) {
        Set<String> result = new HashSet<>();
        if (programName == null) return result;

        for (FunctionInfo f : functions.values()) {
            if (f.programName().equalsIgnoreCase(programName)) {
                result.add(f.functionName());
            }
        }
        return result;
    }

    //This func returns all programs that directly use the given function
    public Set<String> getProgramsUsingFunction(String functionName) {
        Set<String> result = new HashSet<>();
        if (functionName == null) return result;

        for (FunctionInfo f : functions.values()) {
            if (f.functionName().equalsIgnoreCase(functionName)) {
                result.add(f.programName());
            }
        }
        return result;
    }

    //This func returns all functions from the same program as the given function
    public Set<String> getRelatedFunctions(String functionName) {
        Set<String> result = new HashSet<>();
        if (functionName == null) return result;

        String targetProgram = null;
        for (FunctionInfo f : functions.values()) {
            if (f.functionName().equalsIgnoreCase(functionName)) {
                targetProgram = f.programName();
                break;
            }
        }

        if (targetProgram != null) {
            for (FunctionInfo f : functions.values()) {
                if (f.programName().equalsIgnoreCase(targetProgram)) {
                    result.add(f.functionName());
                }
            }
        }

        result.remove(functionName);
        return result;
    }
}
