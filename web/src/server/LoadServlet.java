package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.LoadService;
import emulator.api.dto.LoadResult;
import emulator.logic.user.UserManager;
import emulator.logic.user.User;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@WebServlet("/load")
@MultipartConfig
public class LoadServlet extends HttpServlet {
    private final LoadService loadService = new LoadService();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Part filePart = req.getPart("file");
            if (filePart == null) {
                resp.getWriter().write(gson.toJson(Map.of(
                        "status", "error",
                        "message", "No file part received"
                )));
                return;
            }

            EmulatorEngine engine = EngineHolder.getEngine();
            String username = UserManager.getCurrentUser()
                    .map(User::getUsername)
                    .orElse("unknown");

            LoadResult result;
            try (InputStream fileStream = filePart.getInputStream()) {
                result = loadService.loadFromStream(engine, fileStream, username);
            }

            ServerEventManager.broadcast("PROGRAM_UPLOADED");

            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "success",
                    "programName", result.programName(),
                    "instructionCount", result.instructionCount(),
                    "maxDegree", result.maxDegree()
            )));

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }
}
