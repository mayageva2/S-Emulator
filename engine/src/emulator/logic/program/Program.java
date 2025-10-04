package emulator.logic.program;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.List;
import java.util.Set;

public interface Program {

    String getName();
    void addInstruction(Instruction instruction);
    List<Instruction> getInstructions();
    Set<Variable> getVariables();
    Instruction instructionAt(Label label);
    int calculateMaxDegree();
    public List<String> getInputVariableNames();
}
