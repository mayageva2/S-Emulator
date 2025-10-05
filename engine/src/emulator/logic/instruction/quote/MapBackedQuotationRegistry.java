package emulator.logic.instruction.quote;

import emulator.logic.program.Program;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MapBackedQuotationRegistry implements QuotationRegistry {
    private final Map<String, Program> funcs;

    public MapBackedQuotationRegistry(Map<String, Program> funcs) {
        this.funcs = funcs;
    }

    @Override
    public Program getProgramByName(String functionName) {
        if (functionName == null) throw new IllegalArgumentException("functionName is null");
        Program p = funcs.get(functionName);
        if (p != null) return p;

        String upper = functionName.toUpperCase(Locale.ROOT);
        p = funcs.get(upper);
        if (p != null) return p;

        for (Map.Entry<String, Program> e : funcs.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(functionName)) {
                return e.getValue();
            }
        }
        throw new IllegalArgumentException("Unknown function: " + functionName);
    }

    @Override
    public void putProgram(String name, Program program) {
        if (name == null || program == null) return;
        funcs.put(name, program);
        funcs.put(name.toUpperCase(Locale.ROOT), program);
    }

    @Override
    public Set<String> allNames() {
        return funcs.keySet();
    }
}
