package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.RunRecord;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/history")
public class HistoryServlet extends HttpServlet {
    private static final EmulatorEngine engine = EngineHolder.getEngine();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            if (!engine.hasProgramLoaded()) {
                out.println("{\"error\": \"No program loaded.\"}");
                return;
            }

            List<RunRecord> history = engine.history();
            if (history.isEmpty()) {
                out.println("{\"history\": [], \"message\": \"No runs recorded yet.\"}");
                return;
            }

            out.println("[");
            for (int i = 0; i < history.size(); i++) {
                RunRecord r = history.get(i);
                out.printf("""
                        {
                            "programName": "%s",
                            "runNumber": %d,
                            "degree": %d,
                            "inputs": "%s",
                            "y": %d,
                            "cycles": %d
                        }%s
                        """,
                        r.programName(), r.runNumber(), r.degree(),
                        r.inputsCsv(), r.y(), r.cycles(),
                        (i < history.size() - 1 ? "," : "")
                );
            }
            out.println("]");
        } catch (Exception e) {
            out.printf("{\"error\":\"%s: %s\"}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
