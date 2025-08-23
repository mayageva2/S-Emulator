package emulator.logic.expansion;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.LabelImpl;
import emulator.logic.variable.VariableImpl;

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
            if (ins instanceof Expandable e) {
                List<Instruction> produced = e.expand(helper);
                if (produced == null || produced.isEmpty()) {
                    out.add(ins);
                } else {
                    out.addAll(produced);
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
                name -> new VariableImpl(null, -1),
                name -> {
                    int num;
                    try {
                        num = Integer.parseInt(name.replaceAll("[^\\d]", ""));
                    } catch (Exception e) {
                        num = 0;
                    }
                    return new LabelImpl(num);
                }
        );
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
}
