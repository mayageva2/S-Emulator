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
import emulator.api.dto.RunRecord;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

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

        instructionsController.setOnRowSelected(selected -> {
            if (selected == null) {
                historyChainController.clear();
                return;
            }

            try {
                historyChainController.showForSelected(selected, null);
            } catch (Exception e) {
                System.err.println("Failed to update history chain: " + e.getMessage());
                historyChainController.clear();
            }
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
        runButtonsController.setInstructionsController(instructionsController);
        runButtonsController.setMainController(this);
        statisticsCommandsController.setStatisticsTableController(statisticsTableController);
        statisticsCommandsController.setToolbarController(toolbarController);
        statisticsCommandsController.setMainController(this);
    }

    private void onProgramLoaded(HeaderAndLoadButtonController.LoadedEvent ev) {
        try {
            String jsonBody = new Gson().toJson(Map.of("path", ev.xmlPath().toString()));
            String response = httpPost(BASE_URL + "load", jsonBody);

            System.out.println("SERVER LOAD RESPONSE:");
            System.out.println(response);
            Map<String, Object> map = gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());

            if (map.containsKey("programName"))
                currentProgram = String.valueOf(map.get("programName"));
            if (map.containsKey("maxDegree"))
                maxDegree = ((Double) map.get("maxDegree")).intValue();
            else
                maxDegree = ev.maxDegree();
            currentDegree = 0;

            if (runButtonsController != null) {
                runButtonsController.setCurrentProgram(currentProgram);
                runButtonsController.setCurrentDegree(0);
            }

            refreshProgramView(currentDegree);

            List<String> programs = new ArrayList<>();
            Object funcsObj = map.get("functions");
            if (funcsObj instanceof List<?> funcList) {
                for (Object f : funcList) {
                    if (f != null && !f.toString().isBlank()) programs.add(f.toString());
                }
            }

            Platform.runLater(() -> {
                toolbarController.bindDegree(currentDegree, maxDegree);
                toolbarController.setHighlightEnabled(true);
                toolbarController.setPrograms(programs);
                toolbarController.setDegreeButtonEnabled(true);
                toolbarController.setExpandEnabled(maxDegree > 0);
                toolbarController.setCollapseEnabled(false);
                runButtonsController.enableRunButtonsAfterLoad();
            });
            refreshProgramView(0);

        } catch (Exception e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    private String httpPostForm(String urlStr, String formData) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(formData.getBytes(StandardCharsets.UTF_8));
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();
        conn.disconnect();
        return sb.toString();
    }

    private void refreshProgramView(int degree) {
        try {
            String json = fetchProgramViewJson(degree);
            Map<String, Object> program = parseAndValidateResponse(json);
            if (program == null) return;

            updateProgramDegrees(program);
            updateInputsBox(program);
            renderInstructions(program);
            updateToolbarHighlights(program);
            updateToolbarPrograms(program);
            updateSummaryLine(program);

        } catch (Exception e) {
            showError("Render failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String fetchProgramViewJson(int degree) throws Exception {
        String programParam = (currentProgram == null || currentProgram.equalsIgnoreCase("Main Program"))
                ? "" : "&program=" + URLEncoder.encode(currentProgram, StandardCharsets.UTF_8);
        String url = BASE_URL + "view?degree=" + degree + programParam;
        return httpGet(url);
    }

    private Map<String, Object> parseAndValidateResponse(String response) {
        Map<String, Object> map = gson.fromJson(response, new TypeToken<Map<String, Object>>() {}.getType());
        if (!"success".equals(map.get("status"))) {
            showError("View failed: " + map.get("message"));
            return null;
        }

        Map<String, Object> program = (Map<String, Object>) map.get("program");
        if (program == null) {
            showError("Missing program data in response");
            return null;
        }
        return program;
    }

    private void renderInstructions(Map<String, Object> program) {
        List<Map<String, Object>> instructionsList =
                (List<Map<String, Object>>) program.get("instructions");

        if (instructionsList == null) return;

        if (instructionsController != null) {
            instructionsController.renderFromJson(instructionsList);
        }
    }

    private void updateProgramDegrees(Map<String, Object> program) {
        Object maxDegObj = program.get("maxDegree");
        if (maxDegObj instanceof Number n)
            maxDegree = n.intValue();

        Object degreeObj = program.get("degree");
        if (degreeObj instanceof Number n)
            currentDegree = n.intValue();
    }

    @SuppressWarnings("unchecked")
    private void updateInputsBox(Map<String, Object> program) {
        if (inputsBoxController == null) return;

        Object inputsObj = program.get("inputs");
        List<String> inputNames = new ArrayList<>();

        if (inputsObj instanceof List<?> list) {
            for (Object o : list) {
                if (o != null && !o.toString().isBlank())
                    inputNames.add(o.toString().trim());
            }
        }

        if (inputNames.isEmpty()) {
            Object varsObj = program.get("variables");
            if (varsObj instanceof List<?> list) {
                for (Object o : list) {
                    String name = Objects.toString(o, "").trim();
                    if (name.matches("(?i)x\\d+"))
                        inputNames.add(name);
                }
            }
        }

        Platform.runLater(() -> {
            inputsBoxController.showNames(inputNames);
        });
    }


    @SuppressWarnings("unchecked")
    private void updateToolbarHighlights(Map<String, Object> program) {
        if (toolbarController == null) return;

        List<Map<String, Object>> instructionsList = (List<Map<String, Object>>) program.get("instructions");
        if (instructionsList == null) return;
        Set<String> highlightSet = new LinkedHashSet<>();

        for (Map<String, Object> ins : instructionsList) {
            String label = Objects.toString(ins.get("label"), "").trim();
            if (!label.isBlank()) highlightSet.add(label);

            Object argsObj = ins.get("args");
            if (argsObj instanceof List<?> argsList) {
                for (Object a : argsList) {
                    String arg = Objects.toString(a, "").trim();
                    if (arg.matches("(?i)[xyz]\\d*") || arg.matches("(?i)L\\d+") || arg.equalsIgnoreCase("y")) {
                        highlightSet.add(arg);
                    }
                    int eq = arg.indexOf('=');
                    if (eq > 0) {
                        String val = arg.substring(eq + 1).trim();
                        if (val.matches("(?i)[xyz]\\d*") || val.matches("(?i)L\\d+") || val.equalsIgnoreCase("y")) {
                            highlightSet.add(val);
                        }
                    }
                }
            }
        }

        List<String> highlights = new ArrayList<>(highlightSet);
        highlights.sort((a, b) -> {
            if (a == null) return -1;
            if (b == null) return 1;
            a = a.trim();
            b = b.trim();
            String prefixA = extractPrefix(a);
            String prefixB = extractPrefix(b);
            int orderA = prefixOrder(prefixA);
            int orderB = prefixOrder(prefixB);
            if (orderA != orderB) return Integer.compare(orderA, orderB);
            int numA = extractTrailingNumber(a);
            int numB = extractTrailingNumber(b);
            return Integer.compare(numA, numB);
        });

        Platform.runLater(() -> {
            toolbarController.bindDegree(currentDegree, maxDegree);
            toolbarController.setHighlightOptions(highlights);
            toolbarController.setHighlightEnabled(!highlights.isEmpty());
            toolbarController.setExpandEnabled(currentDegree < maxDegree);
            toolbarController.setCollapseEnabled(currentDegree > 0);
        });
    }

    @SuppressWarnings("unchecked")
    private void updateToolbarPrograms(Map<String, Object> program) {
        Object funcsObj = program.get("functions");
        if (!(funcsObj instanceof List<?> funcList)) return;

        List<String> programs = new ArrayList<>();
        for (Object o : funcList) {
            if (o != null) {
                String name = o.toString().trim();
                if (!name.isEmpty()) programs.add(name);
            }
        }

        programs.sort((a, b) -> {
            String pa = a.replaceAll("[^A-Za-z]+.*$", "");
            String pb = b.replaceAll("[^A-Za-z]+.*$", "");
            if (pa.equalsIgnoreCase(pb)) {
                int na = extractTrailingNumber(a);
                int nb = extractTrailingNumber(b);
                return Integer.compare(na, nb);
            }
            return pa.compareToIgnoreCase(pb);
        });

        Platform.runLater(() -> {
            toolbarController.setPrograms(programs);
            toolbarController.setSelectedProgram(currentProgram);
        });
    }

    private void updateSummaryLine(Map<String, Object> program) {
        if (summaryLineController == null) return;
        try {
            summaryLineController.updateFromJson(program);
        } catch (Exception ex) {
            System.err.println("Failed to update summary line: " + ex.getMessage());
        }
    }

    private static String extractPrefix(String s) {
        if (s == null) return "";
        var m = java.util.regex.Pattern.compile("^([A-Za-z]+)").matcher(s);
        return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : "";
    }

    private static int extractTrailingNumber(String s) {
        if (s == null) return 0;
        var m = java.util.regex.Pattern.compile("(\\d+)$").matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static int prefixOrder(String p) {
        return switch (p.toUpperCase(Locale.ROOT)) {
            case "X" -> 1;
            case "Y" -> 2;
            case "Z" -> 3;
            case "L" -> 4;
            default -> 5;
        };
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

    public void refreshHistory() {
        try {
            String histJson = httpGet(BASE_URL + "history");
            System.out.println("HISTORY RESPONSE: " + histJson);

            Map<String, Object> histMap = gson.fromJson(histJson, new TypeToken<Map<String, Object>>(){}.getType());
            List<Map<String, Object>> records = (List<Map<String, Object>>) histMap.get("history");

            if (records != null && !records.isEmpty()) {
                List<RunRecord> runRecords = new ArrayList<>();
                for (Map<String, Object> rec : records) {
                    String programName = Objects.toString(rec.get("programName"), "Main Program");
                    int runNumber = ((Number) rec.get("runNumber")).intValue();
                    int degree = ((Number) rec.get("degree")).intValue();
                    long y = ((Number) rec.get("y")).longValue();
                    int cycles = ((Number) rec.get("cycles")).intValue();

                    Object inputsObj = rec.get("inputs");
                    List<Long> inputsList = new ArrayList<>();

                    if (inputsObj instanceof List<?> list) {
                        for (Object val : list) {
                            try { inputsList.add(Long.parseLong(val.toString())); } catch (Exception ignored) {}
                        }
                    } else if (inputsObj != null) {
                        String csv = inputsObj.toString().trim();
                        if (!csv.isEmpty()) {
                            for (String part : csv.split(",")) {
                                try { inputsList.add(Long.parseLong(part.trim())); } catch (Exception ignored) {}
                            }
                        }
                    }

                    runRecords.add(new RunRecord(programName, runNumber, degree, inputsList, y, cycles));
                }

                Platform.runLater(() -> {
                    statisticsTableController.setHistory(runRecords, (Function<String, String>) s -> s);
                });
            }

        } catch (Exception e) {
            showError("History refresh failed: " + e.getMessage());
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

    public void initServerMode(String baseUrl) {
        try {
            if (baseUrl != null && !baseUrl.isBlank()) {
                this.currentProgram = null;
                this.currentDegree = 0;
                this.maxDegree = 0;

                if (headerController != null) {
                    headerController.setOnLoaded(this::onProgramLoaded);
                }

                if (toolbarController != null) {
                    toolbarController.setOnExpand(this::onExpandOne);
                    toolbarController.setOnCollapse(this::onCollapseOne);
                    toolbarController.setOnJumpToDegree(this::onJumpToDegree);
                    toolbarController.setOnProgramSelected(this::onProgramPicked);
                    toolbarController.setOnHighlightChanged(term -> {
                        if (instructionsController != null)
                            instructionsController.setHighlightTerm(term);
                    });
                }

                if (runButtonsController != null) {
                    runButtonsController.setInputsBoxController(inputsBoxController);
                    runButtonsController.setVarsBoxController(varsBoxController);
                    runButtonsController.setStatisticsTableController(statisticsTableController);
                    runButtonsController.setProgramToolbarController(toolbarController);
                    runButtonsController.setCurrentDegree(currentDegree);
                }

                Platform.runLater(() -> {
                    HBox.setHgrow(contentBox, Priority.ALWAYS);
                    HBox.setHgrow(sidePanels, Priority.ALWAYS);
                    varsBox.prefWidthProperty().bind(sidePanels.widthProperty().divide(2));
                    inputsBox.prefWidthProperty().bind(sidePanels.widthProperty().divide(2));
                });
            }
        } catch (Exception e) {
            showError("initServerMode failed: " + e.getMessage());
        }
    }

    public VariablesBoxController getVarsBoxController() {
        return varsBoxController;
    }

    public InputsBoxController getInputsBoxController() {
        return inputsBoxController;
    }

    public void refreshProgramViewPublic(int degree) {
        refreshProgramView(degree);
    }

    public String httpPostFormPublic(String urlStr, String formData) throws Exception {
        return httpPostForm(urlStr, formData);
    }

}
