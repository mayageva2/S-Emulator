package server;

import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/view")
public class ProgramViewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
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

            String degreeParam = req.getParameter("degree");
            int degree = 0;
            if (degreeParam != null && !degreeParam.isBlank()) {
                try {
                    degree = Integer.parseInt(degreeParam.trim());
                } catch (NumberFormatException e) {
                    out.println("{\"error\":\"Invalid degree parameter\"}");
                    return;
                }
            }

            ProgramView pv = engine.programView(degree);
            out.println("{");
            out.println("  \"programName\": \"" + pv.programName() + "\",");
            out.println("  \"degree\": " + pv.degree() + ",");
            out.println("  \"maxDegree\": " + pv.maxDegree() + ",");
            out.println("  \"totalCycles\": " + pv.totalCycles() + ",");
            out.println("  \"instructions\": [");

            var list = pv.instructions();
            for (int i = 0; i < list.size(); i++) {
                var ins = list.get(i);
                out.println("    {");
                out.println("      \"index\": " + ins.index() + ",");
                out.println("      \"opcode\": \"" + ins.opcode() + "\",");
                out.println("      \"label\": \"" + (ins.label() == null ? "" : ins.label()) + "\",");
                out.println("      \"cycles\": " + ins.cycles() + ",");
                out.println("      \"args\": \"" + ins.args() + "\"");
                out.print("    }");
                if (i < list.size() - 1) out.println(",");
                else out.println();
            }

            out.println("  ]");
            out.println("}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            out.println("{\"error\":\"" + e.getClass().getSimpleName() + ": " + e.getMessage() + "\"}");
        }
    }
}
