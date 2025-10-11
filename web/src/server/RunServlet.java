package server;

import emulator.api.EmulatorEngine;
import emulator.api.dto.RunResult;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

@WebServlet("/run")
public class RunServlet extends HttpServlet {

    EmulatorEngine engine = EngineHolder.getEngine();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String degreeStr = req.getParameter("degree");
            String inputsStr = req.getParameter("inputs");

            if (degreeStr == null || inputsStr == null) {
                out.println("{\"error\":\"Missing parameters 'degree' or 'inputs'\"}");
                return;
            }

            int degree = Integer.parseInt(degreeStr);
            Long[] inputs = Arrays.stream(inputsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .toArray(Long[]::new);

            RunResult result = engine.run(degree, inputs);

            out.printf(
                    "{ \"y\": %d, \"cycles\": %d, \"varsCount\": %d }",
                    result.y(),
                    result.cycles(),
                    result.vars() == null ? 0 : result.vars().size()
            );

        } catch (Exception e) {
            out.printf("{\"error\": \"%s: %s\"}",
                    e.getClass().getSimpleName(),
                    e.getMessage().replace("\"", "'"));
            e.printStackTrace(out);
        }
    }
}
