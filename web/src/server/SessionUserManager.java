package server;

import emulator.api.dto.UserDTO;
import jakarta.servlet.http.HttpSession;

public class SessionUserManager {
    private static final String USER_KEY = "sessionUser";

    public static UserDTO getUser(HttpSession session) {
        return (UserDTO) session.getAttribute(USER_KEY);
    }

    public static void setUser(HttpSession session, UserDTO user) {
        session.setAttribute(USER_KEY, user);
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(USER_KEY);
    }
}
