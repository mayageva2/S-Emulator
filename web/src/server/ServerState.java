package server;

import emulator.api.dto.UserDTO;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerState {
    // Shared singleton map for all servlets
    private static final Map<String, UserDTO> activeUsers =
            Collections.synchronizedMap(new HashMap<>());

    public static Map<String, UserDTO> getActiveUsers() {
        return activeUsers;
    }

    public static void addUser(String sessionId, UserDTO user) {
        activeUsers.put(sessionId, user);
    }

    public static void removeUser(String sessionId) {
        activeUsers.remove(sessionId);
    }

    public static void clearAll() {
        activeUsers.clear();
    }
}
