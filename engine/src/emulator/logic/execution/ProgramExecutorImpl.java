package emulator.logic.execution;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableType;

import java.util.*;

public class ProgramExecutorImpl implements ProgramExecutor{

    private final ExecutionContext context = new ExecutionContextImpl();
    private final Program program;
    private int lastExecutionCycles = 0;

    public ProgramExecutorImpl(Program program) {
        this.program = Objects.requireNonNull(program, "program must not be null");
    }

    @Override
    public long run(Long... input) {
        lastExecutionCycles = 0;
        List<Instruction> instructions = program.getInstructions();

        final int len = instructions.size();
        if (len == 0) throw new IllegalStateException("empty program");

        int need = requiredInputCount();
        if (input.length < need) {
            throw new IllegalArgumentException(
                    "Not enough input values: got " + input.length + ", need " + need + " (for x1..x" + need + ")"
            );
        }

        for (Variable v : program.getVariables()) {
            switch (v.getType()) {
                case INPUT -> {
                    int idx = v.getNumber() - 1;
                    long val = (idx >= 0 && idx < input.length) ? input[idx] : 0L;
                    context.updateVariable(v, val);
                }
                case WORK, RESULT -> context.updateVariable(v, 0L);
            }
        }

        int currentIndex = 0;
        while (currentIndex >= 0 && currentIndex < len) {
            Instruction currentInstruction = instructions.get(currentIndex);
            Label nextLabel = currentInstruction.execute(context);
            lastExecutionCycles += currentInstruction.cycles();

            if (nextLabel == FixedLabel.EXIT) {
                break;
            } else if (nextLabel == FixedLabel.EMPTY) {
                currentIndex++;
            } else {
                Instruction target = program.instructionAt(nextLabel);
                currentIndex = instructions.indexOf(target);
            }
        }

        return context.getVariableValue(Variable.RESULT);
    }

    @Override
    public Map<Variable, Long> variableState() {
        return context.getAllVariables();
    }

    @Override
    public int getLastExecutionCycles() {
        return lastExecutionCycles;
    }

    private int requiredInputCount() {
        int maxIdx = 0;
        for (Variable v : program.getVariables()) {
            if (v.getType() == VariableType.INPUT) {
                maxIdx = Math.max(maxIdx, v.getNumber());
            }
        }
        return maxIdx;
    }
}
