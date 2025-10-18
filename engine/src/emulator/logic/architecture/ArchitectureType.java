package emulator.logic.architecture;

import emulator.logic.instruction.InstructionData;

public enum ArchitectureType {
    I, II, III, IV;

    public static ArchitectureType fromInstruction(InstructionData instr) {
        int cost = instr.getBaseCreditCost();
        return switch (cost) {
            case 5 -> I;
            case 100 -> II;
            case 500 -> III;
            case 1000 -> IV;
            default -> throw new IllegalArgumentException("Unknown cost: " + cost);
        };
    }
}
