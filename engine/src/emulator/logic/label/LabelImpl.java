package emulator.logic.label;

public class LabelImpl implements Label{

    private final String label;

    public LabelImpl(String name) {
        if (!name.startsWith("L")) {
            throw new IllegalArgumentException("Invalid label name: " + name);
        }
        this.label = name;
    }

    public LabelImpl(int number) {
        label = "L" + number;
    }

    public String getLabelRepresentation() {
        return label;
    }

    public String toString() {
        return getLabelRepresentation();
    }

}
