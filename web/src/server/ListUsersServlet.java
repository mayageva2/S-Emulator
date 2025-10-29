package server;

import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/user/list")
public class ListUsersServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private static final Map<String, UserDTO> activeUsers = Collections.synchronizedMap(new HashMap<>());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        Map<String, Object> response = new LinkedHashMap<>();

        if (session == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.put("status", "error");
            response.put("message", "No active session");
            writeJson(resp, response);
            return;
        }

        UserDTO user = SessionUserManager.getUser(session);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.put("status", "error");
            response.put("message", "No user logged in for this session");
            writeJson(resp, response);
            return;
        }
        activeUsers.put(session.getId(), user);
        activeUsers.keySet().removeIf(id -> !session.getId().equals(id) && !session.isNew());

        response.put("status", "success");
        response.put("users", new ArrayList<>(activeUsers.values()));
        writeJson(resp, response);
    }

    private void writeJson(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        resp.getWriter().write(gson.toJson(data));
    }
}
