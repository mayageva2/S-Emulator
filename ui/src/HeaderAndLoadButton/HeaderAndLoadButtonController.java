package HeaderAndLoadButton;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class HeaderAndLoadButtonController {
    @FXML private Button loadButton;
    @FXML private Button chargeButton;
    @FXML private TextField xmlPathField;
    @FXML private TextField creditsAmount;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progress;
    @FXML private Label lblUsername;
    @FXML private Label lblCredits;

    private Path lastXmlPath;
    private int lastMaxDegree = 0;
    private String lastProgramName;
    private Consumer<LoadedEvent> onLoaded;

    private String baseUrl = "http://localhost:8080/semulator/";
    public void setBaseUrl(String baseUrl) {this.baseUrl = baseUrl;}

    private final Gson gson = new Gson();

    public record LoadedEvent(Path xmlPath, String programName, int maxDegree) {}

    @FXML
    private void initialize() {
        assert xmlPathField != null;
        assert statusLabel != null;
        assert loadButton != null;

        updateUserHeader();

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(HeaderAndLoadButtonController.this::updateUserHeader);
            }
        }, 3000, 3000);
    }

    private void updateUserHeader() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "user/current").openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) return;

            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

            if ("success".equals(map.get("status"))) {
                Object userObj = map.get("user");
                if (userObj instanceof Map<?, ?> user) {
                    String username = (String) user.get("username");

                    Object creditsObj = user.get("credits");
                    long credits = (creditsObj instanceof Number n) ? n.longValue() : 0L;

                    Platform.runLater(() -> {
                        lblUsername.setText("User: " + username);
                        lblCredits.setText("Credits: " + credits);
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh user header: " + e.getMessage());
        }
    }

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

    @FXML
    private void handlechargeButtonClick() {
        String amountStr = creditsAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showAlert("Please enter credit amount.");
            return;
        }

        try {
            long amount = Long.parseLong(amountStr);
            if (amount <= 0) {
                showAlert("Amount must be positive.");
                return;
            }

            String formData = "amount=" + URLEncoder.encode(String.valueOf(amount), StandardCharsets.UTF_8);
            String response = httpPost(baseUrl + "user/charge", formData);

            Map<String, Object> map = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            if ("success".equals(map.get("status"))) {
                updateUserHeader();
                if (onCreditsChanged != null) onCreditsChanged.run();
            } else {
                showAlert("Error: " + map.get("message"));
            }

        } catch (NumberFormatException e) {
            showAlert("Invalid amount format.");
        } catch (Exception e) {
            showAlert("Connection failed: " + e.getMessage());
        }
    }

    private Runnable onCreditsChanged;
    public void setOnCreditsChanged(Runnable onCreditsChanged) {
        this.onCreditsChanged = onCreditsChanged;
    }

    private void showAlert(String msg) {
        Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait());
    }

    private void sendLoadRequest(Path xmlPath) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Preparing request...");
                updateProgress(0.1, 1);

                String urlStr = baseUrl + "load";
                Gson gson = new Gson();
                String jsonBody = gson.toJson(Map.of("path", xmlPath.toString()));

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                String json;
                try (InputStream in = conn.getInputStream()) {
                    json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }

                Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

                Platform.runLater(() -> {
                    statusLabel.textProperty().unbind();
                    statusLabel.setText("Loaded: " + map.getOrDefault("programName", "?"));
                    lastProgramName = (String) map.getOrDefault("programName", "");
                    Object degObj = map.get("maxDegree");
                    if (degObj instanceof Number n) lastMaxDegree = n.intValue();
                    lastXmlPath = xmlPath;
                    if (onLoaded != null)
                        onLoaded.accept(new LoadedEvent(lastXmlPath, lastProgramName, lastMaxDegree));
                    try {
                        URL url = new URL(baseUrl + "user/list");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.getResponseCode();
                    } catch (Exception ignored) {}
                });
                updateProgress(1, 1);
                updateMessage("Done");
                return null;
            }
        };

        progress.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        progress.setVisible(true);
        loadButton.setDisable(true);

        task.setOnSucceeded(e -> {
            progress.setVisible(false);
            loadButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            progress.setVisible(false);
            loadButton.setDisable(false);
            new Alert(Alert.AlertType.ERROR, "File load failed.").showAndWait();
        });

        new Thread(task).start();
    }

    private String httpPost(String urlStr, String formData) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(formData.getBytes(StandardCharsets.UTF_8));
        }

        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public void setOnLoaded(Consumer<LoadedEvent> onLoaded) {
        this.onLoaded = onLoaded;
    }

    public void refreshUserHeader() {
        updateUserHeader();
    }
}
