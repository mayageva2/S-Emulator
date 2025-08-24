package emulator.api.dto;

import java.util.List;

public record ProgramView(
        List<InstructionView> instructions
) {}
