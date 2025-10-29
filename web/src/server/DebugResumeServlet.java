package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.UserDTO;
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            HttpSession session = req.getSession(false);
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                responseMap.put("status", "error");
                responseMap.put("message", "No active session");
                writeJson(resp, responseMap);
                return;
            }

            UserDTO user = SessionUserManager.getUser(session);
            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                responseMap.put("status", "error");
                responseMap.put("message", "No user logged in for this session");
                writeJson(resp, responseMap);
                return;
            }

            EmulatorEngine engine = EngineSessionManager.getEngine(session);
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
                Map<String, Object> debugData = makeDebugData(impl);
                responseMap.put("status", "stopped");
                responseMap.put("message", "Program already finished");
                responseMap.put("finished", true);
                responseMap.put("debug", debugData);
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
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }

            Map<String, Object> debugData = makeDebugData(impl);

            if (finished) {
                impl.recordDebugSession(
                        impl.lastRunProgramName(),
                        impl.lastRunDegree(),
                        impl.lastRunInputs().toArray(new Long[0]),
                        impl.debugVarsSnapshot(),
                        impl.debugCycles()
                );

                responseMap.put("status", "stopped");
                responseMap.put("message", "Program finished");
                responseMap.put("finished", true);
                responseMap.put("debug", debugData);
                ServerEventManager.broadcast("PROGRAM_RUN");
            } else {
                responseMap.put("status", "resumed");
                responseMap.put("message", "Debug resumed and still running");
                responseMap.put("finished", false);
                responseMap.put("debug", debugData);
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("status", "error");
            responseMap.put("message", e.getMessage());
            responseMap.put("exception", e.getClass().getSimpleName());
        }

        writeJson(resp, responseMap);
    }

    private Map<String, Object> makeDebugData(EmulatorEngineImpl impl) {
        Map<String, Object> debugData = new LinkedHashMap<>();
        Map<String, String> vars = impl.debugVarsSnapshot();

        long yVal = 0;
        if (vars != null && vars.containsKey("y")) {
            try {
                yVal = Long.parseLong(vars.get("y"));
            } catch (NumberFormatException ignored) {}
        }

        debugData.put("y", yVal);
        debugData.put("pc", impl.debugCurrentPC());
        debugData.put("cycles", impl.debugCycles());
        debugData.put("vars", vars);
        return debugData;
    }

    private void writeJson(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.write(gson.toJson(data));
        }
    }
}
