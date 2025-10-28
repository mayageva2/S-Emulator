package server;

import emulator.api.dto.UserDTO;
import jakarta.servlet.http.HttpSession;

public class SessionUserBinder {

    private static final String USER_SNAPSHOT_KEY = "userSnapshot";

    public static void bind(HttpSession session) {
        UserDTO user = SessionUserManager.getUser(session);
        if (user != null) {
            session.setAttribute(USER_SNAPSHOT_KEY, new UserDTO(user.getUsername(), user.getCredits()));
        }
    }

    public static void snapshotBack(HttpSession session, UserDTO updatedUser) {
        if (updatedUser != null) {
            SessionUserManager.setUser(session, updatedUser);
            session.setAttribute(USER_SNAPSHOT_KEY, new UserDTO(updatedUser.getUsername(), updatedUser.getCredits()));
        }
    }

    public static UserDTO getSnapshot(HttpSession session) {
        Object obj = session.getAttribute(USER_SNAPSHOT_KEY);
        return (obj instanceof UserDTO) ? (UserDTO) obj : null;
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(USER_SNAPSHOT_KEY);
    }
}
