package emulator.api.dto;

import emulator.logic.user.UserManager;
import java.util.Optional;

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

    public void addCredits(long amount) {
        UserManager.addCredits(amount);
    }

    public void logout() {
        UserManager.logout();
    }
}
