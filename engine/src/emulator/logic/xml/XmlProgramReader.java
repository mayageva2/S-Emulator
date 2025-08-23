package emulator.logic.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class XmlProgramReader {

    public ProgramXml read(Path xmlPath) throws XmlReadException {
        requireXmlFile(xmlPath);
        try {
            if (Files.size(xmlPath) == 0) {
                throw new XmlReadException("Empty XML file: " + xmlPath.getFileName());
            }
        } catch (IOException io) {
            throw new XmlReadException("Failed to read file size: " + xmlPath, io);
        }
        try {
            JAXBContext ctx = JAXBContext.newInstance(ProgramXml.class);
            Unmarshaller um = ctx.createUnmarshaller();
            return (ProgramXml) um.unmarshal(xmlPath.toFile());
        } catch (JAXBException e) {
            throw new XmlReadException("Failed to parse XML: " + xmlPath.getFileName(), e);
        }
    }

    public XmlResult<ProgramXml> tryRead(Path xmlPath) {
        try {
            return XmlResult.ok(readAndSanitize(xmlPath));
        } catch (XmlReadException e) {
            return XmlResult.error(e.getMessage());
        }
    }

    private static void requireXmlFile(Path path) throws XmlReadException {
        if (path == null) {
            throw new XmlReadException("No path provided.");
        }
        if (!Files.exists(path)) {
            throw new XmlReadException("File does not exist: " + path);
        }
        String n = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!n.endsWith(".xml")) {
            throw new XmlReadException("Expected an .xml file: " + n);
        }
    }


    public ProgramXml readAndSanitize(Path xmlPath) throws XmlReadException {
        ProgramXml p = read(xmlPath);
        sanitize(p);
        return p;
    }

    private static void sanitize(ProgramXml p) {
        if (p == null) return;
        if (p.getName() != null) p.setName(p.getName().trim());

        if (p.getInstructions() != null && p.getInstructions().getInstructions() != null) {
            for (InstructionXml i : p.getInstructions().getInstructions()) {
                sanitize(i);
            }
        }
    }

    private static void sanitize(InstructionXml i) {
        if (i == null) return;
        if (i.getName() != null) i.setName(i.getName().trim());
        if (i.getType() != null) i.setType(i.getType().trim());
        if (i.getVariable() != null) i.setVariable(i.getVariable().trim());
        if (i.getLabel() != null) i.setLabel(i.getLabel().trim());

        if (i.getArgs() != null && i.getArgs().getArgs() != null) {
            for (InstructionArgXml a : i.getArgs().getArgs()) {
                if (a.getName() != null) a.setName(a.getName().trim());
                if (a.getValue() != null) a.setValue(a.getValue().trim());
            }
        }
    }
}
