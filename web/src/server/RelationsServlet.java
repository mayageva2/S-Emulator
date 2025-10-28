package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.FunctionService;
import emulator.api.dto.ProgramService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@WebServlet(urlPatterns = {
        "/relations/functions",
        "/relations/programs",
        "/relations/related-functions"
})
public class RelationsServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getServletPath();
        Map<String, Object> response = new LinkedHashMap<>();

        try (PrintWriter out = resp.getWriter()) {
            // 🔹 קחי את המנוע הספציפי ל-session הזה
            EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());
            ProgramService programService = engine.getProgramService();
            FunctionService functionService = engine.getFunctionService();

            if (path.endsWith("/functions")) {
                String rawParam = req.getQueryString();
                String program = URLDecoder.decode(req.getParameter("program"), StandardCharsets.UTF_8);

                if (rawParam != null && rawParam.contains("%2B")) {  // edge case של '+'
                    program = program.replace(' ', '+');
                }

                System.out.println(">>> RelationsServlet /functions – looking for program: [" + program + "]");
                Set<String> funcs = functionService.getFunctionsByProgram(program);
                System.out.println(">>> Found functions: " + funcs);
                out.write(gson.toJson(funcs));

            } else if (path.endsWith("/programs")) {
                String func = URLDecoder.decode(req.getParameter("function"), StandardCharsets.UTF_8);
                Set<String> progs = functionService.getProgramsUsingFunction(func);
                out.write(gson.toJson(progs));

            } else if (path.endsWith("/related-functions")) {
                String func = URLDecoder.decode(req.getParameter("function"), StandardCharsets.UTF_8);
                Set<String> related = functionService.getRelatedFunctions(func);
                out.write(gson.toJson(related));

            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.put("error", "Unknown relation path");
                out.write(gson.toJson(response));
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
