package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.dto.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.ByteArrayInputStream;
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

            // Get engine + user
            EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());
            Map<String, FunctionInfo> stats = engine.getFunctionStats();
            UserDTO user = SessionUserManager.getUser(req.getSession());
            String username = (user != null) ? user.getUsername() : "unknown";

            var loadService = new LoadService(
                    engine.getProgramService(),
                    engine.getFunctionService(),
                    new ProgramStatsRepository()
            );

            // === 1) Read raw file bytes once ===
            byte[] fileBytes;
            try (InputStream fileStream = filePart.getInputStream()) {
                fileBytes = fileStream.readAllBytes();
            }

            // === 2) Load into THIS user's engine ===
            LoadResult result;
            try (InputStream reloadStream = new ByteArrayInputStream(fileBytes)) {
                result = loadService.loadFromStream(engine, reloadStream, username);
            }

            if (result != null) {
                // === 3) Save program metadata ===
                GlobalDataCenter.addProgram(
                        result.programName(),
                        username,
                        result.instructionCount(),
                        result.maxDegree()
                );

                // === 4) Save XML file globally for all users ===
                GlobalDataCenter.storeProgramFile(result.programName(), fileBytes);

                // === 5) Store functions ===
                for (String funcName : result.functions()) {
                    FunctionInfo info = stats.get(funcName);
                    if (info == null) {
                        info = new FunctionInfo(funcName, result.programName(),
                                username, 0, 0, 0.0);
                    }
                    GlobalDataCenter.addFunction(info);
                }

                // === 6) Update program stats ===
                ProgramStats loadedProgram = new ProgramStats(
                        result.programName(),
                        username,
                        result.instructionCount(),
                        result.maxDegree(),
                        0, 0.0
                );
                ProgramRegistry.register(loadedProgram);

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
