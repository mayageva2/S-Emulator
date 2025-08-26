package emulator.logic.expansion;

import emulator.logic.instruction.AbstractInstruction;
import emulator.logic.instruction.Instruction;
import emulator.logic.label.LabelImpl;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Expander {

    public List<Instruction> expandOnce(List<Instruction> input) {
        Objects.requireNonNull(input, "input");

        ExpansionHelper helper = buildHelper(input);
        List<Instruction> out = new ArrayList<>(Math.max(16, input.size() * 2));

        for (Instruction ins : input) {
            if (ins == null) continue;
            if (ins instanceof Expandable e) {
                List<Instruction> produced = e.expand(helper);
                if (produced == null || produced.isEmpty()) {
                    out.add(ins);
                } else {
                    for (Instruction child : produced) {
                        if (child == null) continue;
                        if (child instanceof AbstractInstruction ai && ai.getCreatedFrom() == null) {
                            ai.setCreatedFrom(ins);
                        }
                        out.add(child);
                    }
                }
            } else {
                out.add(ins);
            }
        }
        return Collections.unmodifiableList(out);
    }

    private ExpansionHelper buildHelper(List<Instruction> input) {
        return ExpansionHelper.fromInstructions(
                input,
                name -> {
                    String rep = (name == null) ? "" : name.trim();
                    int num = extractInt(rep);
                    VariableType vt = mapVarType(rep);
                    return new VariableImpl(vt, num);
                },
                name -> new LabelImpl(extractInt(name))
        );
    }

    private static VariableType mapVarType(String rep) {
        if (rep == null || rep.isEmpty()) return VariableType.WORK;
        char c = Character.toLowerCase(rep.charAt(0));
        if (c == 'x') return VariableType.INPUT;
        if (c == 'z') return VariableType.WORK;
        if (c == 'y') return VariableType.RESULT;
        return VariableType.WORK;
    }

    private static int extractInt(String s) {
        if (s == null) return 0;
        int n = 0, len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {

                int j = i;
                long val = 0;
                while (j < len) {
                    char d = s.charAt(j);
                    if (d < '0' || d > '9') break;
                    val = val * 10 + (d - '0');
                    if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                    j++;
                }
                n = (int) val;
                break;
            }
        }
        return n;
    }

    public List<Instruction> expandToDegree(List<Instruction> original, int degree) {
        Objects.requireNonNull(original, "original");
        if (degree <= 0) return Collections.unmodifiableList(new ArrayList<>(original));

        List<Instruction> curr = new ArrayList<>(original);
        for (int d = 0; d < degree; d++) {
            if (isFullyBasic(curr)) break;
            curr = new ArrayList<>(expandOnce(curr));
        }
        return Collections.unmodifiableList(curr);
    }

    public int calculateMaxDegree(List<Instruction> original) {
        Objects.requireNonNull(original, "original");
        int degree = 0;
        List<Instruction> curr = new ArrayList<>(original);
        while (!isFullyBasic(curr)) {
            curr = new ArrayList<>(expandOnce(curr));
            degree++;
        }
        return degree;
    }

    public boolean isFullyBasic(List<Instruction> list) {
        Objects.requireNonNull(list, "list");
        for (Instruction ins : list) {
            if (ins instanceof Expandable) return false;
        }
        return true;
    }

    public static List<Instruction> expandInstructions(List<Instruction> src, ExpansionHelper helper) {
        List<Instruction> out = new ArrayList<>();
        if (src == null) return out;

        for (Instruction ins : src) {
            if (ins == null) continue;

            if (ins instanceof Expandable ex) {
                List<Instruction> expanded = ex.expand(helper);
                if (expanded == null || expanded.isEmpty()) {
                    out.add(ins);
                } else {
                    for (Instruction child : expanded) {
                        if (child == null) continue;
                        if (child instanceof AbstractInstruction ai && ai.getCreatedFrom() == null) {
                            ai.setCreatedFrom(ins);
                        }
                        out.add(child);
                    }
                }
            } else {
                out.add(ins);
            }
        }
        return out;
    }

    public static List<Instruction> expandProgramOnce(List<Instruction> src, ExpansionHelper helper) {
        return expandInstructions(src, helper);
    }
}
