package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.RunRecord;
import emulator.api.dto.UserDTO;
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
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            UserDTO currentUser = SessionUserManager.getUser(req.getSession());
            if (currentUser == null) {
                responseMap.put("status", "error");
                responseMap.put("message", "No user logged in for this session");
                writeJson(resp, responseMap);
                return;
            }

            String usernameParam = req.getParameter("username");
            String runStr = req.getParameter("runNumber");

            if (runStr == null || runStr.isBlank()) {
                responseMap.put("status", "error");
                responseMap.put("message", "Missing runNumber parameter");
                writeJson(resp, responseMap);
                return;
            }

            int runNumber;
            try {
                runNumber = Integer.parseInt(runStr);
            } catch (NumberFormatException e) {
                responseMap.put("status", "error");
                responseMap.put("message", "Invalid runNumber format");
                writeJson(resp, responseMap);
                return;
            }

            EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());
            if (engine instanceof EmulatorEngineImpl impl) {
                impl.setSessionUser(currentUser);
            }

            if (engine == null) {
                responseMap.put("status", "error");
                responseMap.put("message", "Engine not initialized");
                writeJson(resp, responseMap);
                return;
            }

            List<RunRecord> history = engine.history();
            if (history == null || history.isEmpty()) {
                responseMap.put("status", "error");
                responseMap.put("message", "No run history available");
                writeJson(resp, responseMap);
                return;
            }

            String effectiveUser = (usernameParam != null && !usernameParam.isBlank())
                    ? usernameParam
                    : currentUser.getUsername();

            Optional<RunRecord> recOpt = history.stream()
                    .filter(r -> r.runNumber() == runNumber &&
                            effectiveUser.equalsIgnoreCase(r.username()))
                    .findFirst();

            if (recOpt.isEmpty()) {
                responseMap.put("status", "error");
                responseMap.put("message", "Run not found for user " + effectiveUser);
                writeJson(resp, responseMap);
                return;
            }

            RunRecord rec = recOpt.get();
            Map<String, Long> vars = rec.getVarsSnapshot();
            responseMap.put("status", "success");
            responseMap.put("message", "Run status retrieved successfully");
            responseMap.put("program", rec.programName());
            responseMap.put("degree", rec.degree());
            responseMap.put("cycles", rec.cycles());
            responseMap.put("y", rec.y());
            responseMap.put("vars", vars != null ? vars : Map.of());

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
