package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.FunctionInfo;
import emulator.api.dto.FunctionService;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/functions")
public class FunctionsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> response;

        try {
            HttpSession session = req.getSession(false);
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response = Map.of(
                        "status", "error",
                        "message", "No active session"
                );
                resp.getWriter().write(gson.toJson(response));
                return;
            }

            UserDTO user = SessionUserManager.getUser(session);
            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response = Map.of(
                        "status", "error",
                        "message", "No user logged in for this session"
                );
                resp.getWriter().write(gson.toJson(response));
                return;
            }

            EmulatorEngine engine = EngineSessionManager.getEngine(session);
            if (engine == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response = Map.of(
                        "status", "error",
                        "message", "No engine found for this session"
                );
                resp.getWriter().write(gson.toJson(response));
                return;
            }

            FunctionService functionService = engine.getFunctionService();
            List<FunctionInfo> all = functionService.getAllFunctions();

            response = Map.of(
                    "status", "success",
                    "functions", all
            );

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response = Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }

        resp.getWriter().write(gson.toJson(response));
    }
}
