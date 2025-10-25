package FunctionsTable;

import Main.Execution.MainExecutionController;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Timer;

public class FunctionsTableController {

    @FXML private TableView<FunctionRow> functionsTable;
    @FXML private TableColumn<FunctionRow, String> colFunction;
    @FXML private TableColumn<FunctionRow, String> colProgram;
    @FXML private TableColumn<FunctionRow, String> colUser;
    @FXML private TableColumn<FunctionRow, Integer> colInstructions;
    @FXML private TableColumn<FunctionRow, Integer> colMaxDegree;
    @FXML private Button btnExecuteFunc;

    private final Gson gson = new Gson();
    private String baseUrl = "http://localhost:8080/semulator/";
    public void setBaseUrl(String baseUrl) {this.baseUrl = baseUrl;}

    public void refreshFunctions() {
        try {
            String json = HttpSessionClient.get(baseUrl + "functions");
            Map<String, Object> resp = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            if (!"success".equals(resp.get("status"))) return;

            List<Map<String, Object>> funcs = (List<Map<String, Object>>) resp.get("functions");
            List<FunctionRow> rows = new ArrayList<>();
            for (Map<String, Object> f : funcs) {
                rows.add(new FunctionRow(
                        (String) f.get("functionName"),
                        (String) f.get("programName"),
                        (String) f.get("username"),
                        ((Number) f.get("instructionCount")).intValue(),
                        ((Number) f.get("maxDegree")).intValue()
                ));
            }
            functionsTable.getItems().setAll(rows);

        } catch (Exception e) {
            System.err.println("Failed to refresh functions: " + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        colFunction.setCellValueFactory(new PropertyValueFactory<>("functionName"));
        colProgram.setCellValueFactory(new PropertyValueFactory<>("programName"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colInstructions.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        colMaxDegree.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));

        btnExecuteFunc.setDisable(true);
        functionsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> btnExecuteFunc.setDisable(newSel == null));

        functionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (var col : functionsTable.getColumns()) col.setSortable(false);
        functionsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            functionsTable.getStylesheets().add(css.toExternalForm());
            functionsTable.getStyleClass().add("instructions");
        }

        refreshFunctions();
    }

    @FXML
    private void onExecuteFuncClicked() {
        try {
            FunctionRow selected = functionsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a function to execute.").showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Execution/mainExecution.fxml"));
            Parent root = loader.load();
            MainExecutionController controller = loader.getController();
            controller.setProgramToExecute(selected.getFunctionName());

            Stage stage = (Stage) btnExecuteFunc.getScene().getWindow();
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();

            double desiredWidth = Math.min(stage.getWidth(), bounds.getWidth() - 20);
            double desiredHeight = Math.min(stage.getHeight(), bounds.getHeight() - 20);

            Scene scene = new Scene(root, desiredWidth, desiredHeight);
            stage.setScene(scene);
            stage.setX(bounds.getMinX() + (bounds.getWidth() - desiredWidth) / 2);
            stage.setY(bounds.getMinY() + (bounds.getHeight() - desiredHeight) / 2);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load mainExecution.fxml: " + e.getMessage());
        }
    }
}
