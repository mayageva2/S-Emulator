package server;

import emulator.api.dto.UserDTO;
import emulator.api.dto.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/login")
public class LoginServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String username = req.getParameter("username");
        if (username == null || username.isBlank()) {
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"Missing username\"}");
            return;
        }

        if (userService.userExists(username)) {
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"Username already exists\"}");
            return;
        }

        UserDTO user = userService.loginUser(username);
        ServerEventManager.broadcast("USER_LOGIN");
        resp.getWriter().printf(
                "{\"status\":\"success\",\"username\":\"%s\",\"credits\":%d}",
                user.getUsername(), user.getCredits()
        );
    }
}
