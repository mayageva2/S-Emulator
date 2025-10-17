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
        String username = req.getParameter("username");
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username");
            return;
        }

        UserDTO user = userService.loginUser(username);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().printf(
                "{\"status\":\"success\",\"username\":\"%s\",\"credits\":%d}",
                user.getUsername(), user.getCredits()
        );
    }
}


