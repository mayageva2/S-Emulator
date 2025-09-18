package emulator.logic.program;

import emulator.logic.expansion.ProgramExpander;
import emulator.logic.instruction.Instruction;

public final class ProgramCost {
    public int cyclesAtDegree(Program program, int degree) {
        Program expanded = new ProgramExpander().expandToDegree(program, degree);
        int total = 0;
        for (Instruction ins : expanded.getInstructions()) {
            total += ins.cycles();  // uses per-instruction static cost for basics
        }
        return total;
    }

    // Returns the dynamic cost of program expanded to its max degree
    public int cyclesFullyExpanded(Program program) {
        int maxDeg = program.calculateMaxDegree();
        return cyclesAtDegree(program, maxDeg);
    }
}
