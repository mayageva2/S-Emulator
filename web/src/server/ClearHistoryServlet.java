package server;

import emulator.api.EmulatorEngine;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/history/clear")
public class ClearHistoryServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();
            if (!engine.hasProgramLoaded()) {
                out.println("{\"warning\":\"No program loaded, but history cleared anyway\"}");
            }

            engine.clearHistory();
            out.println("{\"status\":\"History cleared successfully\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            out.println("{\"error\":\"" + msg.replace("\"","'") + "\"}");
        }
    }
}
