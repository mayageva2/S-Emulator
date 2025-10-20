package server;

import com.google.gson.Gson;
import emulator.api.dto.FunctionInfo;
import emulator.api.dto.FunctionService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/functions")
public class FunctionsServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private final FunctionService functionService = FunctionService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<FunctionInfo> all = functionService.getAllFunctions();
            String json = gson.toJson(Map.of(
                    "status", "success",
                    "functions", all
            ));
            resp.getWriter().write(json);
        } catch (Exception e) {
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }
}
