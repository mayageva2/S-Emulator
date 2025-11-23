package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/programs/download")
public class DownloadProgramServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        byte[] data = GlobalDataCenter.getProgramFile(name);
        if (data == null) {
            resp.setStatus(404);
            resp.getWriter().write("Program not found");
            return;
        }

        resp.setContentType("application/octet-stream");
        resp.getOutputStream().write(data);
    }
}