package FunctionsTable;

import Main.Execution.MainExecutionController;
import Utils.ClientContext;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FunctionsTableController {

    @FXML private TableView<FunctionRow> functionsTable;
    @FXML private TableColumn<FunctionRow, String> colFunction;
    @FXML private TableColumn<FunctionRow, String> colProgram;
    @FXML private TableColumn<FunctionRow, String> colUser;
    @FXML private TableColumn<FunctionRow, Integer> colInstructions;
    @FXML private TableColumn<FunctionRow, Integer> colMaxDegree;
    @FXML private Button btnExecuteFunc;

    private final Gson gson = new Gson();
    private HttpSessionClient httpClient;
    private String baseUrl;

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @FXML
    private void initialize() {
        this.httpClient = ClientContext.getHttpClient();
        this.baseUrl = ClientContext.getBaseUrl();

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

        functionsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(FunctionRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("highlighted");
                if (!empty && item != null && item.isHighlighted()) {
                    getStyleClass().add("highlighted");
                }
            }
        });

        refreshFunctions();
    }

    public void refreshFunctions() {
        new Thread(() -> {
            try {
                String json = httpClient.get(baseUrl + "functions");
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

                javafx.application.Platform.runLater(() -> {
                    functionsTable.getItems().setAll(rows);
                });

            } catch (Exception e) {
                System.err.println("Failed to refresh functions: " + e.getMessage());
            }
        }).start();
    }

    public void highlightFunctions(Set<String> functionNames) {
        var set = (functionNames != null) ? functionNames : Set.<String>of();
        for (FunctionRow row : functionsTable.getItems()) {
            row.setHighlighted(set.contains(row.getFunctionName()));
        }
        functionsTable.refresh();
    }

    public void clearHighlights() {
        for (FunctionRow row : functionsTable.getItems()) row.setHighlighted(false);
        functionsTable.refresh();
    }

    public TableView<FunctionRow> getFunctionsTable() { return functionsTable; }

    @FXML
    private void onExecuteFuncClicked() {
        try {
            FunctionRow selected = functionsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a function to execute.").showAndWait();
                return;
            }

            if (!loadProgramXML(selected.getProgramName())) {
                return; // failed to load, cannot execute
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/Execution/mainExecution.fxml"));
            Parent root = loader.load();
            MainExecutionController controller = loader.getController();

            controller.setHttpClient(httpClient);
            controller.setBaseUrl(baseUrl);

            controller.setProgram(selected.getProgramName());
            controller.setSelectedFunction(selected.getFunctionName());
            controller.loadProgramAndSelectFunction();

            Stage stage = (Stage) btnExecuteFunc.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load mainExecution.fxml: " + e.getMessage());
        }
    }

    private boolean loadProgramXML(String programName) {
        try {
            //Download XML
            byte[] xmlBytes = httpClient.getBytes(baseUrl + "programs/download?name=" + URLEncoder.encode(programName, StandardCharsets.UTF_8));

            //Upload to
            httpClient.postFile(baseUrl + "load", "file", programName + ".xml", xmlBytes);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load program '" + programName + "':\n" + e.getMessage())
                    .showAndWait();
            return false;
        }
    }

}
