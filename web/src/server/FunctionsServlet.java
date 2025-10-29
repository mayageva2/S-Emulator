package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.FunctionInfo;
import emulator.api.dto.FunctionService;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/functions")
public class FunctionsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            List<emulator.api.dto.FunctionInfo> allFuncs = GlobalDataCenter.getFunctions();
            resp.getWriter().write(gson.toJson(Map.of("status", "success", "functions", allFuncs)));
        } catch (Exception e) {
            resp.getWriter().write(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }
}

