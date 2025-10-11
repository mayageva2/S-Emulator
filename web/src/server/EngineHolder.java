package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;

public class EngineHolder {
    private static final EmulatorEngine engine = new EmulatorEngineImpl();

    public static EmulatorEngine getEngine() {
        return engine;
    }
}
