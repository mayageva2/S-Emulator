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

    public boolean isNestedCall(String arg) {
        if (arg == null) return false;
        String s = arg.trim();
        if (s.isEmpty()) return false;

        int open = s.indexOf('(');
        int close = s.lastIndexOf(')');
        if (open > 0 && close == s.length()-1 && close > open) {
            return true;
        }
        if (s.charAt(0) == '(' && s.charAt(s.length()-1) == ')') {
            int comma = findFirstTopLevelComma(s.substring(1, s.length()-1));
            return comma > 0;
        }
        return false;
    }

    @Override
    public NestedCall parseNestedCall(String arg) {
        if (arg == null) throw new IllegalArgumentException("null arg");
        String s = arg.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty arg");

        if (s.charAt(0) == '(' && s.charAt(s.length()-1) == ')') {
            String inner = s.substring(1, s.length()-1).trim();
            int firstComma = findFirstTopLevelComma(inner);
            if (firstComma < 0) return new NestedCall(inner.trim(), "");
            String name = inner.substring(0, firstComma).trim();
            String argsCsv = inner.substring(firstComma + 1).trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Missing function name in nested call: " + arg);
            return new NestedCall(name, argsCsv);
        }

        int open = s.indexOf('(');
        int close = s.lastIndexOf(')');
        if (open < 0 || close != s.length()-1 || open == 0) {
            throw new IllegalArgumentException("Not a nested call: " + arg);
        }
        String name = s.substring(0, open).trim();
        String argsCsv = s.substring(open + 1, close).trim();
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