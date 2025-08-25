package emulator.exception;

public class ProgramNotLoadedException extends ProgramException {
    public ProgramNotLoadedException() {
        super(
                "PROGRAM_NOT_LOADED",
                "No valid program is currently loaded.",
                java.util.Map.of()
        );
    }
}
