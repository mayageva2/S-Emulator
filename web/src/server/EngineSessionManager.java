package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.FunctionInfo;
import emulator.api.dto.UserDTO;
import jakarta.servlet.http.HttpSession;

import java.util.concurrent.ConcurrentHashMap;

public class EngineSessionManager {

    private static final String ENGINE_KEY = "sessionEngine";

    public static EmulatorEngine getEngine(HttpSession session) {
        if (session == null) return null;

        synchronized (session) {
            EmulatorEngine engine = (EmulatorEngine) session.getAttribute(ENGINE_KEY);

            // Engine doesn't exist yet → create personal engine
            if (engine == null) {
                engine = new EmulatorEngineImpl();

                // load shared functions from global DB
                for (FunctionInfo f : GlobalDataCenter.getFunctions()) {
                    engine.getFunctionService().addFunction(
                            f.functionName(),
                            f.programName(),
                            f.username(),
                            f.instructionCount(),
                            f.maxDegree(),
                            0
                    );
                }

                session.setAttribute(ENGINE_KEY, engine);
            }

            // always sync engine user with session user
            UserDTO user = SessionUserManager.getUser(session);
            if (user != null) { engine.setSessionUser(user); }

            return engine;
        }
    }

    public static void clearEngine(HttpSession session) {
        if (session != null) {
            session.removeAttribute(ENGINE_KEY);
        }
    }
}

