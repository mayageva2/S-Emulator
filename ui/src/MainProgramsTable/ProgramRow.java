package MainProgramsTable;

public class ProgramRow {
    private final String programName;
    private final String username;
    private final int instructionCount;
    private final int maxDegree;
    private final int runCount;
    private final double avgCreditCost;

    public ProgramRow(String programName, String username, int instructionCount,
                      int maxDegree, int runCount, double avgCreditCost) {
        this.programName = programName;
        this.username = username;
        this.instructionCount = instructionCount;
        this.maxDegree = maxDegree;
        this.runCount = runCount;
        this.avgCreditCost = avgCreditCost;
    }

    public String getProgramName() { return programName; }
    public String getUsername() { return username; }
    public int getInstructionCount() { return instructionCount; }
    public int getMaxDegree() { return maxDegree; }
    public int getRunCount() { return runCount; }
    public double getAvgCreditCost() { return avgCreditCost; }
}
