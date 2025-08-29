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

    //This func creates an ExpansionHelper collecting all used variables and labels
    public static ExpansionHelper fromInstructions(
            List<Instruction> instructions,
            Function<String, Variable> varFactory,
            Function<String, Label> labelFactory
    ) {
        Set<String> usedVars = collectUsedVariableNames(instructions);
        Set<String> usedLabs = collectUsedLabelNames(instructions);
        return new ExpansionHelper(usedVars, usedLabs, varFactory, labelFactory);
    }

    //This func creates an ExpansionHelper initialized with used variables and labels
    public static ExpansionHelper fromUsedSets(
            Set<String> usedVarNames,
            Set<String> usedLabelNames,
            Function<String, Variable> varFactory,
            Function<String, Label> labelFactory
    ) {
        return new ExpansionHelper(usedVarNames, usedLabelNames, varFactory, labelFactory);
    }

    //This func collects and returns all unique variable names
    public static Set<String> collectUsedVariableNames(List<Instruction> list) {
        Set<String> out = new HashSet<>();
        if (list == null) return out;

        for (Instruction ins : list) {
            if (ins == null) continue;
            collectMainVariable(ins, out);
            collectVariablesFromArgs(ins, out);
        }
        return out;
    }

    //This func adds the instruction's main variable
    private static void collectMainVariable(Instruction ins, Set<String> out) {
        Variable v = ins.getVariable();
        if (v == null) return;
        String name = v.getRepresentation();
        addName(name, out);
    }

    //This func scans argument values for variable-like tokens
    private static void collectVariablesFromArgs(Instruction ins, Set<String> out) {
        Map<String, String> args = ins.getArguments();
        if (args == null) return;

        for (String raw : args.values()) {
            String rep = safeTrim(raw);
            if (rep.isEmpty()) continue;

            String normalized = normalizeVarToken(rep);
            if (!normalized.isEmpty()) out.add(normalized);
        }
    }

    //This func normalize tokens
    private static String normalizeVarToken(String rep) {
        if (isY(rep)) return "y";
        if (isXorZDigits(rep)) {
            char c0 = Character.toLowerCase(rep.charAt(0));
            return (c0 == 'x' ? "x" : "z") + rep.substring(1);
        }
        return "";
    }

    //This func checks if variables is y(result)
    private static boolean isY(String rep) {
        return "y".equalsIgnoreCase(rep);
    }
    
    //This func checks if x/z var
    private static boolean isXorZDigits(String rep) {
        if (rep.length() <= 1) return false;
        char c0 = Character.toLowerCase(rep.charAt(0));
        if (c0 != 'x' && c0 != 'z') return false;
        return rep.substring(1).chars().allMatch(Character::isDigit);
    }

    //This func adds name 
    private static void addName(String name, Set<String> out) {
        if (name != null && !name.isEmpty()) out.add(name);
    }

    //This func trims a string
    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    //This func collects and returns all unique label names
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
                    if (rep.matches("(?i)^L\\d+$")) {   // Match label format "L#" (case-insensitive)
                        s.add("L" + rep.substring(1));
                    }
                }
            }
        }
        return s;
    }
}

