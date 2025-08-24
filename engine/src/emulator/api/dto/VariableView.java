package emulator.api.dto;

public record VariableView(
        String name,
        VarType  type,
        int number,
        long value
) {}