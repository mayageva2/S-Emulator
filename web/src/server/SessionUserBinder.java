package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.UserDTO;
import jakarta.servlet.http.HttpSession;

public class SessionUserBinder {

    private static final String USER_SNAPSHOT_KEY = "userSnapshot";
    public static void bind(HttpSession session) {
        if (session == null) return;

        UserDTO user = SessionUserManager.getUser(session);
        if (user != null) {
            session.setAttribute(USER_SNAPSHOT_KEY, cloneUser(user));
            syncWithEngine(session, user);
        }
    }

    public static void snapshotBack(HttpSession session, UserDTO updatedUser) {
        if (session == null || updatedUser == null) return;

        SessionUserManager.setUser(session, updatedUser);
        session.setAttribute(USER_SNAPSHOT_KEY, cloneUser(updatedUser));
        syncWithEngine(session, updatedUser);
    }

    public static UserDTO getSnapshot(HttpSession session) {
        if (session == null) return null;
        Object obj = session.getAttribute(USER_SNAPSHOT_KEY);
        return (obj instanceof UserDTO) ? (UserDTO) obj : null;
    }

    public static void clear(HttpSession session) {
        if (session != null) {
            session.removeAttribute(USER_SNAPSHOT_KEY);
        }
    }

    private static UserDTO cloneUser(UserDTO src) {
        return new UserDTO(src.getUsername(), src.getCredits());
    }

    private static void syncWithEngine(HttpSession session, UserDTO user) {
        if (session == null || user == null) return;

        EmulatorEngine engine = EngineSessionManager.getEngine(session);
        if (engine instanceof EmulatorEngineImpl impl) {
            impl.setSessionUser(user);
        }
    }
}
