package server;

import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/user/charge")
public class AddCreditsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> response;

        try {
            long amount = Long.parseLong(req.getParameter("amount"));
            if (amount <= 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid amount");
                return;
            }

            UserDTO currentUser = SessionUserManager.getUser(req.getSession());
            if (currentUser == null) {
                response = Map.of("status", "error", "message", "No user logged in for this session");
                resp.getWriter().write(gson.toJson(response));
                return;
            }

            long newCredits = currentUser.getCredits() + amount;
            UserDTO updatedUser = new UserDTO(currentUser.getUsername(), newCredits);

            SessionUserManager.setUser(req.getSession(), updatedUser);
            SessionUserBinder.snapshotBack(req.getSession(), updatedUser);
            ServerEventManager.broadcast("USER_CREDITS_UPDATED");

            response = Map.of(
                    "status", "success",
                    "credits", updatedUser.getCredits()
            );

        } catch (NumberFormatException e) {
            response = Map.of("status", "error", "message", "Invalid number format");
        } catch (Exception e) {
            response = Map.of("status", "error", "message", e.getMessage());
        }

        resp.getWriter().write(gson.toJson(response));
    }
}
