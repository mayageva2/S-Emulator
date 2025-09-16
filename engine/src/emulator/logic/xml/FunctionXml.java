package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class FunctionXml {
    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "user-string", required = true)
    private String userString;

    @XmlElement(name = "S-Instructions", required = true)
    private InstructionsXml instructions;

    // --- getters/setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserString() { return userString; }
    public void setUserString(String userString) { this.userString = userString; }

    public InstructionsXml getInstructions() { return instructions; }
    public void setInstructions(InstructionsXml instructions) { this.instructions = instructions; }
}
