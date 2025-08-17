package emulator.logic.program;

import emulator.logic.instruction.SInstruction;

import java.util.List;

public interface SProgram {

    String getName();
    void addInstruction(SInstruction instruction);
    List<SInstruction> getInstructions();

    boolean validate();
    int calculateMaxDegree();
    int calculateCycles();

}
