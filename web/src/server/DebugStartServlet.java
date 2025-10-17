package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/debug/start")
public class DebugStartServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();

            if (engine == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("status", "error");
                responseMap.put("message", "Engine is null (EngineHolder not initialized)");
                writeJson(resp, responseMap);
                return;
            }

            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "No program loaded");
                writeJson(resp, responseMap);
                return;
            }

            // Parse parameters
            String programName = req.getParameter("program");
            String degreeStr = req.getParameter("degree");
            String inputsStr = req.getParameter("inputs");

            int degree = 0;
            if (degreeStr != null && !degreeStr.isBlank()) {
                try {
                    degree = Integer.parseInt(degreeStr.trim());
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    responseMap.put("status", "error");
                    responseMap.put("message", "Invalid degree parameter");
                    writeJson(resp, responseMap);
                    return;
                }
            }

            Long[] inputs = parseInputs(inputsStr);

            if (!(engine instanceof EmulatorEngineImpl impl)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("status", "error");
                responseMap.put("message", "Engine is not EmulatorEngineImpl");
                writeJson(resp, responseMap);
                return;
            }

            if (programName == null || programName.isEmpty()) {
                impl.debugStart(inputs, degree);
            }
            else {
                impl.debugStart(programName, inputs, degree);
            }

            Map<String, String> varsSnapshot = impl.debugVarsSnapshot();
            Map<String, Object> debug = new LinkedHashMap<>();
            debug.put("pc", impl.debugCurrentPC());
            debug.put("cycles", impl.debugCycles());
            debug.put("vars", varsSnapshot);
            debug.put("finished", impl.debugIsFinished());

            responseMap.put("status", "success");
            responseMap.put("message", "Debug session started successfully");
            responseMap.put("debug", debug);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("status", "error");
            responseMap.put("message", e.getMessage());
            responseMap.put("exception", e.getClass().getSimpleName());
        }

        writeJson(resp, responseMap);
    }

    // Parse CSV of inputs
    private Long[] parseInputs(String csv) {
        if (csv == null || csv.isBlank()) return new Long[0];
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
    }

    //Write JSON response to client
    private void writeJson(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        String json = gson.toJson(data);
        try (PrintWriter out = resp.getWriter()) {
            out.write(json);
        }
    }
}
