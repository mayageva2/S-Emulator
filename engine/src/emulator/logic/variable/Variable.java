package emulator.logic.variable;

public interface Variable {
    VariableType getType();
    public int getNumber();
    String getRepresentation();

    Variable RESULT = new VariableImpl(VariableType.RESULT, 0);
}
