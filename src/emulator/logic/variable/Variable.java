package emulator.logic.variable;

public interface Variable {
    VariableType getType();
    public int getNumber();
    String getRepresentation();
    public String getName();
    public boolean isEmpty();

    Variable RESULT = new VariableImpl(VariableType.RESULT, 0, "y");
}
