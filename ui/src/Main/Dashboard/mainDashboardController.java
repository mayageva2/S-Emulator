package Main.Dashboard;

import ConnectedUsersTable.ConnectedUsersTableController;
import HeaderAndLoadButton.HeaderAndLoadButtonController;
import StatisticsTable.StatisticsTableController;
import MainProgramsTable.MainProgramsTableController;
import FunctionsTable.FunctionsTableController;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class mainDashboardController {

    @FXML private HeaderAndLoadButtonController headerController;
    @FXML private ConnectedUsersTableController connectedUsersController;
    @FXML private StatisticsTableController statisticsController;
    @FXML private MainProgramsTableController mainProgramsController;
    @FXML private FunctionsTableController functionsController;

    private String baseUrl;

    @FXML
    public void initialize() {}

    public void initServerMode(String baseUrl) {
        this.baseUrl = baseUrl;
        Platform.runLater(this::safeInitAfterLoad);
    }

    private void safeInitAfterLoad() {
        if (connectedUsersController != null)
            connectedUsersController.setBaseUrl(baseUrl);
        if (statisticsController != null)
            statisticsController.loadUserHistory(baseUrl, getCurrentUsername());
        if (mainProgramsController != null)
            mainProgramsController.setBaseUrl(baseUrl);
        if (functionsController != null)
            functionsController.setBaseUrl(baseUrl);

        if (headerController != null) {
            headerController.setOnProgramUploaded(() -> {
                System.out.println("Program uploaded – refreshing tables...");
                Platform.runLater(() -> {
                    if (mainProgramsController != null)
                        mainProgramsController.refreshPrograms();
                    if (functionsController != null)
                        functionsController.refreshFunctions();
                    if (connectedUsersController != null)
                        connectedUsersController.refreshUsers();
                });
            });
        }

        startEventListener();
        setupSelectionListener();
    }

    private void setupSelectionListener() {
        connectedUsersController.getUsersTable().getSelectionModel()
                .selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null)
                        statisticsController.loadUserHistory(baseUrl, newSel.getUsername());
                    else
                        statisticsController.loadUserHistory(baseUrl, getCurrentUsername());
                });
    }

    private String getCurrentUsername() {
        try {
            String json = HttpSessionClient.get(baseUrl + "user/current");
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

    private void startEventListener() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String json = HttpSessionClient.get(baseUrl + "events/latest");
                    Map<String, Object> map = new Gson().fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                    String event = (String) map.get("event");
                    if (!"NONE".equalsIgnoreCase(event))
                        Platform.runLater(() -> onSystemEvent(event));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 3000, 2000);
    }

    public void onSystemEvent(String eventType) {
        try {
            switch (eventType) {
                case "USER_LOGIN", "USER_LOGOUT" -> connectedUsersController.refreshUsers();
                case "PROGRAM_UPLOADED" -> {
                    mainProgramsController.refreshPrograms();
                    functionsController.refreshFunctions();
                    connectedUsersController.refreshUsers();
                }
                case "PROGRAM_RUN" -> {
                    statisticsController.loadUserHistory(baseUrl, getCurrentUsername());
                    connectedUsersController.refreshUsers();
                }
                default -> refreshAll();
            }
            if (headerController != null)
                headerController.refreshUserHeader();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshAll() {
        if (headerController != null) headerController.refreshUserHeader();
        if (connectedUsersController != null) connectedUsersController.refreshUsers();
        if (mainProgramsController != null) mainProgramsController.refreshPrograms();
        if (functionsController != null) functionsController.refreshFunctions();
    }
}
