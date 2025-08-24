package emulator.logic.xml;

import java.util.*;

public class XmlProgramValidator {

    public void validate(ProgramXml p) throws XmlReadException {
        validateBasic(p);
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
            throw new XmlReadException("Unknown labels referenced: " + missing);
        }
    }

    public XmlResult<Void> tryValidate(ProgramXml p) {
        try {
            validateBasic(p);
            validateLabelsExist(p);
            return XmlResult.ok(null);
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

    private List<String> collectReferencedLabels(ProgramXml p) {
        List<String> out = new ArrayList<>();
        if (p.getInstructions() != null && p.getInstructions().getInstructions() != null) {
            for (InstructionXml i : p.getInstructions().getInstructions()) {
                if (i.getArguments() == null) continue;
                for (InstructionArgXml a : i.getArguments()) {
                    if (a.getName() == null) continue;
                    if (a.getName().equals("gotoLabel")
                            || a.getName().equals("JNZLabel")
                            || a.getName().equals("JZLabel")
                            || a.getName().equals("JEConstantLabel")
                            || a.getName().equals("JEVariableLabel")) {
                        out.add(a.getValue());
                    }
                }
            }
        }
        return out;
    }
}
