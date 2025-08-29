package emulator.logic.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class LabelXml {

    @XmlValue
    private String value;

    // --- getters/setters --- //
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
