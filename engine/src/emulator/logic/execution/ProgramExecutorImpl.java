package emulator.logic.execution;

import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.JumpEqualFunctionInstruction;
import emulator.logic.instruction.quote.QuotationInstruction;
import emulator.logic.instruction.quote.QuoteUtils;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableType;

import java.util.*;

public class ProgramExecutorImpl implements ProgramExecutor{

    private final ExecutionContext context = new ExecutionContextImpl();
    private final QuoteEvaluator quoteEval;
    private final Program program;
    private int lastExecutionCycles = 0;
    private int lastDynamicCycles = 0;
    private Long[] lastInputs = new Long[0];
    private int observedDynamicCycles = 0;
    private StepListener stepListener;
    private int baseCycles = 0; // base offset from previous nested executions

    public void setBaseCycles(int base) {
        this.baseCycles = base;
    }

    public int getBaseCycles() {
        return baseCycles;
    }

    public ProgramExecutorImpl(Program program) {
        this.program = Objects.requireNonNull(program, "program must not be null");
        this.quoteEval = null;
    }

    public ProgramExecutorImpl(Program program, QuoteEvaluator quoteEval) {
        this.program = Objects.requireNonNull(program, "program must not be null");
        this.quoteEval = quoteEval;
    }

    @Override public void setStepListener(StepListener l) { this.stepListener = l; }

    //This func executes the loaded program with the given inputs
    @Override
    public long run(Long... input) {
        lastExecutionCycles = 0;
        lastDynamicCycles = 0;
        observedDynamicCycles = 0;

        if (quoteEval != null) {
            context.setQuoteEvaluator(quoteEval);
        }
        this.lastInputs = (input == null) ? new Long[0] : Arrays.copyOf(input, input.length);

        List<Instruction> instructions = program.getInstructions();
        validateNotEmpty(instructions);

        int need = Math.max(requiredInputCount(), (input != null ? input.length : 0));
        long[] finalInputs = normalizeInputs(input, need);
        seedVariables(finalInputs);

        System.out.println("current program is" + program.getName());
        executeProgram(instructions);
//        lastExecutionCycles += baseCycles;
        lastDynamicCycles = QuoteUtils.drainCycles();
        return context.getVariableValue(Variable.RESULT);
    }

    // ---- helpers ----

    private Map<String,String> snapshotVarsForDebug() {
        Map<String,String> out = new LinkedHashMap<>();
        for (var e : variableState().entrySet()) {
            String name = (e.getKey() == null) ? "" : e.getKey().getRepresentation();
            if (name != null && !name.isBlank()) {
                out.put(name, String.valueOf(e.getValue()));
            }
        }
        return out;
    }

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
        System.out.println("STEP " + currentIndex + " " + ins + " Vars=" + context.getAllVariables());
        lastExecutionCycles += ins.cycles();
        System.out.println("total cycles in " + ins + " is " + (QuoteUtils.getCurrentCycles() + lastExecutionCycles));
        int dynamicIncrement = 0;
        if (context instanceof ExecutionContextImpl ectx) {
            dynamicIncrement = ectx.drainDynamicCycles();
            if (dynamicIncrement != 0) {
                observedDynamicCycles += dynamicIncrement;
            }
        }

        int nextIndex;
        if (isExit(next)) {
            nextIndex = instructions.size();
        } else if (isEmpty(next)) {
            nextIndex = currentIndex + 1;
        } else {
            Instruction target = program.instructionAt(next);
            nextIndex = instructions.indexOf(target);
        }

        boolean finished = (nextIndex < 0 || nextIndex >= instructions.size());
        if (stepListener != null) {
            stepListener.onStep(nextIndex, QuoteUtils.getCurrentCycles() + lastExecutionCycles, snapshotVarsForDebug(), finished);
        }
        return nextIndex;
    }

    public int getLastDynamicCycles() {
        return lastDynamicCycles;
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

    //This func checks if label is EXIT
    private static boolean isExit(Label l) {
        if (l == null) return false;
        if (l == FixedLabel.EXIT) return true; // fast-path for your singleton
        String s = l.getLabelRepresentation();
        return s != null && s.trim().equalsIgnoreCase("EXIT");
    }

    //This func checks if label is EMPTY
    private static boolean isEmpty(Label l) {
        if (l == null) return true;
        if (l == FixedLabel.EMPTY) return true;
        String s = l.getLabelRepresentation();
        return s == null || s.trim().isEmpty();
    }

    private Map<String, Long> buildCurrentEnvMapLowerCase() {
        Map<String, Long> env = new HashMap<>();
        for (int i = 0; i < lastInputs.length; i++) {
            env.put(("x" + (i + 1)).toLowerCase(Locale.ROOT), lastInputs[i]);
        }
        variableState().forEach((var, val) ->
                env.put(var.getRepresentation().toLowerCase(Locale.ROOT), val)
        );
        return env;
    }
}
