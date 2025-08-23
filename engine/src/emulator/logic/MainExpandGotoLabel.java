package emulator.logic;

import emulator.logic.expansion.Expander;
import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.JumpNotZeroInstruction;
import emulator.logic.instruction.ZeroVariableInstruction;
import emulator.logic.instruction.GoToLabelInstruction;
import emulator.logic.label.LabelImpl;
import emulator.logic.program.ProgramImpl;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

import java.util.List;

public class MainExpandGotoLabel {
    public static void main(String[] args) {
        // --- Labels ---
        LabelImpl lGoto   = new LabelImpl(1); // label of the GOTO instruction itself
        LabelImpl lTarget = new LabelImpl(2); // target label to "go to"

        // --- Variables ---
        // Temp WORK variable for the GOTO constructor (expand() will still use freshVar internally)
        Variable tmp = new VariableImpl(VariableType.WORK, 99, "tmp");
        // INPUT variable to demonstrate ZERO_VARIABLE at the target
        Variable x1  = new VariableImpl(VariableType.INPUT, 1, "x1");

        // --- Program ---
        ProgramImpl program = new ProgramImpl("GOTO_LABEL_DEMO");
        // [lStart] GOTO_LABEL lTarget
        program.addInstruction(new GoToLabelInstruction(tmp, lGoto, lTarget));
        // [lTarget] ZERO_VARIABLE x1
        program.addInstruction(new ZeroVariableInstruction(x1, lTarget));

        // --- Expand program ---
        Expander expander = new Expander();
        int maxDegree = expander.calculateMaxDegree(program.getInstructions());
        int degreeToExpand = Math.max(1, maxDegree);
        List<Instruction> expandedIns = expander.expandToDegree(program.getInstructions(), degreeToExpand);

        // --- Build expanded program (optional, for execution) ---
        ProgramImpl expandedProgram = new ProgramImpl("GOTO_LABEL_DEMO_EXPANDED");
        for (Instruction ins : expandedIns) {
            expandedProgram.addInstruction(ins);
        }

        // --- Debug print: safe against nulls/NPEs in getRepresentation()
        System.out.println("=== Expanded Instructions ===");
        for (int i = 0; i < expandedIns.size(); i++) {
            Instruction ins = expandedIns.get(i);
            String lbl = "-";
            if (ins.getLabel() != null) {
                try {
                    lbl = String.valueOf(ins.getLabel().getLabelRepresentation());
                } catch (Throwable t) {
                    lbl = String.valueOf(ins.getLabel());
                }
            }
            String varStr = "-";
            if (ins.getVariable() != null) {
                try {
                    // Avoid getRepresentation() to prevent NPE if type is null in some temps
                    varStr = String.valueOf(ins.getVariable());
                } catch (Throwable t) {
                    varStr = "?";
                }
            }
            String extra = "";
            try {
                if (ins instanceof JumpNotZeroInstruction) {
                    JumpNotZeroInstruction jnz = (JumpNotZeroInstruction) ins;
                    if (jnz.getJnzLabel() != null) {
                        String tgt = String.valueOf(jnz.getJnzLabel().getLabelRepresentation());
                        extra = " -> " + tgt;
                    }
                }
            } catch (Throwable t) {
                // ignore
            }

            System.out.printf("%02d) %-28s label=%-8s var=%-8s%s%n",
                    i,
                    ins.getClass().getSimpleName(),
                    lbl,
                    varStr,
                    extra);
        }
        System.out.println("Total expanded instructions: " + expandedIns.size());

        // --- Execute with example input (x1 = 5)
        ProgramExecutor executor = new ProgramExecutorImpl(expandedProgram);
        long result = executor.run(5L);
        System.out.println("Execution Result: " + result);
    }
}