package server;

import jakarta.servlet.http.HttpSession;
import java.util.*;

public class ServerEventManager {

    private static final Map<String, Deque<String>> userEvents = new HashMap<>();
    private static final int MAX_EVENTS_PER_SESSION = 50;

    public static synchronized void registerSession(HttpSession session) {
        if (session == null) return;
        String id = session.getId();
        userEvents.putIfAbsent(id, new ArrayDeque<>());
        System.out.println("[ServerEventManager] Registered session: " + id);
    }

    public static synchronized void removeSession(HttpSession session) {
        if (session == null) return;
        String id = session.getId();
        if (userEvents.remove(id) != null) {
            System.out.println("[ServerEventManager] Removed session: " + id);
        }
    }

    public static synchronized void broadcast(String eventType) {
        if (eventType == null) return;

        System.out.println("[ServerEventManager] Broadcasting event: " + eventType +
                " to " + userEvents.size() + " sessions");

        Iterator<Map.Entry<String, Deque<String>>> it = userEvents.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<String>> entry = it.next();
            Deque<String> queue = entry.getValue();

            if (queue == null) {
                it.remove();
                continue;
            }

            if (queue.size() >= MAX_EVENTS_PER_SESSION) {
                queue.pollFirst();
            }

            queue.addLast(eventType);
        }
    }

    public static synchronized String consumeEvent(HttpSession session) {
        if (session == null) return "NONE";
        String id = session.getId();

        Deque<String> queue = userEvents.get(id);
        if (queue == null) {
            queue = new ArrayDeque<>();
            userEvents.put(id, queue);
            return "NONE";
        }

        String event = queue.pollFirst();
        if (event != null) {
            System.out.println("[ServerEventManager] Session " + id + " consumed event: " + event);
            return event;
        }
        return "NONE";
    }

    public static synchronized void clearAll() {
        userEvents.clear();
        System.out.println("[ServerEventManager] Cleared all sessions");
    }
}
