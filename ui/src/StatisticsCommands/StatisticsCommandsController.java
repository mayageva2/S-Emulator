package StatisticsCommands;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StatisticsCommandsController {
    @FXML private Button showButton, rerunButton;

    private static final String BASE_URL = "http://localhost:8080/semulator/";
    private static final Gson gson = new Gson();

    private Map<String, String> lastVarsSnapshot = Map.of();
    private ProgramToolBar.ProgramToolbarController toolbarController;
    private Main.MainController mainController;
    private StatisticsTable.StatisticsTableController statisticsTableController;

    public void setToolbarController(ProgramToolBar.ProgramToolbarController c) {
        this.toolbarController = c;
    }
    public void setMainController(Main.MainController c) {
        this.mainController = c;
    }
    public void setStatisticsTableController(StatisticsTable.StatisticsTableController c) {
        this.statisticsTableController = c;
    }

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
            if (statisticsTableController != null) {
                var optRec = statisticsTableController.getSelectedRunRecord();
                if (optRec.isPresent()) {
                    var rec = optRec.get();

                    if (toolbarController != null) {
                        toolbarController.setSelectedProgram(rec.programName());
                    }

                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("program", rec.programName());
                    data.put("degree", rec.degree());
                    data.put("inputs", rec.inputs());

                    String json = gson.toJson(data);
                    String resp = httpPost(BASE_URL + "run", json);

                    Map<String, Object> outer = gson.fromJson(resp, new TypeToken<Map<String, Object>>(){}.getType());
                    if (!"success".equals(outer.get("status"))) {
                        throw new RuntimeException("Run failed: " + outer.get("message"));
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) outer.get("result");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> varsList = (List<Map<String, Object>>) result.get("vars");

                    Map<String, String> snapshot = new LinkedHashMap<>();
                    if (varsList != null) {
                        for (Map<String, Object> v : varsList) {
                            Object name = v.get("name");
                            Object value = v.get("value");
                            if (name != null) snapshot.put(name.toString(), String.valueOf(value));
                        }
                    }

                    if (snapshot.isEmpty()) {
                        new Alert(Alert.AlertType.INFORMATION, "No variables to show for this run.").showAndWait();
                    } else {
                        showVarsPopup(snapshot);
                    }
                    return;
                }
            }

            if (lastVarsSnapshot == null || lastVarsSnapshot.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "No snapshot from last run yet.");
                a.setHeaderText("SHOW STATUS");
                a.showAndWait();
                return;
            }
            showVarsPopup(lastVarsSnapshot);

        } catch (Exception ex) {
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

            if (toolbarController != null)
                toolbarController.setSelectedProgram(rec.programName());

            String csvInputs = String.join(",", rec.inputs().stream().map(String::valueOf).toList());
            String formData = "program=" + URLEncoder.encode(rec.programName(), StandardCharsets.UTF_8)
                    + "&degree=" + rec.degree()
                    + "&inputs=" + URLEncoder.encode(csvInputs, StandardCharsets.UTF_8);

            String resp = mainController.httpPostFormPublic("http://localhost:8080/semulator/run", formData);

            Map<String, Object> outer = new Gson().fromJson(resp, new TypeToken<Map<String, Object>>(){}.getType());
            if (!"success".equals(outer.get("status"))) {
                throw new RuntimeException("Run failed: " + outer.get("message"));
            }

            Map<String, Object> result = (Map<String, Object>) outer.get("result");
            List<Map<String, Object>> varsList = (List<Map<String, Object>>) result.get("vars");
            Number cycles = (Number) result.getOrDefault("cycles", 0);

            Map<String, Object> varsMap = new LinkedHashMap<>();
            if (varsList != null) {
                for (Map<String, Object> v : varsList) {
                    Object name = v.get("name");
                    Object value = v.get("value");
                    if (name == null) continue;

                    String displayValue;
                    if (value instanceof Number num) {
                        double d = num.doubleValue();
                        if (Math.floor(d) == d) {
                            displayValue = String.valueOf((long) d);
                        } else {
                            displayValue = String.valueOf(d);
                        }
                    } else {
                        displayValue = String.valueOf(value);
                    }
                    varsMap.put(name.toString(), displayValue);
                }
            }

            if (mainController != null) {
                var varsBox = mainController.getVarsBoxController();
                if (varsBox != null) {
                    Platform.runLater(() -> {
                        varsBox.renderAll(varsMap);
                        varsBox.setCycles(cycles.intValue());
                    });
                }

                Platform.runLater(() -> {
                    mainController.refreshHistory();
                    mainController.refreshProgramViewPublic(rec.degree());
                });
            }

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Re-run failed:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void showVarsPopup(Map<String, String> vars) {
        Map<String, String> fixedVars = new LinkedHashMap<>();
        for (var entry : vars.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (val != null && val.matches("-?\\d+\\.0+")) {
                val = val.substring(0, val.indexOf('.'));
            }
            fixedVars.put(key, val);
        }

        TableView<Map.Entry<String,String>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Map.Entry<String,String>, String> cName = new TableColumn<>("Variable");
        cName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getKey()));
        TableColumn<Map.Entry<String,String>, String> cVal  = new TableColumn<>("Value");
        cVal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getValue()));
        table.getColumns().setAll(cName, cVal);

        List<Map.Entry<String,String>> rows = new ArrayList<>(fixedVars.entrySet());
        rows.sort((a,b) -> {
            int ra = rank(a.getKey()), rb = rank(b.getKey());
            if (ra != rb) return Integer.compare(ra, rb);
            if (ra == 1 || ra == 2) return Integer.compare(numSuffix(a.getKey()), numSuffix(b.getKey()));
            return a.getKey().compareToIgnoreCase(b.getKey());
        });
        table.setItems(FXCollections.observableArrayList(rows));

        Label title = new Label("Program status (end of selected/last run)");
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

    private static int rank(String k) {
        String s = (k == null) ? "" : k.trim().toLowerCase(Locale.ROOT);
        if (s.equals("y")) return 0;
        if (s.startsWith("x")) return 1;
        if (s.startsWith("z")) return 2;
        return 3;
    }

    private static int numSuffix(String k) {
        String s = (k == null) ? "" : k.trim().toLowerCase(Locale.ROOT);
        int i = 0; while (i < s.length() && !Character.isDigit(s.charAt(i))) i++;
        if (i >= s.length()) return Integer.MAX_VALUE;
        try { return Integer.parseInt(s.substring(i)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    // == HTTP helpers == //
    private String httpPost(String urlStr, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();
        conn.disconnect();
        return sb.toString();
    }
}
