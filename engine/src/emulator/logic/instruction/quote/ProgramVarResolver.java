package emulator.logic.instruction.quote;

import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

public final class ProgramVarResolver implements VarResolver {
    private final Program program;
    public ProgramVarResolver(Program program) { this.program = program; }

    @Override public Variable resolve(String name) {
        String want = (name == null) ? "" : name.trim();
        for (Variable v : program.getVariables()) {
            if (v.getRepresentation().equalsIgnoreCase(want)) return v;
        }
        throw new IllegalArgumentException("Variable not found in current program: " + name);
    }
}
