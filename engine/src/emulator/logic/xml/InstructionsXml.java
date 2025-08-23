package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class InstructionsXml {
    @XmlElement(name = "S-Instruction", required = true)
    private List<InstructionXml> instructions = new ArrayList<>();


    public List<InstructionXml> getInstructions() { return instructions; }
    public void setInstructions(List<InstructionXml> instructions) { this.instructions = instructions; }
}