package emulator.api.dto;

import java.io.Serializable;

public record ArchitectureInfo(
        String name,          // Architecture name (e.g., I, II, III, IV)
        int cost,             // Execution cost
        String description    // Description
) implements Serializable {}