package engine.model;

import java.util.List;

public class InstructionBuilder {

    //Formatting helpers
    private static String safe(String s) { return (s == null) ? "" : s.trim(); }

    private static String fmtAssign(String dest, String expr) {
        return String.format("%s <- %s", dest, expr);
    }

    private static String fmtGoto(String targetLabel) {
        return String.format("GOTO %s", targetLabel);
    }

    private static String fmtIf(String condition, String targetLabel) {
        return String.format("IF %s GOTO %s", condition, targetLabel);
    }

    private static String condNeqZero(String v) { return String.format("%s != 0", v); }
    private static String condEqZero(String v)  { return String.format("%s = 0",  v); }
    private static String condEqConst(String v, int k) { return String.format("%s = %d", v, k); }
    private static String condEqVar(String v, String other) { return String.format("%s = %s", v, other); }

    // ========== B A S I C  I N S T R U C T I O N S ========== //

    public static Instruction basic(int id, String label, BOp op, List<String> args) {
        String cmd;
        int cycles;

        switch (op) {
            case NEUTRAL: // v <- v
                cmd = fmtAssign(args.get(0), args.get(0));
                cycles = 0;
                break;
            case INCREASE: // v <- v + 1
                cmd = fmtAssign(args.get(0), args.get(0) + " + 1");
                cycles = 1;
                break;
            case DECREASE: // v <- v - 1
                cmd = fmtAssign(args.get(0), args.get(0) + " - 1");
                cycles = 1;
                break;
            case JUMP_NOT_ZERO: // IF v != 0 GOTO Lk
                cmd = fmtIf(condNeqZero(args.get(0)), args.get(1));
                cycles = 2;
                break;
            default:
                throw new IllegalArgumentException("Unknown op: " + op);
        }
        return new Instruction(id, Instruction.Kind.BASIC, safe(label), cmd, cycles);
    }

    public static Instruction selfAssign(int id, String label, String var) {
        return basic(id, label, BOp.NEUTRAL, List.of(var));
    }

    public static Instruction inc(int id, String label, String var) {
        return basic(id, label, BOp.INCREASE, List.of(var));
    }

    public static Instruction dec(int id, String label, String var) {
        return basic(id, label, BOp.DECREASE, List.of(var));
    }

    public static Instruction ifNzGoto(int id, String label, String var, String targetLabel) {
        return basic(id, label, BOp.JUMP_NOT_ZERO, List.of(var, targetLabel));
    }

    // ========== S Y N T H E T I C   I N S T R U C T I O N S ========== //

    // ZERO_VARIABLE: V <- 0   (cycles = 1)
    public static Instruction zeroVariable(int id, String label, String var) {
        return new Instruction(id, Instruction.Kind.SYNTHETIC, safe(label),
                fmtAssign(var, "0"), 1);
    }

    // GOTO_LABEL: GOTO Lk   (cycles = 1)
    public static Instruction gotoLabel(int id, String label, String targetLabel) {
        return new Instruction(id, Instruction.Kind.SYNTHETIC, safe(label),
                fmtGoto(targetLabel), 1);
    }

    // ASSIGNMENT: V <- V'   (cycles = 4)
    public static Instruction assignment(int id, String label, String destVar, String srcVar) {
        return new Instruction(id, Instruction.Kind.SYNTHETIC, safe(label),
                fmtAssign(destVar, srcVar), 4);
    }

    // CONSTANT_ASSIGNMENT: V <- K   (cycles = 2)
    public static Instruction constantAssignment(int id, String label, String var, int k) {
        return new Instruction(id, Instruction.Kind.SYNTHETIC, safe(label),
                fmtAssign(var, String.valueOf(k)), 2);
    }

    // JUMP_ZERO: IF V = 0 GOTO Lk   (cycles = 2)
    public static Instruction jumpZero(int id, String label, String var, String targetLabel) {
        return new Instruction(id, Instruction.Kind.SYNTHETIC, safe(label),
                fmtIf(condEqZero(var), targetLabel), 2);
    }

    // JUMP_EQUAL_CONSTANT: IF V = K GOTO Lk   (cycles = 2)
    public static Instruction jumpEqualConstant(int id, String label, String var, int k, String targetLabel) {
        return new Instruction(id, Instruction.Kind.SYNTHETIC, safe(label),
                fmtIf(condEqConst(var, k), targetLabel), 2);
    }

    // JUMP_EQUAL_VARIABLE: IF V = V' GOTO Lk   (cycles = 2)
    public static Instruction jumpEqualVariable(int id, String label, String var, String otherVar, String targetLabel) {
        return new Instruction(id, Instruction.Kind.SYNTHETIC, safe(label),
                fmtIf(condEqVar(var, otherVar), targetLabel), 2);
    }
}
