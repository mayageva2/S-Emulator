package server;

import emulator.api.dto.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/addCredits")
public class AddCreditsServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long amount = 0;
        try {
            amount = Long.parseLong(req.getParameter("amount"));
        } catch (Exception ignore) {}

        if (amount <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid amount");
            return;
        }

        userService.addCredits(amount);
        var dto = userService.getCurrentUser().orElseThrow();

        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().printf(
                "{\"status\":\"success\",\"credits\":%d}", dto.getCredits()
        );
    }
}

