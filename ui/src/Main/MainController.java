package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import ProgramToolBar.ProgramToolbarController;
import InstructionsTable.InstructionsTableController;
import SummaryLine.SummaryLineController;
import VariablesBox.VariablesBoxController;
import InputsBox.InputsBoxController;
import StatisticsTable.StatisticsTableController;
import StatisticsCommands.StatisticsCommandsController;
import SelectedInstructionHistoryChainTable.SelectedInstructionHistoryChainTableController;
import RunButtons.RunButtonsController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class MainController {
    @FXML private HeaderAndLoadButtonController headerController;
    @FXML private ProgramToolbarController toolbarController;
    @FXML private InstructionsTableController instructionsController;
    @FXML private SummaryLineController summaryLineController;
    @FXML private SelectedInstructionHistoryChainTableController historyChainController;
    @FXML private RunButtonsController runButtonsController;
    @FXML private VariablesBoxController varsBoxController;
    @FXML private InputsBoxController inputsBoxController;
    @FXML private StatisticsTableController statisticsTableController;
    @FXML private StatisticsCommandsController statisticsCommandsController;
    @FXML private VBox contentBox;
    @FXML private VBox historyChainBox;
    @FXML private VBox statisticsBox;
    @FXML private VBox leftCol;
    @FXML private VBox rightCol;
    @FXML private HBox sidePanels;
    @FXML private BorderPane varsBox;
    @FXML private BorderPane inputsBox;
    @FXML private Node toolbar;
    @FXML private Region instructions;
    @FXML private Region summaryLine;
    @FXML private Region historyChain;
    @FXML private Region runButtons;
    @FXML private TextArea centerOutput;

    private static final String BASE_URL = "http://localhost:8080/semulator/";
    private static final Gson gson = new Gson();
    private String currentProgram = null;
    private int currentDegree = 0;
    private int maxDegree = 0;
    private Consumer<String> onHighlightChanged;

    @FXML
    private void initialize() {
        headerController.setOnLoaded(this::onProgramLoaded);
        toolbarController.setOnExpand(this::onExpandOne);
        toolbarController.setOnCollapse(this::onCollapseOne);
        toolbarController.setOnJumpToDegree(this::onJumpToDegree);
        toolbarController.bindDegree(0, 0);
        toolbarController.setHighlightEnabled(false);
        toolbarController.setDegreeButtonEnabled(false);
        toolbarController.setOnProgramSelected(this::onProgramPicked);
        toolbarController.setOnHighlightChanged(term -> {
            if (instructionsController != null) instructionsController.setHighlightTerm(term);
        });
        Platform.runLater(() -> {
            HBox.setHgrow(contentBox, Priority.ALWAYS);
            HBox.setHgrow(sidePanels, Priority.ALWAYS);
            varsBox.prefWidthProperty().bind(sidePanels.widthProperty().divide(2));
            inputsBox.prefWidthProperty().bind(sidePanels.widthProperty().divide(2));
        });
        runButtonsController.setStatisticsTableController(statisticsTableController);
        runButtonsController.setInputsBoxController(inputsBoxController);
        runButtonsController.setVarsBoxController(varsBoxController);
        runButtonsController.setProgramToolbarController(toolbarController);
    }

    private void onProgramLoaded(HeaderAndLoadButtonController.LoadedEvent ev) {
        try {
            String response = httpPost(BASE_URL + "LoadServlet", "{\"path\":\"" + ev.xmlPath().toString() + "\"}");
            Map<String, Object> map = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            if (map.containsKey("programName")) currentProgram = String.valueOf(map.get("programName"));
            refreshProgramView(0);
        } catch (Exception e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    private void refreshProgramView(int degree) {
        try {
            String url = BASE_URL + "ProgramViewServlet?degree=" + degree;
            if (currentProgram != null) url += "&program=" + currentProgram;
            String response = httpGet(url);
            Map<String, Object> map = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            List<Map<String, Object>> instructionsList = (List<Map<String, Object>>) map.get("instructions");
            if (instructionsController != null) instructionsController.renderFromJson(instructionsList);
            summaryLineController.refreshFromServer(null);
        } catch (Exception e) {
            showError("Render failed: " + e.getMessage());
        }
    }

    private void onExpandOne() {
        if (currentDegree < maxDegree) {
            currentDegree++;
            toolbarController.bindDegree(currentDegree, maxDegree);
            refreshProgramView(currentDegree);
        }
    }

    private void onCollapseOne() {
        if (currentDegree > 0) {
            currentDegree--;
            toolbarController.bindDegree(currentDegree, maxDegree);
            refreshProgramView(currentDegree);
        }
    }

    private void onJumpToDegree(Integer target) {
        if (target == null) return;
        currentDegree = Math.max(0, target);
        toolbarController.bindDegree(currentDegree, maxDegree);
        refreshProgramView(currentDegree);
    }

    private void onProgramPicked(String program) {
        currentProgram = program;
        currentDegree = 0;
        refreshProgramView(currentDegree);
    }

    public void runProgram() {
        try {
            Long[] inputs = inputsBoxController.collectAsLongsOrThrow();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("program", currentProgram);
            payload.put("degree", currentDegree);
            payload.put("inputs", inputs);
            String response = httpPost(BASE_URL + "RunServlet", gson.toJson(payload));
            Map<String, Object> map = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            varsBoxController.renderAll((Map<String, Object>) map.get("vars"));
            varsBoxController.setCycles(((Double) map.get("cycles")).intValue());
            statisticsTableController.clear();
        } catch (Exception e) {
            showError("Run failed: " + e.getMessage());
        }
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();
        conn.disconnect();
        return sb.toString();
    }

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

    private void showError(String msg) {
        Platform.runLater(() -> {
            if (centerOutput != null) centerOutput.setText(msg);
        });
    }
}
