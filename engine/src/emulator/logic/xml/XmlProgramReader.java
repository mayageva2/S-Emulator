package emulator.logic.xml;

import emulator.exception.XmlInvalidContentException;
import emulator.exception.XmlNotFoundException;
import emulator.exception.XmlReadException;
import emulator.exception.XmlWrongExtensionException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class XmlProgramReader {

    //This func reads a program XML file, validates it, and converts it into a ProgramXml object
    public ProgramXml read(Path xmlPath) throws XmlReadException {
        requireXmlFile(xmlPath); //Ensure the file path is a valid XML file
        try {
            if (Files.size(xmlPath) == 0) {
                throw new XmlInvalidContentException("Empty XML file: " + xmlPath.getFileName(), Collections.emptyMap());
            }
        } catch (IOException io) {
            throw new XmlReadException("Failed to read file size: " + xmlPath, io);
        }
        try {    //Create a JAXB context for ProgramXml and unmarshal the file
            JAXBContext ctx = JAXBContext.newInstance(ProgramXml.class);
            Unmarshaller um = ctx.createUnmarshaller();
            return (ProgramXml) um.unmarshal(xmlPath.toFile());
        } catch (JAXBException e) {
            throw new XmlInvalidContentException(
                    "Failed to parse XML: " + xmlPath.getFileName(),
                    Map.of("exception", e.getClass().getSimpleName())
            );
        }
    }

    //This func ensures the given path refers to an existing .xml file
    private static void requireXmlFile(Path path) throws XmlReadException {
        if (path == null) {   //Check for null path
            throw new XmlNotFoundException("No path provided.");
        }
        if (!Files.exists(path)) {  //Check that the file actually exists
            throw new XmlNotFoundException("File does not exist: " + path);
        }
        String n = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!n.endsWith(".xml")) {     // Ensure the file name ends with ".xml"
            throw new XmlWrongExtensionException("Expected an .xml file: " + n);
        }
    }
}
