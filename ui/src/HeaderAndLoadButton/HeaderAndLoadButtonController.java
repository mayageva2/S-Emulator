package HeaderAndLoadButton;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class HeaderAndLoadButtonController {
    @FXML private Button loadButton;
    @FXML private TextField xmlPathField;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progress;

    private Path lastXmlPath;
    private int lastMaxDegree = 0;
    private String lastProgramName;
    private Consumer<LoadedEvent> onLoaded;

    public record LoadedEvent(Path xmlPath, String programName, int maxDegree) {}

    @FXML
    private void initialize() {
        assert xmlPathField != null;
        assert statusLabel  != null;
        assert loadButton   != null;
    }

    public void setOnLoaded(Consumer<LoadedEvent> onLoaded) { this.onLoaded = onLoaded; }

    @FXML
    private void handleLoadButtonClick() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open S-Program XML");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml"));
        File file = fc.showOpenDialog(loadButton.getScene().getWindow());
        if (file == null) return;
        xmlPathField.setText(file.getAbsolutePath());
        sendLoadRequest(file.toPath());
    }

    private void sendLoadRequest(Path xmlPath) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Preparing request...");
                updateProgress(0.02, 1);
                Thread.sleep(120);
                updateProgress(0.05, 1);
                Thread.sleep(120);
                updateProgress(0.10, 1);

                String urlStr = "http://localhost:8080/semulator/load";
                Gson gson = new Gson();
                String jsonBody = gson.toJson(Map.of("path", xmlPath.toString()));

                updateMessage("Opening connection...");
                updateProgress(0.15, 1);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                Thread.sleep(150);
                updateProgress(0.25, 1);

                updateMessage("Sending JSON to server...");
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
                    int chunk = Math.max(256, payload.length / 10);
                    int written = 0;
                    while (written < payload.length) {
                        int toWrite = Math.min(chunk, payload.length - written);
                        os.write(payload, written, toWrite);
                        os.flush();
                        written += toWrite;
                        double p = 0.25 + (written / (double) payload.length) * (0.45 - 0.25);
                        updateProgress(p, 1);
                        Thread.sleep(60);
                    }
                }

                updateMessage("Waiting for response...");
                Thread.sleep(200);
                int code = conn.getResponseCode();
                updateProgress(0.55, 1);
                if (code != 200) {
                    String err = "";
                    try (InputStream es = conn.getErrorStream()) {
                        if (es != null) err = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    throw new IOException("Server returned status " + code + (err.isBlank() ? "" : (": " + err)));
                }

                updateMessage("Reading response...");
                String json;
                try (InputStream in = conn.getInputStream()) {
                    int contentLength = conn.getContentLength();
                    if (contentLength > 0) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(1024, contentLength));
                        byte[] buf = new byte[4096];
                        int read;
                        int total = 0;
                        while ((read = in.read(buf)) != -1) {
                            bos.write(buf, 0, read);
                            total += read;
                            double frac = total / (double) contentLength;
                            double p = 0.55 + frac * (0.90 - 0.55);
                            updateProgress(Math.min(p, 0.90), 1);
                            Thread.sleep(20);
                        }
                        json = bos.toString(StandardCharsets.UTF_8);
                    } else {
                        Thread.sleep(120);
                        json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        updateProgress(0.72, 1);
                        Thread.sleep(120);
                        updateProgress(0.85, 1);
                        Thread.sleep(120);
                        updateProgress(0.90, 1);
                    }
                }
                System.out.println("LOAD RESPONSE = " + json);

                updateMessage("Parsing JSON...");
                Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
                Thread.sleep(150);
                updateProgress(0.96, 1);

                updateMessage("Finalizing...");
                Platform.runLater(() -> {
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Loaded: " + map.getOrDefault("programName", "?"));
                    lastProgramName = (String) map.getOrDefault("programName", "");
                    Object degObj = map.get("maxDegree");
                    if (degObj instanceof Number n) lastMaxDegree = n.intValue();
                    lastXmlPath = xmlPath;
                    if (onLoaded != null)
                        onLoaded.accept(new LoadedEvent(lastXmlPath, lastProgramName, lastMaxDegree));
                });
                Thread.sleep(150);
                updateProgress(1.0, 1);
                updateMessage("Done");
                Thread.sleep(100);

                return null;
            }
        };

        progress.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        progress.setVisible(true);
        loadButton.setDisable(true);

        task.setOnSucceeded(e -> {
            progress.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progress.setVisible(false);
            loadButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            progress.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progress.setVisible(false);
            loadButton.setDisable(false);
            Throwable ex = task.getException();
            statusLabel.setText("Load failed");
            new Alert(Alert.AlertType.ERROR,
                    "File load failed:\n" + (ex != null ? ex.getMessage() : "Unknown error"),
                    ButtonType.OK).showAndWait();
        });

        new Thread(task, "load-http-task").start();
    }
}
