package server;

import jakarta.servlet.http.HttpSession;

import java.util.*;

public class ServerEventManager {
    private static final Map<String, List<String>> userEvents = Collections.synchronizedMap(new HashMap<>());

    public static void broadcast(String eventType) {
        synchronized (userEvents) {
            for (List<String> queue : userEvents.values()) {
                queue.add(eventType);
            }
        }
    }

    public static String consumeEvent(HttpSession session) {
        String id = session.getId();
        synchronized (userEvents) {
            userEvents.putIfAbsent(id, new ArrayList<>());
            List<String> queue = userEvents.get(id);
            if (!queue.isEmpty()) {
                return queue.remove(0);
            }
        }
        return "NONE";
    }
}

