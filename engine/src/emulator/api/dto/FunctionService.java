package emulator.api.dto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionService {
    private static final Map<String, FunctionInfo> functions = new ConcurrentHashMap<>();
    private static final FunctionService instance = new FunctionService();

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

    public void clear() {functions.clear();}
}
