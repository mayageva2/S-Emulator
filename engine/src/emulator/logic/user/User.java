package emulator.logic.user;

public class User {
    private final String username;
    private long credits;

    private int mainPrograms = 0;
    private int functions = 0;
    private int usedCredits = 0;
    private int runs = 0;

    public User(String username, long initialCredits) {
        this.username = username;
        this.credits = initialCredits;
    }

    public String getUsername() { return username; }
    public long getCredits() { return credits; }
    public int getMainPrograms() { return mainPrograms; }
    public int getFunctions() { return functions; }
    public int getUsedCredits() { return usedCredits; }
    public int getRuns() { return runs; }

    public void addCredits(long amount) {
        if (amount > 0) credits += amount;
    }

    public boolean deductCredits(long cost) {
        if (credits >= cost) {
            credits -= cost;
            usedCredits += cost;
            return true;
        }
        return false;
    }

    public void incrementMainPrograms() { mainPrograms++; }

    public void incrementFunctions(int count) {
        if (count > 0) functions += count;
    }

    public void incrementRuns() { runs++; }
}
