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
        List<InstructionView> createdFromViews
) {
    public InstructionView(int index, String opcode, String label, boolean basic, int cycles,
                           List<String> args, List<Integer> createdFromChain) {
        this(index, opcode, label, basic, cycles, args, createdFromChain, List.of());
    }
}
