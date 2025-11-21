package Main.Dashboard;

import ConnectedUsersTable.ConnectedUsersTableController;
import HeaderAndLoadButton.HeaderAndLoadButtonController;
import Main.Execution.MainExecutionController;
import StatisticsCommands.StatisticsCommandsController;
import StatisticsTable.StatisticsTableController;
import MainProgramsTable.MainProgramsTableController;
import FunctionsTable.FunctionsTableController;
import Utils.ClientContext;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class mainDashboardController {

    @FXML private HeaderAndLoadButtonController headerController;
    @FXML private ConnectedUsersTableController connectedUsersController;
    @FXML private StatisticsTableController statisticsController;
    @FXML private MainProgramsTableController mainProgramsController;
    @FXML private FunctionsTableController functionsController;
    @FXML private StatisticsCommandsController statisticsCommandsController;

    private String baseUrl;
    private HttpSessionClient httpClient;
    private boolean lockedOnSelectedUser = false;

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @FXML
    private void initialize() {
        this.httpClient = ClientContext.getHttpClient();
        this.baseUrl = ClientContext.getBaseUrl();

        if (httpClient == null || baseUrl == null) {
            System.err.println("ClientContext not initialized yet. Make sure login completed successfully.");
            return;
        }

        setHttpClient(httpClient);
        setBaseUrl(baseUrl);
        Platform.runLater(() -> {
            if (ClientContext.isInitialized()) {
                safeInitAfterLoad();
            } else {
                System.err.println("ClientContext not ready yet, delaying dashboard initialization...");
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (ClientContext.isInitialized()) {
                            Platform.runLater(() -> safeInitAfterLoad());
                            cancel();
                        }
                    }
                }, 1000, 1000);
            }
        });

    }

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
        if (headerController != null) headerController.setHttpClient(client);
        if (connectedUsersController != null) connectedUsersController.setHttpClient(client);
        if (statisticsController != null) statisticsController.setHttpClient(client);
        if (statisticsController != null) statisticsController.setBaseUrl(baseUrl);
        if (mainProgramsController != null) mainProgramsController.setHttpClient(client);
        if (functionsController != null) functionsController.setHttpClient(client);
        if (statisticsCommandsController != null) statisticsCommandsController.setHttpClient(client);
        if (statisticsCommandsController != null) statisticsCommandsController.setBaseUrl(baseUrl);
    }

    public void initServerMode(String baseUrl) {
        this.baseUrl = baseUrl;
        Platform.runLater(this::safeInitAfterLoad);
    }

    private void safeInitAfterLoad() {
        if (connectedUsersController != null) {
            connectedUsersController.setBaseUrl(baseUrl);
            connectedUsersController.setDashboardController(this);
        }
        if (statisticsController != null)
            statisticsController.loadUserHistory(baseUrl, getCurrentUsername());
        if (statisticsCommandsController != null && statisticsController != null) {
            statisticsCommandsController.setStatisticsTableController(statisticsController);
            statisticsCommandsController.setDashboardController(this);
        }
        if (mainProgramsController != null) {
            mainProgramsController.setBaseUrl(baseUrl);
            mainProgramsController.setHttpClient(httpClient);
            mainProgramsController.refreshPrograms();
        }
        if (functionsController != null) {
            functionsController.setBaseUrl(baseUrl);
            functionsController.setHttpClient(httpClient);
            functionsController.refreshFunctions();
        }

        startEventListener();
        setupSelectionListener();
    }

    private void startEventListener() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String json = httpClient .get(baseUrl + "events/latest");
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
            System.out.println("[Dashboard] Received event: " + eventType);
            switch (eventType) {
                case "USER_LOGIN", "USER_LOGOUT" -> {
                    connectedUsersController.refreshUsers();
                    headerController.refreshUserHeader();
                }
                case "PROGRAM_UPLOADED" -> {
                    mainProgramsController.refreshPrograms();
                    functionsController.refreshFunctions();
                    statisticsController.loadUserHistory(baseUrl, getCurrentUsername());
                    connectedUsersController.refreshUsers();
                }
                case "PROGRAM_RUN" -> {
                    statisticsController.loadUserHistory(baseUrl, getCurrentUsername());
                    connectedUsersController.refreshUsers();
                }
                default -> refreshAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSelectionListener() {
        connectedUsersController.getUsersTable().getSelectionModel()
                .selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        lockedOnSelectedUser = true;
                        statisticsController.loadUserHistory(baseUrl, newSel.getUsername());
                    } else {
                        lockedOnSelectedUser = false;
                        statisticsController.loadUserHistory(baseUrl, getCurrentUsername());
                    }
                });

        mainProgramsController.getProgramsTable().getSelectionModel()
                .selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        functionsController.clearHighlights();
                        mainProgramsController.clearHighlights();
                        highlightFunctionsForProgram(newSel.getProgramName());
                    } else {
                        functionsController.clearHighlights();
                    }
                });

        functionsController.getFunctionsTable().getSelectionModel()
                .selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        mainProgramsController.clearHighlights();
                        functionsController.clearHighlights();
                        highlightProgramsForFunction(newSel.getFunctionName());
                        highlightRelatedFunctions(newSel.getFunctionName());
                    } else {
                        mainProgramsController.clearHighlights();
                        functionsController.clearHighlights();
                    }
                });
    }

    private void highlightFunctionsForProgram(String programName) {
        try {
            String encodedProgram = URLEncoder.encode(programName, StandardCharsets.UTF_8).replace("+", "%2B");
            String json = httpClient.get(baseUrl + "relations/functions?program=" + encodedProgram);
            Gson gson = new Gson();
            Map<String, Object> response = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

            if (!"success".equals(response.get("status"))) {
                System.err.println("Server returned error: " + response.get("message"));
                return;
            }

            List<String> funcs = (List<String>) response.get("functions");
            if (funcs == null) funcs = List.of();
            functionsController.highlightFunctions(new HashSet<>(funcs));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void highlightProgramsForFunction(String functionName) {
        try {
            String json = httpClient.get(baseUrl + "relations/programs?function=" +
                    URLEncoder.encode(functionName, StandardCharsets.UTF_8));

            Gson gson = new Gson();
            Map<String, Object> response = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

            if (!"success".equals(response.get("status"))) {
                System.err.println("Server returned error: " + response.get("message"));
                return;
            }

            List<String> programsList = (List<String>) response.get("programs");
            if (programsList == null) programsList = List.of();
            mainProgramsController.highlightPrograms(new HashSet<>(programsList));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void highlightRelatedFunctions(String functionName) {
        try {
            String json = httpClient .get(baseUrl + "relations/related-functions?function=" + URLEncoder.encode(functionName, StandardCharsets.UTF_8));
            Set<String> funcs = new Gson().fromJson(json, new TypeToken<Set<String>>(){}.getType());
            functionsController.highlightFunctions(funcs);
        } catch (Exception e) {}
    }

    public String getCurrentUsername() {
        try {
            String json = httpClient .get(baseUrl + "user/current");
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
        if (headerController != null) headerController.refreshUserHeader();
        if (connectedUsersController != null) connectedUsersController.refreshUsers();
        if (mainProgramsController != null) mainProgramsController.refreshPrograms();
        if (functionsController != null) functionsController.refreshFunctions();
    }

    public void openExecutionScreenWithRun(String program, int degree, String inputsCsv, String architecture) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Execution/mainExecution.fxml"));
            Parent root = loader.load();
            MainExecutionController execController = loader.getController();
            execController.setHttpClient(this.httpClient);
            execController.prepareForRerun(program, degree, inputsCsv, architecture);
            Stage stage = (Stage) statisticsController.getTableView().getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StatisticsTableController getStatisticsController() {
        return statisticsController;
    }
}
