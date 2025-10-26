package server;

import com.google.gson.Gson;
import emulator.api.dto.UserService;
import emulator.api.dto.UserStats;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/user/list")
public class ListUsersServlet extends HttpServlet {
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<UserStats> users = userService.getAllUserStats();

            String json = gson.toJson(Map.of(
                    "status", "success",
                    "users", users
            ));

            resp.getWriter().write(json);

        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = gson.toJson(Map.of(
                    "status", "error",
                    "message", "Failed to retrieve user list: " + e.getMessage()
            ));
            resp.getWriter().write(errorJson);
        }
    }
}
