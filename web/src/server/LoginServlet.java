package server;

import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/user/login")
public class LoginServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String username = req.getParameter("username");
            if (username == null || username.isBlank()) {
                response.put("status", "error");
                response.put("message", "Missing username");
                writeJson(resp, response);
                return;
            }

            HttpSession session = req.getSession(false);
            if (session != null) {
                SessionUserManager.clearUser(session);
                ServerEventManager.removeSession(session);
                session.invalidate();
            }

            session = req.getSession(true);
            ServerEventManager.registerSession(session);

            UserDTO user = new UserDTO(username, 0);
            SessionUserManager.setUser(session, user);

            ServerEventManager.broadcast("USER_LOGIN");

            System.out.printf("[LoginServlet] Login success for '%s' | session=%s%n",
                    username, session.getId());

            response.put("status", "success");
            response.put("username", user.getUsername());
            response.put("credits", user.getCredits());
            response.put("sessionId", session.getId());

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        writeJson(resp, response);
    }

    private void writeJson(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        resp.getWriter().write(gson.toJson(data));
    }
}
