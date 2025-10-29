package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.FunctionService;
import emulator.api.dto.ProgramService;
import emulator.api.dto.UserDTO;
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

        Map<String, Object> response = new LinkedHashMap<>();
        String path = req.getServletPath();

        try (PrintWriter out = resp.getWriter()) {

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
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.put("status", "error");
                response.put("message", "No engine found for this session");
                out.write(gson.toJson(response));
                return;
            }

            ProgramService programService = engine.getProgramService();
            FunctionService functionService = engine.getFunctionService();

            if (path.endsWith("/functions")) {
                String program = req.getParameter("program");
                if (program == null || program.isBlank()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.put("status", "error");
                    response.put("message", "Missing 'program' parameter");
                    out.write(gson.toJson(response));
                    return;
                }

                program = URLDecoder.decode(program, StandardCharsets.UTF_8);
                Set<String> funcs = functionService.getFunctionsByProgram(program);

                response.put("status", "success");
                response.put("program", program);
                response.put("functions", funcs);

                out.write(gson.toJson(response));
                return;
            }

            if (path.endsWith("/programs")) {
                String func = req.getParameter("function");
                if (func == null || func.isBlank()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.put("status", "error");
                    response.put("message", "Missing 'function' parameter");
                    out.write(gson.toJson(response));
                    return;
                }

                func = URLDecoder.decode(func, StandardCharsets.UTF_8);
                Set<String> progs = functionService.getProgramsUsingFunction(func);

                response.put("status", "success");
                response.put("function", func);
                response.put("programs", progs);

                out.write(gson.toJson(response));
                return;
            }

            if (path.endsWith("/related-functions")) {
                String func = req.getParameter("function");
                if (func == null || func.isBlank()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.put("status", "error");
                    response.put("message", "Missing 'function' parameter");
                    out.write(gson.toJson(response));
                    return;
                }

                func = URLDecoder.decode(func, StandardCharsets.UTF_8);
                Set<String> related = functionService.getRelatedFunctions(func);

                response.put("status", "success");
                response.put("function", func);
                response.put("related", related);

                out.write(gson.toJson(response));
                return;
            }

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.put("status", "error");
            response.put("message", "Unknown relation path: " + path);
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
