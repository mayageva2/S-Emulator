package StatisticsCommands;

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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StatisticsCommandsController {
    @FXML private Button showButton, rerunButton;

    private static final String BASE_URL = "http://localhost:8080/semulator/";
    private static final Gson gson = new Gson();

    private Map<String, String> lastVarsSnapshot = Map.of();

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
        if (lastVarsSnapshot == null || lastVarsSnapshot.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "No snapshot from last run yet.");
            a.setHeaderText("SHOW STATUS");
            a.showAndWait();
            return;
        }
        showVarsPopup(lastVarsSnapshot);
    }

    private void onReRun() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Re-Run Program");
        dlg.setHeaderText("Enter program name to re-run:");
        dlg.setContentText("Program:");
        Optional<String> ans = dlg.showAndWait();
        if (ans.isEmpty() || ans.get().isBlank()) return;

        String prog = ans.get().trim();
        try {
            Map<String, Object> payload = Map.of("program", prog);
            String json = gson.toJson(payload);
            String resp = httpPost(BASE_URL + "rerun", json);
            Map<String, Object> response = gson.fromJson(resp, new TypeToken<Map<String, Object>>(){}.getType());

            Map<String, Object> vars = (Map<String, Object>) response.get("vars");
            Number cycles = (Number) response.getOrDefault("cycles", 0);
            Alert done = new Alert(Alert.AlertType.INFORMATION,
                    "Re-run completed.\nCycles: " + cycles + "\nVars: " + vars);
            done.setHeaderText("RE-RUN SUCCESS");
            done.showAndWait();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Re-run failed:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void showVarsPopup(Map<String, String> vars) {
        TableView<Map.Entry<String,String>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Map.Entry<String,String>, String> cName = new TableColumn<>("Variable");
        cName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getKey()));
        TableColumn<Map.Entry<String,String>, String> cVal  = new TableColumn<>("Value");
        cVal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getValue()));
        table.getColumns().setAll(cName, cVal);

        List<Map.Entry<String,String>> rows = new ArrayList<>(vars.entrySet());
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
