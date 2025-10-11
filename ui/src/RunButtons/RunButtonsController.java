package RunButtons;

import InputsBox.InputsBoxController;
import VariablesBox.VariablesBoxController;
import StatisticsTable.StatisticsTableController;
import ProgramToolBar.ProgramToolbarController;
import InstructionsTable.InstructionsTableController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RunButtonsController {
    @FXML private Button btnNewRun, btnRun, btnDebug, btnStop, btnResume, btnStepOver;
    @FXML private HBox runButtonsHBox;

    private VariablesBoxController varsBoxController;
    private InputsBoxController inputsBoxController;
    private StatisticsTableController statisticsTableController;
    private ProgramToolbarController toolbarController;
    private InstructionsTableController instructionsController;

    private int currentDegree = 0;
    private String currentProgram = "";
    private static final String BASE_URL = "http://localhost:8080/semulator/";
    private static final Gson gson = new Gson();

    private DropShadow glow;

    @FXML
    private void initialize() {
        glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setColor(Color.web("#b46ad4"));
        glow.setRadius(25);
        glow.setSpread(0.5);

        btnNewRun.setTooltip(new Tooltip("New Run: clear state"));
        btnRun.setTooltip(new Tooltip("Run program"));
        btnDebug.setTooltip(new Tooltip("Start debug session"));
        btnStop.setTooltip(new Tooltip("Stop debug session"));
        btnResume.setTooltip(new Tooltip("Resume debug session"));
        btnStepOver.setTooltip(new Tooltip("Step over instruction"));

        btnRun.setEffect(glow);
        btnDebug.setEffect(glow);
    }

    public void setVarsBoxController(VariablesBoxController c) { this.varsBoxController = c; }
    public void setInputsBoxController(InputsBoxController c) { this.inputsBoxController = c; }
    public void setStatisticsTableController(StatisticsTableController c) { this.statisticsTableController = c; }
    public void setProgramToolbarController(ProgramToolbarController c) { this.toolbarController = c; }
    public void setInstructionsController(InstructionsTableController c) { this.instructionsController = c; }

    public void setCurrentProgram(String name) { this.currentProgram = (name == null ? "" : name); }
    public void setCurrentDegree(int degree) { this.currentDegree = Math.max(0, degree); }

    @FXML
    private void onNewRun(ActionEvent e) {
        if (varsBoxController != null) varsBoxController.clearForNewRun();
        if (statisticsTableController != null) statisticsTableController.clear();
        if (inputsBoxController != null) inputsBoxController.clearInputs();
    }

    @FXML
    private void onRun(ActionEvent e) {
        try {
            Long[] inputs = inputsBoxController.collectAsLongsOrThrow();
            Map<String, Object> payload = Map.of(
                    "program", currentProgram,
                    "degree", currentDegree,
                    "inputs", inputs
            );
            String response = httpPost(BASE_URL + "run", gson.toJson(payload));
            Map<String, Object> result = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());

            Map<String, Object> vars = (Map<String, Object>) result.get("vars");
            Number cycles = (Number) result.getOrDefault("cycles", 0);

            if (varsBoxController != null) {
                varsBoxController.renderAll(vars);
                varsBoxController.setCycles(cycles.intValue());
            }

            if (statisticsTableController != null)
                Platform.runLater(() -> statisticsTableController.clear()); // refill later from server history

        } catch (Exception ex) {
            alertError("Run failed", ex.getMessage());
        }
    }

    @FXML
    private void onDebug(ActionEvent e) {
        try {
            Long[] inputs = inputsBoxController.collectAsLongsOrThrow();
            Map<String, Object> payload = Map.of(
                    "program", currentProgram,
                    "degree", currentDegree,
                    "inputs", inputs
            );
            httpPost(BASE_URL + "debug/start", gson.toJson(payload));
            alertInfo("Debug started", "Debug session started successfully.");
        } catch (Exception ex) {
            alertError("Debug start failed", ex.getMessage());
        }
    }

    @FXML
    private void onStepOver(ActionEvent e) {
        try {
            String response = httpGet(BASE_URL + "debug/step");
            Map<String, Object> result = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            updateVarsFromDebug(result);
        } catch (Exception ex) {
            alertError("Step failed", ex.getMessage());
        }
    }

    @FXML
    private void onResume(ActionEvent e) {
        try {
            String response = httpGet(BASE_URL + "debug/resume");
            Map<String, Object> result = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            updateVarsFromDebug(result);
        } catch (Exception ex) {
            alertError("Resume failed", ex.getMessage());
        }
    }

    @FXML
    private void onStop(ActionEvent e) {
        try {
            String response = httpGet(BASE_URL + "debug/stop");
            Map<String, Object> result = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            updateVarsFromDebug(result);
        } catch (Exception ex) {
            alertError("Stop failed", ex.getMessage());
        }
    }

    private void updateVarsFromDebug(Map<String, Object> json) {
        if (json == null || varsBoxController == null) return;
        Map<String, Object> vars = (Map<String, Object>) json.get("vars");
        Number cycles = (Number) json.getOrDefault("cycles", 0);
        Platform.runLater(() -> {
            varsBoxController.renderAll(vars);
            varsBoxController.setCycles(cycles.intValue());
        });
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

    private void alertError(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(title);
            a.showAndWait();
        });
    }

    private void alertInfo(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(title);
            a.showAndWait();
        });
    }
}
