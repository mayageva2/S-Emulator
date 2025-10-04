package emulator.logic.instruction.quote;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.ExecutionContextImpl;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.execution.QuoteEvaluator;
import emulator.logic.instruction.InstructionData;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class QuoteUtils {
    private QuoteUtils() {}

    public static final ThreadLocal<AtomicInteger> dynamicCycles = ThreadLocal.withInitial(AtomicInteger::new);
    public static void resetCycles() {dynamicCycles.get().set(0);}
    public static void addCycles(int n) {
        if (n > 0){
            int after = dynamicCycles.get().addAndGet(n);
        }
    }
    public static int getCurrentCycles() {
        return dynamicCycles.get().get();
    }
    public static int drainCycles() {
        int val = dynamicCycles.get().get();
        dynamicCycles.get().set(0);
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
        return evalArgWithDetails(token, ctx, parser, registry, varResolver, quoteEval).value();
    }

    private static ResolvedArg evalArgWithDetails(
            String token,
            ExecutionContext ctx,
            QuoteParser parser,
            QuotationRegistry registry,
            VarResolver varResolver,
            QuoteEvaluator quoteEval
    ) {
        String trimmedToken = (token == null) ? "" : token.trim();
        if (trimmedToken.isEmpty()) {
            return new ResolvedArg(trimmedToken, 0L, null, ResolvedArg.Origin.LITERAL_ZERO);
        }

        if (parser.isNestedCall(trimmedToken)) {
            QuoteParser.NestedCall nc = parser.parseNestedCall(trimmedToken);
            String fname = normalizeFunctionName(nc.name(), registry);

            Program qProgram = registry.getProgramByName(fname);
            if (qProgram == null) {
                throw new IllegalArgumentException("Unknown function: " + fname + " (from: " + nc.name() + ")");
            }

            List<String> args = parser.parseTopLevelArgs(nc.argsCsv());
            int need = requiredInputCount(qProgram);

            Long[] inputs = new Long[Math.max(need, 0)];
            Arrays.fill(inputs, 0L);
            List<ResolvedArg> resolvedArgs = new ArrayList<>();
            List<String> displayArgs = new ArrayList<>(args.size());
            for (int i = 0; i < args.size(); i++) {
                String raw = args.get(i);
                displayArgs.add(raw == null ? "" : raw.trim());
            }

            for (int i = 0; i < Math.min(need, args.size()); i++) {
                String argToken = args.get(i);
                ResolvedArg resolved = evalArgWithDetails(argToken, ctx, parser, registry, varResolver, quoteEval);
                inputs[i] = resolved.value();
                resolvedArgs.add(resolved);
            }

            String callSignature = nc.name() + "(" + String.join(",", displayArgs) + ")";
            System.out.println("EVAL nested call: " + callSignature);
            if (!resolvedArgs.isEmpty()) {
                System.out.println("  resolved order: " + buildResolvedOrderLog(resolvedArgs));
                System.out.println("  resolved inputs: " + buildResolvedValuesLog(resolvedArgs));
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

            return new ResolvedArg(trimmedToken, resultY, null, ResolvedArg.Origin.NESTED_CALL);
        }

        if (varResolver != null) {
            try {
                Variable v = varResolver.resolve(trimmedToken);
                long value = ctx.getVariableValue(v);
                return new ResolvedArg(trimmedToken, value, v, ResolvedArg.Origin.RESOLVED_VARIABLE);
            } catch (Exception ignored) {
            }
        }

        ContextValue ctxValue = resolveFromExecutionContextDetailed(trimmedToken, ctx);
        if (ctxValue != null) {
            return new ResolvedArg(trimmedToken, ctxValue.value(), ctxValue.variable(), ResolvedArg.Origin.CONTEXT_LOOKUP);
        }

        Long num = tryParseLong(trimmedToken);
        if (num != null) {
            return new ResolvedArg(trimmedToken, num, null, ResolvedArg.Origin.NUMERIC_LITERAL);
        }

        throw new IllegalArgumentException("Unrecognized token in QUOTE args: " + trimmedToken);
    }

    private static String buildResolvedOrderLog(List<ResolvedArg> resolvedArgs) {
        List<String> orderLog = new ArrayList<>(resolvedArgs.size());
        for (int i = 0; i < resolvedArgs.size(); i++) {
            ResolvedArg resolved = resolvedArgs.get(i);
            String label = resolved.displayName();
            if (!equalsIgnoreCaseSafe(label, resolved.token())) {
                orderLog.add("input" + (i + 1) + "<-" + label + " (token='" + resolved.token() + "')");
            } else {
                orderLog.add("input" + (i + 1) + "<-" + label);
            }
        }
        return String.join(", ", orderLog);
    }

    private static String buildResolvedValuesLog(List<ResolvedArg> resolvedArgs) {
        List<String> valueLog = new ArrayList<>(resolvedArgs.size());
        for (int i = 0; i < resolvedArgs.size(); i++) {
            ResolvedArg resolved = resolvedArgs.get(i);
            String label = resolved.displayName();
            StringBuilder sb = new StringBuilder();
            sb.append("input").append(i + 1).append("=").append(resolved.value());
            if (label != null && !label.isBlank()) {
                sb.append(" (from ").append(label);
                if (!equalsIgnoreCaseSafe(label, resolved.token())) {
                    sb.append(", token='").append(resolved.token()).append("'");
                }
                sb.append(")");
            }
            valueLog.add(sb.toString());
        }
        return String.join(", ", valueLog);
    }

    private static boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static ContextValue resolveFromExecutionContextDetailed(String token, ExecutionContext ctx) {
        if (ctx == null) {
            return null;
        }

        Map<Variable, Long> vars = ctx.getAllVariables();
        if (vars == null || vars.isEmpty()) {
            return null;
        }

        Variable fallback = tryResolveVariableByName(token);
        if (fallback != null) {
            if (vars.containsKey(fallback)) {
                return new ContextValue(fallback, vars.get(fallback));
            }
            // If the concrete instance is not present, fall back to name based lookup below.
        }

        String normalized = token.toLowerCase(Locale.ROOT);
        for (var entry : vars.entrySet()) {
            Variable var = entry.getKey();
            if (var == null) continue;
            String rep = var.getRepresentation();
            if (rep == null) continue;
            if (rep.equalsIgnoreCase(token) || rep.equalsIgnoreCase(normalized)) {
                return new ContextValue(var, entry.getValue());
            }
        }

        return null;
    }

    private record ResolvedArg(String token, long value, Variable variable, Origin origin) {
        enum Origin {
            RESOLVED_VARIABLE,
            CONTEXT_LOOKUP,
            NUMERIC_LITERAL,
            LITERAL_ZERO,
            NESTED_CALL
        }

        String displayName() {
            if (variable != null) {
                String rep = variable.getRepresentation();
                if (rep != null && !rep.isBlank()) {
                    return rep;
                }
            }
            if (token != null && !token.isBlank()) {
                return token;
            }
            return "<value>";
        }
    }

    private record ContextValue(Variable variable, long value) {}

    static Variable tryResolveVariableByName(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.equals("y")) {
            return Variable.RESULT;
        }
        if (normalized.matches("x[1-9]\\d*")) {
            int number = Integer.parseInt(normalized.substring(1));
            return new VariableImpl(VariableType.INPUT, number);
        }
        if (normalized.matches("z[1-9]\\d*")) {
            int number = Integer.parseInt(normalized.substring(1));
            return new VariableImpl(VariableType.WORK, number);
        }
        return null;
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
