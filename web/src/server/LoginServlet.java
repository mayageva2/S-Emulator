package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

@WebServlet("/user/login")
public class LoginServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        if (username == null || username.isBlank()) {
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", "Missing username"
            )));
            return;
        }

        req.getSession().invalidate();
        HttpSession session = req.getSession(true);
        ServerEventManager.registerSession(session);
        UserDTO newUser = new UserDTO(username, 0);
        SessionUserManager.setUser(session, newUser);
        EmulatorEngine engine = new EmulatorEngineImpl();
        engine.setSessionUser(newUser);
        session.setAttribute("sessionEngine", engine);
        EngineSessionManager.clearEngine(session);
        EngineSessionManager.getEngine(session);
        ServerEventManager.broadcast("USER_LOGIN");

        resp.getWriter().write(gson.toJson(Map.of(
                "status", "success",
                "username", newUser.getUsername(),
                "credits", newUser.getCredits(),
                "sessionId", session.getId()
        )));
    }
}
