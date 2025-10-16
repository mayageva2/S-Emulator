package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.RunRecord;
import emulator.api.dto.RunResult;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/run")
public class RunServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();

            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "No program loaded");
                writeJson(resp, responseMap);
                return;
            }

            String degreeStr = req.getParameter("degree");
            String inputsStr = req.getParameter("inputs");

            if (degreeStr == null || inputsStr == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "Missing parameters: 'degree' or 'inputs'");
                writeJson(resp, responseMap);
                return;
            }

            int degree = Integer.parseInt(degreeStr.trim());
            Long[] inputs = parseInputs(inputsStr);

            // Run the program
            RunResult result = engine.run(degree, inputs);

            // Build response JSON
            Map<String, Object> resultData = new LinkedHashMap<>();
            resultData.put("y", result.y());
            resultData.put("cycles", result.cycles());
            resultData.put("varsCount", result.vars() == null ? 0 : result.vars().size());
            resultData.put("vars", result.vars());

            responseMap.put("status", "success");
            responseMap.put("result", resultData);

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("status", "error");
            responseMap.put("message", "Invalid degree or input format");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("status", "error");
            responseMap.put("message", e.getMessage());
            responseMap.put("exception", e.getClass().getSimpleName());
        }

        writeJson(resp, responseMap);
    }

    private Long[] parseInputs(String csv) {
        if (csv == null || csv.isBlank()) return new Long[0];
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
    }

    private void writeJson(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        String json = gson.toJson(data);
        try (PrintWriter out = resp.getWriter()) {
            out.write(json);
        }
    }
}
