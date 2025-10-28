package server;

import jakarta.servlet.http.HttpSession;
import java.util.*;

public class ServerEventManager {

    private static final Map<String, Queue<String>> userEvents = Collections.synchronizedMap(new HashMap<>());

    public static void registerSession(HttpSession session) {
        String id = session.getId();
        synchronized (userEvents) {
            userEvents.putIfAbsent(id, new LinkedList<>());
            System.out.println("[ServerEventManager] Registered session: " + id);
        }
    }

    public static void removeSession(HttpSession session) {
        String id = session.getId();
        synchronized (userEvents) {
            userEvents.remove(id);
            System.out.println("[ServerEventManager] Removed session: " + id);
        }
    }

    public static void broadcast(String eventType) {
        synchronized (userEvents) {
            System.out.println("[ServerEventManager] Broadcasting event: " + eventType +
                    " to " + userEvents.size() + " sessions");
            for (Queue<String> queue : userEvents.values()) {
                queue.add(eventType);
            }
        }
    }

    public static String consumeEvent(HttpSession session) {
        String id = session.getId();
        synchronized (userEvents) {
            userEvents.putIfAbsent(id, new LinkedList<>());
            Queue<String> queue = userEvents.get(id);
            if (!queue.isEmpty()) {
                String event = queue.poll();
                System.out.println("[ServerEventManager] Session " + id + " consumed event: " + event);
                return event;
            }
        }
        return "NONE";
    }
}
