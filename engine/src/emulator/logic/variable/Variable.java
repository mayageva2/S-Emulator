package emulator.logic.variable;

public interface Variable {
    VariableType getType();
    public int getNumber();
    String getRepresentation();
    boolean isEmpty();

    Variable RESULT = new VariableImpl(VariableType.RESULT, 0);
}
