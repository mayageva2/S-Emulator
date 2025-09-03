package emulator.logic.instruction.quote;

import java.util.List;

public interface QuoteParser {
    List<String> parseTopLevelArgs(String s);
    boolean isNestedCall(String arg);
    NestedCall parseNestedCall(String arg);
    record NestedCall(String name, String argsCsv) {}
}
