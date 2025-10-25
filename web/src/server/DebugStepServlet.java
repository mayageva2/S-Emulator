package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
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

            if (!(engine instanceof EmulatorEngineImpl impl)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("status", "error");
                responseMap.put("message", "Engine is not EmulatorEngineImpl");
                writeJson(resp, responseMap);
                return;
            }

            String dbgError = impl.getDebugErrorMessage();
            if (dbgError != null) {
                responseMap.put("status", "error");
                responseMap.put("message", dbgError);
                impl.clearDebugErrorMessage();
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

            impl.debugStepOver();

            int oldCycles = impl.debugCycles();
            long start = System.currentTimeMillis();
            boolean finished = false;

            while (System.currentTimeMillis() - start < 1000) {
                Thread.sleep(20);

                int newCycles = impl.debugCycles();
                if (newCycles != oldCycles || impl.debugIsFinished()) {
                    finished = impl.debugIsFinished();
                    break;
                }
            }

            dbgError = impl.getDebugErrorMessage();
            if (dbgError != null) {
                responseMap.put("status", "error");
                responseMap.put("message", dbgError);
                impl.clearDebugErrorMessage();
                writeJson(resp, responseMap);
                return;
            }

            Map<String, String> vars = impl.debugVarsSnapshot();
            int cycles = impl.debugCycles();
            long yVal = 0;
            if (vars != null && vars.containsKey("y")) {
                try { yVal = Long.parseLong(vars.get("y")); } catch (NumberFormatException ignored) {}
            }

            if (finished) {
                impl.recordDebugSession(
                        impl.lastRunProgramName(),
                        impl.lastRunDegree(),
                        impl.lastRunInputs().toArray(new Long[0]),
                        vars,
                        cycles
                );
                ServerEventManager.broadcast("PROGRAM_RUN");
            }

            Map<String, Object> debugData = new LinkedHashMap<>();
            debugData.put("y", yVal);
            debugData.put("cycles", cycles);
            debugData.put("vars", vars);
            debugData.put("pc", impl.debugCurrentPC());

            responseMap.put("status", "success");
            responseMap.put("message", finished ? "Program finished" : "Step executed");
            responseMap.put("finished", finished);
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

