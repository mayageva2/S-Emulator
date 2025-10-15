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

@WebServlet("/debug/step")
public class DebugStepServlet extends HttpServlet {

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

            if (!(engine instanceof EmulatorEngineImpl impl)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("status", "error");
                responseMap.put("message", "Engine is not EmulatorEngineImpl");
                writeJson(resp, responseMap);
                return;
            }

            int beforePc = impl.debugCurrentPC();
            int beforeCycles = impl.debugCycles();

            impl.debugStepOver();

            long deadline = System.currentTimeMillis() + 500;
            while (System.currentTimeMillis() < deadline) {
                if (impl.debugCurrentPC() != beforePc ||
                        impl.debugCycles() != beforeCycles ||
                        impl.debugIsFinished()) {
                    break;
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            int pc = impl.debugCurrentPC();
            int cycles = impl.debugCycles();
            boolean finished = impl.debugIsFinished();
            Map<String, String> vars = impl.debugVarsSnapshot();

            Map<String, Object> debugData = new LinkedHashMap<>();
            debugData.put("finished", finished);
            debugData.put("pc", pc);
            debugData.put("cycles", cycles);
            debugData.put("vars", vars);

            responseMap.put("status", "success");
            responseMap.put("message", "Step executed successfully");
            responseMap.put("debug", debugData);
            responseMap.put("vars", vars);

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