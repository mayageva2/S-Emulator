package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/debug/resume")
public class DebugResumeServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();

            if (!(engine instanceof EmulatorEngineImpl impl)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("status", "error");
                responseMap.put("message", "Engine is not EmulatorEngineImpl");
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

            if (impl.debugIsFinished()) {
                responseMap.put("status", "success");
                responseMap.put("message", "Already finished");
                responseMap.put("finished", true);
                writeJson(resp, responseMap);
                return;
            }

            impl.debugResume();

            long start = System.currentTimeMillis();
            long timeoutMs = 3000;
            boolean finished = false;

            while (System.currentTimeMillis() - start < timeoutMs) {
                if (impl.debugIsFinished()) {
                    finished = true;
                    break;
                }
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }

            if (!finished) {
                responseMap.put("status", "running");
                responseMap.put("message", "Still running");
                responseMap.put("finished", false);
                responseMap.put("pc", impl.debugCurrentPC());
                responseMap.put("cycles", impl.debugCycles());
                writeJson(resp, responseMap);
                return;
            }

            Map<String, String> vars = impl.debugVarsSnapshot();
            int cycles = impl.debugCycles();
            long yVal = 0;
            if (vars != null && vars.containsKey("y")) {
                try {
                    yVal = Long.parseLong(vars.get("y"));
                } catch (NumberFormatException ignored) {}
            }

            impl.recordDebugSession(
                    impl.lastRunProgramName(),
                    impl.lastRunDegree(),
                    impl.lastRunInputs().toArray(new Long[0]),
                    vars,
                    cycles
            );

            Map<String, Object> debugData = new LinkedHashMap<>();
            debugData.put("y", yVal);
            debugData.put("cycles", cycles);
            debugData.put("vars", vars);

            responseMap.put("status", "success");
            responseMap.put("message", "Program finished");
            responseMap.put("finished", true);
            responseMap.put("debug", debugData);

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