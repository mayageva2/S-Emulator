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

            // main var
            Variable v = i.getVariable();
            if (v != null) {
                String name = v.getRepresentation();
                if (name != null && !name.isEmpty()) s.add(name);
            }

            // vars inside args
            Map<String,String> args = i.getArguments();
            if (args != null) {
                for (String val : args.values()) {
                    if (val == null) continue;
                    String rep = val.trim();
                    if (rep.isEmpty()) continue;

                    char c0 = Character.toLowerCase(rep.charAt(0));
                    if ((c0 == 'x' || c0 == 'z') && rep.length() > 1 && rep.substring(1).chars().allMatch(Character::isDigit)) {
                        s.add(Character.toLowerCase(c0) == 'x' ? "x" + rep.substring(1) : "z" + rep.substring(1));
                    } else if (rep.equalsIgnoreCase("y")) {
                        s.add("y");
                    }
                }
            }
        }
        return s;
    }

    public static Set<String> collectUsedLabelNames(List<Instruction> list) {
        Set<String> s = new LinkedHashSet<>();
        if (list == null) return s;

        for (Instruction i : list) {
            if (i == null) continue;

            Label lab = i.getLabel();
            if (lab != null) {
                String name = lab.getLabelRepresentation();
                if (name != null && !name.isEmpty()) s.add(name.startsWith("l") ? "L" + name.substring(1) : name);
            }

            Map<String,String> args = i.getArguments();
            if (args != null) {
                for (String val : args.values()) {
                    if (val == null) continue;
                    String rep = val.trim();
                    if (rep.matches("(?i)^L\\d+$")) {
                        s.add("L" + rep.substring(1));
                    }
                }
            }
        }
        return s;
    }
}

