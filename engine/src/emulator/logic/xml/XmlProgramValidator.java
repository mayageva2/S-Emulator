package emulator.logic.xml;

import emulator.exception.InvalidInstructionException;
import emulator.exception.MissingLabelException;
import emulator.exception.XmlInvalidContentException;

import java.util.*;
import java.util.regex.Pattern;

public class XmlProgramValidator {

    private static final Pattern LABEL_FMT = Pattern.compile("^L\\d+$", Pattern.CASE_INSENSITIVE);

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
            if ("EXIT".equals(ref)) continue;
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
        out.add(norm(valRaw));
    }

    //This func recognizes the label argument names
    private boolean isLabelArgName(String name) {
        return switch (name) {
            case "GOTOLABEL", "JNZLABEL", "JZLABEL", "JECONSTANTLABEL", "JEVARIABLELABEL" -> true;
            default -> false;
        };
    }
}
