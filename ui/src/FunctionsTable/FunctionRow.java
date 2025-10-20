package FunctionsTable;

public class FunctionRow {
    private final String functionName;
    private final String programName;
    private final String username;
    private final int instructionCount;
    private final int maxDegree;

    public FunctionRow(String functionName, String programName, String username, int instructionCount, int maxDegree) {
        this.functionName = functionName;
        this.programName = programName;
        this.username = username;
        this.instructionCount = instructionCount;
        this.maxDegree = maxDegree;
    }

    public String getFunctionName() { return functionName; }
    public String getProgramName() { return programName; }
    public String getUsername() { return username; }
    public int getInstructionCount() { return instructionCount; }
    public int getMaxDegree() { return maxDegree; }
}
