package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import emulator.api.dto.UserDTO;
import emulator.api.dto.FunctionInfo;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@WebServlet("/view")
public class ProgramViewServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> response = new LinkedHashMap<>();

        try (PrintWriter out = resp.getWriter()) {

            // === Validate session & user ===
            HttpSession session = req.getSession(false);
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.put("status", "error");
                response.put("message", "No active session");
                out.write(gson.toJson(response));
                return;
            }

            UserDTO user = SessionUserManager.getUser(session);
            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.put("status", "error");
                response.put("message", "No user logged in");
                out.write(gson.toJson(response));
                return;
            }

            EmulatorEngine engine = EngineSessionManager.getEngine(session);
            if (engine == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.put("status", "error");
                response.put("message", "No engine available for session");
                out.write(gson.toJson(response));
                return;
            }

            // === Degree ===
            int degree = 0;
            String degreeParam = req.getParameter("degree");
            if (degreeParam != null && !degreeParam.isBlank()) {
                try {
                    degree = Integer.parseInt(degreeParam.trim());
                } catch (Exception ex) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.put("status", "error");
                    response.put("message", "Invalid degree value: " + degreeParam);
                    out.write(gson.toJson(response));
                    return;
                }
            }

            // === Program name ===
            String programParam = req.getParameter("program");
            if (programParam != null && !programParam.isBlank()) {
                programParam = URLDecoder.decode(programParam, StandardCharsets.UTF_8);
            }

            if (!engine.hasProgramLoaded()) {

                if (programParam != null && !programParam.isBlank()) {

                    byte[] xmlBytes = GlobalDataCenter.getProgramFile(programParam);

                    if (xmlBytes != null) {
                        try (var xmlStream = new ByteArrayInputStream(xmlBytes)) {
                            engine.loadProgramFromStream(xmlStream);
                            System.out.println("[ProgramViewServlet] Loaded XML program '" +
                                    programParam + "' for user: " + user.getUsername());
                        }
                    }
                }
            }

            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.put("status", "error");
                response.put("message",
                        "No program loaded or available for user: " + user.getUsername());
                out.write(gson.toJson(response));
                return;
            }

            ProgramView pv = (
                    programParam == null ||
                            programParam.isBlank() ||
                            "Main Program".equalsIgnoreCase(programParam)
            )
                    ? engine.programView(degree)
                    : engine.programView(programParam, degree);

            Map<String, Object> programMap =
                    gson.fromJson(gson.toJson(pv), Map.class);

            List<FunctionInfo> allFuncInfos = GlobalDataCenter.getFunctions();
            Map<String, Map<String, Object>> funcViews = new LinkedHashMap<>();

            for (FunctionInfo info : allFuncInfos) {
                try {
                    ProgramView fpv = engine.programView(info.functionName(), degree);
                    funcViews.put(info.functionName(),
                            gson.fromJson(gson.toJson(fpv), Map.class));
                } catch (Exception ignore) {}
            }

            programMap.put("functions", funcViews);

            response.put("status", "success");
            response.put("program", programMap);
            out.write(gson.toJson(response));

        } catch (Exception e) {

            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("exception", e.getClass().getSimpleName());

            resp.getWriter().write(gson.toJson(response));
        }
    }
}
