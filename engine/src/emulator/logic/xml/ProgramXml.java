package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "S-Program")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProgramXml {
    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "S-Instructions", required = true)
    private InstructionsXml instructions;


    // --- getters/setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public InstructionsXml getInstructions() { return instructions; }
    public void setInstructions(InstructionsXml instructions) { this.instructions = instructions; }
}
