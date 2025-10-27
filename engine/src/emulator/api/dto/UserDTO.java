package emulator.api.dto;

public class UserDTO {
    private final String username;
    private final long credits;

    public UserDTO(String username, long credits) {
        this.username = username;
        this.credits = credits;
    }

    // GETTERS
    public String getUsername() { return username; }
    public long getCredits() { return credits; }
}

