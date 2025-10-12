package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.RunRecord;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/history")
public class HistoryServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();

            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("status", "error");
                responseMap.put("message", "No program loaded");
                writeJson(resp, responseMap);
                return;
            }

            List<RunRecord> history = engine.history();

            if (history == null || history.isEmpty()) {
                responseMap.put("status", "success");
                responseMap.put("history", Collections.emptyList());
                responseMap.put("message", "No runs recorded yet");
                writeJson(resp, responseMap);
                return;
            }

            List<Map<String, Object>> records = new ArrayList<>();
            for (RunRecord r : history) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("programName", r.programName());
                record.put("runNumber", r.runNumber());
                record.put("degree", r.degree());
                record.put("inputs", r.inputsCsv());
                record.put("y", r.y());
                record.put("cycles", r.cycles());
                records.add(record);
            }

            responseMap.put("status", "success");
            responseMap.put("history", records);
            responseMap.put("count", records.size());

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