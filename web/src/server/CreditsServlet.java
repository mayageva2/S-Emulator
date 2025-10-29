package server;

import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/user/credits")
public class CreditsServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", "No active session"
            )));
            return;
        }

        UserDTO currentUser = SessionUserManager.getUser(session);
        if (currentUser == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", "No user logged in for this session"
            )));
            return;
        }

        resp.getWriter().write(gson.toJson(Map.of(
                "status", "success",
                "user", currentUser
        )));
    }
}
