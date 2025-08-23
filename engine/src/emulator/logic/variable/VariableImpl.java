package emulator.logic.variable;

public class VariableImpl implements Variable {

    private final VariableType type;
    private final int number;

    public VariableImpl(VariableType type, int number) {
        this.type = type;
        this.number = number;
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
    public String toString() {
        return getRepresentation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable other)) return false;

        String n1 = this.getRepresentation();
        String n2 = other.getRepresentation();

        if (n1 != null && !n1.isBlank() && n2 != null && !n2.isBlank()) {
            return n1.equals(n2);
        }

        return this.getType() == other.getType() && this.getNumber() == other.getNumber();
    }

    @Override
    public int hashCode() {
        String n = this.getRepresentation();
        if (n != null && !n.isBlank()) return n.hashCode();
        int result = getType().hashCode();
        result = 31 * result + Integer.hashCode(getNumber());
        return result;
    }
}
