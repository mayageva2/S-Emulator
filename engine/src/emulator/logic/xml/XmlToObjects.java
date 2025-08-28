package emulator.logic.xml;

import emulator.exception.InvalidInstructionException;
import emulator.exception.XmlInvalidContentException;
import emulator.exception.XmlReadException;
import emulator.logic.instruction.*;
import emulator.logic.label.*;
import emulator.logic.program.*;
import emulator.logic.variable.*;

import java.util.*;

public final class XmlToObjects {
    private XmlToObjects(){}

    public static Program toProgram(ProgramXml pxml) {
        ProgramImpl program = new ProgramImpl(pxml.getName());

        int idx = 0;
        for (InstructionXml ix : pxml.getInstructions().getInstructions()) {
            idx++;
            program.addInstruction(toInstruction(ix, idx));
        }
        return program;
    }

    private static Instruction toInstruction(InstructionXml ix, int index) {
        String name = safe(ix.getName());
        String opcode = name.isEmpty() ? "<unknown>" : name.trim().toUpperCase(Locale.ROOT);

        String varName = safe(ix.getVariable());
        Variable v = varName.isEmpty() ? null : parseVariable(varName, opcode, index);
        Label lbl = toLabelObj(ix.getLabel(), opcode, index);

        Map<String,String> args = toArgMap(ix);

        Instruction ins = switch (opcode) {
            case "NEUTRAL" -> new NeutralInstruction(v, lbl);
            case "INCREASE" -> new IncreaseInstruction(v, lbl);
            case "DECREASE" -> new DecreaseInstruction(v, lbl);
            case "ZERO_VARIABLE" -> new ZeroVariableInstruction(v, lbl);

            case "JUMP_NOT_ZERO" -> {
                Label target = parseJumpLabel(req(args, "JNZLABEL", opcode, index), opcode, index);
                yield new JumpNotZeroInstruction(v, target, lbl);
            }

            case "ASSIGNMENT" -> {
                Variable src = parseVariable(req(args, "ASSIGNEDVARIABLE", opcode, index), opcode, index);
                yield new AssignmentInstruction(v, src, lbl);
            }

            case "CONSTANT_ASSIGNMENT" -> {
                long k = parseNonNegInt(req(args, "CONSTANTVALUE", opcode, index), opcode, index);
                yield new ConstantAssignmentInstruction(v, k, lbl);
            }

            case "GOTO_LABEL" -> {
                Label target = parseJumpLabel(req(args, "GOTOLABEL", opcode, index), opcode, index);
                yield new GoToLabelInstruction(lbl, target);
            }

            case "JUMP_ZERO" -> {
                Label target = parseJumpLabel(req(args, "JZLABEL", opcode, index), opcode, index);
                yield new JumpZeroInstruction(v, target, lbl);
            }

            case "JUMP_EQUAL_CONSTANT" -> {
                Label target = parseJumpLabel(req(args, "JECONSTANTLABEL", opcode, index), opcode, index);
                long k = parseNonNegInt(req(args, "CONSTANTVALUE", opcode, index), opcode, index);
                yield new JumpEqualConstantInstruction(v, k, target, lbl);
            }

            case "JUMP_EQUAL_VARIABLE" -> {
                Label target = parseJumpLabel(req(args, "JEVARIABLELABEL", opcode, index), opcode, index);
                Variable other = parseVariable(req(args, "VARIABLENAME", opcode, index), opcode, index);
                yield new JumpEqualVariableInstruction(v, other, target, lbl);
            }
            /*
            case "QUOTE" -> {
                String fn = req(args, "FUNCTIONNAME", opcode, index);
                String fnArgs = req(args, "FUNCTIONARGUMENTS", opcode, index);
                yield new QuotationInstruction(v, fn, fnArgs, attached);
            }
            case "JUMP_EQUAL_FUNCTION" -> {
                Label target = parseJumpLabel(req(args, "JEFUNCTIONLABEL", opcode, index), opcode, index);
                String fn = req(args, "FUNCTIONNAME", opcode, index);
                String fnArgs = req(args, "FUNCTIONARGUMENTS", opcode, index);
                yield new JumpEqualFunctionInstruction(v, fn, fnArgs, target);
            }*/
            default -> throw new InvalidInstructionException(opcode, "Unsupported or invalid instruction", index);
        };

        if (ix.getArguments() != null && ins instanceof AbstractInstruction ai) {
            for (InstructionArgXml arg : ix.getArguments()) {
                ai.setArgument(arg.getName(), arg.getValue());
            }
        }

        return ins;
    }

