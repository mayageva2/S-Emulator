package emulator.logic.expansion;

import emulator.logic.instruction.Instruction;
import emulator.logic.program.Program;
import emulator.logic.program.ProgramImpl;

import java.util.List;
import java.util.Objects;

public class ProgramExpander {
    private final Expander expander = new Expander();

    public Program expandToDegree(Program original, int degree) {
        Objects.requireNonNull(original, "original");
        List<Instruction> out = expander.expandToDegree(original.getInstructions(), degree);
        return toProgramImpl(original.getName(), out);
    }

    public Program expandOnce(Program original) {
        Objects.requireNonNull(original, "original");
        List<Instruction> out = expander.expandOnce(original.getInstructions());
        return toProgramImpl(original.getName(), out);
    }

    public int calculateMaxDegree(Program original) {
        Objects.requireNonNull(original, "original");
        return expander.calculateMaxDegree(original.getInstructions());
    }

    public boolean isFullyBasic(Program original) {
        Objects.requireNonNull(original, "original");
        return expander.isFullyBasic(original.getInstructions());
    }

    private static Program toProgramImpl(String name, List<Instruction> instructions) {
        ProgramImpl p = new ProgramImpl(name);
        for (Instruction ins : instructions) {
            p.addInstruction(ins);
        }
        return p;
    }
}
