package server;

import com.google.gson.Gson;
import emulator.api.dto.ArchitectureInfo;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/architectures")
public class ArchitectureServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> responseMap = new LinkedHashMap<>();
        try {
            List<ArchitectureInfo> list = List.of(
                    new ArchitectureInfo("I", 5, "Basic architecture"),
                    new ArchitectureInfo("II", 100, "Optimized architecture"),
                    new ArchitectureInfo("III", 500, "High performance architecture"),
                    new ArchitectureInfo("IV", 1000, "Ultimate architecture")
            );

            responseMap.put("status", "success");
            responseMap.put("architectures", list);

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("status", "error");
            responseMap.put("message", e.getMessage());
        }

        try (PrintWriter out = resp.getWriter()) {
            out.write(gson.toJson(responseMap));
        }
    }
}
