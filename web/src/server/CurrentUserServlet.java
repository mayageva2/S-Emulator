package server;

import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import emulator.api.dto.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@WebServlet("/user/current")
public class CurrentUserServlet extends HttpServlet {
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        Optional<UserDTO> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            String json = gson.toJson(Map.of(
                    "status", "error",
                    "message", "No user logged in"
            ));
            resp.getWriter().write(json);
            return;
        }

        UserDTO user = currentUser.get();
        String json = gson.toJson(Map.of(
                "status", "success",
                "user", user
        ));
        resp.getWriter().write(json);
    }
}
