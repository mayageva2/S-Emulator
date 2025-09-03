package emulator.logic.xml;

import emulator.exception.InvalidInstructionException;
import emulator.logic.instruction.*;
import emulator.logic.instruction.quote.ProgramVarResolver;
import emulator.logic.instruction.quote.QuotationInstruction;
import emulator.logic.instruction.quote.QuotationRegistry;
import emulator.logic.instruction.quote.QuoteParserImpl;
import emulator.logic.label.*;
import emulator.logic.program.*;
import emulator.logic.variable.*;

import java.util.*;

public final class XmlToObjects {

    private static record ParsedParts(String opcode, Variable v, Label lbl, Map<String,String> args) {}
    private XmlToObjects(){}
    private enum LabelPolicy { REQUIRED, OPTIONAL }

    //This func converts a ProgramXml into a Program object
    public static Program toProgram(ProgramXml pxml, QuotationRegistry registry) {
        ProgramImpl program = new ProgramImpl(pxml.getName());
        int idx = 0;
        for (InstructionXml ix : pxml.getInstructions().getInstructions()) {
            idx++;
            program.addInstruction(toInstruction(ix, idx, program, registry));
        }
        return program;
    }

    //This func converts a InstructionXml into an Instruction object
    private static Instruction toInstruction(InstructionXml ix, int index, Program program, QuotationRegistry registry) {
        ParsedParts p = parseParts(ix, index);
        return buildInstruction(p, index, program, registry);
    }

    //This func parse opcode, main variable, label, and args once
    private static ParsedParts parseParts(InstructionXml ix, int index) {
        String name = safe(ix.getName());
        String opcode = name.isEmpty() ? "<unknown>" : name.trim().toUpperCase(Locale.ROOT);

        String varName = safe(ix.getVariable());
        Variable v = varName.isEmpty() ? null : parseVariable(varName, opcode, index);
        Label lbl = parseLabel(ix.getLabel(), opcode, index, LabelPolicy.OPTIONAL,  "instruction label");

        Map<String, String> args = toArgMap(ix);
        return new ParsedParts(opcode, v, lbl, args);
    }

    //This func switches to instruction
    private static Instruction buildInstruction(ParsedParts p, int index, Program program, QuotationRegistry registry) {
        String opcode = p.opcode();
        Variable v = p.v();
        Label lbl = p.lbl();
        Map<String,String> args = p.args();

        return switch (opcode) {
            case "NEUTRAL"              -> new NeutralInstruction(v, lbl);
            case "INCREASE"             -> new IncreaseInstruction(v, lbl);
            case "DECREASE"             -> new DecreaseInstruction(v, lbl);
            case "ZERO_VARIABLE"        -> new ZeroVariableInstruction(v, lbl);
            case "JUMP_NOT_ZERO" -> {
                Label target = parseLabel(req(args, "JNZLABEL", opcode, index), opcode, index, LabelPolicy.REQUIRED, "JNZLABEL");
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
                Label target = parseLabel(req(args, "GOTOLABEL", opcode, index), opcode, index, LabelPolicy.REQUIRED, "GOTOLABEL");
                yield new GoToLabelInstruction(lbl, target);
            }
            case "JUMP_ZERO" -> {
                Label target = parseLabel(req(args, "JZLABEL", opcode, index), opcode, index, LabelPolicy.REQUIRED, "JZLABEL");
                yield new JumpZeroInstruction(v, target, lbl);
            }
            case "JUMP_EQUAL_CONSTANT" -> {
                Label target = parseLabel(req(args, "JECONSTANTLABEL", opcode, index), opcode, index, LabelPolicy.REQUIRED, "JECONSTANTLABEL");
                long k = parseNonNegInt(req(args, "CONSTANTVALUE", opcode, index), opcode, index);
                JumpEqualConstantInstruction.Builder b = new JumpEqualConstantInstruction.Builder().variable(v).constantValue(k).jeConstantLabel(target);
                if (lbl != null) b.myLabel(lbl);
                yield b.build();
            }
            case "JUMP_EQUAL_VARIABLE" -> {
                Label target = parseLabel(req(args, "JEVARIABLELABEL", opcode, index), opcode, index, LabelPolicy.REQUIRED, "JEVARIABLELABEL");
                Variable other = parseVariable(req(args, "VARIABLENAME", opcode, index), opcode, index);
                JumpEqualVariableInstruction.Builder b = new JumpEqualVariableInstruction.Builder().variable(v).compareVariable(other).jeVariableLabel(target);
                if (lbl != null) b.myLabel(lbl);
                yield b.build();
            }
            case "QUOTE" -> {
                String fname = req(args, "FUNCTIONNAME", opcode, index);
                String fargs = args.getOrDefault("FUNCTIONARGUMENTS", "");

                QuotationInstruction.Builder b = new QuotationInstruction.Builder()
                        .variable(v)
                        .funcName(fname)
                        .funcArguments(fargs)
                        .myLabel(lbl)
                        .parser(new QuoteParserImpl())
                        .varResolver(new ProgramVarResolver(program))
                        .registry(registry);

                yield b.build();
            }

            default -> throw new InvalidInstructionException(opcode, "Unsupported or invalid instruction", index);
        };
    }

    //This func converts an instruction’s arguments into a map
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

    //This func retrieves a required argument
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

    //This func returns a trimmed string
    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    //This func checks whether a string is non-empty
    private static boolean isAllDigits(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    //This func parses a string into a Variable object
    private static Variable parseVariable(String s, String opcode, int index) {
        if (s == null || s.isBlank()) {
            throw new InvalidInstructionException(opcode, "Missing variable", index);
        }
        s = s.trim();
        String su = s.toUpperCase(Locale.ROOT);

        if (su.equals("Y")) {
            return Variable.RESULT;
        }

        if (su.length() >= 2) {   // General case
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

    //This func parses a string into an int
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

    //This func parses a string into a valid Label object
    private static Label parseLabel(String s, String opcode, int index, LabelPolicy policy, String errCtx) {
        if (s == null || s.isBlank()) {
            if (policy == LabelPolicy.OPTIONAL) return FixedLabel.EMPTY;
            throw new InvalidInstructionException(opcode, "Missing label" + (errCtx == null ? "" : " (" + errCtx + ")"), index);
        }
        String su = s.trim().toUpperCase(Locale.ROOT);
        if (su.equals("EXIT")) return FixedLabel.EXIT;
        if (su.length() >= 2 && su.charAt(0) == 'L') {
            String num = su.substring(1);
            if (isAllDigits(num)) {
                int n = Integer.parseInt(num);
                if (n >= 1) return new LabelImpl(n);
            }
        }
        throw new InvalidInstructionException(opcode, "Illegal label: " + s + (errCtx == null ? "" : " (" + errCtx + ")"), index);
    }
}
