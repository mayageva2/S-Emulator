package emulator.api.debug;

public interface DebugSession extends AutoCloseable {
    DebugSnapshot start(Long[] inputs, int degree) throws Exception;
    DebugSnapshot stepOver() throws Exception;
    DebugSnapshot resume() throws Exception;
    DebugSnapshot stop() throws Exception;
    boolean isAlive();

    @Override
    default void close() throws Exception { stop(); }
}
