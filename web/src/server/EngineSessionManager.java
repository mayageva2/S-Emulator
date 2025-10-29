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
        if (session == null) return null;

        String sessionId = session.getId();

        synchronized (session) {
            EmulatorEngine engine = (EmulatorEngine) session.getAttribute(ENGINE_KEY);
            if (engine == null) {
                engine = engines.get(sessionId);
            }

            if (engine == null) {
                engine = new EmulatorEngineImpl();
                engines.put(sessionId, engine);
                session.setAttribute(ENGINE_KEY, engine);
            }

            UserDTO user = SessionUserManager.getUser(session);
            if (user != null) {
                if (engine instanceof EmulatorEngineImpl impl) {
                    UserDTO existing = impl.getSessionUser();

                    if (existing == null || !existing.getUsername().equals(user.getUsername())) {
                        impl.setSessionUser(user);
                    } else {
                        impl.setSessionUser(user);
                    }
                } else {
                    engine.setSessionUser(user);
                }
            }

            engines.put(sessionId, engine);
            session.setAttribute(ENGINE_KEY, engine);

            return engine;
        }
    }

    public static void clearEngine(HttpSession session) {
        if (session == null) return;
        String sessionId = session.getId();
        engines.remove(sessionId);
        session.removeAttribute(ENGINE_KEY);
    }

    public static void clearAllEngines() {
        engines.clear();
    }
}
