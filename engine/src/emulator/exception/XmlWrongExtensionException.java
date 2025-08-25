package emulator.exception;

import java.util.Map;

public class XmlWrongExtensionException extends ProgramException {
    public XmlWrongExtensionException(String path) {
        super("XML_BAD_EXTENSION", "Expected .xml file extension", Map.of("path", path));
    }
}
