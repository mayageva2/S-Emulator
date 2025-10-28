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

@WebServlet("/debug/state")
public class DebugStateServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());

            if (!(engine instanceof EmulatorEngineImpl impl)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("status", "error");
                responseMap.put("message", "Engine is not EmulatorEngineImpl");
                writeJson(resp, responseMap);
                return;
            }

            if (!impl.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "No program loaded");
                writeJson(resp, responseMap);
                return;
            }

            boolean finished = impl.debugIsFinished();
            int pc = impl.debugCurrentPC();
            int cycles = impl.debugCycles();
            Map<String, String> vars = impl.debugVarsSnapshot();

            long yVal = 0L;
            if (vars != null && vars.containsKey("y")) {
                try {
                    yVal = Long.parseLong(vars.get("y"));
                } catch (NumberFormatException ignored) {}
            }

            Map<String, Object> debugData = new LinkedHashMap<>();
            debugData.put("finished", finished);
            debugData.put("pc", pc);
            debugData.put("cycles", cycles);
            debugData.put("y", yVal);
            debugData.put("vars", vars);

            responseMap.put("status", "success");
            responseMap.put("message", "Current debug state retrieved successfully");
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
