package emulator.logic.instruction.quote;

import emulator.logic.program.Program;

public interface QuotationRegistry {
    Program getProgramByName(String functionName);
    void putProgram(String name, Program program);
}