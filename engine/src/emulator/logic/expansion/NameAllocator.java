package emulator.logic.expansion;

import java.util.HashSet;
import java.util.Set;

public final class NameAllocator {
    private final String prefix;
    private final Set<String> used;
    private int counter;

    NameAllocator(String prefix, Set<String> initiallyUsed) {
        this.prefix = prefix;
        this.used = new HashSet<>();
        if (initiallyUsed != null) {
            for (String s : initiallyUsed) {
                if (s == null) continue;
                String t = s.trim();
                if (t.isEmpty()) continue;
                if (t.length() > 1 && Character.toLowerCase(t.charAt(0)) == Character.toLowerCase(prefix.charAt(0))) {
                    int n = 0;
                    for (int i = 1; i < t.length() && Character.isDigit(t.charAt(i)); i++) {
                        n = n * 10 + (t.charAt(i) - '0');
                    }
                    counter = Math.max(counter, n);
                }
                used.add(normalize(t));
            }
        }
    }

    String next() {
        int n = counter + 1;
        String candidate = format(n);
        while (used.contains(candidate)) {
            n++;
            candidate = format(n);
        }
        counter = n;
        used.add(candidate);
        return candidate;
    }

    private String format(int n) { return prefix + n; }
    private String normalize(String s) {
        if (s.length() > 1 && Character.toLowerCase(s.charAt(0)) == Character.toLowerCase(prefix.charAt(0))) {
            return Character.toUpperCase(prefix.charAt(0)) + s.substring(1);
        }
        return s;
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

}
