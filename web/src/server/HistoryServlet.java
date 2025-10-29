package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.RunRecord;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/user/history")
public class HistoryServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            HttpSession session = req.getSession(false);
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                responseMap.put("status", "error");
                responseMap.put("message", "No active session");
                writeJson(resp, responseMap);
                return;
            }

            UserDTO currentUser = SessionUserManager.getUser(session);
            if (currentUser == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                responseMap.put("status", "error");
                responseMap.put("message", "No user logged in for this session");
                writeJson(resp, responseMap);
                return;
            }

            String username = currentUser.getUsername();

            EmulatorEngine engine = EngineSessionManager.getEngine(session);
            if (engine == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("status", "error");
                responseMap.put("message", "Engine not found for session");
                writeJson(resp, responseMap);
                return;
            }

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
                record.put("type", r.getType());
                record.put("program", r.programName());
                record.put("arch", r.architecture());
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
        try (PrintWriter out = resp.getWriter()) {
            out.write(gson.toJson(data));
        }
    }
}
