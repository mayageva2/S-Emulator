package RunButtons;

import InputsBox.InputsBoxController;
import Main.Execution.MainExecutionController;
import Utils.HttpSessionClient;
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
import java.net.URLEncoder;
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
    private MainExecutionController mainExecutionController;

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
        disableAllRunButtons(true);
    }

    public void setVarsBoxController(VariablesBoxController c) { this.varsBoxController = c; }
    public void setInputsBoxController(InputsBoxController c) { this.inputsBoxController = c; }
    public void setStatisticsTableController(StatisticsTableController c) { this.statisticsTableController = c; }
    public void setInstructionsController(InstructionsTableController c) { this.instructionsController = c; }
    public void setProgramToolbarController(ProgramToolbarController c) {this.toolbarController = c;}

    public void setCurrentProgram(String name) { this.currentProgram = (name == null ? "" : name); }
    public void setCurrentDegree(int degree) { this.currentDegree = Math.max(0, degree); }

    @FXML
    private void onNewRun(ActionEvent e) {
        if (varsBoxController != null) varsBoxController.clearForNewRun();
        if (inputsBoxController != null) inputsBoxController.clearInputs();
    }

    @FXML
    private void onRun(ActionEvent e) {
        try {
            Long[] inputs = inputsBoxController.collectAsLongsOrThrow();
            String effectiveProgram = (currentProgram == null || currentProgram.isBlank() ||
                            currentProgram.equalsIgnoreCase("Main Program")) ? "" : currentProgram;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("program", effectiveProgram);
            data.put("degree", currentDegree);
            data.put("inputs", inputs);

            System.out.println("▶ Sending run request:");
            System.out.println("  program='" + currentProgram + "'");
            System.out.println("  effectiveProgram='" + effectiveProgram + "'");
            System.out.println("  degree=" + currentDegree);
            System.out.println("  inputs=" + Arrays.toString(inputs));

            String json = gson.toJson(data);
            String response = httpPostJson(BASE_URL + "run", json);

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
                if (mainExecutionController != null) {
                    var statsCmd = mainExecutionController.getStatisticsCommandsController();
                    if (statsCmd != null) {
                        Map<String, String> snapshot = new LinkedHashMap<>();
                        for (var es : varsMap.entrySet()) {
                            snapshot.put(es.getKey(), String.valueOf(es.getValue()));
                        }
                        statsCmd.setLastVarsSnapshot(snapshot);
                    }
                }
                varsBoxController.setCycles(cycles.intValue());
            }

            if (statisticsTableController != null)
                Platform.runLater(() -> statisticsTableController.clear());

            if (mainExecutionController != null) {
                mainExecutionController.refreshHistory();
            }

        } catch (Exception ex) {
            alertError("Run failed", ex.getMessage());
        }
    }


    @FXML
    private void onDebug(ActionEvent e) {
        try {
            Long[] inputs = inputsBoxController.collectAsLongsOrThrow();
            String effectiveProgram =
                    (currentProgram == null || currentProgram.isBlank() ||
                            currentProgram.equalsIgnoreCase("Main Program"))
                            ? "" : currentProgram;

            String formData = "program=" + URLEncoder.encode(effectiveProgram, StandardCharsets.UTF_8)
                    + "&degree=" + currentDegree
                    + "&inputs=" + Arrays.toString(inputs).replaceAll("[\\[\\]\\s]", "");
            String response = httpPost(BASE_URL + "debug/start", formData);
            Map<String, Object> json = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            if (!"success".equals(json.get("status"))) {
                throw new RuntimeException(String.valueOf(json.get("message")));
            }

            Map<String, Object> debug = (Map<String, Object>) json.get("debug");
            if (debug != null) {
                updateVarsFromDebug(debug);
                Number idx = (Number) debug.getOrDefault("pc", 0);
                boolean finished = Boolean.TRUE.equals(debug.get("finished"));

                Platform.runLater(() -> {
                    if (finished) {
                        instructionsController.clearHighlight();
                        disableDebugButtons(true);
                        if (mainExecutionController != null) {
                            mainExecutionController.refreshHistory();
                        }
                    } else {
                        instructionsController.highlightRow(idx.intValue());
                        disableDebugButtons(false);
                    }
                });
            }

        } catch (Exception ex) {
            alertError("Debug start failed", ex.getMessage());
        }
    }

    @FXML
    private void onStepOver(ActionEvent e) {handleDebugAction("debug/step", "Step failed");}

    @FXML
    private void onResume(ActionEvent e) {handleDebugAction("debug/resume", "Resume failed");}

    @FXML
    private void onStop(ActionEvent e) {
        handleDebugAction("debug/stop", "Stop failed");
    }

    private void handleDebugAction(String endpoint, String errorTitle) {
        try {
            String response = httpPost(BASE_URL + endpoint, "");
            Map<String, Object> result = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());

            String status = String.valueOf(result.getOrDefault("status", ""));
            boolean finished = Boolean.TRUE.equals(result.get("finished"));

            @SuppressWarnings("unchecked")
            Map<String, Object> debug = (Map<String, Object>) result.get("debug");

            if (debug != null) {
                updateVarsFromDebug(debug);
            }

            if (instructionsController == null) return;

            int pc = 0;
            Object pcObj =
                    (debug != null ? debug.get("pc") : null);
            if (!(pcObj instanceof Number)) {
                pcObj = result.get("pc");
            }
            if (pcObj instanceof Number n) {
                pc = n.intValue();
            }
            final int fPc = pc;

            Platform.runLater(() -> {
                switch (status) {
                    case "stopped" -> {
                        instructionsController.clearHighlight();
                        disableDebugButtons(true);
                        if (mainExecutionController != null) mainExecutionController.refreshHistory();
                    }
                    case "resumed" -> {
                        if (finished) {
                            instructionsController.clearHighlight();
                            disableDebugButtons(true);
                            if (mainExecutionController != null) mainExecutionController.refreshHistory();
                        } else {
                            instructionsController.highlightRow(fPc);
                            disableDebugButtons(false);
                        }
                    }
                    case "running" -> {
                        instructionsController.highlightRow(fPc);
                        disableDebugButtons(false);
                    }
                    case "success" -> {
                        if (finished) {
                            instructionsController.clearHighlight();
                            disableDebugButtons(true);
                            if (mainExecutionController != null) mainExecutionController.refreshHistory();
                        } else {
                            instructionsController.highlightRow(fPc);
                            disableDebugButtons(false);
                        }
                    }
                    case "error" -> {
                        alertError(errorTitle, String.valueOf(result.get("message")));
                    }
                    default -> {
                        if (finished) {
                            instructionsController.clearHighlight();
                            disableDebugButtons(true);
                            if (mainExecutionController != null) mainExecutionController.refreshHistory();
                        } else {
                            instructionsController.highlightRow(fPc);
                        }
                    }
                }
            });

        } catch (Exception ex) {
            alertError(errorTitle, ex.getMessage());
        }
    }

    private void updateVarsFromDebug(Map<String, Object> json) {
        if (json == null || varsBoxController == null) return;
        Map<String, Object> vars = (Map<String, Object>) json.get("vars");
        Number cycles = (Number) json.getOrDefault("cycles", 0);
        Platform.runLater(() -> {
            varsBoxController.renderAll(vars);
            varsBoxController.setCycles(cycles.intValue());
            if (mainExecutionController != null) {
                var statsCmd = mainExecutionController.getStatisticsCommandsController();
                if (statsCmd != null && vars != null) {
                    Map<String, String> snapshot = new LinkedHashMap<>();
                    for (var e : vars.entrySet()) {
                        snapshot.put(e.getKey(), String.valueOf(e.getValue()));
                    }
                    statsCmd.setLastVarsSnapshot(snapshot);
                }
            }
        });
    }

    private String httpPost(String urlStr, String body) throws Exception {
        return HttpSessionClient.post(urlStr, body, "application/x-www-form-urlencoded; charset=UTF-8");
    }

    private void alertError(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(title);
            a.showAndWait();
        });
    }

    private void disableDebugButtons(boolean disable) {
        Platform.runLater(() -> {
            btnStop.setDisable(disable);
            btnResume.setDisable(disable);
            btnStepOver.setDisable(disable);
        });
    }

    public void disableAllRunButtons(boolean disable) {
        Platform.runLater(() -> {
            btnNewRun.setDisable(disable);
            btnRun.setDisable(disable);
            btnDebug.setDisable(disable);
            btnStop.setDisable(true);
            btnResume.setDisable(true);
            btnStepOver.setDisable(true);
        });
    }

    public void enableRunButtonsAfterLoad() {
        Platform.runLater(() -> {
            btnNewRun.setDisable(false);
            btnRun.setDisable(false);
            btnDebug.setDisable(false);
            disableDebugButtons(true);
        });
    }

    private String httpPostJson(String urlStr, String json) throws Exception {
        return HttpSessionClient.post(urlStr, json, "application/json; charset=UTF-8");
    }

    public void setMainController(MainExecutionController mainExecutionController) {
        this.mainExecutionController = mainExecutionController;
    }
}