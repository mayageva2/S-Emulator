package goToDashboardButton;

import Main.Dashboard.mainDashboardController;
import Main.Execution.MainExecutionController;
import Utils.ClientContext;
import Utils.HttpSessionClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class goToDashboardController {
    @FXML private Button btnGoToDashboard;
    private MainExecutionController parentController;

    private HttpSessionClient httpClient;
    private String baseUrl;

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setParentController(MainExecutionController controller) {
        this.parentController = controller;
    }

    @FXML
    public void initialize() {
        this.httpClient = ClientContext.getHttpClient();
        this.baseUrl = ClientContext.getBaseUrl();
    }

    @FXML
    public void onGoToDashboardClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Dashboard/mainDashboard.fxml"));
            Parent root = loader.load();

            mainDashboardController controller = loader.getController();
            controller.setHttpClient(httpClient);
            controller.initServerMode(baseUrl);
            controller.refreshAll();

            Stage stage = (Stage) btnGoToDashboard.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load mainDashboard.fxml: " + e.getMessage());
        }
    }
}
