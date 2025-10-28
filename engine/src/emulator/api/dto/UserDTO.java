package emulator.api.dto;

public class UserDTO {
    private final String username;
    private long credits;
    private long runsCount;

    public UserDTO(String username, long credits) {
        this.username = username;
        this.credits = credits;
        this.runsCount = 0;
    }

    public UserDTO(UserDTO other) {
        this.username = other.username;
        this.credits = other.credits;
        this.runsCount = other.runsCount;
    }

    // GETTERS
    public String getUsername() { return username; }
    public long getCredits() { return credits; }
    public long getRunsCount() { return runsCount; }

    // SETTERS
    public void setCredits(long credits) {
        this.credits = credits;
    }

    public void setRunsCount(long runsCount) {
        this.runsCount = runsCount;
    }

    public void incrementRuns() {
        this.runsCount++;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "username='" + username + '\'' +
                ", credits=" + credits +
                ", runsCount=" + runsCount +
                '}';
    }
}
