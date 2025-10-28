package emulator.logic.user;

import emulator.api.dto.UserDTO;
import emulator.api.dto.UserStats;

import java.util.*;
import java.util.stream.Collectors;

public class UserService {

    private final Map<String, User> users = new HashMap<>();
    private User currentUser;

    public void login(String username) {
        currentUser = users.computeIfAbsent(username, u -> new User(u, 100));
    }

    public void logout() {
        currentUser = null;
    }

    public Optional<UserDTO> getCurrentUser() {
        if (currentUser == null) return Optional.empty();
        return Optional.of(new UserDTO(currentUser.getUsername(), currentUser.getCredits()));
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    public void addCredits(long amount) {
        if (currentUser != null) currentUser.addCredits(amount);
    }

    public List<UserStats> getAllUserStats() {
        return users.values().stream()
                .map(u -> new UserStats(
                        u.getUsername(),
                        u.getMainPrograms(),
                        u.getFunctions(),
                        (int) u.getCredits(),
                        u.getUsedCredits(),
                        u.getRuns()))
                .collect(Collectors.toList());
    }

    public void incrementMainProgramsAndFunctions(int functionsCount) {
        if (currentUser != null) {
            currentUser.incrementMainPrograms();
            currentUser.incrementFunctions(functionsCount);
        }
    }

    public void incrementRuns() {
        if (currentUser != null) currentUser.incrementRuns();
    }

    public UserDTO getSessionUser() {
        return (currentUser == null)
                ? null
                : new UserDTO(currentUser.getUsername(), currentUser.getCredits());
    }
}
