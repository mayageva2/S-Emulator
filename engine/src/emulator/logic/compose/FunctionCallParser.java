package emulator.logic.compose;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallParser {
    private final String s;
    private int i;

    private FunctionCallParser(String s) { this.s = s; }

    public static List<ArgNode> parseArgList(String s) {
        if (s == null || s.isBlank()) return List.of();
        return new FunctionCallParser(s).parseArgsTop();
    }

    private List<ArgNode> parseArgsTop() {
        skipWs();
        List<ArgNode> out = new ArrayList<>();
        while (i < s.length()) {
            out.add(parseExpr());
            skipWs();
            if (i < s.length() && s.charAt(i) == ',') { i++; skipWs(); continue; }
            break;
        }
        return out;
    }

    private ArgNode parseExpr() {
        skipWs();
        if (i >= s.length()) throw err("Unexpected end");
        char c = s.charAt(i);
        if (c == '(') return parseCall();
        if (c == '-' || Character.isDigit(c)) return parseConst();
        return parseVarOrBareName();
    }

    private ArgNode.Call parseCall() {
        expect('(');
        String fname = parseName();
        expectCommaOrClose();
        List<ArgNode> args = new ArrayList<>();
        skipWs();
        if (peek() != ')') {
            do { args.add(parseExpr()); skipWs(); } while (consumeIf(','));
        }
        expect(')');
        return new ArgNode.Call(fname, args);
    }

    private ArgNode.Const parseConst() {
        int start = i; if (s.charAt(i) == '-') i++;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        long v = Long.parseLong(s.substring(start, i));
        return new ArgNode.Const(v);
    }

    private ArgNode.Var parseVarOrBareName() {
        String name = parseName();
        return new ArgNode.Var(name);
    }

    private String parseName() {
        skipWs(); int start = i;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (ch=='('||ch==')'||ch==','||Character.isWhitespace(ch)) break;
            i++;
        }
        if (i == start) throw err("Expected name");
        return s.substring(start, i).trim();
    }

    private void expect(char ch){ if(i>=s.length()||s.charAt(i)!=ch) throw err("Expected '"+ch+"'"); i++; skipWs(); }
    private boolean consumeIf(char ch){ if(i<s.length()&&s.charAt(i)==ch){ i++; skipWs(); return true; } return false; }
    private void expectCommaOrClose(){ skipWs(); if(peek()==','||peek()==')') return; throw err("Expected ',' or ')'"); }
    private char peek(){ return i < s.length() ? s.charAt(i) : '\0'; }
    private void skipWs(){ while(i<s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private IllegalArgumentException err(String m){ return new IllegalArgumentException(m+" at pos "+i+" in: "+s); }
}
