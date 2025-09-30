package emulator.logic.instruction.quote;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class QuoteUtils {
    private QuoteUtils() {}

    private static final ThreadLocal<AtomicInteger> dynamicCycles = ThreadLocal.withInitial(AtomicInteger::new);
    public static void resetCycles() {dynamicCycles.get().set(0);}
    public static void addCycles(int n) {if (n > 0) dynamicCycles.get().addAndGet(n);}
    public static int drainCycles() {
        int val = dynamicCycles.get().getAndSet(0);
        return val;
    }

    public static ExecutionContext newScratchCtx() {
        return new ExecutionContext() {
            private final Map<Variable, Long> m = new HashMap<>();
            @Override public long getVariableValue(Variable v) { return m.getOrDefault(v, 0L); }
            @Override public void updateVariable(Variable v, long value) { m.put(v, value); }
            @Override public Map<Variable, Long> getAllVariables() { return m; }
        };
    }

    public static long runQuotedEval(String fname,
                                     String argsCsv,
                                     ExecutionContext ctx,
                                     QuotationRegistry registry,
                                     QuoteParser parser,
                                     VarResolver varResolver) {
        Program qProgram = registry.getProgramByName(fname);
        int need = requiredInputCount(qProgram);

        List<String> args = parser.parseTopLevelArgs(argsCsv);
        Long[] inputs = new Long[need];
        Arrays.fill(inputs, 0L);

        int copy = Math.min(need, args.size());
        for (int i = 0; i < copy; i++) {
            inputs[i] = evalArgToValue(args.get(i), ctx, parser, registry, varResolver);
        }

        ProgramExecutorImpl exec = new ProgramExecutorImpl(qProgram);
        long resultY = exec.run(inputs);
        addCycles(exec.getLastExecutionCycles());
        return Math.max(resultY, 0);
    }

    public static int requiredInputCount(Program p) {
        int max = 0;
        for (var v : p.getVariables()) {
            if (v.getType() == emulator.logic.variable.VariableType.INPUT) {
                max = Math.max(max, v.getNumber());
            }
        }
        return max;
    }

    public static Long evalArgToValue(String token, ExecutionContext ctx, QuoteParser parser, QuotationRegistry registry, VarResolver varResolver) {
        if (parser.isNestedCall(token)) {
            QuoteParser.NestedCall nc = parser.parseNestedCall(token);
            int before = drainCycles();
            long val = runQuotedEval(nc.name(), nc.argsCsv(), ctx, registry, parser, varResolver);
            int nestedCycles = drainCycles();
            addCycles(nestedCycles);
            addCycles(1);
            return val;
        } else {
            if (varResolver != null) {
                try {
                    Variable v = varResolver.resolve(token);
                    return ctx.getVariableValue(v);
                } catch (Exception ignored) {
                    return safeParseLong(token);
                }
            } else {
                return safeParseLong(token);
            }
        }
    }

    public static long safeParseLong(String s) {
        try { return Long.parseLong(s.trim()); }
        catch (Exception e) { return 0L; }
    }

    public static boolean isOutputVar(Variable v) {
        var t = v.getType();
        if (t == null) return false;
        String n = t.name();
        return "OUTPUT".equals(n) || "RESULT".equals(n) || "Y".equals(n);
    }
}
