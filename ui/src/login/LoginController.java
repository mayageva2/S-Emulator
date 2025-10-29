package login;

import Main.Dashboard.mainDashboardController;
import Utils.ClientContext;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import emulator.api.dto.UserDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private Label errorLabel;

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/semulator/";
    private final Gson gson = new Gson();
    private final HttpSessionClient httpClient = new HttpSessionClient();

    @FXML
    private void onLoginClicked(ActionEvent event) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showError("Please enter a username.");
            return;
        }

        try {
            String formData = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
            String response = httpClient.post(DEFAULT_BASE_URL + "user/login", formData,
                    "application/x-www-form-urlencoded; charset=UTF-8");

            Map<String, Object> map = gson.fromJson(response, Map.class);
            if (!"success".equals(map.get("status"))) {
                showError(String.valueOf(map.get("message")));
                return;
            }

            long credits = ((Number) map.get("credits")).longValue();
            UserDTO user = new UserDTO(username, credits);

            ClientContext.setHttpClient(httpClient);
            ClientContext.setBaseUrl(DEFAULT_BASE_URL);
            ClientContext.init(httpClient, DEFAULT_BASE_URL, user);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Dashboard/mainDashboard.fxml"));
            Parent root = loader.load();
            mainDashboardController main = loader.getController();
            main.setHttpClient(httpClient);
            main.setBaseUrl(DEFAULT_BASE_URL);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle("S-Emulator (Server Mode)");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Connection failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
