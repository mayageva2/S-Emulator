package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.LoadResult;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;

@WebServlet("/load")
public class LoadServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try (PrintWriter out = resp.getWriter()) {
            String body = req.getReader().lines().reduce("", (acc, line) -> acc + line);
            Map<?, ?> json = gson.fromJson(body, Map.class);
            String pathStr = (json == null) ? null : (String) json.get("path");

            if (pathStr == null || pathStr.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "Missing 'path' parameter");
                out.write(gson.toJson(responseMap));
                return;
            }

            EmulatorEngine engine = EngineHolder.getEngine();
            LoadResult result = engine.loadProgram(Path.of(pathStr));

            responseMap.put("status", "success");
            responseMap.put("programName", result.programName());
            responseMap.put("instructionCount", result.instructionCount());
            responseMap.put("maxDegree", result.maxDegree());

            List<String> functions;
            try {
                if (engine instanceof EmulatorEngineImpl impl) {
                    functions = impl.displayProgramNames();
                } else {
                    functions = List.of("Main Program");
                }
            } catch (Exception ex) {
                functions = List.of("Main Program");
            }
            responseMap.put("functions", functions);

            out.write(gson.toJson(responseMap));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> err = Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "exception", e.getClass().getSimpleName()
            );
            resp.getWriter().write(gson.toJson(err));
        }
    }
}