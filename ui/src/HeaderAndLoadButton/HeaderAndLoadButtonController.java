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
                updateMessage("Loading...");
                updateProgress(0.1, 1);

                String urlStr = "http://localhost:8080/semulator/load";
                String postData = "path=" + URLEncoder.encode(xmlPath.toString(), StandardCharsets.UTF_8);
                byte[] postBytes = postData.getBytes(StandardCharsets.UTF_8);

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.getOutputStream().write(postBytes);

                int code = conn.getResponseCode();
                if (code != 200) throw new IOException("Server returned status " + code);

                try (InputStream in = conn.getInputStream()) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    updateMessage("Parsing response...");
                    updateProgress(0.9, 1);

                    Gson gson = new Gson();
                    Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

                    Platform.runLater(() -> {
                        statusLabel.setText("Loaded: " + map.getOrDefault("programName", "?"));
                        lastProgramName = (String) map.getOrDefault("programName", "");
                        Object degObj = map.get("maxDegree");
                        if (degObj instanceof Number n) lastMaxDegree = n.intValue();
                        lastXmlPath = xmlPath;
                        if (onLoaded != null)
                            onLoaded.accept(new LoadedEvent(lastXmlPath, lastProgramName, lastMaxDegree));
                    });
                }

                updateMessage("Done");
                updateProgress(1, 1);
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
