package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.LoadService;
import emulator.api.dto.LoadResult;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@WebServlet("/load")
@MultipartConfig
public class LoadServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            SessionUserBinder.bind(req.getSession());
            Part filePart = req.getPart("file");
            if (filePart == null) {
                resp.getWriter().write(gson.toJson(Map.of(
                        "status", "error",
                        "message", "No file part received"
                )));
                return;
            }

            EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());
            UserDTO user = SessionUserManager.getUser(req.getSession());
            String username = (user != null) ? user.getUsername() : "unknown";

            var loadService = new emulator.api.dto.LoadService(
                    engine.getProgramService(),
                    engine.getFunctionService(),
                    new emulator.api.dto.ProgramStatsRepository()
            );

            LoadResult result;
            try (InputStream fileStream = filePart.getInputStream()) {
                result = loadService.loadFromStream(engine, fileStream, username);
            }

            if (result != null) {
                GlobalDataCenter.addProgram(
                        result.programName(),
                        username,
                        result.instructionCount(),
                        result.maxDegree()
                );
                ServerEventManager.broadcast("PROGRAM_UPLOADED");
            }

            SessionUserBinder.snapshotBack(req.getSession(), user);

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
