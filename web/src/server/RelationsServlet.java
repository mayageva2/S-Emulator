package server;

import com.google.gson.Gson;
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
    private final ProgramService programService = ProgramService.getInstance();
    private final FunctionService functionService = FunctionService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getServletPath();
        Map<String, Object> response = new LinkedHashMap<>();

        try (PrintWriter out = resp.getWriter()) {
            if (path.endsWith("/functions")) {
                String program = URLDecoder.decode(req.getParameter("program"), StandardCharsets.UTF_8);
                Set<String> funcs = functionService.getFunctionsByProgram(program);
                out.write(gson.toJson(funcs));

            } else if (path.endsWith("/programs")) {
                String func = URLDecoder.decode(req.getParameter("function"), StandardCharsets.UTF_8);
                Set<String> progs = FunctionService.getInstance().getProgramsUsingFunction(func);
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
