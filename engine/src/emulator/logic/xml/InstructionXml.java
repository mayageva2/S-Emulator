package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class InstructionXml {
    @XmlAttribute(name = "type", required = true)
    private String type;

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "S-Variable")
    private String variable;

    @XmlElement(name = "S-Label")
    private String label;

    @XmlElement(name = "S-Instruction-Arguments")
    private InstructionArgsXml args;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getVariable() { return variable; }
    public void setVariable(String variable) { this.variable = variable; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public InstructionArgsXml getArgs() { return args; }
    public void setArgs(InstructionArgsXml args) { this.args = args; }
}
