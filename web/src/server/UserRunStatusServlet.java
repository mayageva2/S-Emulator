package server;

import com.google.gson.Gson;
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
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> out = new LinkedHashMap<>();

        try {
            String usernameParam = req.getParameter("username");
            String runStr = req.getParameter("runNumber");

            if (usernameParam == null || usernameParam.isBlank()) {
                error(resp, "Missing username");
                return;
            }
            if (runStr == null || runStr.isBlank()) {
                error(resp, "Missing runNumber");
                return;
            }

            int runNumber;
            try {
                runNumber = Integer.parseInt(runStr);
            } catch (Exception e) {
                error(resp, "Invalid runNumber");
                return;
            }

            List<RunRecord> history = GlobalHistoryCenter.getHistory(usernameParam);

            if (history.isEmpty()) {
                error(resp, "No run history available");
                return;
            }

            RunRecord target = null;
            for (RunRecord r : history) {
                if (r.runNumber() == runNumber) {
                    target = r;
                    break;
                }
            }

            if (target == null) {
                error(resp, "Run not found for user " + usernameParam);
                return;
            }

            out.put("status", "success");
            out.put("vars", target.getVarsSnapshot());

            try (PrintWriter writer = resp.getWriter()) {
                writer.write(gson.toJson(out));
            }

        } catch (Exception e) {
            error(resp, "Failed to get run status: " + e.getMessage());
        }
    }

    private void error(HttpServletResponse resp, String msg) throws IOException {
        Map<String, Object> out = Map.of(
                "status", "error",
                "message", msg
        );
        resp.getWriter().write(gson.toJson(out));
    }
}
