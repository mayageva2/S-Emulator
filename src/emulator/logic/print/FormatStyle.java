package emulator.logic.print;

public enum FormatStyle {

    USER(true,false),
    DEBUG(true, true),
    EXPANDED(true, false),
    ;

    private final boolean showLabels;
    private final boolean showCycles;

    FormatStyle (boolean showLabels, boolean showCycles) {
        this.showLabels = showLabels;
        this.showCycles = showCycles;
    }

    public boolean isShowLabels() { return showLabels; }
    public boolean isShowCycles() { return showCycles; }
}
