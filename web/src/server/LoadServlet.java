package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.LoadService;
import emulator.api.dto.LoadResult;
import emulator.logic.user.UserManager;
import emulator.logic.user.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

@WebServlet("/load")
public class LoadServlet extends HttpServlet {
    private final LoadService loadService = new LoadService();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String json = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> body = gson.fromJson(json, Map.class);
            Path xmlPath = Path.of((String) body.get("path"));

            EmulatorEngine engine = EngineHolder.getEngine();

            String username = UserManager.getCurrentUser()
                    .map(User::getUsername)
                    .orElse("unknown");

            LoadResult result = loadService.load(engine, xmlPath, username);

            String responseJson = gson.toJson(Map.of(
                    "status", "success",
                    "programName", result.programName(),
                    "instructionCount", result.instructionCount(),
                    "maxDegree", result.maxDegree()
            ));
            resp.getWriter().write(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }
}
