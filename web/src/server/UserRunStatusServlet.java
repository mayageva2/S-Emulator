package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.RunRecord;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/user/run/status")
public class UserRunStatusServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        System.out.println("🔸 /user/run/status servlet called!");
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            String username = req.getParameter("username");
            String runStr = req.getParameter("runNumber");

            if (runStr == null || runStr.isBlank()) {
                responseMap.put("status", "error");
                responseMap.put("message", "Missing runNumber parameter");
                writeJson(resp, responseMap);
                return;
            }

            int runNumber = Integer.parseInt(runStr);
            EmulatorEngine engine = EngineHolder.getEngine();
            if (engine == null) {
                responseMap.put("status", "error");
                responseMap.put("message", "Engine not initialized");
                writeJson(resp, responseMap);
                return;
            }

            List<RunRecord> history = engine.history();
            System.out.println("🔸 /user/run/status called");
            System.out.println("🔸 username=" + username + " | run=" + runStr);
            System.out.println("🔸 total history size=" + history.size());

            Optional<RunRecord> recOpt = history.stream()
                    .filter(r -> r.runNumber() == runNumber &&
                            (username == null || username.isBlank() ||
                                    username.equalsIgnoreCase(r.username())))
                    .findFirst();

            if (recOpt.isEmpty()) {
                responseMap.put("status", "error");
                responseMap.put("message", "Run not found");
                writeJson(resp, responseMap);
                return;
            }

            RunRecord rec = recOpt.get();
            Map<String, Long> vars = rec.getVarsSnapshot();

            System.out.println("🔸 found run record: " + rec.programName() + " #" + rec.runNumber());
            System.out.println("🔸 varsSnapshot=" + rec.getVarsSnapshot());

            responseMap.put("status", "success");
            responseMap.put("message", "Run status retrieved successfully");
            responseMap.put("vars", vars != null ? vars : Map.of());
            responseMap.put("program", rec.programName());
            responseMap.put("degree", rec.degree());
            responseMap.put("cycles", rec.cycles());
            responseMap.put("y", rec.y());

        } catch (Exception e) {
            responseMap.put("status", "error");
            responseMap.put("message", "Failed to get run status: " + e.getMessage());
        }

        writeJson(resp, responseMap);
    }

    private void writeJson(HttpServletResponse resp, Map<String, Object> map) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.write(gson.toJson(map));
        }
    }
}
