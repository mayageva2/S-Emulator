package server;

import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

@WebServlet("/user/current")
public class CurrentUserServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        UserDTO currentUser = SessionUserManager.getUser(req.getSession());
        if (currentUser == null) {
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", "No active session"
            )));
            return;
        }

        resp.getWriter().write(gson.toJson(Map.of(
                "status", "success",
                "user", Map.of(
                        "username", currentUser.getUsername(),
                        "credits", currentUser.getCredits()
                )
        )));
    }
}
