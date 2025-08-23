package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class InstructionArgsXml {
    @XmlElement(name = "S-Instruction-Argument")
    private List<InstructionArgXml> args = new ArrayList<>();


    public List<InstructionArgXml> getArgs() { return args; }
    public void setArgs(List<InstructionArgXml> args) { this.args = args; }
}