    private static Map<String,String> toArgMap(InstructionXml ix) {
        Map<String,String> m = new LinkedHashMap<>();
        if (ix.getArguments() != null) {
            for (InstructionArgXml a : ix.getArguments()) {
                if (a.getName() != null) {
                    String key = a.getName().trim().toUpperCase(Locale.ROOT);
                    m.put(key, safe(a.getValue()));
                }
            }
        }
        return m;
    }

    private static String req(Map<String,String> m, String keyUpper, String opcode, int index) {
        String v = m.get(keyUpper);
        if (v == null || v.isBlank()) {
            throw new InvalidInstructionException(
                    opcode,
                    "Missing required argument: '" + keyUpper + "'",
                    index
            );
        }
        return v.trim();
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static boolean isAllDigits(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static Variable parseVariable(String s, String opcode, int index) {
        if (s == null || s.isBlank()) {
            throw new InvalidInstructionException(opcode, "Missing variable", index);
        }
        s = s.trim();
        String su = s.toUpperCase(Locale.ROOT);

        if (su.equals("Y")) {
            return Variable.RESULT;
        }

        if (su.length() >= 2) {
            char kind = su.charAt(0);
            String numPart = su.substring(1);
            if ((kind == 'X' || kind == 'Z') && isAllDigits(numPart)) {
                int n = Integer.parseInt(numPart);
                if (n >= 1) {
                    VariableType t = (kind == 'X') ? VariableType.INPUT : VariableType.WORK;
                    return new VariableImpl(t, n);
                }
            }
        }
        throw new InvalidInstructionException(opcode, "Illegal variable: " + s, index);
    }

    private static long parseNonNegInt(String s, String opcode, int index) {
        if (s == null) {
            throw new InvalidInstructionException(opcode, "Missing integer value", index);
        }
        s = s.trim();
        if (s.isEmpty() || !isAllDigits(s)) {
            throw new InvalidInstructionException(opcode, "Not a non-negative integer: " + s, index);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new InvalidInstructionException(opcode, "Not a non-negative integer: " + s, index);
        }
    }

    private static Label parseJumpLabel(String s, String opcode, int index) {
        if (s == null || s.isBlank()) {
            throw new InvalidInstructionException(opcode, "Missing label", index);
        }
        s = s.trim();
        String su = s.toUpperCase(Locale.ROOT);

        if (su.equals("EXIT")) return FixedLabel.EXIT;

        if (su.length() >= 2 && su.charAt(0) == 'L') {
            String numPart = su.substring(1);
            if (isAllDigits(numPart)) {
                int n = Integer.parseInt(numPart);
                if (n >= 1) return new LabelImpl(n);
            }
        }
        throw new InvalidInstructionException(opcode, "Illegal label: " + s, index);
    }

    private static Label toLabelObj(String s, String opcode, int index) {
        if (s == null || s.isBlank()) return FixedLabel.EMPTY;
        s = s.trim();
        String su = s.toUpperCase(Locale.ROOT);

        if (su.equals("EXIT")) return FixedLabel.EXIT;

        if (su.length() >= 2 && su.charAt(0) == 'L') {
            String numPart = su.substring(1);
            if (isAllDigits(numPart)) {
                int n = Integer.parseInt(numPart);
                if (n >= 1) return new LabelImpl(n);
            }
        }
        throw new InvalidInstructionException(opcode, "Illegal label on instruction: " + s, index);
    }
}
