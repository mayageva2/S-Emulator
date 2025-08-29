package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class VariableXml {

    @XmlValue
    private String name;

    // --- getters/setters --- //
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
