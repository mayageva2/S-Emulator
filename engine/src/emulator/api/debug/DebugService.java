package emulator.api.debug;

public interface DebugService {
    DebugSnapshot start(Long[] inputs, int degree) throws Exception;   // Start debug func
    DebugSnapshot start(Long[] inputs, int degree, String programName) throws Exception;  // Start debug func
    DebugSnapshot stepOver() throws Exception;     // Execute one step
    DebugSnapshot resume() throws Exception;       // Continue execution
    DebugSnapshot stop() throws Exception;         // Stop debug
    boolean isAlive();   // Checks if debug session is active
}