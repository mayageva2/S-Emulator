package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.UserDTO;
import jakarta.servlet.http.HttpSession;

import java.util.concurrent.ConcurrentHashMap;

public class EngineSessionManager {

    private static final ConcurrentHashMap<String, EmulatorEngine> engines = new ConcurrentHashMap<>();
    private static final String ENGINE_KEY = "sessionEngine";

    public static EmulatorEngine getEngine(HttpSession session) {
        String sessionId = session.getId();
        synchronized (session) {
            EmulatorEngine engine = engines.computeIfAbsent(sessionId, id -> new EmulatorEngineImpl());
            UserDTO user = SessionUserManager.getUser(session);

            if (user != null) {
                if (engine instanceof EmulatorEngineImpl impl) {
                    UserDTO existing = impl.getSessionUser();
                    if (existing == null || !existing.getUsername().equals(user.getUsername())) {
                        engine = new EmulatorEngineImpl();
                        engine.setSessionUser(user);
                        engines.put(sessionId, engine);
                        session.setAttribute(ENGINE_KEY, engine);
                    } else {
                        engine.setSessionUser(user);
                    }
                } else {
                    engine.setSessionUser(user);
                }
            }

            session.setAttribute(ENGINE_KEY, engine);
            return engine;
        }
    }

    public static void clearEngine(HttpSession session) {
        String sessionId = session.getId();
        engines.remove(sessionId);
        session.removeAttribute(ENGINE_KEY);
    }

    public static void clearAllEngines() {
        engines.clear();
    }
}
