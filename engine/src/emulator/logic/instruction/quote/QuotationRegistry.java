package emulator.logic.instruction.quote;

import emulator.logic.program.Program;

import java.util.Set;

public interface QuotationRegistry {
    Program getProgramByName(String functionName);
    void putProgram(String name, Program program);
    default Set<String> allNames() { return Set.of(); }
}