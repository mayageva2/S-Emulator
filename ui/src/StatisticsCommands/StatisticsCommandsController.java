package StatisticsCommands;

import emulator.api.dto.RunRecord;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Supplier;

public class StatisticsCommandsController {
    @FXML private Button showButton, rerunButton;

    private Supplier<Map<String, String>> statusSupplier;

    public void setStatusSupplier(Supplier<Map<String, String>> supplier) {
        this.statusSupplier = supplier;
    }

    @FXML
    private void initialize() {
        if (showButton != null) {
            showButton.setOnAction(e -> onShowStatus());
        }
    }

    private void onShowStatus() {
        Map<String, String> vars = (statusSupplier != null) ? statusSupplier.get() : Collections.emptyMap();
        if (vars == null || vars.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "No snapshot from last run yet.");
            a.setHeaderText("SHOW STATUS");
            a.showAndWait();
            return;
        }
        showVarsPopup(vars);
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
}
