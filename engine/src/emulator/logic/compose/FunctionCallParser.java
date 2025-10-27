package emulator.logic.compose;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallParser {
    private final String s;
    private int i;

    private FunctionCallParser(String s) { this.s = s; }

    //This func parses an argument list from a string
    public static List<ArgNode> parseArgList(String s) {
        if (s == null || s.isBlank()) return List.of();
        return new FunctionCallParser(s).parseArgsTop();
    }

    //This func parses top level arguments
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

    //This func parses a single expression
    private ArgNode parseExpr() {
        skipWs();
        if (i >= s.length()) throw err("Unexpected end");
        char c = s.charAt(i);
        if (c == '(') return parseCall();
        if (c == '-' || Character.isDigit(c)) return parseConst();
        return parseVarOrBareName();
    }

    //This func parses a function call
    private ArgNode.Call parseCall() {
        expect('(');
        String fname = parseName();
        skipWs();
        if (consumeIf(')')) {
            return new ArgNode.Call(fname, List.of());
        }

        expect(',');
        skipWs();
        java.util.List<ArgNode> args = new java.util.ArrayList<>();
        do {
            args.add(parseExpr());
            skipWs();
        } while (consumeIf(','));
        expect(')');
        return new ArgNode.Call(fname, args);
    }

    //This func parses a numeric constant
    private ArgNode.Const parseConst() {
        int start = i; if (s.charAt(i) == '-') i++;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        long v = Long.parseLong(s.substring(start, i));
        return new ArgNode.Const(v);
    }

    //This func parses a variable or bare function name
    private ArgNode.Var parseVarOrBareName() {
        String name = parseName();
        return new ArgNode.Var(name);
    }

    //This func parses a name
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

    // helpers
    private void expect(char ch){ if(i>=s.length()||s.charAt(i)!=ch) throw err("Expected '"+ch+"'"); i++; skipWs(); }
    private boolean consumeIf(char ch){ if(i<s.length()&&s.charAt(i)==ch){ i++; skipWs(); return true; } return false; }
    private char peek(){ return i < s.length() ? s.charAt(i) : '\0'; }
    private void skipWs(){ while(i<s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private IllegalArgumentException err(String m){ return new IllegalArgumentException(m+" at pos "+i+" in: "+s); }
}
