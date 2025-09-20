package emulator.logic.execution;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface QuoteEvaluator {
    List<Long> eval(String functionName, String functionArguments, Map<String, Long> env, int degree);
}
