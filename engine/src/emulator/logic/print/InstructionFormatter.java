package emulator.logic.print;

import emulator.logic.instruction.*;
import emulator.logic.label.FixedLabel;

public class InstructionFormatter {

    private final FormatStyle style;

    public InstructionFormatter() { this(FormatStyle.USER); }

    public InstructionFormatter(FormatStyle style) { this.style = java.util.Objects.requireNonNull(style); }

    public String format(Instruction ins) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatBody(ins));

        if (style.isShowCycles()) {
            sb.append("  // cycles=").append(ins.cycles())
                    .append(", name=").append(ins.getName());
        }
        return sb.toString();
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
            return "IF " + jnz.getVariable() + " ≠ 0 GOTO " + jnz.getJnzLabel();
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
            return qi.getVariable() + " ← (" + qi.getFunctionName()
                    + (args.isEmpty() ? "" : ("," + args)) + ")";
        }
        return ins.getName();
    }

}
