package emulator.logic.label;

import java.io.Serializable;

public class LabelImpl implements Label, Serializable {

    private final String label;
    private static final long serialVersionUID = 1L;

    public LabelImpl(String name) {  //Constructor that receives a string
        if (!name.startsWith("L")) {
            throw new IllegalArgumentException("Invalid label name: " + name);
        }
        this.label = name;
    }

    public LabelImpl(int number) {   //Constructor that receives an int
        label = "L" + number;
    }

    //This func returns label representation
    public String getLabelRepresentation() { return label; }

    //This func returns label representation
    public String toString() { return getLabelRepresentation(); }

}
