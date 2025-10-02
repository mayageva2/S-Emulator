package emulator.logic.instruction.quote;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.ExecutionContextImpl;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.execution.QuoteEvaluator;
import emulator.logic.instruction.InstructionData;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class QuoteUtils {
    private QuoteUtils() {}

    public static final ThreadLocal<AtomicInteger> dynamicCycles = ThreadLocal.withInitial(AtomicInteger::new);
    public static void resetCycles() {dynamicCycles.get().set(0);}
    public static void addCycles(int n) {
        if (n > 0){
            int after = dynamicCycles.get().addAndGet(n);
            System.out.println("[ADD] +" + n + " => " + after + " on " + Thread.currentThread().getName());
        }
    }
    public static int getCurrentCycles() {
        return dynamicCycles.get().get();
    }
    public static int drainCycles() {
        int val = dynamicCycles.get().get();
        dynamicCycles.get().set(0);
        System.out.println("[DRAIN] " + val + " on " + Thread.currentThread().getName());
        return val;
    }


    public static ExecutionContext newScratchCtx() {
        return new ExecutionContext() {
            private final Map<Variable, Long> m = new HashMap<>();
            private QuoteEvaluator quoteEval;
            @Override public long getVariableValue(Variable v) { return m.getOrDefault(v, 0L); }
            @Override public void updateVariable(Variable v, long value) { m.put(v, value); }
            @Override public Map<Variable, Long> getAllVariables() { return m; }
            @Override public void setQuoteEvaluator(QuoteEvaluator evaluator) {this.quoteEval = evaluator;}
            @Override public QuoteEvaluator getQuoteEvaluator() {return this.quoteEval;}
        };
    }

    public static long runQuotedEval(String fname,
                                     String argsCsv,
                                     ExecutionContext ctx,
                                     QuotationRegistry registry,
                                     QuoteParser parser,
                                     VarResolver varResolver,
                                     QuoteEvaluator quoteEval) {
        Program qProgram = registry.getProgramByName(fname);
        if (qProgram == null) {
            throw new IllegalArgumentException("Unknown function: " + fname);
        }
        List<String> topArgs = parser.parseTopLevelArgs(argsCsv);

        Long[] inputs = new Long[requiredInputCount(qProgram)];
        Arrays.fill(inputs, 0L);

        int i = 0;
        for (String arg : topArgs) {
            if (i >= inputs.length) break;
            if (parser.isNestedCall(arg)) {
                QuoteParser.NestedCall nc = parser.parseNestedCall(arg);
                long val = runQuotedEval(nc.name(), nc.argsCsv(), ctx, registry, parser, varResolver, quoteEval);
                inputs[i] = val;
            } else if (looksLikeVariable(arg)) {
                inputs[i] = evalArgToValue(arg, ctx, parser, registry, varResolver, quoteEval);
            } else if (isFunctionCall(arg, registry)) {
                long val = runQuotedEval(arg.trim(), "", ctx, registry, parser, varResolver, quoteEval);
                inputs[i] = val;
            } else {
                inputs[i] = evalArgToValue(arg, ctx, parser, registry, varResolver, quoteEval);
            }
            i++;
        }

        ProgramExecutorImpl exec = (quoteEval == null) ? new ProgramExecutorImpl(qProgram) : new ProgramExecutorImpl(qProgram, quoteEval);
        long resultY = exec.run(inputs);
        QuoteUtils.addCycles(exec.getLastExecutionCycles());

        return resultY;
    }

    private static boolean looksLikeVariable(String token) {
        if (token == null) return false;
        String t = token.trim().toUpperCase(Locale.ROOT);
        if (t.equals("Y")) return true;
        if (t.matches("[XZ][0-9]+")) return true;
        return false;
    }

    private static boolean isFunctionCall(String token, QuotationRegistry registry) {
        if (token == null || token.isBlank()) return false;
        return registry.getProgramByName(token.trim().toUpperCase(Locale.ROOT)) != null;
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
            System.out.println("EVAL nested call: " + nc.name() + "(" + nc.argsCsv() + ")");
            String fname = normalizeFunctionName(nc.name(), registry);

            Program qProgram = registry.getProgramByName(fname);
            if (qProgram == null) {
                throw new IllegalArgumentException("Unknown function: " + fname + " (from: " + nc.name() + ")");
            }

            List<String> args = parser.parseTopLevelArgs(nc.argsCsv());
            int need = requiredInputCount(qProgram);

            Long[] inputs = new Long[Math.max(need, 0)];
            Arrays.fill(inputs, 0L);
            for (int i = 0; i < Math.min(need, args.size()); i++) {
                inputs[i] = evalArgToValue(args.get(i), ctx, parser, registry, varResolver, quoteEval);
            }

            ProgramExecutorImpl exec = (quoteEval == null)
                    ? new ProgramExecutorImpl(qProgram)
                    : new ProgramExecutorImpl(qProgram, quoteEval);

            long resultY = exec.run(inputs);
            int argCycles = exec.getLastExecutionCycles();
            int quoteBase = emulator.logic.instruction.InstructionData.QUOTATION.getCycles();
            QuoteUtils.addCycles(argCycles + quoteBase);
            if (ctx instanceof ExecutionContextImpl ectx) {
                ectx.addDynamicCycles(argCycles + quoteBase);
            }

            return resultY;
        } else {
            if (varResolver != null) {
                try {
                    Variable v = varResolver.resolve(token);
                    return ctx.getVariableValue(v);
                } catch (Exception ignored) {
                }
            }
            Long num = tryParseLong(token);
            if (num != null) return num;

            if (token.matches("[xX][1-9]\\d*") || token.equalsIgnoreCase("y") || token.matches("[zZ][1-9]\\d*")) {
                return 0L;
            }

            throw new IllegalArgumentException("Unrecognized token in QUOTE args: " + token);
        }
    }

    private static String normalizeFunctionName(String name, QuotationRegistry registry) {
        if (name == null) return null;
        String n = name.trim();
        if (n.isEmpty()) return n;

        Program p = registry.getProgramByName(n);
        if (p != null) return p.getName();
        Program p2 = registry.getProgramByName(n.toUpperCase(java.util.Locale.ROOT));
        if (p2 != null) return p2.getName();

        return n;
    }

    private static Long tryParseLong(String s) {
        try { return Long.parseLong(s.trim()); }
        catch (Exception e) { return null; }
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
