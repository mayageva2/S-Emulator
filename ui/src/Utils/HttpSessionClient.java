package Utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

public class HttpSessionClient {
    private String sessionCookie = null;

    // --- GET ---
    public String get(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (sessionCookie != null)
            conn.setRequestProperty("Cookie", sessionCookie);

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(stream.readAllBytes());
        stream.close();

        captureCookie(conn);
        return response;
    }

    // --- POST ---
    public String post(String urlStr, String body, String contentType) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", contentType);
        if (sessionCookie != null)
            conn.setRequestProperty("Cookie", sessionCookie);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(stream.readAllBytes());
        stream.close();

        captureCookie(conn);
        return response;
    }


    public String postMultipart(String urlStr, Path filePath, String fieldName) throws IOException {
        String boundary = "Boundary-" + System.currentTimeMillis();
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (sessionCookie != null)
            conn.setRequestProperty("Cookie", sessionCookie);

        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);
             FileInputStream inputStream = new FileInputStream(filePath.toFile())) {

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"")
                    .append(fieldName)
                    .append("\"; filename=\"")
                    .append(filePath.getFileName().toString())
                    .append("\"\r\n");
            writer.append("Content-Type: application/xml\r\n\r\n");
            writer.flush();

            inputStream.transferTo(output);
            output.flush();

            writer.append("\r\n--").append(boundary).append("--\r\n");
            writer.flush();
        }

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(stream.readAllBytes());
        stream.close();

        captureCookie(conn);
        return response;
    }

    private void captureCookie(HttpURLConnection conn) {
        String setCookie = conn.getHeaderField("Set-Cookie");
        if (setCookie != null && setCookie.contains("JSESSIONID")) {
            int end = setCookie.indexOf(';');
            sessionCookie = setCookie.substring(0, end);
            System.out.println("📥 Received cookie: " + sessionCookie);
        }
    }
}
