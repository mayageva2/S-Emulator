package emulator.logic.xml;

import emulator.exception.MissingLabelException;

import java.util.*;
import java.util.regex.Pattern;

public class XmlProgramValidator {

    private static final Pattern LABEL_FMT = Pattern.compile("^L\\d+$");
    private enum ArgType { LABEL, VAR, CONST_INT }
    private static final class ReqArg {
        final String name;
        final ArgType type;
        ReqArg(String name, ArgType type) { this.name = name; this.type = type; }
    }
    private static final Set<String> SUPPORTED_OPCODES = Set.of("INC", "DEC", "SET", "GOTO_LABEL", "JNZ", "JZ", "JE_CONST", "JE_VAR");
    private static final Map<String, List<ReqArg>> OPCODE_REQS = Map.of(
            "GOTO_LABEL", List.of(new ReqArg("gotoLabel", ArgType.LABEL)),
            "JNZ",        List.of(new ReqArg("JNZLabel",  ArgType.LABEL)),
            "JZ",         List.of(new ReqArg("JZLabel",   ArgType.LABEL)),
            "JE_CONST",   List.of(
                    new ReqArg("JEConstantLabel", ArgType.LABEL),
                    new ReqArg("const", ArgType.CONST_INT)
            ),
            "JE_VAR",     List.of(new ReqArg("JEVariableLabel", ArgType.LABEL))
    );


    public void validate(ProgramXml p) throws XmlReadException {
        validateBasic(p);
        validateLabelDuplicatesAndFormat(p);
        validateLabelsExist(p);
        validateOpcodeSemantics(p);
    }

    public void validateBasic(ProgramXml p) throws XmlReadException {
        if (p == null) {
            throw new XmlReadException("Empty program document.");
        }
        if (p.getName() == null || p.getName().isBlank()) {
            throw new XmlReadException("Missing S-Program@name.");
        }
        if (p.getInstructions() == null || p.getInstructions().getInstructions() == null
                || p.getInstructions().getInstructions().isEmpty()) {
            throw new XmlReadException("Missing or empty S-Instructions section.");
        }
    }

    private void validateLabelDuplicatesAndFormat(ProgramXml p) throws XmlReadException {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        List<String> badFormat = new ArrayList<>();

        if (p.getInstructions() != null && p.getInstructions().getInstructions() != null) {
            for (InstructionXml i : p.getInstructions().getInstructions()) {
                String raw = i.getLabel();
                if (raw == null || raw.isBlank()) continue;

                String label = raw.trim();
                if (!LABEL_FMT.matcher(label).matches()) {
                    badFormat.add(label);
                }
                if (!seen.add(label)) {
                    duplicates.add(label);
                }
            }
        }

        if (!duplicates.isEmpty()) {
            throw new XmlReadException("Duplicate labels found: " + new ArrayList<>(duplicates));
        }
        if (!badFormat.isEmpty()) {
            throw new XmlReadException("Invalid label format: " + badFormat + " (expected L<number>)");
        }
    }


    public void validateLabelsExist(ProgramXml p) throws XmlReadException {
        Set<String> defined = collectDefinedLabels(p);
        List<String> refs = collectReferencedLabels(p);

        List<String> missing = new ArrayList<>();
        for (String ref : refs) {
            if (ref == null || ref.isBlank()) continue;
            if ("EXIT".equals(ref)) continue;
            if (!defined.contains(ref)) {
                missing.add(ref);
            }
        }
        if (!missing.isEmpty()) {
            if (missing.size() == 1) {
                throw new MissingLabelException(missing.get(0));
            } else {
                throw new XmlReadException("Unknown labels referenced: " + missing);
            }
        }
    }

