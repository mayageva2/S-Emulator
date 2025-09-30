package emulator.logic.xml;

import emulator.exception.InvalidInstructionException;
import emulator.logic.execution.QuoteEvaluator;
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

    public static Program toProgram(ProgramXml pxml, QuotationRegistry registry, QuoteEvaluator quoteEval) {
        Objects.requireNonNull(pxml, "pxml");
        Objects.requireNonNull(registry, "registry");

        FunctionsXml fblock = pxml.getFunctions();
        List<FunctionXml> functions = (fblock == null || fblock.getFunctions() == null) ? List.of() : fblock.getFunctions();

        Map<String, ProgramImpl> funcPrograms = new LinkedHashMap<>();
        for (FunctionXml fxml : functions) {
            String fname = fxml.getName();
            if (fname == null || fname.isBlank()) continue;
            ProgramImpl fprog = new ProgramImpl(fname);
            funcPrograms.put(fname.toUpperCase(Locale.ROOT), fprog);
            registry.putProgram(fname.toUpperCase(Locale.ROOT), fprog);
        }

        for (FunctionXml fxml : functions) {
            ProgramImpl fprog = funcPrograms.get(fxml.getName().toUpperCase(Locale.ROOT));
            if (fprog == null) continue;

            int fidx = 0;
            List<InstructionXml> finstr = (fxml.getInstructions() == null) ? List.of() : fxml.getInstructions().getInstructions();
            for (InstructionXml ix : finstr) {
                fidx++;
                fprog.addInstruction(toInstruction(ix, fidx, fprog, registry, quoteEval));
            }
        }

        ProgramImpl program = new ProgramImpl(pxml.getName());
        int idx = 0;
        List<InstructionXml> pinstr =
                (pxml.getInstructions() == null) ? List.of() : pxml.getInstructions().getInstructions();

        for (InstructionXml ix : pinstr) {
            idx++;
            program.addInstruction(toInstruction(ix, idx, program, registry, quoteEval));
        }

        registry.putProgram(program.getName().toUpperCase(Locale.ROOT), program);
        return program;
    }

    private static Instruction toInstruction(InstructionXml ix, int index, Program program, QuotationRegistry registry, QuoteEvaluator quoteEval) {
        ParsedParts p = parseParts(ix, index);
        return buildInstruction(p, index, program, registry, quoteEval);
    }

    private static ParsedParts parseParts(InstructionXml ix, int index) {
        String name = safe(ix.getName());
        String opcode = name.isEmpty() ? "<unknown>" : name.trim().toUpperCase(Locale.ROOT);

        String varName = safe(ix.getVariable());
        Variable v = varName.isEmpty() ? null : parseVariable(varName, opcode, index);
        Label lbl = parseLabel(ix.getLabel(), opcode, index, LabelPolicy.OPTIONAL,  "instruction label");

        Map<String, String> args = toArgMap(ix);
        return new ParsedParts(opcode, v, lbl, args);
    }

    private static Instruction buildInstruction(ParsedParts p, int index, Program program, QuotationRegistry registry, QuoteEvaluator quoteEval) {
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
                String arg = p.args().get("GOTOLABEL");
                String targetText = (arg != null && !arg.isBlank()) ? arg.trim() : (p.lbl() == null ? "" : p.lbl().getLabelRepresentation());
                Label target = parseLabel(targetText, opcode, index, LabelPolicy.REQUIRED, "target label");
                Label my = (arg != null && !arg.isBlank()) ? p.lbl() : FixedLabel.EMPTY;
                yield new GoToLabelInstruction(my, target);
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
                String fname = req(args, "FUNCTIONNAME", opcode, index).toUpperCase(Locale.ROOT);
                String fargs = args.getOrDefault("FUNCTIONARGUMENTS", "");

                QuotationInstruction.Builder b = new QuotationInstruction.Builder()
                        .variable(v)
                        .funcName(fname)
                        .funcArguments(fargs)
                        .myLabel(lbl)
                        .parser(new QuoteParserImpl())
                        .varResolver(new ProgramVarResolver(program))
                        .registry(registry)
                        .quoteEval(quoteEval);
                yield b.build();
            }
            case "JUMP_EQUAL_FUNCTION" -> {
                String fname = req(args, "FUNCTIONNAME", opcode, index).toUpperCase(Locale.ROOT);
                String fargs = args.getOrDefault("FUNCTIONARGUMENTS", "");
                Label target = parseLabel(req(args, "JEFUNCTIONLABEL", opcode, index),
                        opcode, index, LabelPolicy.REQUIRED, "JEFUNCTIONLABEL");

                JumpEqualFunctionInstruction.Builder b = new JumpEqualFunctionInstruction.Builder()
                        .variable(v)
                        .jeFunctionLabel(target)
                        .funcName(fname)
                        .funcArguments(fargs)
                        .myLabel(lbl)
                        .parser(new QuoteParserImpl())
                        .registry(registry)
                        .varResolver(new ProgramVarResolver(program))
                        .quoteEval(quoteEval);
                yield b.build();
            }
            default -> throw new InvalidInstructionException(opcode, "Unsupported or invalid instruction", index);
        };
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
