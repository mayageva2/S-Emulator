package emulator.logic.user;

public class User {
    private final String username;
    private long credits;

    public User(String username, long initialCredits) {
        this.username = username;
        this.credits = initialCredits;
    }

    public String getUsername() { return username; }
    public long getCredits() { return credits; }

    public void addCredits(long amount) {
        if (amount > 0) credits += amount;
    }

    public boolean deductCredits(long cost) {
        if (credits >= cost) {
            credits -= cost;
            return true;
        }
        return false;
    }
}
