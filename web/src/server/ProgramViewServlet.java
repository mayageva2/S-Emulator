package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@WebServlet("/view")
public class ProgramViewServlet extends HttpServlet {

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

            int degree = 0;
            String degreeParam = req.getParameter("degree");
            if (degreeParam != null && !degreeParam.isBlank()) {
                try {
                    degree = Integer.parseInt(degreeParam.trim());
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    responseMap.put("status", "error");
                    responseMap.put("message", "Invalid degree parameter");
                    writeJson(resp, responseMap);
                    return;
                }
            }

            String programParam = req.getParameter("program");
            if (programParam != null) {
                programParam = URLDecoder.decode(programParam, StandardCharsets.UTF_8);
            }

            ProgramView pv;
            if (programParam == null || programParam.isBlank() ||
                    "Main Program".equalsIgnoreCase(programParam)) {
                pv = engine.programView(degree); // Main
            } else {
                pv = engine.programView(programParam, degree); // Chosen Func
            }

            Map<String, Object> programData = new LinkedHashMap<>();
            programData.put("programName", pv.programName());
            programData.put("degree", pv.degree());
            programData.put("maxDegree", pv.maxDegree());
            programData.put("totalCycles", pv.totalCycles());

            List<Map<String, Object>> instructions = new ArrayList<>();
            pv.instructions().forEach(ins -> {
                Map<String, Object> insMap = new LinkedHashMap<>();
                insMap.put("index", ins.index());
                insMap.put("opcode", ins.opcode());
                insMap.put("label", ins.label());
                insMap.put("cycles", ins.cycles());
                insMap.put("args", ins.args());
                insMap.put("basic", ins.basic());
                instructions.add(insMap);
            });

            programData.put("instructions", instructions);

            responseMap.put("status", "success");
            responseMap.put("program", programData);

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