package emulator.logic.instruction.quote;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.ExecutionContextImpl;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.execution.QuoteEvaluator;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

import java.util.*;

public final class QuoteUtils {
    private QuoteUtils() {}

    public static long runQuotedEval(String fname,
                                     String argsCsv,
                                     ExecutionContext ctx,
                                     QuotationRegistry registry,
                                     QuoteParser parser,
                                     VarResolver varResolver,
                                     QuoteEvaluator quoteEval) {
        return runQuotedEval(fname, argsCsv, ctx, registry, parser, varResolver, quoteEval, Map.of());
    }

    private static long runQuotedEval(String fname,
                                      String argsCsv,
                                      ExecutionContext ctx,
                                      QuotationRegistry registry,
                                      QuoteParser parser,
                                      VarResolver varResolver,
                                      QuoteEvaluator quoteEval,
                                      Map<String, Long> outerEnv) {

        Program qProgram = registry.getProgramByName(fname);
        if (qProgram == null) throw new IllegalArgumentException("Unknown function: " + fname);

        List<String> argTokens = parser.parseTopLevelArgs(argsCsv);
        List<String> paramNames = qProgram.getVariables().stream()
                .filter(v -> v.getType() == emulator.logic.variable.VariableType.INPUT)
                .sorted(Comparator.comparingInt(v -> v.getNumber()))
                .map(v -> "x" + v.getNumber())
                .toList();

        Long[] inputs = new Long[paramNames.size()];
        Arrays.fill(inputs, 0L);

        for (int i = 0; i < argTokens.size() && i < inputs.length; i++) {
            String token = argTokens.get(i).trim();
            long val;

            // nested call
            if (parser.isNestedCall(token)) {
                QuoteParser.NestedCall nc = parser.parseNestedCall(token);
                val = runQuotedEval(nc.name(), nc.argsCsv(), ctx, registry, parser, varResolver, quoteEval, outerEnv);
            } else if (outerEnv.containsKey(token)) {
                val = outerEnv.get(token);
            } else {
                val = evalArgToValue(token, ctx, parser, registry, varResolver, quoteEval);
            }

            inputs[i] = val;
        }

        Map<String, Long> innerEnv = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.size(); i++) {
            innerEnv.put(paramNames.get(i), inputs[i]);
        }

        ProgramExecutorImpl exec = (quoteEval == null)
                ? new ProgramExecutorImpl(qProgram)
                : new ProgramExecutorImpl(qProgram, quoteEval);

        long result = exec.run(inputs);
        int totalCycles = exec.getLastExecutionCycles() + exec.getLastDynamicCycles();
        registerQuoteCycles(ctx, totalCycles);

        return result;
    }

    public static Long evalArgToValue(
            String token,
            ExecutionContext ctx,
            QuoteParser parser,
            QuotationRegistry registry,
            VarResolver varResolver,
            QuoteEvaluator quoteEval
    ) {
        token = (token == null) ? "" : token.trim();
        if (token.isEmpty()) return 0L;

        if (parser.isNestedCall(token)) {
            QuoteParser.NestedCall nc = parser.parseNestedCall(token);
            return runQuotedEval(nc.name(), nc.argsCsv(), ctx, registry, parser, varResolver, quoteEval);
        }

        Long byName = valueFromContextByName(ctx, token);
        if (byName != null) return byName;

        if (varResolver != null) {
            try {
                Variable v = varResolver.resolve(token);
                return ctx.getVariableValue(v);
            } catch (RuntimeException ignored) {}
        }

        if (isFunctionCall(token, registry)) {
            return runQuotedEval(token.trim(), "", ctx, registry, parser, varResolver, quoteEval);
        }

        try {
            return Long.parseLong(token);
        } catch (NumberFormatException ignored) {}

        return 0L;
    }

    private static Long valueFromContextByName(ExecutionContext ctx, String name) {
        if (ctx == null || name == null || name.isBlank()) return null;
        String wanted = name.trim();
        for (var e : ctx.getAllVariables().entrySet()) {
            Variable var = e.getKey();
            if (var == null) continue;
            String rep = var.getRepresentation();
            if (rep != null && rep.equalsIgnoreCase(wanted)) {
                return e.getValue();
            }
        }
        return null;
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

    private static boolean looksLikeVariable(String token) {
        if (token == null) return false;
        String t = token.trim().toUpperCase(Locale.ROOT);
        if (t.equals("Y")) return true;
        return t.matches("[XZ][0-9]+");
    }

    public static boolean isFunctionCall(String token, QuotationRegistry registry) {
        if (token == null || token.isBlank()) return false;
        return registry.getProgramByName(token.trim().toUpperCase(Locale.ROOT)) != null;
    }

    private static void registerQuoteCycles(ExecutionContext ctx, int cycles) {
        if (cycles <= 0) return;
        addCycles(cycles);
        if (ctx instanceof ExecutionContextImpl ectx) {
            ectx.addDynamicCycles(cycles);
        }
    }

    public static void addCycles(int n) {
        QuoteCycles.add(n);
    }

    public static int getCurrentCycles() {
        return QuoteCycles.get();
    }

    public static int drainCycles() {
        return QuoteCycles.drain();
    }

    private static final class QuoteCycles {
        private static final ThreadLocal<Integer> cycles = ThreadLocal.withInitial(() -> 0);
        static void add(int n) { cycles.set(cycles.get() + Math.max(0, n)); }
        static int get() { return cycles.get(); }
        static int drain() { int c = cycles.get(); cycles.set(0); return c; }
    }

    public static boolean isOutputVar(emulator.logic.variable.Variable v) {
        if (v == null) return false;
        return v.getType() == emulator.logic.variable.VariableType.RESULT;
    }

    public static emulator.logic.variable.Variable tryResolveVariableByName(String name) {
        if (name == null || name.isBlank()) return null;
        String t = name.trim();
        if ("y".equalsIgnoreCase(t)) {
            return new emulator.logic.variable.VariableImpl(
                    emulator.logic.variable.VariableType.RESULT, 0);
        }
        if (t.length() >= 2 && (t.charAt(0) == 'x' || t.charAt(0) == 'z')) {
            char kind = t.charAt(0);
            try {
                int idx = Integer.parseInt(t.substring(1));
                if (idx <= 0) return null;
                return new emulator.logic.variable.VariableImpl(
                        (kind == 'x')
                                ? emulator.logic.variable.VariableType.INPUT
                                : emulator.logic.variable.VariableType.WORK,
                        idx);
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    public static long safeParseLong(String s) {
        if (s == null) return 0L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
