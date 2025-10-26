package Utils;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HttpSessionClient {
    private static final Map<String, String> cookies = new HashMap<>();

    // GET
    public static String get(String urlStr) throws IOException {
        HttpURLConnection conn = openConnection(urlStr, "GET");
        return readResponse(conn);
    }

    // POST
    public static String post(String urlStr, String body, String contentType) throws IOException {
        HttpURLConnection conn = openConnection(urlStr, "POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", contentType);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    // MULTIPART POST
    public static String postMultipart(String urlStr, Path file, String fieldName) throws IOException {
        String boundary = "Boundary" + System.currentTimeMillis();
        HttpURLConnection conn = openConnection(urlStr, "POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getFileName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            os.write("Content-Type: text/xml\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            Files.copy(file, os);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        return readResponse(conn);
    }

    // HELPERS
    private static HttpURLConnection openConnection(String urlStr, String method) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        applyCookies(conn);
        conn.setRequestMethod(method);
        return conn;
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        storeCookies(conn);
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // COOKIES HELPERS
    private static void applyCookies(URLConnection conn) {
        if (!cookies.isEmpty()) {
            String cookieHeader = cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("");
            conn.setRequestProperty("Cookie", cookieHeader);
        }
    }

    private static void storeCookies(URLConnection conn) {
        Map<String, List<String>> headers = conn.getHeaderFields();
        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies != null) {
            for (String cookie : setCookies) {
                String[] parts = cookie.split(";", 2);
                String[] pair = parts[0].split("=", 2);
                if (pair.length == 2) {
                    cookies.put(pair[0].trim(), pair[1].trim());
                }
            }
        }
    }

    public static String getSessionCookie() {
        if (cookies.isEmpty()) return "";
        return cookies.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }
}
