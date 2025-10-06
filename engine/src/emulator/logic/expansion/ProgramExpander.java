package emulator.logic.expansion;

import emulator.logic.instruction.Instruction;
import emulator.logic.program.Program;
import emulator.logic.program.ProgramImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProgramExpander {
    private final Expander expander = new Expander();

    //This func expands a program’s instructions up to the specified degree
    public Program expandToDegree(Program original, int degree) {
        Objects.requireNonNull(original, "original");
        if (degree <= 0) return original;

        ExpansionHelper helper = ExpansionHelper.fromInstructions(
                original.getInstructions(),
                name -> new emulator.logic.variable.VariableImpl(
                        Expander.mapVarType(name),
                        Expander.extractInt(name)
                ),
                name -> new emulator.logic.label.LabelImpl(Expander.extractInt(name))
        );

        List<Instruction> curr = original.getInstructions();
        for (int d = 0; d < degree; d++) {
            if (expander.isFullyBasic(curr)) break;
            List<Instruction> next = expander.expandOnce(curr, helper);
            if (next == curr) break;
            curr = next;
        }
        return toProgramImpl(original.getName(), curr);
    }

    //This func expands a program’s instructions by one degree
    public Program expandOnce(Program original) {
        Objects.requireNonNull(original, "original");
        List<Instruction> out = expander.expandOnce(
                original.getInstructions(),
                ExpansionHelper.fromInstructions(
                        original.getInstructions(),
                        name -> new emulator.logic.variable.VariableImpl(
                                Expander.mapVarType(name),
                                Expander.extractInt(name)
                        ),
                        name -> new emulator.logic.label.LabelImpl(Expander.extractInt(name))
                )
        );
        return toProgramImpl(original.getName(), out);
    }

    //This func builds a new ProgramImpl
    private static Program toProgramImpl(String name, List<Instruction> instructions) {
        ProgramImpl p = new ProgramImpl(name);
        for (Instruction ins : instructions) {
            p.addInstruction(ins);
        }
        return p;
    }
}
