package server;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/programs/list")
public class ListProgramsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<GlobalDataCenter.ProgramEntry> list = GlobalDataCenter.getPrograms();
            resp.getWriter().write(gson.toJson(Map.of("status", "success", "programs", list)));
        } catch (Exception e) {
            resp.getWriter().write(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }
}

