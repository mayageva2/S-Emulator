package emulator.api.debug;

public interface DebugSession extends AutoCloseable {
    DebugSnapshot start(Long[] inputs, int degree) throws Exception; // Start debug
    DebugSnapshot stepOver() throws Exception;   // Execute one step
    DebugSnapshot resume() throws Exception;     // Continue execution
    DebugSnapshot stop() throws Exception;       // Stop debug
    boolean isAlive();    // Checks if debug session is active

    // Automatically stop when closed
    @Override
    default void close() throws Exception { stop(); }
}
