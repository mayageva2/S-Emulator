package server;

import com.google.gson.Gson;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.ArchitectureInfo;
import emulator.api.dto.RunResult;
import emulator.api.dto.UserDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.util.*;

@WebServlet("/run")
public class RunServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    private static final Map<String, ArchitectureInfo> ARCHITECTURES = Map.of(
            "I",   new ArchitectureInfo("I",   5,    "Basic architecture"),
            "II",  new ArchitectureInfo("II",  100,  "Optimized architecture"),
            "III", new ArchitectureInfo("III", 500,  "High performance architecture"),
            "IV",  new ArchitectureInfo("IV",  1000, "Ultimate architecture")
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> response = new LinkedHashMap<>();

        try (PrintWriter out = resp.getWriter()) {
            HttpSession session = req.getSession(false);
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.put("status", "error");
                response.put("message", "No active session");
                out.write(gson.toJson(response));
                return;
            }

            UserDTO currentUser = SessionUserManager.getUser(session);
            if (currentUser == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.put("status", "error");
                response.put("message", "No user logged in");
                out.write(gson.toJson(response));
                return;
            }

            EmulatorEngine engine = EngineSessionManager.getEngine(session);
            if (engine == null || !engine.hasProgramLoaded()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.put("status", "error");
                response.put("message", "No program loaded for user: " + currentUser.getUsername());
                out.write(gson.toJson(response));
                return;
            }

            // === parse JSON body safely ===
            String body = new BufferedReader(new InputStreamReader(req.getInputStream()))
                    .lines().reduce("", (a, b) -> a + b);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(body, Map.class);
            if (data == null) {
                response.put("status", "error");
                response.put("message", "Missing JSON body");
                out.write(gson.toJson(response));
                return;
            }

            String program = (String) data.get("program");
            int degree = ((Double) data.getOrDefault("degree", 0.0)).intValue();

            String architectureName = (String) data.getOrDefault("architecture", "I");
            ArchitectureInfo archInfo = ARCHITECTURES.getOrDefault(architectureName, ARCHITECTURES.get("I"));

            @SuppressWarnings("unchecked")
            List<Double> inputsD = (List<Double>) data.getOrDefault("inputs", List.of());
            Long[] inputs = inputsD.stream().map(Double::longValue).toArray(Long[]::new);

            RunResult result;
            try {
                result = ((EmulatorEngineImpl) engine).run(program, degree, archInfo, inputs);
            } catch (IllegalStateException ex) {
                String msg = ex.getMessage();
                response.put("status", "error");
                response.put("message", msg);
                response.put("errorType", msg.toLowerCase().contains("credits") ? "CREDITS" : "RUNTIME");
                out.write(gson.toJson(response));
                return;
            }

            UserDTO updatedUser = engine.getSessionUser();
            if (updatedUser != null) {
                SessionUserManager.setUser(session, updatedUser);
                SessionUserBinder.snapshotBack(session, updatedUser);
            }

            response.put("status", "success");
            response.put("result", result);
            response.put("userCredits", updatedUser != null ? updatedUser.getCredits() : 0L);

            ServerEventManager.broadcast("PROGRAM_RUN");
            out.write(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("exception", e.getClass().getSimpleName());
            resp.getWriter().write(gson.toJson(response));
        }
    }
}
