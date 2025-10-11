package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@WebServlet("/debug/resume")
public class DebugResumeServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();
            if (!(engine instanceof EmulatorEngineImpl impl)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("{\"error\":\"Engine is not EmulatorEngineImpl\"}");
                return;
            }
            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"error\":\"No program loaded\"}");
                return;
            }
            if (impl.debugIsFinished()) {
                out.println("{\"status\":\"Already finished\"}");
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
                out.println("{\"status\":\"Still running\",\"pc\":" + impl.debugCurrentPC() +
                        ",\"cycles\":" + impl.debugCycles() + "}");
                return;
            }

            Map<String, String> vars = impl.debugVarsSnapshot();
            int cycles = impl.debugCycles();
            long yVal = vars.containsKey("y") ? Long.parseLong(vars.get("y")) : 0;

            out.println("{");
            out.println("  \"status\": \"Program finished\",");
            out.println("  \"y\": " + yVal + ",");
            out.println("  \"cycles\": " + cycles + ",");
            out.println("  \"vars\": {");
            int i = 0, n = vars.size();
            for (var e : vars.entrySet()) {
                out.print("    \"" + e.getKey() + "\": \"" + e.getValue() + "\"");
                if (++i < n) out.println(",");
                else out.println();
            }
            out.println("  }");
            out.println("}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            out.println("{\"error\":\"" + msg.replace("\"","'") + "\"}");
        }
    }
}
