package emulator.logic.expansion;

import java.util.HashSet;
import java.util.Set;

public final class NameAllocator {
    private final String prefix;
    private final Set<String> usedNames;
    private int counter;

    public NameAllocator(String prefix, Set<String> alreadyUsed) {
        this.prefix = prefix;
        this.usedNames = new HashSet<>(alreadyUsed);
        this.counter = startFrom(alreadyUsed, prefix);
    }

    private static int startFrom(java.util.Set<String> used, String prefix) {
        int max = 0;
        for (String s : used) {
            if (s != null && s.startsWith(prefix)) {
                try {
                    int n = Integer.parseInt(s.substring(prefix.length()));
                    if (n > max) max = n;
                } catch (NumberFormatException ignore) {}
            }
        }
        return Math.max(1, max + 1);
    }

    String next() {
        while (true) {
            String fullName = prefix + counter++;
            if (usedNames.add(fullName)) return fullName;
        }
    }
}
