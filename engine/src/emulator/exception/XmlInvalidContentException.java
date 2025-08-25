package emulator.exception;

import java.util.Map;

public class XmlInvalidContentException extends ProgramException {
    public XmlInvalidContentException(String details, Map<String,Object> ctx) {
        super("XML_INVALID", details, ctx);
    }
}
