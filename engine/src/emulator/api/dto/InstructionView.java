package emulator.api.dto;

import java.util.List;

public record InstructionView(
        int index,
        String opcode,
        String label,
        boolean basic,
        int cycles,
        List<String> args,
        List<Integer> createdFromChain,
        List<InstructionView> createdFromViews,
        long creditCost,
        String architecture
) { }
