package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@WebServlet("/debug/step")
public class DebugStepServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();

            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"error\":\"No program loaded\"}");
                return;
            }
            if (!(engine instanceof EmulatorEngineImpl impl)) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("{\"error\":\"Engine is not EmulatorEngineImpl\"}");
                return;
            }

            int beforePc = impl.debugCurrentPC();
            int beforeCycles = impl.debugCycles();

            impl.debugStepOver();
            long deadline = System.currentTimeMillis() + 500;
            while (System.currentTimeMillis() < deadline) {
                if (impl.debugCurrentPC() != beforePc || impl.debugCycles() != beforeCycles || impl.debugIsFinished()) {
                    break;
                }
                try { Thread.sleep(20); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            int pc = impl.debugCurrentPC();
            int cycles = impl.debugCycles();
            boolean finished = impl.debugIsFinished();
            Map<String,String> vars = impl.debugVarsSnapshot();

            out.println("{");
            out.println("  \"status\": \"step done\",");
            out.println("  \"finished\": " + finished + ",");
            out.println("  \"pc\": " + pc + ",");
            out.println("  \"cycles\": " + cycles + ",");
            out.println("  \"vars\": {");
            if (vars != null && !vars.isEmpty()) {
                int i = 0, n = vars.size();
                for (var e : vars.entrySet()) {
                    String k = escapeJson(e.getKey());
                    String v = escapeJson(String.valueOf(e.getValue()));
                    out.print("    \"" + k + "\": \"" + v + "\"");
                    if (++i < n) out.println(",");
                    else out.println();
                }
            }
            out.println("  }");
            out.println("}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            out.println("{\"error\":\"" + escapeJson(msg) + "\"}");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
