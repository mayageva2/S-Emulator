package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

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

            out.println("{");
            out.println("  \"status\": \"Debug stopped successfully\",");
            out.println("  \"finished\": " + impl.debugIsFinished() + ",");
            out.println("  \"pc\": " + impl.debugCurrentPC() + ",");
            out.println("  \"cycles\": " + impl.debugCycles());
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
