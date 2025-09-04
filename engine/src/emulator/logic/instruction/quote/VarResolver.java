package emulator.logic.instruction.quote;

import emulator.logic.variable.Variable;

public interface VarResolver {
    Variable resolve(String name);
}
