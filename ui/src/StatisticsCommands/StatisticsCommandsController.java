package StatisticsCommands;

import Main.Dashboard.mainDashboardController;
import Main.Execution.MainExecutionController;
import ProgramToolBar.ProgramToolbarController;
import StatisticsTable.StatisticsTableController;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StatisticsCommandsController {

    @FXML private Button showButton, rerunButton;

    private static final String BASE_URL = "http://localhost:8080/semulator/";
    private static final Gson gson = new Gson();
    private Map<String, String> lastVarsSnapshot = Map.of();
    private HttpSessionClient httpClient = new HttpSessionClient();

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    private ProgramToolbarController toolbarController;
    private MainExecutionController mainExecutionController;
    private StatisticsTableController statisticsTableController;
    private mainDashboardController dashboardController;

    public void setToolbarController(ProgramToolbarController c) { this.toolbarController = c; }
    public void setMainController(MainExecutionController c) { this.mainExecutionController = c; }
    public void setStatisticsTableController(StatisticsTableController c) { this.statisticsTableController = c; }
    public void setDashboardController(mainDashboardController c) { this.dashboardController = c; }

    @FXML
    private void initialize() {
        if (showButton != null)
            showButton.setOnAction(e -> onShowStatus());
        if (rerunButton != null)
            rerunButton.setOnAction(e -> onReRun());
    }

    public void setLastVarsSnapshot(Map<String, String> vars) {
        this.lastVarsSnapshot = (vars != null) ? vars : Map.of();
    }

    private void onShowStatus() {
        try {
            if (statisticsTableController == null) {
                new Alert(Alert.AlertType.ERROR, "Statistics table is not connected.").showAndWait();
                return;
            }

            var optRec = statisticsTableController.getSelectedRunRecord();
            if (optRec.isEmpty()) {
                if (lastVarsSnapshot == null || lastVarsSnapshot.isEmpty()) {
                    new Alert(Alert.AlertType.INFORMATION, "No variable data available yet.").showAndWait();
                    return;
                }
                showVarsPopup(lastVarsSnapshot);
                return;
            }

            var rec = optRec.get();
            String url = BASE_URL + "user/run/status?username=" +
                    URLEncoder.encode(rec.username(), StandardCharsets.UTF_8) +
                    "&runNumber=" + rec.runNumber();

            String json = httpClient.get(url);
            Map<String, Object> result = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

            if (!"success".equals(result.get("status"))) {
                new Alert(Alert.AlertType.ERROR, "Failed to get run status:\n" + result.get("message")).showAndWait();
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> vars = (Map<String, Object>) result.get("vars");
            if (vars == null || vars.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No variable data found for this run.").showAndWait();
                return;
            }

            Map<String, String> snapshot = new LinkedHashMap<>();
            for (var e : vars.entrySet()) {
                Object val = e.getValue();
                if (val instanceof Double d && d == Math.floor(d)) {
                    snapshot.put(e.getKey(), String.valueOf(d.intValue()));
                } else {
                    snapshot.put(e.getKey(), String.valueOf(val));
                }
            }

            setLastVarsSnapshot(snapshot);
            showVarsPopup(snapshot);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Show Status failed:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void onReRun() {
        try {
            if (statisticsTableController == null) {
                new Alert(Alert.AlertType.ERROR, "Statistics table is not connected.").showAndWait();
                return;
            }

            var optRec = statisticsTableController.getSelectedRunRecord();
            if (optRec.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please select a row in the statistics table to re-run.").showAndWait();
                return;
            }

            var rec = optRec.get();
            String csvInputs = String.join(",", rec.inputs().stream().map(String::valueOf).toList());

            if (dashboardController != null) {
                dashboardController.openExecutionScreenWithRun(rec.programName(), rec.degree(), csvInputs, rec.architecture());
            }

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Re-run failed:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void showVarsPopup(Map<String, String> vars) {
        TableView<Map.Entry<String, String>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Map.Entry<String, String>, String> cName = new TableColumn<>("Variable");
        cName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getKey()));
        TableColumn<Map.Entry<String, String>, String> cVal = new TableColumn<>("Value");
        cVal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getValue()));

        table.getColumns().setAll(cName, cVal);
        table.setItems(FXCollections.observableArrayList(vars.entrySet()));

        Label title = new Label("Program status");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 10 10 6 10;");

        VBox root = new VBox(6, title, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setStyle("-fx-padding: 12;");

        Stage dlg = new Stage();
        dlg.initModality(Modality.NONE);
        dlg.setTitle("SHOW STATUS");
        dlg.setScene(new Scene(new BorderPane(root), 420, 480));
        dlg.show();
    }
}
