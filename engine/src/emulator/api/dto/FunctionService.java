package emulator.api.dto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionService {
    private static final Map<String, FunctionInfo> functions = new ConcurrentHashMap<>();
    private static final FunctionService instance = new FunctionService();

    private FunctionService() {}

    public static FunctionService getInstance() {return instance;}

    public void addFunction(String functionName, String programName, String username,
                            int instructionCount, int maxDegree) {
        functions.put(functionName, new FunctionInfo(
                functionName,
                programName,
                username,
                instructionCount,
                maxDegree
        ));
    }

    public List<FunctionInfo> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }

    public void clear() {functions.clear();}
}
