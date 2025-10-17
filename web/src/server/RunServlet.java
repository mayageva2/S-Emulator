package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.RunResult;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/run")
public class RunServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            String body = req.getReader().lines().collect(Collectors.joining());
            Map<String, Object> data = gson.fromJson(body, Map.class);

            if (data == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "Invalid JSON body");
                writeJson(resp, responseMap);
                return;
            }

            String program = (String) data.get("program");

            Number degreeNum = (Number) data.getOrDefault("degree", 0);
            int degree = degreeNum.intValue();

            @SuppressWarnings("unchecked")
            List<Double> inputsJson = (List<Double>) data.getOrDefault("inputs", List.of());
            Long[] inputs = inputsJson.stream().map(Double::longValue).toArray(Long[]::new);

            EmulatorEngine engine = EngineHolder.getEngine();
            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "No program loaded");
                writeJson(resp, responseMap);
                return;
            }

            RunResult result;
            if (program == null || program.isEmpty()) {
                result = engine.run(degree, inputs);
            }
            else {
                result = engine.run(program, degree, inputs);
            }
            responseMap.put("status", "success");
            responseMap.put("result", result);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
