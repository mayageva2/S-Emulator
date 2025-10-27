package emulator.api.dto;

import java.util.List;

public record ProgramView(
        List<InstructionView> instructions,  // List of all instructions
        String programName,                  // Program name
        int degree,                          // Current expansion degree
        int maxDegree,                       // Maximum expansion degree
        int totalCycles,                     // Total cycles
        List<String> inputs                  // Program input variable names
) { }


