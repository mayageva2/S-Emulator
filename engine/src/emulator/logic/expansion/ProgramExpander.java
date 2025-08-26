package emulator.logic.expansion;

import emulator.logic.instruction.Instruction;
import emulator.logic.program.Program;
import emulator.logic.program.ProgramImpl;
import emulator.logic.variable.VariableImpl;
import emulator.logic.label.LabelImpl;

import java.util.List;

public class ProgramExpander {
    public Program expandToDegree(Program src, int degree) {
        if (degree <= 0) return src;
        Program cur = src;
        for (int d = 0; d < degree; d++) {
            cur = expandOnce(cur);
        }
        return cur;
    }

    private Program expandOnce(Program src) {
        var helper = ExpansionHelper.fromInstructions(
                src.getInstructions(),
                VariableImpl::new,
                LabelImpl::new
        );
        List<Instruction> cur = Expander.expandInstructions(src.getInstructions(), helper);
        boolean changed;
        int guard = 0;
        do {
            changed = false;
            List<Instruction> next = new java.util.ArrayList<>();
            for (Instruction ins : cur) {
                boolean isBasic = ins.getInstructionData().isBasic();
                if (!isBasic && ins instanceof emulator.logic.expansion.Expandable ex) {
                    List<Instruction> more = ex.expand(helper);
                    next.addAll(more);
                    changed = true;
                } else {
                    next.add(ins);
                }
            }
            cur = next;
        } while (changed && ++guard < 5);

        Program out = new ProgramImpl(src.getName() + "_exp");
        for (Instruction ins : cur) out.addInstruction(ins);
        return out;
    }
}
