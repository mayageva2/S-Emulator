package emulator.api.dto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionService {
    private static final Map<String, FunctionInfo> functions = new ConcurrentHashMap<>();
    private static final FunctionService instance = new FunctionService();
    private static final Map<String, Set<String>> programToFunctions = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> funcToFuncs       = new ConcurrentHashMap<>();

    private FunctionService() {}

    public static FunctionService getInstance() {return instance;}

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

    public List<FunctionInfo> getAllFunctions() {
        List<FunctionInfo> result = new ArrayList<>();
        for (FunctionInfo f : functions.values()) {
            double avg = ProgramStatsRepository.getInstance().getAverage(f.functionName());
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

    public void registerFunctionDependency(String parentFunc, String childFunc) {
        if (parentFunc == null || childFunc == null) return;
        funcToFuncs
                .computeIfAbsent(parentFunc, k -> ConcurrentHashMap.newKeySet())
                .add(childFunc);
    }

    public Set<String> allFunctionsOfProgram(String programName) {
        Set<String> direct = programToFunctions.getOrDefault(programName, Set.of());
        return closureOverFunctions(direct);
    }

    public Set<String> allProgramsUsingFunction(String funcName) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Set<String>> e : programToFunctions.entrySet()) {
            if (closureOverFunctions(e.getValue()).contains(funcName)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public Set<String> allRelatedFunctions(String funcName) {
        if (funcName == null) return Set.of();
        return closureOverFunctions(Set.of(funcName));
    }

    private Set<String> closureOverFunctions(Set<String> start) {
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>(start);
        while (!stack.isEmpty()) {
            String f = stack.pop();
            if (!visited.add(f)) continue;
            for (String child : funcToFuncs.getOrDefault(f, Set.of())) {
                if (!visited.contains(child)) stack.push(child);
            }
        }
        return visited;
    }

    public void clear() {
        functions.clear();
        programToFunctions.clear();
        funcToFuncs.clear();
    }

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
