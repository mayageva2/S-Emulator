package emulator.logic.user;

import java.util.Optional;

public class UserManager {
    private static User currentUser;

    public static void login(String username) {
        currentUser = new User(username, 100);
    }

    public static void logout() { currentUser = null; }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }
}
