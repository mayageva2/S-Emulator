package emulator.logic.execution;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgramExecutorImpl implements ProgramExecutor{

    private final Program program;
    private int lastExecutionCycles = 0;

    public ProgramExecutorImpl(Program program) {
        this.program = program;
    }

    @Override
    public long run(Long... input) {
        lastExecutionCycles = 0;
        List<Instruction> instructions = program.getInstructions();
        final int len = instructions.size();
        if (len == 0) throw new IllegalStateException("empty program");

        ExecutionContext context = new ExecutionContextImpl();

        int currentIndex = 0;
        Instruction currentInstruction = instructions.get(currentIndex);
        Label nextLabel;
        do {
            nextLabel = currentInstruction.execute(context);
            lastExecutionCycles += currentInstruction.cycles();

            if (nextLabel == FixedLabel.EMPTY) {
                currentIndex++;
                if (currentIndex < len) currentInstruction = instructions.get(currentIndex);
            } else if (nextLabel != FixedLabel.EXIT) {
                currentInstruction = program.instructionAt(nextLabel);
                currentIndex = instructions.indexOf(currentInstruction);
            }
        } while (nextLabel != FixedLabel.EXIT && currentIndex < len);

        return context.getVariableValue(Variable.RESULT);
    }

    @Override
    public Map<Variable, Long> variableState() {
        return Map.of();
    }

    @Override
    public int getLastExecutionCycles() {
        return lastExecutionCycles;
    }
}
