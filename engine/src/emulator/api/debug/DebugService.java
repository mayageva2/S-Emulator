package emulator.api.debug;

public interface DebugService {
    DebugSnapshot start(Long[] inputs, int degree) throws Exception;
    DebugSnapshot start(Long[] inputs, int degree, String programName) throws Exception;
    DebugSnapshot stepOver() throws Exception;
    DebugSnapshot resume() throws Exception;
    DebugSnapshot stop() throws Exception;
    boolean isAlive();
}