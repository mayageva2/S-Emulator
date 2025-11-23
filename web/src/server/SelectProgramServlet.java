//package server;
//
//import com.google.gson.Gson;
//import emulator.api.EmulatorEngine;
//import emulator.logic.program.Program;
//import emulator.logic.program.ProgramImpl;
//import jakarta.servlet.annotation.WebServlet;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//import java.io.IOException;
//import java.util.Map;
//
//@WebServlet("/selectProgram")
//public class SelectProgramServlet extends HttpServlet {
//    private final Gson gson = new Gson();
//
//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        resp.setContentType("application/json;charset=UTF-8");
//
//        String programName = req.getParameter("name");
//        if (programName == null) programName = req.getParameter("programName");
//        if (programName == null || programName.isBlank()) {
//            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            resp.getWriter().write(gson.toJson(
//                    Map.of("error", "Missing parameter: programName")
//            ));
//            return;
//        }
//
//        Program program = GlobalDataCenter.getProgramAsObject(programName);
//        if (program == null) {
//            program = GlobalProgramRegistry.get(programName);
//        }
//
//        if (program == null) {
//            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            resp.getWriter().write(gson.toJson(
//                    Map.of("error", "Program not found: " + programName)
//            ));
//            return;
//        }
//
//        EmulatorEngine engine = EngineSessionManager.getEngine(req.getSession());
//        if (engine == null) {
//            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            resp.getWriter().write(gson.toJson(
//                    Map.of("error", "Engine not available for this session")
//            ));
//            return;
//        }
//
//        try {
//            engine.setProgramFromGlobal(program, programName);
//            resp.setStatus(HttpServletResponse.SC_OK);
//            resp.getWriter().write(gson.toJson(
//                    Map.of(
//                            "status", "ok",
//                            "programName", programName
//                    )
//            ));
//        } catch (Exception e) {
//            e.printStackTrace();
//            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            resp.getWriter().write(gson.toJson(
//                    Map.of(
//                            "error", "Failed to load program into engine",
//                            "details", e.getMessage() == null ? "unknown" : e.getMessage()
//                    )
//            ));
//        }
//    }
//
//    @Override
//    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        doPost(req, resp);
//    }
//}
