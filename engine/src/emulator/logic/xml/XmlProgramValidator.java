package emulator.logic.xml;

import emulator.exception.InvalidInstructionException;
import emulator.exception.MissingLabelException;
import emulator.exception.XmlInvalidContentException;
import emulator.logic.instruction.InstructionData;

import java.util.*;
import java.util.regex.Pattern;

public class XmlProgramValidator {

    private static final Pattern LABEL_FMT = Pattern.compile("^L\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VAR_FMT = Pattern.compile("^(?:[xz][1-9]\\d*|y)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INT_FMT   = Pattern.compile("^-?\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NO_SPACE  = Pattern.compile("^\\S+$");

    //This func normalizes a string
    private static String norm(String s) {
        return (s == null) ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    //This func checks if a string is null ot empty
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    //This func runs validation checks on a program
    public void validate(ProgramXml p) {
        validateBasic(p);
        validateLabelDuplicatesAndFormat(p);
        validateLabelsExist(p);
        validateInstructionTypeAndName(p);
        validateVariables(p);
        validateNonLabelArgs(p);
        validateFunctions(p);
    }

    //This func validates that a ProgramXml has a name and a non-empty instructions
    public void validateBasic(ProgramXml p) {
        if (p == null) {
            throw new XmlInvalidContentException(
                    "Empty program document.",
                    Map.of("section", "S-Program")
            );
        }
        if (p.getName() == null || p.getName().isBlank()) {
            throw new XmlInvalidContentException(
                    "Missing S-Program@name.",
                    Map.of("section", "S-Program", "field", "name")
            );
        }
        if (p.getInstructions() == null
                || p.getInstructions().getInstructions() == null
                || p.getInstructions().getInstructions().isEmpty()) {
            throw new XmlInvalidContentException(
                    "Missing or empty S-Instructions section.",
                    Map.of("section", "S-Instructions")
            );
        }
    }

    //This func validate that labels are unique
    private void validateLabelDuplicatesAndFormat(ProgramXml p) {
        List<String> labels = collectNonBlankTrimmedLabels(p);
        checkDuplicateLabels(labels);   //Check duplicates
        checkLabelFormats(labels);    //Check format issues
    }

    //This func collect all non-blank labels from instructions
    private List<String> collectNonBlankTrimmedLabels(ProgramXml p) {
        List<String> out = new ArrayList<>();
        if (p.getInstructions() == null || p.getInstructions().getInstructions() == null) return out;

        for (InstructionXml i : p.getInstructions().getInstructions()) {
            String raw = i.getLabel();
            if (isBlank(raw)) continue;
            out.add(raw.trim());
        }
        return out;
    }

    //This func checks if there are case-insensitive duplicates
    private void checkDuplicateLabels(List<String> labels) {
        Set<String> seenKeys = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();

        for (String label : labels) {
            String key = norm(label);
            if (!seenKeys.add(key)) {
                duplicates.add(label);
            }
        }

        if (!duplicates.isEmpty()) {
            throw new InvalidInstructionException(
                    "LABEL",
                    "Duplicate labels found (case-insensitive): " + new ArrayList<>(duplicates),
                    -1
            );
        }
    }

    //This func checks if there are labels that doesn't match
    private void checkLabelFormats(List<String> labels) {
        List<String> badFormat = new ArrayList<>();
        for (String label : labels) {
            if (isExitLabel(label)) {
                throw new InvalidInstructionException(
                        "LABEL",
                        "Reserved label name cannot be used as an instruction label: 'EXIT'",
                        -1
                );
            }
            if (!LABEL_FMT.matcher(label).matches()) {
                badFormat.add(label);
            }
        }
        if (!badFormat.isEmpty()) {
            throw new InvalidInstructionException(
                    "LABEL",
                    "Invalid label format: " + badFormat + " (expected L<number>)",
                    -1
            );
        }
    }

    //This func validates that all labels referenced in a program are properly defined
    public void validateLabelsExist(ProgramXml p) {
        Set<String> defined = collectDefinedLabels(p);
        List<String> refs = collectReferencedLabels(p);

        List<String> missing = new ArrayList<>();
        for (String ref : refs) {
            if (isBlank(ref)) continue;
            if (isExitLabel(ref)) continue;
            if (!defined.contains(ref)) {
                missing.add(ref);
            }
        }
        if (!missing.isEmpty()) {
            if (missing.size() == 1) {
                throw new MissingLabelException(missing.get(0));
            } else {
                throw new InvalidInstructionException(
                        "LABEL",
                        "Unknown labels referenced: " + missing,
                        -1
                );
            }
        }
    }

    //This func collects all non-blank labels
    private Set<String> collectDefinedLabels(ProgramXml p) {
        Set<String> out = new HashSet<>();
        if (p.getInstructions() != null && p.getInstructions().getInstructions() != null) {
            for (InstructionXml i : p.getInstructions().getInstructions()) {
                String lbl = i.getLabel();
                if (!isBlank(lbl)) {
                    out.add(norm(lbl));
                }
            }
        }
        return out;
    }

    //This func collects all referenced labels
    private List<String> collectReferencedLabels(ProgramXml p) {
        List<String> out = new ArrayList<>();
        List<InstructionXml> list = getInstructionList(p);

        for (int idx = 1; idx <= list.size(); idx++) {
            InstructionXml ins = list.get(idx - 1);
            processInstructionLabelArgs(ins, idx, out);
        }
        return out;
    }

    //This func returns instruction list
    private List<InstructionXml> getInstructionList(ProgramXml p) {
        return (p.getInstructions() != null && p.getInstructions().getInstructions() != null)
                ? p.getInstructions().getInstructions()
                : Collections.emptyList();
    }

    //This func processes all arguments
    private void processInstructionLabelArgs(InstructionXml ins, int idx, List<String> out) {
        if (ins.getArguments() == null) return;
        for (InstructionArgXml arg : ins.getArguments()) {
            handlePotentialLabelArg(ins, idx, arg, out);
        }
    }

    //This func validates args value
    private void handlePotentialLabelArg(InstructionXml ins, int idx, InstructionArgXml arg, List<String> out) {
        String nameRaw = arg.getName();
        if (nameRaw == null) return;

        String name = norm(nameRaw);
        if (!isLabelArgName(name)) return;

        String valRaw = arg.getValue();
        if (isBlank(valRaw)) {
            String opcode = (ins.getName() == null ? "<unknown>" : ins.getName());
            throw new InvalidInstructionException(
                    norm(opcode),
                    "Missing required label value for argument '" + nameRaw + "' at instruction #" + idx,
                    idx
            );
        }

        String valTrim = valRaw.trim();
        if (!isExitLabel(valTrim) && !LABEL_FMT.matcher(valTrim).matches()) {
            String opcode = (ins.getName() == null ? "<unknown>" : ins.getName());
            throw new InvalidInstructionException(
                    norm(opcode),
                    "Invalid label format for argument '" + nameRaw + "' at instruction #" + idx +
                            ": '" + valRaw + "' (expected L<number> or EXIT)",
                    idx
            );
        }
        out.add(norm(valRaw));
    }

    //This func recognizes the label argument names
    private boolean isLabelArgName(String name) {
        return switch (name) {
            case "GOTOLABEL", "JNZLABEL", "JZLABEL", "JECONSTANTLABEL", "JEVARIABLELABEL" -> true;
            default -> false;
        };
    }

    //This func checks if label is EXIT
    private static boolean isExitLabel(String s) {
        return s != null && s.trim().equalsIgnoreCase("EXIT");
    }

    //This func validates instruction type/name
    private void validateInstructionTypeAndName(ProgramXml p) {
        if (p.getInstructions() == null || p.getInstructions().getInstructions() == null) return;

        for (int i = 0; i < p.getInstructions().getInstructions().size(); i++) {
            InstructionXml ins = p.getInstructions().getInstructions().get(i);
            int idx = i + 1;

            String type = ins.getType();
            String name = ins.getName();

            if (isBlank(type) || isBlank(name)) {
                throw new XmlInvalidContentException(
                        "Missing S-Instruction @type or @name at instruction #" + idx,
                        Map.of("section", "S-Instruction", "index", String.valueOf(idx))
                );
            }

            // Find matching enum entry by name (case-insensitive compare of getName())
            InstructionData def = null;
            for (InstructionData d : InstructionData.values()) {
                if (d.getName().equalsIgnoreCase(name)) { def = d; break; }
            }
            if (def == null) {
                throw new InvalidInstructionException(
                        "INSTRUCTION",
                        "Unknown instruction name '" + name + "' at #" + idx,
                        idx
                );
            }

            boolean declaredBasic = "basic".equalsIgnoreCase(type);
            if (def.isBasic() != declaredBasic) {
                throw new XmlInvalidContentException(
                        "Instruction type mismatch for '" + name + "' at #" + idx,
                        Map.of(
                                "expected", def.isBasic() ? "basic" : "synthetic",
                                "found", type
                        )
                );
            }
        }
    }

    // This func validates Variables
    private void validateVariables(ProgramXml p) {
        if (p.getInstructions() == null || p.getInstructions().getInstructions() == null) return;

        for (int i = 0; i < p.getInstructions().getInstructions().size(); i++) {
            InstructionXml ins = p.getInstructions().getInstructions().get(i);
            int idx = i + 1;

            String v = ins.getVariable();
            if (v == null) continue; // element may be absent per your model
            String t = v.trim();
            if (t.isEmpty()) continue; // empty is allowed by spec

            if (!VAR_FMT.matcher(t).matches()) {
                throw new InvalidInstructionException(
                        "VARIABLE",
                        "Invalid variable '" + v + "' at instruction #" + idx + " (expected xN, zN, or y)",
                        idx
                );
            }
        }
    }

    // This func validates variable/constant/functionName args
    private void validateNonLabelArgs(ProgramXml p) {
        if (p.getInstructions() == null || p.getInstructions().getInstructions() == null) return;

        for (int i = 0; i < p.getInstructions().getInstructions().size(); i++) {
            InstructionXml ins = p.getInstructions().getInstructions().get(i);
            int idx = i + 1;

            if (ins.getArguments() == null) continue;

            for (InstructionArgXml arg : ins.getArguments()) {
                String rawName = arg.getName();
                if (isBlank(rawName)) continue;
                String normName = norm(rawName);
                if (isLabelArgName(normName)) continue; // already validated in handlePotentialLabelArg
                if ("FUNCTIONARGUMENTS".equals(normName)) continue;

                String val = arg.getValue();
                String opcode = ins.getName() == null ? "<unknown>" : ins.getName();

                // variable-like argument names
                if (looksLikeVariableArg(rawName)) {
                    if (isBlank(val) || !VAR_FMT.matcher(val.trim()).matches()) {
                        throw new InvalidInstructionException(
                                norm(opcode),
                                "Invalid variable value for argument '" + rawName + "' at instruction #" + idx,
                                idx
                        );
                    }
                }
                // constant/number-like argument names
                else if (looksLikeConstantArg(rawName)) {
                    if (isBlank(val) || !INT_FMT.matcher(val.trim()).matches()) {
                        throw new InvalidInstructionException(
                                norm(opcode),
                                "Invalid numeric constant for argument '" + rawName + "' at instruction #" + idx,
                                idx
                        );
                    }
                }
                // simple function name arg (no spaces)
                else if (looksLikeFunctionArg(rawName)) {
                    if (isBlank(val) || !NO_SPACE.matcher(val.trim()).matches()) {
                        throw new InvalidInstructionException(
                                norm(opcode),
                                "Invalid function name for argument '" + rawName + "' at instruction #" + idx,
                                idx
                        );
                    }
                }
            }
        }
    }

    private static boolean looksLikeVariableArg(String name) {
        // ends with "Variable" but not "...Label"
        return name.endsWith("Variable") && !name.endsWith("Label");
    }

    private static boolean looksLikeConstantArg(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return (n.contains("constant") || n.equals("value")) && !n.endsWith("label");
    }

    private static boolean looksLikeFunctionArg(String name) {
        return "functionName".equalsIgnoreCase(name);
    }

    private void validateFunctions(ProgramXml p) {
        if (p.getFunctions() == null || p.getFunctions().getFunctions() == null) return;

        Set<String> functionNames = new HashSet<>();

        for (FunctionXml f : p.getFunctions().getFunctions()) {
            String fname = f.getName();
            if (isBlank(fname) || !NO_SPACE.matcher(fname).matches()) {
                throw new XmlInvalidContentException(
                        "Invalid function name (empty or contains spaces).",
                        Map.of("section", "S-Function", "name", String.valueOf(fname))
                );
            }
            String normName = norm(fname);
            if (!functionNames.add(normName)) {
                throw new XmlInvalidContentException(
                        "Duplicate function name: " + fname,
                        Map.of("section", "S-Functions", "name", fname)
                );
            }
            String user = f.getUserString();
            if (isBlank(user) || !NO_SPACE.matcher(user).matches()) {
                throw new XmlInvalidContentException(
                        "Invalid function user-string (empty or contains spaces).",
                        Map.of("section", "S-Function", "name", fname, "field", "user-string")
                );
            }

            // Optionally validate the function’s own instructions using same rules:
            if (f.getInstructions() != null && f.getInstructions().getInstructions() != null) {
                if (f.getInstructions().getInstructions().isEmpty()) {
                    throw new XmlInvalidContentException(
                            "Empty instructions inside function '" + fname + "'.",
                            Map.of("section", "S-Function", "name", fname)
                    );
                }
            }
        }
        boolean hasFunctions = !functionNames.isEmpty();

       /* // Cross-reference from main instructions    //NOTED cause its a 2 exercise validation
        if (p.getInstructions() != null && p.getInstructions().getInstructions() != null) {
            for (int i = 0; i < p.getInstructions().getInstructions().size(); i++) {
                InstructionXml ins = p.getInstructions().getInstructions().get(i);
                int idx = i + 1;
                if (ins.getArguments() == null) continue;

                String opcode = String.valueOf(ins.getName());

                // 1) JUMP_EQUAL_FUNCTION -> hard require a known function
                if ("JUMP_EQUAL_FUNCTION".equalsIgnoreCase(opcode)) {
                    for (InstructionArgXml arg : ins.getArguments()) {
                        String n = arg.getName();
                        if (n == null) continue;
                        if (looksLikeFunctionArg(n)) {
                            String val = (arg.getValue() == null ? "" : arg.getValue().trim());
                            if (!functionNames.contains(norm(val))) {
                                throw new InvalidInstructionException(
                                        norm(opcode),
                                        "Unknown function '" + val + "' at instruction #" + idx,
                                        idx
                                );
                            }
                        }
                    }
                }

                // 2) QUOTE -> only check if we actually have functions declared
                else if ("QUOTE".equalsIgnoreCase(opcode) && hasFunctions) {
                    for (InstructionArgXml arg : ins.getArguments()) {
                        String n = arg.getName();
                        if (n == null) continue;
                        if (looksLikeFunctionArg(n)) {
                            String val = (arg.getValue() == null ? "" : arg.getValue().trim());
                            if (!functionNames.contains(norm(val))) {
                                throw new InvalidInstructionException(
                                        norm(opcode),
                                        "Unknown function '" + val + "' at instruction #" + idx,
                                        idx
                                );
                            }
                        }
                    }
                }
            }
        }*/
    }
}
