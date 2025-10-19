package Main.Dashboard;

import ConnectedUsersTable.ConnectedUsersTableController;
import HeaderAndLoadButton.HeaderAndLoadButtonController;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class mainDashboardController {

    @FXML private HeaderAndLoadButtonController headerController;
    @FXML private ConnectedUsersTableController connectedUsersController;
    @FXML private VBox leftCol;

    private String baseUrl;

    @FXML
    public void initialize() {
        System.out.println("mainDashboardController initialized");

        if (connectedUsersController != null) {
            connectedUsersController.startAutoRefresh();
        }
    }

    public void initServerMode(String baseUrl) {
        this.baseUrl = baseUrl;

        System.out.println("Server mode initialized with base URL: " + baseUrl);

        if (headerController != null) {
            headerController.setBaseUrl(baseUrl);
            headerController.refreshUserHeader();
        }

        if (connectedUsersController != null) {
            connectedUsersController.setBaseUrl(baseUrl);
            connectedUsersController.startAutoRefresh();
        }
    }

    public void refreshAll() {
        if (headerController != null) {
            headerController.refreshUserHeader();
        }
        if (connectedUsersController != null) {
            connectedUsersController.refreshUsers();
        }
    }
}
