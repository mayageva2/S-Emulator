package emulator.logic.compose;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Composer {

    public interface ProgramInvoker {
        List<Long> run(String functionName, List<Long> inputs);
        Map<String, Long> currentEnv();
    }

    //This func parses and evaluates a function call
    public static List<Long> evaluateArgs(String functionName, String functionArguments, ProgramInvoker invoker) {
        List<ArgNode> exprs = FunctionCallParser.parseArgList(functionArguments);
        return evaluateCall(new ArgNode.Call(functionName, exprs), invoker);
    }

    //This func evaluates a Call node
    public static List<Long> evaluateCall(ArgNode.Call call, ProgramInvoker invoker) {
        List<Long> flatArgs = new ArrayList<>();
        for (ArgNode e : call.args()) flatArgs.addAll(evaluate(e, invoker));
        return invoker.run(call.function(), flatArgs);
    }

    //This func recursively evaluates a single ArgNode
    private static List<Long> evaluate(ArgNode e, ProgramInvoker invoker) {
        if (e instanceof ArgNode.Const c) return List.of(c.value());
        if (e instanceof ArgNode.Var v) {
            String key = v.name().toLowerCase(Locale.ROOT);
            Long val = lookup(invoker.currentEnv(), key);
            if (val == null) throw new IllegalArgumentException("Unknown variable: " + v.name());
            return List.of(val);
        }
        if (e instanceof ArgNode.Call c) return evaluateCall(c, invoker);
        throw new IllegalStateException("Unknown node: " + e);
    }

    //This func searches for a variable name in the environment
    private static Long lookup(Map<String, Long> env, String keyLower) {
        if (env.containsKey(keyLower)) return env.get(keyLower);
        for (var en : env.entrySet()) if (en.getKey().equalsIgnoreCase(keyLower)) return en.getValue();
        return null;
    }
}
