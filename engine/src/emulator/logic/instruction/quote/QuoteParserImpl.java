package emulator.logic.instruction.quote;

import java.util.ArrayList;
import java.util.List;

public final class QuoteParserImpl implements QuoteParser {

    @Override
    public List<String> parseTopLevelArgs(String s) {
        if (s == null) return List.of();
        s = s.trim();
        if (s.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
                cur.append(c);
            } else if (c == ')') {
                depth--;
                if (depth < 0) throw new IllegalArgumentException("Unbalanced parentheses in QUOTE args: " + s);
                cur.append(c);
            } else if (c == ',' && depth == 0) {
                String token = cur.toString().trim();
                if (!token.isEmpty()) out.add(token);
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (depth != 0) throw new IllegalArgumentException("Unbalanced parentheses in QUOTE args: " + s);

        String last = cur.toString().trim();
        if (!last.isEmpty()) out.add(last);
        return out;
    }

    @Override
    public boolean isNestedCall(String arg) {
        if (arg == null) return false;
        arg = arg.trim();
        return arg.startsWith("(") && arg.endsWith(")") && arg.length() >= 2;
    }

    @Override
    public NestedCall parseNestedCall(String arg) {
        if (!isNestedCall(arg)) throw new IllegalArgumentException("Not a nested call: " + arg);
        String inner = arg.substring(1, arg.length() - 1).trim();
        if (inner.isEmpty()) throw new IllegalArgumentException("Empty nested call: " + arg);

        int firstComma = findFirstTopLevelComma(inner);
        if (firstComma < 0) {  // no args
            return new NestedCall(inner.trim(), "");
        }
        String name = inner.substring(0, firstComma).trim();
        String argsCsv = inner.substring(firstComma + 1).trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Missing function name in nested call: " + arg);
        return new NestedCall(name, argsCsv);
    }

    private int findFirstTopLevelComma(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) return i;
        }
        return -1;
    }
}