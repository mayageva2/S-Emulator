package emulator.logic.program;

import emulator.logic.instruction.Instruction;

import java.util.List;

public interface Program {

    String getName();
    void addInstruction(Instruction instruction);
    List<Instruction> getInstructions();

    boolean validate();
    int calculateMaxDegree();
    int calculateCycles();

}
