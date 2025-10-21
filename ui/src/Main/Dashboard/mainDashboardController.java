package Main.Dashboard;

import ConnectedUsersTable.ConnectedUsersTableController;
import HeaderAndLoadButton.HeaderAndLoadButtonController;
import StatisticsTable.StatisticsTableController;
import MainProgramsTable.MainProgramsTableController;
import FunctionsTable.FunctionsTableController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class mainDashboardController {

    @FXML private HeaderAndLoadButtonController headerController;
    @FXML private ConnectedUsersTableController connectedUsersController;
    @FXML private StatisticsTableController statisticsTableController;
    @FXML private MainProgramsTableController mainProgramsController;
    @FXML private FunctionsTableController functionsController;
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

        if (connectedUsersController != null)
            connectedUsersController.setBaseUrl(baseUrl);

        if (statisticsTableController != null)
            statisticsTableController.loadUserHistory(baseUrl, getCurrentUsername());

        if (mainProgramsController != null) {
            mainProgramsController.setBaseUrl(baseUrl);
            mainProgramsController.startAutoRefresh();
        }

        if (functionsController != null) {
            functionsController.setBaseUrl(baseUrl);
            functionsController.startAutoRefresh();
        }

        System.out.println("connectedUsersController = " + connectedUsersController);
        setupSelectionListener();
    }

    private void setupSelectionListener() {
        connectedUsersController.getUsersTable().getSelectionModel()
                .selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        statisticsTableController.loadUserHistory(baseUrl, newSel.getUsername());
                    } else {
                        statisticsTableController.loadUserHistory(baseUrl, getCurrentUsername());
                    }
                });
    }

    private String getCurrentUsername() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "user/current").openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) return "";
            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> map = new Gson().fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            if ("success".equals(map.get("status"))) {
                Map<String, Object> user = (Map<String, Object>) map.get("user");
                return (String) user.get("username");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void refreshAll() {
        if (headerController != null) {
            headerController.refreshUserHeader();
        }
        if (connectedUsersController != null) {
            connectedUsersController.refreshUsers();
        }
        if (mainProgramsController != null) {
            mainProgramsController.refreshPrograms();
        }
        if (functionsController != null) {
            functionsController.refreshFunctions();
        }
    }
}
