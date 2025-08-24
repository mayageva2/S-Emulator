package emulator.api.dto;

import emulator.logic.variable.VariableType;

public record VariableView(
        String name,
        VariableType type,
        int number,
        long value
) {}