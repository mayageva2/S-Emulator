package emulator.api.dto;

import java.util.List;

public record InstructionView(
        int index,                                    // Instruction index in program
        String opcode,                                // Operation code
        String label,                                 // Optional label name
        boolean basic,                                // True if it's a basic instruction
        int cycles,                                   // Number of cycles
        List<String> args,                            // Instruction arguments
        List<Integer> createdFromChain,               // Chain of source instruction indices
        List<InstructionView> createdFromViews,       // Original expanded instructions
        long creditCost,                              // Total credit cost
        String architecture                           // Architecture type
) { }
