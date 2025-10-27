package emulator.logic.architecture;

public enum ArchitectureType {
    I(5), II(100), III(500), IV(1000);

    private final int cost;

    ArchitectureType(int cost) { this.cost = cost; }
    public int getCost() { return cost; }
}
