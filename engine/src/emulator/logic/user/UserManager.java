package emulator.logic.user;

import emulator.api.dto.UserDTO;
import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class UserManager {

    private static final ConcurrentHashMap<String, User> allUsers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, User> activeSessions = new ConcurrentHashMap<>();

    public static User login(HttpSession session, String username) {
        User user = allUsers.computeIfAbsent(username, u -> new User(u, 100));
        activeSessions.put(session.getId(), user);
        session.setAttribute("currentUser", user);
        return user;
    }

    public static Optional<User> getCurrentUser(HttpSession session) {
        Object obj = session.getAttribute("currentUser");
        if (obj instanceof User) {
            return Optional.of((User) obj);
        }
        User fromMap = activeSessions.get(session.getId());
        return Optional.ofNullable(fromMap);
    }

    public static void addCredits(HttpSession session, long amount) {
        getCurrentUser(session).ifPresent(u -> u.addCredits(amount));
    }

    public static boolean charge(HttpSession session, long amount) {
        return getCurrentUser(session)
                .map(u -> u.deductCredits(amount))
                .orElse(false);
    }

    public static void logout(HttpSession session) {
        activeSessions.remove(session.getId());
        session.removeAttribute("currentUser");
    }

    public static Iterable<User> getAllUsers() {
        return allUsers.values();
    }
}
