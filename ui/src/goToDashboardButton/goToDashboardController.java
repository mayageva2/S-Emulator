package goToDashboardButton;

import Main.Dashboard.mainDashboardController;
import Main.Execution.MainExecutionController;
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
    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    public void setParentController(MainExecutionController controller) {
        this.parentController = controller;
    }

    @FXML
    public void onGoToDashboardClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Dashboard/mainDashboard.fxml"));
            Parent root = loader.load();

            mainDashboardController controller = loader.getController();
            controller.initServerMode();
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
