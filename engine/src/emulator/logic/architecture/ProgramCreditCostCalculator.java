package emulator.logic.architecture;

import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.program.Program;

public class ProgramCreditCostCalculator {

    public static long calculateTotalCost(Program program) {
        if (program == null)
            throw new IllegalArgumentException("Program cannot be null");

        long total = 0;

        for (Instruction instr : program.getInstructions()) {
            InstructionData data = instr.getInstructionData();
            if (data != null) {
                total += data.getBaseCreditCost();
            } else {
                System.err.println("Instruction missing InstructionData: " + instr.getName());
            }
        }

        return total;
    }
}
