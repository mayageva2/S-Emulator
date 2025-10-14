package server;

import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import emulator.api.EmulatorEngine;
import emulator.api.dto.LoadResult;
import com.google.gson.Gson;

@WebServlet("/load")
public class LoadServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        String pathStr = req.getParameter("path");
        if (pathStr == null || pathStr.isBlank()) {
            responseMap.put("error", "Missing 'path' parameter");
            writeJson(resp, responseMap);
            return;
        }

        Path xmlPath = Path.of(pathStr);

        try {
            EmulatorEngine engine = EngineHolder.getEngine();
            LoadResult result = engine.loadProgram(xmlPath);

            responseMap.put("status", "success");
            responseMap.put("programName", result.programName());
            responseMap.put("instructionCount", result.instructionCount());
            responseMap.put("maxDegree", result.maxDegree());

            List<String> functions = new ArrayList<>();
            Map<String, String> displayMap = new LinkedHashMap<>();
            if (engine instanceof EmulatorEngineImpl impl) {
                displayMap = impl.getDisplayNameMap();
            }

            for (String internal : engine.getAllProgramNames()) {
                String disp = displayMap.getOrDefault(internal, internal);
                if (disp != null && !disp.isBlank()) {
                    functions.add(disp);
                }
            }

            functions = functions.stream()
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();

            String mainProgramName = result.programName();
            functions = new ArrayList<>(functions);
            functions.removeIf(fn ->
                    fn.equalsIgnoreCase(mainProgramName)
                            || fn.equalsIgnoreCase("main program")
            );
            functions.add(0, "Main Program");

            responseMap.put("functions", functions);

        } catch (Exception e) {
            responseMap.put("status", "error");
            responseMap.put("message", e.getMessage());
            responseMap.put("exception", e.getClass().getSimpleName());
        }

        writeJson(resp, responseMap);
    }

    private void writeJson(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        String json = gson.toJson(data);
        try (PrintWriter out = resp.getWriter()) {
            out.write(json);
        }
    }
}
