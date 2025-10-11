package server;

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.nio.file.*;

import emulator.api.EmulatorEngine;
import emulator.api.dto.LoadResult;

@WebServlet("/load")
public class LoadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String pathStr = req.getParameter("path");
        if (pathStr == null || pathStr.isBlank()) {
            out.println("Missing 'path' parameter");
            return;
        }

        Path xmlPath = Path.of(pathStr);

        try {
            EmulatorEngine engine = EngineHolder.getEngine();
            LoadResult result = engine.loadProgram(xmlPath);

            out.println("Program loaded successfully!");
            out.println("Program name: " + result.programName());
            out.println("Instruction count: " + result.instructionCount());
            out.println("Max degree: " + result.maxDegree());
        } catch (Exception e) {
            out.println("Failed to load program:");
            out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(out);
        }
    }
}
