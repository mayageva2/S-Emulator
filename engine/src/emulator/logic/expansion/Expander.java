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

    //This func expands one layer of expandable instructions
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
                    for (Instruction child : produced) {   // Add each produced child
                        if (child == null) continue;
                        if (child instanceof AbstractInstruction ai) {
                            ai.setCreatedFrom(ins);
                        }
                        out.add(child);
                    }
                }
            } else {   // Non-expandable instruction, keep as is
                out.add(ins);
            }
        }
        return Collections.unmodifiableList(out);
    }

    //This func builds an ExpansionHelper
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

    //This func maps a variable’s string representation
    private static VariableType mapVarType(String rep) {
        if (rep == null || rep.isEmpty()) return VariableType.WORK;
        char c = Character.toLowerCase(rep.charAt(0));
        if (c == 'x') return VariableType.INPUT;
        if (c == 'z') return VariableType.WORK;
        if (c == 'y') return VariableType.RESULT;
        return VariableType.WORK;
    }

    //This func extracts and returns the first integer value
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

    //This func expands program to the desired degree
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

    //This func returns the max degree the program can reach
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

    //This func checks if the instruction is basic
    public boolean isFullyBasic(List<Instruction> list) {
        Objects.requireNonNull(list, "list");
        for (Instruction ins : list) {
            if (ins instanceof Expandable) return false;
        }
        return true;
    }

}
