package emulator.api.dto;

import emulator.logic.user.User;
import emulator.logic.user.UserManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {
    private static final UserService instance = new UserService();

    public static UserService getInstance() {
        return instance;
    }

    public UserDTO loginUser(String username) {
        UserManager.login(username);
        var user = UserManager.getCurrentUser().orElseThrow();
        return new UserDTO(user.getUsername(), user.getCredits());
    }

    public Optional<UserDTO> getCurrentUser() {
        return UserManager.getCurrentUser()
                .map(u -> new UserDTO(u.getUsername(), u.getCredits()));
    }

    public boolean userExists(String username) {
        Collection<User> users = UserManager.getAllUsers();
        return users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    public void addCredits(long amount) {
        UserManager.addCredits(amount);
    }

    public void logout() {
        UserManager.logout();
    }

    public List<UserStats> getAllUserStats() {
        return UserManager.getAllUsers().stream()
                .map(u -> new UserStats(
                        u.getUsername(),
                        u.getMainPrograms(),
                        u.getFunctions(),
                        (int) u.getCredits(),
                        u.getUsedCredits(),
                        u.getRuns()
                ))
                .collect(Collectors.toList());
    }

    public void incrementMainProgramsForCurrentUser() {
        UserManager.getCurrentUser().ifPresent(user -> {
            try {
                var cls = user.getClass();
                var field = cls.getDeclaredField("mainPrograms");
                field.setAccessible(true);
                int current = (int) field.get(user);
                field.set(user, current + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void incrementMainProgramsAndFunctions(int functionsCount) {
        UserManager.getCurrentUser().ifPresent(u -> {
            u.incrementMainPrograms();
            u.incrementFunctions(functionsCount);
        });
    }

    public void incrementRuns() {
        UserManager.getCurrentUser().ifPresent(u -> u.incrementRuns());
    }

}
