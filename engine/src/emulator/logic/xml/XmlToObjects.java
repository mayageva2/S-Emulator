package emulator.logic.xml;

import emulator.logic.instruction.*;
import emulator.logic.label.*;
import emulator.logic.program.*;
import emulator.logic.variable.*;

import java.util.*;

public final class XmlToObjects {
    private XmlToObjects(){}

    public static Program toProgram(ProgramXml pxml) throws XmlReadException {
        ProgramImpl program = new ProgramImpl(pxml.getName());

        for (InstructionXml ix : pxml.getInstructions().getInstructions()) {
            program.addInstruction(toInstruction(ix));
        }
        return program;
    }

    private static Instruction toInstruction(InstructionXml ix) throws XmlReadException {
        String name = ix.getName();
        String varName = safe(ix.getVariable());
        Variable v = varName.isEmpty() ? null : parseVariable(varName);
        Label lbl = toLabelObj(ix.getLabel());

        Map<String,String> args = toArgMap(ix);
        Instruction ins;

        ins = switch (name) {
            case "NEUTRAL" -> new NeutralInstruction(v, toLabelObj(ix.getLabel()));
            case "INCREASE" -> new IncreaseInstruction(v, toLabelObj(ix.getLabel()));
            case "DECREASE" -> new DecreaseInstruction(v, toLabelObj(ix.getLabel()));
            case "ZERO_VARIABLE" -> new ZeroVariableInstruction(v, toLabelObj(ix.getLabel()));
            case "JUMP_NOT_ZERO" -> {
                Label target = parseJumpLabel(req(args, "JNZLabel"));
                Label attached = toLabelObj(ix.getLabel());
                yield new JumpNotZeroInstruction(v, target, attached);
            }
            case "ASSIGNMENT" -> {
                Variable src = parseVariable(req(args, "assignedVariable"));
                yield new AssignmentInstruction(v, src, toLabelObj(ix.getLabel()));
            }
            case "CONSTANT_ASSIGNMENT" -> {
                long k = parseNonNegInt(req(args, "constantValue"));
                yield new ConstantAssignmentInstruction(v, k, toLabelObj(ix.getLabel()));
            }
            case "GOTO_LABEL" -> {
                Label target = parseJumpLabel(req(args, "gotoLabel"));
                yield new GoToLabelInstruction(target);
            }
            case "JUMP_ZERO" -> {
                Label target = parseJumpLabel(req(args, "JZLabel"));
                yield new JumpZeroInstruction(v, target);
            }
            case "JUMP_EQUAL_CONSTANT" -> {
                Label target = parseJumpLabel(req(args, "JEConstantLabel"));
                long k = parseNonNegInt(req(args, "constantValue"));
                yield new JumpEqualConstantInstruction(v, k, target);
            }
            case "JUMP_EQUAL_VARIABLE" -> {
                Label target = parseJumpLabel(req(args, "JEVariableLabel"));
                Variable other = parseVariable(req(args, "variableName"));
                yield new JumpEqualVariableInstruction(v, other, target);
            }
         /*   case "QUOTE" -> {
                String fn = req(args, "functionName");
                String fnArgs = req(args, "functionArguments");
                yield new QuotationInstruction(v, fn, fnArgs, attached);
            }
            case "JUMP_EQUAL_FUNCTION" -> {
                Label target = parseJumpLabel(req(args, "JEFunctionLabel"));
                String fn = req(args, "functionName");
                String fnArgs = req(args, "functionArguments");
                yield new JumpEqualFunctionInstruction(v, fn, fnArgs, target);
            }*/
            default -> throw new XmlReadException("Unsupported instruction: " + name);
        };

        if (ix.getArguments() != null) {
            for (InstructionArgXml arg : ix.getArguments()) {
                if (ins instanceof AbstractInstruction ai) {
                    ai.setArgument(arg.getName(), arg.getValue());
                }
            }
        }

        return ins;
    }

    private static Map<String,String> toArgMap(InstructionXml ix) {
        Map<String,String> m = new LinkedHashMap<>();
        if (ix.getArguments() != null) {
            for (InstructionArgXml a : ix.getArguments()) {
                if (a.getName() != null) {
                    m.put(a.getName().trim(), safe(a.getValue()));
                }
            }
        }
        return m;
    }

    private static String req(Map<String,String> m, String key) throws XmlReadException {
        String v = m.get(key);
        if (v == null || v.isBlank()) throw new XmlReadException("Missing required argument '" + key + "'");
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

    private static Variable parseVariable(String s) throws XmlReadException {
        if (s == null || s.isBlank()) {
            throw new XmlReadException("Missing variable");
        }
        s = s.trim();

        if (s.equals("y")) {
            return Variable.RESULT;
        }

        if (s.length() >= 2) {
            char kind = s.charAt(0); // 'x' or 'z'
            String numPart = s.substring(1);
            if ((kind == 'x' || kind == 'z') && isAllDigits(numPart)) {
                int n = Integer.parseInt(numPart);
                if (n >= 1) {
                    VariableType t = (kind == 'x') ? VariableType.INPUT : VariableType.WORK;
                    return new VariableImpl(t, n);
                }
            }
        }
        throw new XmlReadException("Illegal variable: " + s);
    }

    private static long parseNonNegInt(String s) throws XmlReadException {
        if (s == null) throw new XmlReadException("Missing integer value");
        s = s.trim();
        if (s.isEmpty() || !isAllDigits(s)) {
            throw new XmlReadException("Not a non-negative integer: " + s);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new XmlReadException("Not a non-negative integer: " + s);
        }
    }

    private static Label parseJumpLabel(String s) throws XmlReadException {
        if (s == null || s.isBlank()) throw new XmlReadException("Missing label");
        s = s.trim();
        if (s.equals("EXIT")) return FixedLabel.EXIT;

        if (s.length() >= 2 && s.charAt(0) == 'L') {
            String numPart = s.substring(1);
            if (isAllDigits(numPart)) {
                int n = Integer.parseInt(numPart);
                if (n >= 1) return new LabelImpl(n);
            }
        }
        throw new XmlReadException("Illegal label: " + s);
    }

    private static Label toLabelObj(String s) throws XmlReadException {
        if (s == null || s.isBlank()) return FixedLabel.EMPTY;
        s = s.trim();
        if (s.equals("EXIT")) return FixedLabel.EXIT;

        if (s.length() >= 2 && s.charAt(0) == 'L') {
            String numPart = s.substring(1);
            if (isAllDigits(numPart)) {
                int n = Integer.parseInt(numPart);
                if (n >= 1) return new LabelImpl(n);
            }
        }
        throw new XmlReadException("Illegal label on instruction: " + s);
    }
}
