package emulator.logic.label;

public class LabelImpl implements Label{

    private final String label;

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
