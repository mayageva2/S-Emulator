package server;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.nio.file.Path;

import emulator.api.EmulatorEngine;
import emulator.logic.program.Program;

@WebServlet("/load")
public class LoadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        String path = req.getParameter("path");
        if (path == null || path.isBlank()) {
            resp.getWriter().println("Missing 'path' parameter");
            return;
        }

        try {
            EmulatorEngine engine = new EmulatorEngine();
            Program program = engine.loadProgram(Path.of(path));

            resp.getWriter().println("Program loaded successfully!");
            resp.getWriter().println("Program name: " + program.getName());
        } catch (Exception e) {
            resp.getWriter().println("Failed to load program:");
            resp.getWriter().println(e.getMessage());
            e.printStackTrace(resp.getWriter());
        }
    }
}
