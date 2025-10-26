package emulator.logic.statistics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProgramStatisticsManager {
    private static final Map<String, Double> programAvgCost = new ConcurrentHashMap<>();
    private static final Map<String, Double> functionAvgCost = new ConcurrentHashMap<>();

    public static void updateAverageCost(String programName, double avgCost) {
        programAvgCost.put(programName, avgCost);
    }

    public static void updateAverageCostForFunction(String functionName, double avgCost) {
        functionAvgCost.put(functionName, avgCost);
    }

    public static double getProgramAvgCost(String programName) {
        return programAvgCost.getOrDefault(programName, 0.0);
    }

    public static double getFunctionAvgCost(String functionName) {
        return functionAvgCost.getOrDefault(functionName, 0.0);
    }
}
