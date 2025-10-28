package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramService;
import emulator.api.dto.ProgramStats;
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

            List<Map<String, Object>> jsonList = new ArrayList<>();
            for (var p : list) {
                jsonList.add(Map.of(
                        "programName", p.programName,
                        "username", p.username,
                        "instructionCount", p.instructionCount,
                        "maxDegree", p.maxDegree,
                        "runCount", 0,
                        "avgCreditCost", 0
                ));
            }

            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "success",
                    "programs", jsonList
            )));
        } catch (Exception e) {
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }
}

