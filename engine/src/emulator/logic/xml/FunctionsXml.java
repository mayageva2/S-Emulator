package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class FunctionsXml {
    @XmlElement(name = "S-Function")
    private List<FunctionXml> functions;

    public List<FunctionXml> getFunctions() { return functions; }
    public void setFunctions(List<FunctionXml> functions) { this.functions = functions; }
}
