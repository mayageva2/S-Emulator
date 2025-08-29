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

    public VariableImpl(String name) {
        if (name.equals("y")) {
            this.type = VariableType.RESULT;
            this.number = 0;
        } else if (name.startsWith("x")) {
            this.type = VariableType.INPUT;
            this.number = Integer.parseInt(name.substring(1));
        } else if (name.startsWith("z")) {
            this.type = VariableType.WORK;
            this.number = Integer.parseInt(name.substring(1));
        } else {
            throw new IllegalArgumentException("Unknown variable name: " + name);
        }
        this.representation = name;
    }

    // ---- getters funcs ---- //
    @Override public VariableType getType() { return type; }
    @Override public int getNumber() { return number; }
    @Override public String getRepresentation() { return representation; }

    //This func checks if variable is empty
    @Override public boolean isEmpty() { return false; }

    //This func checks if variables are equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableImpl that)) return false;
        return number == that.number && type == that.type;
    }

    //This func returns hash code
    @Override
    public int hashCode() {
        return Objects.hash(type, number);
    }

    //This func returns variable representation
    @Override
    public String toString() {
        return representation;
    }
}
