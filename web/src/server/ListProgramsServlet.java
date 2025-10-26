package server;

import com.google.gson.Gson;
import emulator.api.dto.ProgramService;
import emulator.api.dto.ProgramStats;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

@WebServlet("/programs/list")
public class ListProgramsServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private final ProgramService programService = ProgramService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            List<ProgramStats> list = programService.getAllPrograms();
            String json = gson.toJson(Map.of("status", "success", "programs", list));
            resp.getWriter().write(json);
        } catch (Exception e) {
            String err = gson.toJson(Map.of("status", "error", "message", e.getMessage()));
            resp.getWriter().write(err);
        }
    }
}
