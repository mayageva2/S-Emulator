package emulator.api.dto;

import emulator.logic.user.User;
import emulator.logic.user.UserManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {
    private static final UserService instance = new UserService();

    // This func returns the singleton instance
    public static UserService getInstance() {
        return instance;
    }

    // This func logs in a user and returns their data
    public UserDTO loginUser(String username) {
        UserManager.login(username);
        var user = UserManager.getCurrentUser().orElseThrow();
        return new UserDTO(user.getUsername(), user.getCredits());
    }

    // This func returns the currently logged-in user
    public Optional<UserDTO> getCurrentUser() {
        return UserManager.getCurrentUser()
                .map(u -> new UserDTO(u.getUsername(), u.getCredits()));
    }

    // This func checks if a user with the given name exists
    public boolean userExists(String username) {
        Collection<User> users = UserManager.getAllUsers();
        return users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    // This func adds credits to the current user
    public void addCredits(long amount) {
        UserManager.addCredits(amount);
    }

    // This func returns a list of stats for all users
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

    // This func increments both main programs and functions
    public void incrementMainProgramsAndFunctions(int functionsCount) {
        UserManager.getCurrentUser().ifPresent(u -> {
            u.incrementMainPrograms();
            u.incrementFunctions(functionsCount);
        });
    }

    // This func increments the total number of runs
    public void incrementRuns() {
        UserManager.getCurrentUser().ifPresent(u -> u.incrementRuns());
    }

}
