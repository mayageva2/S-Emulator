package emulator.logic.xml;

import emulator.exception.MissingLabelException;

import java.util.*;
import java.util.regex.Pattern;

public class XmlProgramValidator {

    private static final Pattern LABEL_FMT = Pattern.compile("^L\\d+$");

    public void validate(ProgramXml p) throws XmlReadException {
        validateBasic(p);
        validateLabelDuplicatesAndFormat(p);
        validateLabelsExist(p);
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

    public XmlResult<Void> tryValidate(ProgramXml p) {
        try {
            validate(p);
            return XmlResult.ok(null);
        } catch (RuntimeException e) {
            return XmlResult.error(e.getMessage());
        } catch (XmlReadException e) {
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
