package MainProgramsTable;

import Main.Execution.MainExecutionController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;

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

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            programsTable.getStylesheets().add(css.toExternalForm());
            programsTable.getStyleClass().add("instructions");
        }

        refreshPrograms();
        startAutoRefresh();
    }

    public void startAutoRefresh() {
        if (refreshTimer != null) refreshTimer.cancel();

        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(MainProgramsTableController.this::refreshPrograms);
            }
        }, 0, 5000); // every 5 seconds
    }

    public void refreshPrograms() {
        try {
            URL url = new URL(baseUrl + "programs/list");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) return;

            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Execution/mainExecution.fxml"));
            Parent root = loader.load();
            MainExecutionController controller = loader.getController();

            Stage stage = (Stage) btnExecuteProgram.getScene().getWindow();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            Scene scene = new Scene(root);
            stage.setScene(scene);
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
}
