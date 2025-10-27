package emulator.api.dto;

public class UserStats {
    private final String username;
    private final int mainPrograms;
    private final int functions;
    private final int credits;
    private final int usedCredits;
    private final int runs;

    public UserStats(String username, int mainPrograms, int functions,
                        int credits, int usedCredits, int runs) {
        this.username = username;
        this.mainPrograms = mainPrograms;
        this.functions = functions;
        this.credits = credits;
        this.usedCredits = usedCredits;
        this.runs = runs;
    }

    // GETTERS
    public String getUsername() { return username; }
    public int getMainPrograms() { return mainPrograms; }
    public int getFunctions() { return functions; }
    public int getCredits() { return credits; }
    public int getUsedCredits() { return usedCredits; }
    public int getRuns() { return runs; }
}
