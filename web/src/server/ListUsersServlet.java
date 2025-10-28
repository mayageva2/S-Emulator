package server;

import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

@WebServlet("/user/list")
public class ListUsersServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private static final Map<String, UserDTO> activeUsers = Collections.synchronizedMap(new HashMap<>());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        UserDTO user = SessionUserManager.getUser(req.getSession());
        if (user != null) {
            activeUsers.put(req.getSession().getId(), user);
        }

        resp.getWriter().write(gson.toJson(Map.of(
                "status", "success",
                "users", new ArrayList<>(activeUsers.values())
        )));
    }
}
