package RunButtons;

import ArchitectureChoiceBox.ArchitectureChoiceBoxController;
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
    private ArchitectureChoiceBoxController architectureController;
    private StatisticsCommands.StatisticsCommandsController statisticsCommandsController;

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
    public void setArchitectureController(ArchitectureChoiceBoxController c) { this.architectureController = c; }
    public void setStatisticsCommandsController(StatisticsCommands.StatisticsCommandsController c) { this.statisticsCommandsController = c; }

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

            String architecture = (architectureController != null &&
                    architectureController.getSelectedArchitecture() != null)
                    ? architectureController.getSelectedArchitecture().name() : null;

            if (architecture == null || architecture.isBlank()) {
                alertError("Missing architecture", "Please select an architecture before running.");
                return;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("program", effectiveProgram);
            data.put("degree", currentDegree);
            data.put("architecture", architecture);
            data.put("inputs", inputs);

            String json = gson.toJson(data);
            String response = httpPostJson(BASE_URL + "run", json);

            Map<String, Object> outer = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
            if (!"success".equals(outer.get("status"))) {
                String msg = String.valueOf(outer.get("message"));
                if (msg != null && msg.toLowerCase().contains("Dashboard")) {
                    handleCreditsDepleted(msg);
                    return;
                }
                throw new RuntimeException("Run failed: " + msg);
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

            Map<String, String> latestVarsSnapshot = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : varsMap.entrySet()) {
                latestVarsSnapshot.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            if (statisticsCommandsController != null) {
                statisticsCommandsController.setLastVarsSnapshot(latestVarsSnapshot);
            }

            if (varsBoxController != null) {
                varsBoxController.renderAll(varsMap);
                varsBoxController.setCycles(cycles.intValue());
            }
            if (statisticsTableController != null) {
                Platform.runLater(() -> statisticsTableController.clear());
            }
            if (mainExecutionController != null) {
                mainExecutionController.refreshHistory();
            }

        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("dashboard")) {
                handleCreditsDepleted(msg);
            } else {
                alertError("Run failed", msg);
            }
        }
    }

    private void handleCreditsDepleted(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Credits Depleted");
            alert.setHeaderText("Out of Credits");
            alert.setContentText("Your available credits have been used up.\n" +
                    "Execution stopped.\n\n" + (msg != null ? msg : ""));
            alert.show();

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {}

                Platform.runLater(() -> {
                    try {
                        alert.close();
                        if (mainExecutionController != null) {
                            System.out.println("Redirecting to dashboard due to depleted credits...");
                            mainExecutionController.triggerGoToDashboard();
                        } else {
                            System.err.println("mainExecutionController is null, cannot redirect.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }).start();
        });
    }

    @FXML
    private void onDebug(ActionEvent e) {
        try {
            Long[] inputs = inputsBoxController.collectAsLongsOrThrow();
            String effectiveProgram =
                    (currentProgram == null || currentProgram.isBlank() ||
                            currentProgram.equalsIgnoreCase("Main Program"))
                            ? "" : currentProgram;

            String architecture = (architectureController != null &&
                    architectureController.getSelectedArchitecture() != null)
                    ? architectureController.getSelectedArchitecture().name() : null;

            if (architecture == null || architecture.isBlank()) {
                alertError("Missing architecture", "Please select an architecture before debugging.");
                return;
            }

            String formData = "program=" + URLEncoder.encode(effectiveProgram, StandardCharsets.UTF_8)
                    + "&degree=" + currentDegree
                    + "&architecture=" + URLEncoder.encode(architecture, StandardCharsets.UTF_8)
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
            String msg = ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("credit")) {
                handleCreditsDepleted(msg);
            } else {
                alertError("Debug start failed", msg);
            }
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

            String msg = String.valueOf(result.get("message"));
            if (msg != null && msg.toLowerCase().contains("credit")) {
                handleCreditsDepleted(msg);
                return;
            }

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