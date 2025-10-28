package emulator.logic.user;

import java.util.*;

public class UserManager {
    private final Map<String, User> users = new HashMap<>();
    private User currentUser;

    public void login(String username) {
        currentUser = users.computeIfAbsent(username, u -> new User(u, 100));
    }

    public void logout() { currentUser = null; }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void addCredits(long amount) {
        if (currentUser != null) currentUser.addCredits(amount);
    }

    public boolean charge(long amount) {
        return currentUser != null && currentUser.deductCredits(amount);
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }
}
