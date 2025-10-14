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

        btnNewRun.setTooltip(new Tooltip("New Run"));
        btnRun.setTooltip(new Tooltip("Run program"));
        btnDebug.setTooltip(new Tooltip("Start debug"));
        btnStop.setTooltip(new Tooltip("Stop debug"));
        btnResume.setTooltip(new Tooltip("Resume debug"));
        btnStepOver.setTooltip(new Tooltip("Step over"));

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

            String formData = "degree=" + currentDegree +
                    "&inputs=" + Arrays.toString(inputs).replaceAll("[\\[\\]\\s]", "");

            String response = httpPost(BASE_URL + "run", formData);
            Map<String, Object> outer = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());

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
                    if (name != null) varsMap.put(name.toString(), value);
                }
            }

            if (varsBoxController != null) {
                varsBoxController.renderAll(varsMap);
                varsBoxController.setCycles(cycles.intValue());
            }

            if (statisticsTableController != null)
                Platform.runLater(() -> statisticsTableController.clear());

        } catch (Exception ex) {
            alertError("Run failed", ex.getMessage());
        }
    }

    @FXML
    private void onDebug(ActionEvent e) {
        try {
            Long[] inputs = inputsBoxController.collectAsLongsOrThrow();
            String formData = "degree=" + currentDegree +
                    "&inputs=" + Arrays.toString(inputs).replaceAll("[\\[\\]\\s]", "");
            httpPost(BASE_URL + "debug/start", formData);
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

    private String httpPost(String urlStr, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
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
