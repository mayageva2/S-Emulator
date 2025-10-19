package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.LoadResult;
import emulator.api.dto.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

@WebServlet("/load")
public class LoadServlet extends HttpServlet {
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String json = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> body = gson.fromJson(json, Map.class);
            Path xmlPath = Path.of((String) body.get("path"));

            EmulatorEngine engine = EngineHolder.getEngine();
            LoadResult result = engine.loadProgram(xmlPath);

            int functionCount = (result.functions() != null) ? result.functions().size() : 0;
            userService.incrementMainProgramsAndFunctions(functionCount);

            String responseJson = gson.toJson(Map.of(
                    "status", "success",
                    "programName", result.programName(),
                    "instructionCount", result.instructionCount(),
                    "maxDegree", result.maxDegree(),
                    "functions", result.functions()
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
