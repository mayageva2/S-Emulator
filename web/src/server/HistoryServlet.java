package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.RunRecord;
import emulator.api.dto.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/user/history")
public class HistoryServlet extends HttpServlet {

    private static final Gson gson = new Gson();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            String username = req.getParameter("username");
            if (username == null || username.isBlank()) {
                username = userService.getCurrentUser()
                        .map(u -> u.getUsername())
                        .orElse(null);
            }

            if (username == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "No username provided and no current user found");
                writeJson(resp, responseMap);
                return;
            }

            EmulatorEngine engine = EngineHolder.getEngine();
            List<RunRecord> allHistory = engine.history();

            List<RunRecord> userHistory = new ArrayList<>();
            for (RunRecord r : allHistory) {
                if (username.equalsIgnoreCase(r.username())) {
                    userHistory.add(r);
                }
            }

            if (userHistory.isEmpty()) {
                responseMap.put("status", "success");
                responseMap.put("runs", Collections.emptyList());
                responseMap.put("message", "No runs recorded yet for user: " + username);
                writeJson(resp, responseMap);
                return;
            }

            List<Map<String, Object>> runs = new ArrayList<>();
            for (RunRecord r : userHistory) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("runNumber", r.runNumber());
                record.put("type", r.programName().equalsIgnoreCase("Main") ? "Main Program" : "Function");
                record.put("program", r.programName());
                record.put("arch", detectArchitecture(r.cycles()));
                record.put("degree", r.degree());
                record.put("y", r.y());
                record.put("cycles", r.cycles());
                record.put("inputs", r.inputs());
                runs.add(record);
            }

            responseMap.put("status", "success");
            responseMap.put("user", username);
            responseMap.put("runs", runs);
            responseMap.put("count", runs.size());

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

    private String detectArchitecture(int cycles) {
        if (cycles <= 10) return "I";
        if (cycles <= 100) return "II";
        if (cycles <= 500) return "III";
        return "IV";
    }
}
