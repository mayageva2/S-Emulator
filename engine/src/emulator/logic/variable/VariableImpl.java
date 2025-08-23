package emulator.logic.variable;

public class VariableImpl implements Variable {

    private final VariableType type;
    private final int number;
    private final String name;

    public VariableImpl(VariableType type, int number, String name) {
        this.type = type;
        this.number = number;
        this.name = name;
    }

    @Override
    public VariableType getType() {
        return type;
    }

    @Override
    public int getNumber() { return number; }

    @Override
    public String getRepresentation() {
        return type.getVariableRepresentation(number);
    }

    @Override
    public String getName() { return name; }

    @Override
    public boolean isEmpty() { return name == null || name.isBlank(); }
}
