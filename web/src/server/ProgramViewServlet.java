package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.InstructionView;
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

            // Parse degree
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

            // Parse program name
            String programParam = req.getParameter("program");
            if (programParam != null) {
                programParam = URLDecoder.decode(programParam, StandardCharsets.UTF_8);
            }

            // Fetch view from engine
            ProgramView pv;
            if (programParam == null || programParam.isBlank()
                    || "Main Program".equalsIgnoreCase(programParam)) {
                pv = engine.programView(degree);
            } else {
                pv = engine.programView(programParam, degree);
            }

            // Build program JSON
            Map<String, Object> programData = new LinkedHashMap<>();
            programData.put("programName", pv.programName());
            programData.put("degree", pv.degree());
            programData.put("maxDegree", pv.maxDegree());
            programData.put("totalCycles", pv.totalCycles());

            // ----- Build instruction list -----
            List<Map<String, Object>> instructions = new ArrayList<>();
            for (InstructionView ins : pv.instructions()) {
                Map<String, Object> insMap = new LinkedHashMap<>();
                insMap.put("index", ins.index());
                insMap.put("opcode", ins.opcode());
                insMap.put("label", ins.label());
                insMap.put("cycles", ins.cycles());
                insMap.put("args", ins.args());
                insMap.put("basic", ins.basic());

                if (ins.createdFromChain() != null && !ins.createdFromChain().isEmpty()) {
                    insMap.put("createdFromChain", ins.createdFromChain());
                }

                if (ins.createdFromViews() != null && !ins.createdFromViews().isEmpty()) {
                    List<Map<String, Object>> subViews = new ArrayList<>();
                    for (InstructionView sub : ins.createdFromViews()) {
                        Map<String, Object> subMap = new LinkedHashMap<>();
                        subMap.put("index", sub.index());
                        subMap.put("opcode", sub.opcode());
                        subMap.put("label", sub.label());
                        subMap.put("cycles", sub.cycles());
                        subMap.put("args", sub.args());
                        subMap.put("basic", sub.basic());
                        subViews.add(subMap);
                    }
                    insMap.put("createdFromViews", subViews);
                }

                instructions.add(insMap);
            }

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