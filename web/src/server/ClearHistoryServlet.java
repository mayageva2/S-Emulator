package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
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
            EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());

            if (!engine.hasProgramLoaded()) {
                responseMap.put("status", "warning");
                responseMap.put("message", "No program loaded, but history cleared anyway");
            } else {
                responseMap.put("status", "success");
                responseMap.put("message", "History cleared successfully");
            }

            engine.clearHistory();

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
}