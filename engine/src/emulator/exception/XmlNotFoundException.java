package emulator.exception;

import java.util.Map;

public class XmlNotFoundException extends ProgramException {
    public XmlNotFoundException(String path) {
        super("XML_NOT_FOUND", "XML file not found: " + path, Map.of("path", path));
    }
}
