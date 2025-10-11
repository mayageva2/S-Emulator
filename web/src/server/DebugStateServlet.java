package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@WebServlet("/debug/state")
public class DebugStateServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

            boolean finished = impl.debugIsFinished();
            int pc = impl.debugCurrentPC();
            int cycles = impl.debugCycles();
            Map<String, String> vars = impl.debugVarsSnapshot();
            long yVal = 0L;
            if (vars != null && vars.containsKey("y")) {
                try { yVal = Long.parseLong(vars.get("y")); } catch (NumberFormatException ignore) {}
            }

            out.println("{");
            out.println("  \"status\": \"ok\",");
            out.println("  \"finished\": " + finished + ",");
            out.println("  \"pc\": " + pc + ",");
            out.println("  \"cycles\": " + cycles + ",");
            out.println("  \"y\": " + yVal + ",");
            out.println("  \"vars\": {");
            if (vars != null && !vars.isEmpty()) {
                int i = 0, n = vars.size();
                for (var e : vars.entrySet()) {
                    out.print("    \"" + e.getKey() + "\": \"" + e.getValue() + "\"");
                    if (++i < n) out.println(",");
                    else out.println();
                }
            }
            out.println("  }");
            out.println("}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            out.println("{\"error\":\"" + msg.replace("\"","'") + "\"}");
        }
    }
}
