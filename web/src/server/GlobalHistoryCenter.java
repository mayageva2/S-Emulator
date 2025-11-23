package server;

import emulator.api.dto.RunRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalHistoryCenter {
    private static final Map<String, List<RunRecord>> history =
            new ConcurrentHashMap<>();

    private GlobalHistoryCenter() {}

    // Add a record for a user
    public static void addRecord(String username, RunRecord record) {
        history
                .computeIfAbsent(username, u -> Collections.synchronizedList(new ArrayList<>()))
                .add(record);
    }

    // Safe copy of a user's full history
    public static List<RunRecord> getHistory(String username) {
        List<RunRecord> list = history.get(username);
        if (list == null) return List.of();

        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    // clear a user's history
    public static void clearUser(String username) {
        history.remove(username);
    }

    // clear everything
    public static void clearAll() {
        history.clear();
    }
}
