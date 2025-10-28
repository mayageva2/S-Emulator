package MainProgramsTable;

import Main.Execution.MainExecutionController;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.Timer;

public class MainProgramsTableController {

    @FXML private TableView<ProgramRow> programsTable;
    @FXML private TableColumn<ProgramRow, String> colProgramName;
    @FXML private TableColumn<ProgramRow, String> colUsername;
    @FXML private TableColumn<ProgramRow, Integer> colInstructionCount;
    @FXML private TableColumn<ProgramRow, Integer> colMaxDegree;
    @FXML private TableColumn<ProgramRow, Integer> colRunCount;
    @FXML private TableColumn<ProgramRow, Double> colAvgCost;
    @FXML private Button btnExecuteProgram;

    private final Gson gson = new Gson();
    private String baseUrl = "http://localhost:8080/semulator/";
    private Timer refreshTimer;
    private Runnable onProgramSelected; // optional callback
    private HttpSessionClient httpClient = new HttpSessionClient();

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @FXML
    private void initialize() {
        colProgramName.setCellValueFactory(new PropertyValueFactory<>("programName"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colInstructionCount.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        colMaxDegree.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
        colRunCount.setCellValueFactory(new PropertyValueFactory<>("runCount"));
        colAvgCost.setCellValueFactory(new PropertyValueFactory<>("avgCreditCost"));

        btnExecuteProgram.setDisable(true);

        programsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            btnExecuteProgram.setDisable(newSel == null);
        });

        programsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (var col : programsTable.getColumns()) col.setSortable(false);
        programsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        programsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ProgramRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("highlighted");
                if (!empty && item != null && item.isHighlighted()) {
                    getStyleClass().add("highlighted");
                }
            }
        });

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            programsTable.getStylesheets().add(css.toExternalForm());
            programsTable.getStyleClass().add("instructions");
        }

        refreshPrograms();
    }

    public void refreshPrograms() {
        try {
            String json = httpClient.get(baseUrl + "programs/list");
            Map<String, Object> resp = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            if (!"success".equals(resp.get("status"))) return;

            List<Map<String, Object>> programs = (List<Map<String, Object>>) resp.get("programs");
            List<ProgramRow> rows = new ArrayList<>();
            for (Map<String, Object> p : programs) {
                rows.add(new ProgramRow(
                        (String) p.get("programName"),
                        (String) p.get("username"),
                        ((Number) p.get("instructionCount")).intValue(),
                        ((Number) p.get("maxDegree")).intValue(),
                        ((Number) p.get("runCount")).intValue(),
                        ((Number) p.get("avgCreditCost")).doubleValue()
                ));
            }

            programsTable.getItems().setAll(rows);
        } catch (Exception e) {
            System.err.println("Failed to refresh programs: " + e.getMessage());
        }
    }

    @FXML
    private void onExecuteProgramClicked() {
        try {
            ProgramRow selectedProgram = programsTable.getSelectionModel().getSelectedItem();
            if (selectedProgram == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Program Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a program to execute.");
                alert.showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Execution/mainExecution.fxml"));
            Parent root = loader.load();
            MainExecutionController controller = loader.getController();
            controller.setProgramToExecute(selectedProgram.getProgramName());

            Stage stage = (Stage) btnExecuteProgram.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

            double desiredWidth = stage.getWidth();
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load mainExecution.fxml: " + e.getMessage());
        }
    }

    public void setOnProgramSelected(Runnable onProgramSelected) {
        this.onProgramSelected = onProgramSelected;
    }

    public TableView<ProgramRow> getProgramsTable() {
        return programsTable;
    }

    public void highlightPrograms(Set<String> programNames) {
        var set = (programNames != null) ? programNames : Set.<String>of();
        for (ProgramRow row : programsTable.getItems()) {
            row.setHighlighted(set.contains(row.getProgramName()));
        }
        programsTable.refresh();
    }

    public void clearHighlights() {
        for (ProgramRow row : programsTable.getItems()) {
            row.setHighlighted(false);
        }
        programsTable.refresh();
    }
}
