package emulator.logic.expansion;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.*;
import java.util.function.Function;

public final class ExpansionHelper {
    private final NameAllocator zAlloc;
    private final NameAllocator lAlloc;
    private final Function<String, Variable> varFactory;
    private final Function<String, Label> labelFactory;

    public ExpansionHelper(Set<String> usedVarNames,
                           Set<String> usedLabelNames,
                           Function<String, Variable> varFactory,
                           Function<String, Label> labelFactory) {
        this.zAlloc = new NameAllocator("z", usedVarNames);
        this.lAlloc = new NameAllocator("L", usedLabelNames);
        this.varFactory = varFactory;
        this.labelFactory = labelFactory;
    }

    public String freshVarName()   { return zAlloc.next(); }
    public String freshLabelName() { return lAlloc.next(); }
    public Variable freshVar()     { return varFactory.apply(freshVarName()); }
    public Label    freshLabel()   { return labelFactory.apply(freshLabelName()); }

    public static ExpansionHelper fromInstructions(
            List<Instruction> instructions,
            Function<String, Variable> varFactory,
            Function<String, Label> labelFactory
    ) {
        Set<String> usedVars = collectUsedVariableNames(instructions);
        Set<String> usedLabs = collectUsedLabelNames(instructions);
        return new ExpansionHelper(usedVars, usedLabs, varFactory, labelFactory);
    }

    public static ExpansionHelper fromUsedSets(
            Set<String> usedVarNames,
            Set<String> usedLabelNames,
            Function<String, Variable> varFactory,
            Function<String, Label> labelFactory
    ) {
        return new ExpansionHelper(usedVarNames, usedLabelNames, varFactory, labelFactory);
    }

    public static Set<String> collectUsedVariableNames(List<Instruction> list) {
        Set<String> s = new HashSet<>();
        if (list == null) return s;
        for (Instruction i : list) {
            if (i == null) continue;
            Variable v = i.getVariable();
            if (v != null) {
                String name = v.getRepresentation();
                if (name != null && !name.isEmpty()) s.add(name);
            }
        }
        return s;
    }

    public static Set<String> collectUsedLabelNames(List<Instruction> list) {
        Set<String> s = new java.util.LinkedHashSet<>();
        if (list == null) return s;
        for (Instruction i : list) {
            if (i == null) continue;
            Label lab = i.getLabel();
            if (lab == null) continue;
            String name = lab.getLabelRepresentation();
            if (name != null && !name.isEmpty()) {
                s.add(name);
            }
        }
        return s;
    }
}