    private void validateOpcodeSemantics(ProgramXml p) throws XmlReadException {
        if (p.getInstructions() == null || p.getInstructions().getInstructions() == null) return;

        int idx = 0;
        for (InstructionXml ins : p.getInstructions().getInstructions()) {
            idx++;
            String opcode = upperOrEmpty(ins.getName());

            // 4.1 opcode נתמך?
            if (opcode.isEmpty() || !SUPPORTED_OPCODES.contains(opcode)) {
                // יש לך InvalidInstructionException – נשתמש בה ל-unknown opcode (Unchecked)
                throw new emulator.exception.InvalidInstructionException(opcode, "Unsupported opcode");
            }

            // 4.2 משתנה ראשי (פקודות שמחייבות variable)
            if (requiresMainVariable(opcode)) {
                String var = (ins.getVariable() == null) ? "" : ins.getVariable().trim();
                if (var.isEmpty() || !isValidVarName(var)) {
                    throw new XmlReadException(
                            "Opcode " + opcode + " requires a valid variable (x#, z#, or y). " +
                                    "Found: '" + (var.isEmpty() ? "<empty>" : var) + "' at instruction #" + idx
                    );
                }
            }

            // 4.3 ארגומנטים נדרשים לפי המפה
            Map<String,String> args = argsToMap(ins.getArguments()); // name->value (null→"")
            List<ReqArg> reqs = OPCODE_REQS.getOrDefault(opcode, List.of());
            for (ReqArg r : reqs) {
                String val = args.getOrDefault(r.name, "").trim();
                if (val.isEmpty()) {
                    throw new XmlReadException(
                            "Missing required argument '" + r.name + "' for opcode " + opcode +
                                    " at instruction #" + idx
                    );
                }
                switch (r.type) {
                    case LABEL -> {
                        if (!val.equalsIgnoreCase("EXIT") && !LABEL_FMT.matcher(val).matches()) {
                            throw new XmlReadException(
                                    "Invalid label format in '" + r.name + "' for opcode " + opcode +
                                            ": " + val + " at instruction #" + idx + " (expected L<number> or EXIT)"
                            );
                        }
                    }
                    case VAR -> {
                        if (!isValidVarName(val)) {
                            throw new XmlReadException(
                                    "Invalid variable format in '" + r.name + "' for opcode " + opcode +
                                            ": " + val + " at instruction #" + idx + " (expected x#, z#, or y)"
                            );
                        }
                    }
                    case CONST_INT -> {
                        if (!isInteger(val)) {
                            throw new XmlReadException(
                                    "Invalid integer constant in '" + r.name + "' for opcode " + opcode +
                                            ": " + val + " at instruction #" + idx
                            );
                        }
                    }
                }
            }
        }
    }

    private static String upperOrEmpty(String s) {
        return (s == null) ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String,String> argsToMap(List<InstructionArgXml> args) {
        if (args == null || args.isEmpty()) return Collections.emptyMap();
        Map<String,String> m = new LinkedHashMap<>();
        for (InstructionArgXml a : args) {
            String k = (a.getName()  == null) ? "" : a.getName().trim();
            String v = (a.getValue() == null) ? "" : a.getValue().trim();
            if (!k.isEmpty()) m.put(k, v);
        }
        return m;
    }

    private static boolean requiresMainVariable(String opcode) {
        return switch (opcode) {
            case "INC", "DEC", "SET", "JNZ", "JZ", "JE_VAR" -> true;
            default -> false;
        };
    }

    private static boolean isInteger(String s) {
        if (s == null || s.isBlank()) return false;
        try { Long.parseLong(s.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    private static boolean isValidVarName(String v) {
        if (v == null) return false;
        String s = v.trim();
        return s.equalsIgnoreCase("y")
                || s.matches("^[xX]\\d+$")
                || s.matches("^[zZ]\\d+$");
    }

    public XmlResult<Void> tryValidate(ProgramXml p) {
        try { validate(p); return XmlResult.ok(null);
        } catch (RuntimeException | XmlReadException e) { return XmlResult.error(e.getMessage()); }
    }

    private Set<String> collectDefinedLabels(ProgramXml p) {
        Set<String> out = new HashSet<>();
        if (p.getInstructions() != null && p.getInstructions().getInstructions() != null) {
            for (InstructionXml i : p.getInstructions().getInstructions()) {
                if (i.getLabel() != null && !i.getLabel().isBlank()) {
                    out.add(i.getLabel());
                }
            }
        }
        return out;
    }

    private List<String> collectReferencedLabels(ProgramXml p) throws XmlReadException{
        List<String> out = new ArrayList<>();
        if (p.getInstructions() != null && p.getInstructions().getInstructions() != null) {
            int idx = 0;
            for (InstructionXml i : p.getInstructions().getInstructions()) {
                idx++;
                if (i.getArguments() == null) continue;

                for (InstructionArgXml a : i.getArguments()) {
                    String name = a.getName();
                    if (name == null) continue;

                    boolean isLabelArg =
                            name.equals("gotoLabel")
                                    || name.equals("JNZLabel")
                                    || name.equals("JZLabel")
                                    || name.equals("JEConstantLabel")
                                    || name.equals("JEVariableLabel");

                    if (!isLabelArg) continue;

                    String val = a.getValue();
                    if (val == null || val.isBlank()) {
                        throw new XmlReadException("Missing required label value for argument '" + name + "' at instruction #" + idx);
                    }
                    out.add(val.trim());
                }
            }
        }
        return out;
    }
}
