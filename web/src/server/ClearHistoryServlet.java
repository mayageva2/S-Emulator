package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/history/clear")
public class ClearHistoryServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
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

            UserDTO user = SessionUserManager.getUser(session);
            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                responseMap.put("status", "error");
                responseMap.put("message", "No user logged in for this session");
                writeJson(resp, responseMap);
                return;
            }

            EmulatorEngine engine = EngineSessionManager.getEngine(session);

            if (!engine.hasProgramLoaded()) {
                responseMap.put("status", "warning");
                responseMap.put("message", "No program loaded, but history cleared anyway");
            } else {
                responseMap.put("status", "success");
                responseMap.put("message", "History cleared successfully");
            }

            engine.clearHistory();
            ServerEventManager.broadcast("HISTORY_CLEARED");

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
