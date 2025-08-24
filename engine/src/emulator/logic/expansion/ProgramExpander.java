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
        List<Instruction> expanded;
        expanded = Expander.expandInstructions(src.getInstructions(), helper);
        Program out = new ProgramImpl(src.getName() + "_exp");
        for (Instruction ins : expanded) {
            out.addInstruction(ins);
        }
        return out;
    }
}
