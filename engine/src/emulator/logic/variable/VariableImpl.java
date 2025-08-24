package emulator.logic.variable;

import java.util.Objects;

public class VariableImpl implements Variable {

    private final VariableType type;
    private final int number;
    private final String representation;

    public VariableImpl(VariableType type, int number) {
        this.type = type;
        this.number = number;
        this.representation = (type == VariableType.RESULT) ? "y"
                : (type == VariableType.INPUT ? "x" + number : "z" + number);
    }

    @Override public VariableType getType() { return type; }
    @Override public int getNumber() { return number; }
    @Override public String getRepresentation() { return representation; }
    @Override public boolean isEmpty() { return false; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableImpl that)) return false;
        return number == that.number && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, number);
    }

    @Override
    public String toString() {
        return representation;
    }
}
