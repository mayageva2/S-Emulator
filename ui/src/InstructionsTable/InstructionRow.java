package InstructionsTable;

import emulator.api.dto.InstructionView;

import java.util.List;

public class InstructionRow {
    public final int index;
    public final boolean basic;
    public final String label;
    public final int cycles;
    public final String opcode;
    public final List<String> args;
    public final int depth;                 // 0 for main table, >0 for history indentation
    public final InstructionView sourceIv;  // << keep the original for callbacks
    public final long creditCost;
    public final String architecture;
    public String display = "";

    public InstructionRow(int index, boolean basic, String label, int cycles,
                          String opcode, List<String> args, int depth,
                          InstructionView sourceIv) {
        this.index = index;
        this.basic = basic;
        this.label = label;
        this.cycles = cycles;
        this.opcode = opcode;
        this.args = args;
        this.depth = depth;
        this.sourceIv = sourceIv;
        this.creditCost = sourceIv.creditCost();
        this.architecture = sourceIv.architecture();
    }
}
