package emulator.api.dto;

import java.io.Serializable;

public record ArchitectureInfo(String name, int cost, String description) implements Serializable {}