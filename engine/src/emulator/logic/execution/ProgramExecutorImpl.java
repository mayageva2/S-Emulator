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

    //This func executes the loaded program with the given inputs
    @Override
    public long run(Long... input) {
        lastExecutionCycles = 0;

        List<Instruction> instructions = program.getInstructions();
        validateNotEmpty(instructions);

        int need = requiredInputCount();
        long[] finalInputs = normalizeInputs(input, need);
        seedVariables(finalInputs);

        executeProgram(instructions);

        return context.getVariableValue(Variable.RESULT);
    }

    // ---- helpers ----

    //This func ensures the instruction list is not empty
    private void validateNotEmpty(List<Instruction> instructions) {
        if (instructions.isEmpty()) {
            throw new IllegalStateException("empty program");
        }
    }

    //This function normalizes inputs
    private long[] normalizeInputs(Long[] input, int need) {
        int provided = (input == null) ? 0 : input.length;
        long[] finalInputs = new long[Math.max(need, 0)];
        int copyLen = Math.min(provided, need);
        for (int i = 0; i < copyLen; i++) {
            Long val = input[i];
            finalInputs[i] = (val != null) ? val : 0L;
        }
        return finalInputs;
    }

    //This func initializes all program variables
    private void seedVariables(long[] inputs) {
        for (Variable v : program.getVariables()) {
            switch (v.getType()) {
                case INPUT -> {
                    int idx = v.getNumber() - 1;
                    long val = (idx >= 0 && idx < inputs.length) ? inputs[idx] : 0L;
                    context.updateVariable(v, val);
                }
                case WORK, RESULT -> context.updateVariable(v, 0L); //default 0 value
            }
        }
    }

    //This func executes the program’s instructions
    private void executeProgram(List<Instruction> instructions) {
        int idx = 0;
        int len = instructions.size();

        while (idx >= 0 && idx < len) {
            idx = step(instructions, idx);
        }
    }

    //This func executes a single instruction and returns the next instruction index
    private int step(List<Instruction> instructions, int currentIndex) {
        Instruction ins = instructions.get(currentIndex);
        Label next = ins.execute(context);
        lastExecutionCycles += ins.cycles();

        if (next == FixedLabel.EXIT) {
            return instructions.size(); // exit loop
        }
        if (next == FixedLabel.EMPTY) {
            return currentIndex + 1;
        }

        Instruction target = program.instructionAt(next);
        return instructions.indexOf(target);
    }

    //This func returns the variable's state
    @Override
    public Map<Variable, Long> variableState() {
        return context.getAllVariables();
    }

    //This func returns the last cycle
    @Override
    public int getLastExecutionCycles() {
        return lastExecutionCycles;
    }

    //returns the highest input variable index
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
