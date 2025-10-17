package server;

import emulator.api.dto.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/credits")
public class CreditsServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var dto = userService.getCurrentUser().orElse(null);
        resp.setContentType("application/json;charset=UTF-8");
        if (dto == null) {
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"No user logged in\"}");
        } else {
            resp.getWriter().printf("{\"status\":\"success\",\"credits\":%d}", dto.getCredits());
        }
    }
}

