package server;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.RunResult;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

@WebServlet("/debug/start")
public class DebugStartServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            EmulatorEngine engine = EngineHolder.getEngine();

            if (!engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"error\":\"No program loaded\"}");
                return;
            }

            String degreeStr = req.getParameter("degree");
            String inputsStr = req.getParameter("inputs");

            int degree = 0;
            if (degreeStr != null && !degreeStr.isBlank()) {
                try {
                    degree = Integer.parseInt(degreeStr.trim());
                } catch (NumberFormatException e) {
                    out.println("{\"error\":\"Invalid degree parameter\"}");
                    return;
                }
            }

            Long[] inputs = new Long[0];
            if (inputsStr != null && !inputsStr.isBlank()) {
                String[] parts = inputsStr.split(",");
                inputs = Arrays.stream(parts)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toArray(Long[]::new);
            }

            if (engine instanceof EmulatorEngineImpl impl) {
                impl.debugStart(inputs, degree);

                out.println("{");
                out.println("  \"status\": \"Debug session started successfully\",");
                out.println("  \"degree\": " + degree + ",");
                out.println("  \"inputs\": \"" + Arrays.toString(inputs) + "\",");
                out.println("  \"finished\": " + impl.debugIsFinished() + ",");
                out.println("  \"pc\": " + impl.debugCurrentPC() + ",");
                out.println("  \"cycles\": " + impl.debugCycles());
                out.println("}");
            } else {
                out.println("{\"error\": \"Engine is not EmulatorEngineImpl\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"error\":\"" + e.getClass().getSimpleName() + ": " + e.getMessage() + "\"}");
        }
    }
}
