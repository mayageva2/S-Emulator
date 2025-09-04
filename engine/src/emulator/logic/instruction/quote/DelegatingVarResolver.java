package emulator.logic.instruction.quote;

import emulator.logic.variable.Variable;

import java.util.Map;

public final class DelegatingVarResolver implements VarResolver {
    private final Map<String, Variable> locals;
    private final VarResolver parent;

    public DelegatingVarResolver(Map<String, Variable> locals, VarResolver parent) {
        this.locals = locals;
        this.parent = parent;
    }

    @Override
    public Variable resolve(String name) {
        String key = (name == null) ? "" : name.trim().toUpperCase(java.util.Locale.ROOT);
        Variable v = locals.get(key);
        if (v != null) return v;
        return parent.resolve(name);
    }
}
