package emulator.api.dto;

public record FunctionInfo(
        String functionName,      // Function name
        String programName,       // Parent program name
        String username,          // Owner username
        int instructionCount,     // Number of instructions
        int maxDegree,            // Maximum expansion degree
        double avgCreditCost      // Average credit cost
) {
    public String name() { return functionName; }
}
