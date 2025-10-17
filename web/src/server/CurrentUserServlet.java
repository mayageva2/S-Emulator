package server;

import emulator.logic.user.UserManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/current")
public class CurrentUserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        var userOpt = UserManager.getCurrentUser();
        if (userOpt.isEmpty()) {
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"No user logged in\"}");
            return;
        }
        var user = userOpt.get();
        resp.getWriter().printf("{\"status\":\"success\",\"username\":\"%s\",\"credits\":%d}",
                user.getUsername(), user.getCredits());
    }
}

