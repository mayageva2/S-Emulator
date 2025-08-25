package emulator.logic.xml;

import emulator.exception.InvalidInstructionException;
import emulator.exception.MissingLabelException;
import emulator.exception.XmlInvalidContentException;
import emulator.exception.XmlReadException;

import java.util.*;
import java.util.regex.Pattern;

public class XmlProgramValidator {

    private static final Pattern LABEL_FMT = Pattern.compile("^L\\d+$");

    public void validate(ProgramXml p) {
        validateBasic(p);
        validateLabelDuplicatesAndFormat(p);
        validateLabelsExist(p);
    }

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

    private void validateLabelDuplicatesAndFormat(ProgramXml p) {
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
            throw new InvalidInstructionException(
                    "LABEL",
                    "Duplicate labels found: " + new ArrayList<>(duplicates),
                    -1
            );
        }
        if (!badFormat.isEmpty()) {
            throw new InvalidInstructionException(
                    "LABEL",
                    "Invalid label format: " + badFormat + " (expected L<number>)",
                    -1
            );
        }
    }


    public void validateLabelsExist(ProgramXml p) {
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
                throw new InvalidInstructionException(
                        "LABEL",
                        "Unknown labels referenced: " + missing,
                        -1
                );
            }
        }
    }

    public XmlResult<Void> tryValidate(ProgramXml p) {
        try {
            validate(p);
            return XmlResult.ok(null);
        } catch (RuntimeException e) {
            return XmlResult.error(e.getMessage());
        }
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

    private List<String> collectReferencedLabels(ProgramXml p) {
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
                        String opcode = (i.getName() == null ? "<unknown>" : i.getName());
                        throw new InvalidInstructionException(opcode, "Missing required label value for argument '" + name + "' at instruction #" + idx, idx);
                    }
                    out.add(val.trim());
                }
            }
        }
        return out;
    }
}
