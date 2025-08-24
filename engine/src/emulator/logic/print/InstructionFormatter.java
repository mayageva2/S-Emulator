package emulator.logic.print;

import emulator.logic.instruction.*;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;

public class InstructionFormatter {

    public String formatInstruction(int index, Instruction ins) {
        String bs = isBasic(ins) ? "B" : "S";

        String label = (ins.getLabel() != null && ins.getLabel() != FixedLabel.EMPTY)
                ? ins.getLabel().toString()
                : "";
        label = (label.length() > 5) ? label.substring(0, 5) : String.format("%-5s", label);

        String body = toAscii(formatBody(ins));

        int cycles = ins.cycles();
        return String.format("#%d (%s) [ %s ] %s (%d)", index, bs, label, body, cycles);
    }

    private boolean isBasic(Instruction ins) {
        return ins instanceof IncreaseInstruction
                || ins instanceof DecreaseInstruction
                || ins instanceof JumpNotZeroInstruction
                || ins instanceof NeutralInstruction;
    }

    private static String toAscii(String s) {
        if (s == null) return "";
        return s.replace("←", "<-").replace("≠", "!=");
    }

    private String formatBody(Instruction ins) {
        if (ins instanceof DecreaseInstruction dec) {
            var v = dec.getVariable(); return v + " <- " + v + " - 1";
        }
        if (ins instanceof IncreaseInstruction inc) {
            var v = inc.getVariable(); return v + " <- " + v + " + 1";
        }
        if (ins instanceof NeutralInstruction nop) {
            var v = nop.getVariable(); return v + " <- " + v;
        }
        if (ins instanceof AssignmentInstruction asg) {
            return asg.getVariable() + " <- " + asg.getAssignedVariable();
        }
        if (ins instanceof ConstantAssignmentInstruction cst) {
            return cst.getVariable() + " <- " + cst.getConstantValue();
        }
        if (ins instanceof GoToLabelInstruction go) {
            return "GOTO " + go.getgtlLabel();
        }
        if (ins instanceof JumpZeroInstruction jz) {
            return "IF " + jz.getVariable() + " = 0 GOTO " + jz.getJzLabel();
        }
        if (ins instanceof JumpNotZeroInstruction jnz) {
            return "IF " + jnz.getVariable() + " != 0 GOTO " + jnz.getJnzLabel();
        }
        if (ins instanceof JumpEqualConstantInstruction jec) {
            return "IF " + jec.getVariable() + " = " + jec.getConstantValue()
                    + " GOTO " + jec.getJeConstantLabel();
        }
        if (ins instanceof JumpEqualVariableInstruction jev) {
            return "IF " + jev.getVariable() + " = " + jev.getCompareVariable()
                    + " GOTO " + jev.getJeVariableLabel();
        }
        if (ins instanceof QuotationInstruction qi) {
            String args = qi.getFunctionArguments();
            if (args == null) args = "";
            return qi.getVariable() + " <- (" + qi.getFunctionName()
                    + (args.isEmpty() ? "" : ("," + args)) + ")";
        }
        return ins.getName();
    }
}
