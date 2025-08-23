package emulator.logic.expansion;

import emulator.logic.instruction.Instruction;

import java.util.List;

public interface Expandable {
    List<Instruction> expand(ExpansionHelper expansionHelper);
}

