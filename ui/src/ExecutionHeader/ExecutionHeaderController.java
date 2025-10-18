package ExecutionHeader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ExecutionHeaderController {
    @FXML private Label lblUsername;
    @FXML private Label lblCredits;

    private static final String BASE_URL = "http://localhost:8080/semulator/";

    @FXML
    public void initialize() {
        updateUserHeader();

        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(ExecutionHeaderController.this::updateUserHeader);
            }
        }, 2000, 2000);
    }

    private void updateUserHeader() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "user/current");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, Object> map = new Gson().fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

                    if ("success".equals(map.get("status"))) {
                        Map<String, Object> user = (Map<String, Object>) map.get("user");
                        String username = (String) user.get("username");
                        long credits = ((Number) user.get("credits")).longValue();

                        Platform.runLater(() -> {
                            lblUsername.setText("User: " + username);
                            lblCredits.setText("Available Credits: " + credits);
                        });
                    } else {
                        Platform.runLater(() -> {
                            lblUsername.setText("User: (none)");
                            lblCredits.setText("Available Credits: —");
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblUsername.setText("User: (none)");
                    lblCredits.setText("Available Credits: —");
                });
            }
        }).start();
    }
}
