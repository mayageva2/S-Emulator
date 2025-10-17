package emulator.logic.user;

import java.util.*;

public class UserManager {
    private static final Map<String, User> users = new HashMap<>();
    private static User currentUser;

    public static void login(String username) {
        currentUser = users.computeIfAbsent(username, u -> new User(u, 100)); // ברירת מחדל 100
    }

    public static void logout() { currentUser = null; }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static void addCredits(long amount) {
        if (currentUser != null) currentUser.addCredits(amount);
    }

    public static boolean charge(long amount) {
        return currentUser != null && currentUser.deductCredits(amount);
    }
}
