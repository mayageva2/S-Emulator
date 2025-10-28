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

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            String body = new BufferedReader(new InputStreamReader(req.getInputStream()))
                    .lines().reduce("", (a, b) -> a + b);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(body, Map.class);

            String program = (String) data.get("program");
            int degree = ((Double) data.getOrDefault("degree", 0.0)).intValue();

            String architectureName = (String) data.getOrDefault("architecture", "I");
            ArchitectureInfo archInfo = ARCHITECTURES.getOrDefault(architectureName, ARCHITECTURES.get("I"));

            @SuppressWarnings("unchecked")
            List<Double> inputsD = (List<Double>) data.getOrDefault("inputs", List.of());
            Long[] inputs = inputsD.stream().map(Double::longValue).toArray(Long[]::new);
            SessionUserBinder.bind(req.getSession());
            EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());
            RunResult result = ((EmulatorEngineImpl) engine).run(program, degree, archInfo, inputs);
            UserDTO currentUser = SessionUserManager.getUser(req.getSession());
            if (currentUser != null) {
                SessionUserBinder.snapshotBack(req.getSession(), currentUser);
            }

            responseMap.put("status", "success");
            responseMap.put("result", result);
            responseMap.put("userCredits", currentUser != null ? currentUser.getCredits() : 0L);

            ServerEventManager.broadcast("PROGRAM_RUN");

        } catch (IllegalStateException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("not enough credits")) {
                responseMap.put("status", "error");
                responseMap.put("message", msg);
                responseMap.put("errorType", "CREDITS");
            } else {
                responseMap.put("status", "error");
                responseMap.put("message", msg != null ? msg : "Unknown runtime error");
                responseMap.put("errorType", "RUNTIME");
            }

        } catch (Exception e) {
            e.printStackTrace();
            responseMap.put("status", "error");
            responseMap.put("message", e.getMessage());
        }

        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(responseMap));
        }
    }
}
