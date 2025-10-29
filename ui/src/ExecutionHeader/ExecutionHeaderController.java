package ExecutionHeader;

import Utils.ClientContext;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ExecutionHeaderController {
    @FXML private Label lblUsername;
    @FXML private Label lblCredits;

    private HttpSessionClient httpClient;
    private String baseUrl;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        this.httpClient = ClientContext.getHttpClient();
        this.baseUrl = ClientContext.getBaseUrl();
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
                String json = httpClient.get(baseUrl + "user/current");
                Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                if ("success".equals(map.get("status"))) {
                    Map<String, Object> user = (Map<String, Object>) map.get("user");
                    String username = (String) user.get("username");
                    long credits = ((Number) user.get("credits")).longValue();

                    Platform.runLater(() -> {
                        lblUsername.setText("User: " + username);
                        lblCredits.setText("Available Credits: " + credits);
                    });
                } else {
                    showNoUser();
                }
            } catch (Exception e) {
                showNoUser();
            }
        }).start();
    }

    private void showNoUser() {
        Platform.runLater(() -> {
            lblUsername.setText("User: (none)");
            lblCredits.setText("Available Credits: —");
        });
    }
}
