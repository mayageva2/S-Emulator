package emulator.logic.xml;

import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class InstructionsXml {

    @XmlElement(name = "S-Instruction")
    private List<InstructionXml> instructions;

    // --- getters/setters --- //
    public List<InstructionXml> getInstructions() {
        return instructions;
    }
    public void setInstructions(List<InstructionXml> instructions) {
        this.instructions = instructions;
    }
}