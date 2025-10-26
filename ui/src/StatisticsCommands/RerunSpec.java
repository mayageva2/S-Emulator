package StatisticsCommands;

import java.util.List;

public record RerunSpec(String programName, int degree, List<Long> inputs, String architecture) {}
