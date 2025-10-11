package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/debug/stop")
public class DebugStopServlet extends HttpServlet {

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

            impl.debugStop();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}

            boolean finished = impl.debugIsFinished();
            int pc = impl.debugCurrentPC();
            int cycles = impl.debugCycles();
            Map<String, String> vars = impl.debugVarsSnapshot();

            List<Long> inputs = new ArrayList<>(impl.lastRunInputs());
            if (inputs.isEmpty()) {
                vars.keySet().stream()
                        .filter(k -> k.matches("x\\d+"))
                        .sorted(Comparator.comparingInt(k -> Integer.parseInt(k.substring(1))))
                        .forEach(k -> {
                            try {
                                inputs.add(Long.parseLong(vars.get(k)));
                            } catch (NumberFormatException ignored2) {}
                        });
            }

            String programName = impl.lastRunProgramName() != null ? impl.lastRunProgramName() : "UNKNOWN";
            int degree = impl.lastRunDegree();

            if (vars != null && !vars.isEmpty()) {
                impl.recordDebugSession(
                        programName,
                        degree,
                        inputs.toArray(new Long[0]),
                        vars,
                        cycles
                );
            }

            long yVal = vars.containsKey("y") ? Long.parseLong(vars.get("y")) : 0L;

            out.println("{");
            out.println("  \"status\": \"Debug stopped successfully\",");
            out.println("  \"finished\": " + finished + ",");
            out.println("  \"pc\": " + pc + ",");
            out.println("  \"cycles\": " + cycles + ",");
            out.println("  \"y\": " + yVal + ",");
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
            out.println("{\"error\":\"" + msg.replace("\"", "'") + "\"}");
        }
    }
}
