package emulator.logic;

import emulator.logic.expansion.Expander;
import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.ZeroVariableInstruction;
import emulator.logic.label.LabelImpl;
import emulator.logic.program.ProgramImpl;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

import java.util.List;

public class MainExpandZeroVar {
    public static void main(String[] args) {

        // --- Build a tiny program: [L1] ZERO_VARIABLE x1
        Variable x1 = new VariableImpl(VariableType.INPUT, 1, "x1");
        LabelImpl l1 = new LabelImpl(1);

        ProgramImpl program = new ProgramImpl("ZERO_VAR_DEMO");
        program.addInstruction(new ZeroVariableInstruction(x1, l1));

        // --- Expand the program (to max degree, or use 1 if you prefer)
        Expander expander = new Expander();
        List<Instruction> expandedIns =
                expander.expandToDegree(program.getInstructions(),
                        expander.calculateMaxDegree(program.getInstructions()));
        // If you want exactly one round:
        // List<Instruction> expandedIns = expander.expandToDegree(program.getInstructions(), 1);

        // --- Build a new Program from the expanded instructions
        ProgramImpl expandedProgram = new ProgramImpl("ZERO_VAR_DEMO_EXPANDED");
        for (Instruction ins : expandedIns) {
            expandedProgram.addInstruction(ins);
        }

        // --- Run the expanded program
        ProgramExecutor executor = new ProgramExecutorImpl(expandedProgram);
        // Adjust inputs to match your ProgramExecutor contract; here we pass a single INPUT for x1:
        long result = executor.run(5L);
        System.out.println("Result: " + result);

        // Optional: print expanded instruction count (or each instruction if you have a formatter)
        System.out.println("Expanded instructions: " + expandedIns.size());
    }
}
