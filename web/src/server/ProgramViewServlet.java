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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            EmulatorEngine engine = EngineHolder.getEngine();

            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(gson.toJson(Map.of(
                        "status", "error",
                        "message", "No program loaded"
                )));
                return;
            }

            // Parse degree
            int degree = 0;
            String degreeParam = req.getParameter("degree");
            if (degreeParam != null && !degreeParam.isBlank()) {
                degree = Integer.parseInt(degreeParam.trim());
            }

            // Parse program name
            String programParam = req.getParameter("program");
            if (programParam != null)
                programParam = URLDecoder.decode(programParam, StandardCharsets.UTF_8);

            // Fetch view from engine
            ProgramView pv = (programParam == null || programParam.isBlank() || "Main Program".equalsIgnoreCase(programParam))
                    ? engine.programView(degree)
                    : engine.programView(programParam, degree);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("program", pv);

            out.write(gson.toJson(response));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "exception", e.getClass().getSimpleName()
            )));
        }
    }
}