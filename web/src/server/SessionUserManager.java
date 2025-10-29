package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.UserDTO;
import jakarta.servlet.http.HttpSession;

public class SessionUserManager {

    private static final String USER_KEY = "sessionUser";
    public static UserDTO getUser(HttpSession session) {
        if (session == null) {
            System.out.println("[SessionUserManager] getUser called with null session");
            return null;
        }

        UserDTO user = (UserDTO) session.getAttribute(USER_KEY);
        if (user == null) {
            System.out.println("[SessionUserManager] No user found for session: " + session.getId());
        } else {
            System.out.println("[SessionUserManager] Found user: " + user.getUsername() +
                    " (credits=" + user.getCredits() + ") for session: " + session.getId());
        }
        return user;
    }

    public static void setUser(HttpSession session, UserDTO user) {
        if (session == null || user == null) return;
        session.setAttribute(USER_KEY, user);
        System.out.println("[SessionUserManager] User set: " + user.getUsername() +
                " for session: " + session.getId());

        EmulatorEngine engine = EngineSessionManager.getEngine(session);
        if (engine instanceof EmulatorEngineImpl impl) {
            impl.setSessionUser(user);
        }

        SessionUserBinder.bind(session);
    }

    public static void clearUser(HttpSession session) {
        if (session == null) return;
        Object existing = session.getAttribute(USER_KEY);
        if (existing != null) {
            System.out.println("[SessionUserManager] Clearing user from session: " + session.getId());
        }
        session.removeAttribute(USER_KEY);
        SessionUserBinder.clear(session);
    }

    public static void clear(HttpSession session) {
        clearUser(session);
    }
}
