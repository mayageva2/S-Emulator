package emulator.api.dto;

import emulator.logic.user.User;
import emulator.logic.user.UserManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {

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
                        u.getUsername(), 0, 0, (int) u.getCredits(), 0, 0)).collect(Collectors.toList());
    }
}
