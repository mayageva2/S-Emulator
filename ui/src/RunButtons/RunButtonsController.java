package RunButtons;

import InputsBox.InputsBoxController;
import VariablesBox.VariablesBoxController;
import emulator.api.EmulatorEngine;
import emulator.api.debug.DebugService;
import emulator.api.debug.DebugSnapshot;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunRecord;
import emulator.api.dto.RunResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class RunButtonsController {
    @FXML private Button btnNewRun, btnRun, btnDebug, btnStop, btnResume, btnStepOver, btnStepBack;
    @FXML private HBox runButtonsHBox;

    private EmulatorEngine engine;
    private int currentDegree = 0;
    private int lastMaxDegree = 0;
    private VariablesBoxController varsBoxController;
    private InputsBoxController inputController;
    private StatisticsTable.StatisticsTableController statisticsController;
    private InstructionsTable.InstructionsTableController instructionsController;
    private ProgramToolBar.ProgramToolbarController toolbarController;
    private Function<String, String> inputsFormatter;
    private Paint runBaseTextFill;
    private Paint debugBaseTextFill;
    private Effect runBaseEffect;
    private Effect debugBaseEffect;
    private DropShadow runGlowEffect;
    private Map<String, String> lastRunVarsSnapshot = Map.of();
    private final Map<String, Map<Integer, Map<String, String>>> varsSnapshotsByProgram = new HashMap<>();
    private String lastRunProgramName = null;

    private DebugService debugSession;
    private enum DebugState { IDLE, RUNNING, PAUSED, STOPPED }
    private DebugState debugState = DebugState.IDLE;
    private Map<String, String> lastVarsSnapshot = new HashMap<>();
    private Long[] inputsAtDebugStart = new Long[0];
    private final List<Control> degreeControls = new ArrayList<>();
    private final List<TitledPane> degreePanes = new ArrayList<>();
    private Supplier<String> selectedProgramSupplier = () -> null;
    private String debugProgramName = null;

    private Function<ProgramView, String> basicRenderer = pv -> pv != null ? pv.toString() : "";
    private Function<ProgramView, String> provenanceRenderer = pv -> pv != null ? pv.toString() : "";

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        refreshButtonsEnabled();
    }

    public void setInstructionsController(InstructionsTable.InstructionsTableController c) {
        this.instructionsController = c;
    }

    public void setSelectedProgramSupplier(Supplier<String> s) {
        this.selectedProgramSupplier = s;
    }

    public void setBasicRenderer(Function<ProgramView, String> renderer) {
        if (renderer != null) this.basicRenderer = renderer;
    }

    public void setProvenanceRenderer(Function<ProgramView, String> renderer) {
        if (renderer != null) this.provenanceRenderer = renderer;
    }

    public Map<String, String> getVarsSnapshotForIndex(int historyIndex) {
        String program = resolveCurrentProgramName();
        var forProgram = varsSnapshotsByProgram.get(canonicalProgramKey(program));
        if (forProgram == null) {
            return Map.of();
        }
        return forProgram.getOrDefault(historyIndex, Map.of());
    }

    public Map<String, String> getLastRunVarsSnapshot() {
        return lastRunVarsSnapshot;
    }

    public void setStatisticsTableController(StatisticsTable.StatisticsTableController c) {
        this.statisticsController = c;
    }

    public void setInputsFormatter(Function<String, String> f) {
        this.inputsFormatter = f;
    }

    public void setLastMaxDegree(int maxDegree) {
        this.lastMaxDegree = Math.max(0, maxDegree);
    }

    public void registerDegreeControl(Control c) {
        if (c != null) {
            degreeControls.add(c);
            refreshButtonsEnabled();
        }
    }

    public void registerDegreePane(TitledPane p) {
        if (p != null) {
            degreePanes.add(p);
            refreshButtonsEnabled();
        }
    }

    public void setProgramToolbarController(ProgramToolBar.ProgramToolbarController c) {
        this.toolbarController = c;
        refreshButtonsEnabled();
    }

    public void notifyProgramSelection(String internalProgramName) {
        if (internalProgramName == null) return;
        clearStatisticsIfProgramChanged(internalProgramName);
    }

    private boolean isInDebug() {
        return debugState == DebugState.RUNNING || debugState == DebugState.PAUSED;
    }

    @FXML
    private void initialize() {
        if (btnNewRun != null) btnNewRun.setTooltip(new Tooltip("New Run: Clear"));
        if (btnDebug != null) btnDebug.setTooltip(new Tooltip("Debug"));
        if (btnStop != null) btnStop.setTooltip(new Tooltip("Stop"));
        if (btnResume != null) btnResume.setTooltip(new Tooltip("Resume"));
        if (btnStepBack != null) btnStepBack.setTooltip(new Tooltip("Step backward"));
        if (btnStepOver != null) btnStepOver.setTooltip(new Tooltip("Step over"));
        if (btnRun != null) {
            btnRun.setTooltip(new Tooltip("Run"));
            runBaseTextFill = btnRun.getTextFill();
            runBaseEffect = btnRun.getEffect();
        }
        if (btnDebug != null) {
            debugBaseTextFill = btnDebug.getTextFill();
            debugBaseEffect = btnDebug.getEffect();
        }
        runGlowEffect = new DropShadow();
        runGlowEffect.setBlurType(BlurType.GAUSSIAN);
        runGlowEffect.setColor(Color.web("#b46ad4"));
        runGlowEffect.setRadius(25);
        runGlowEffect.setSpread(0.5);
        if (runButtonsHBox != null) {
            runButtonsHBox.setSpacing(10);
            runButtonsHBox.setFillHeight(false);
            runButtonsHBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        }
        refreshButtonsEnabled();
    }

    @FXML
    private void onNewRun(ActionEvent e) {
        if (inputController != null) inputController.clearInputs();
        if (varsBoxController != null) {
            try { varsBoxController.clearForNewRun(); }
            catch (Exception ex) { varsBoxController.clear(); }
        }
        setReadyToRunVisual(true);
    }

    private void setReadyToRunVisual(boolean on) {
        if (btnRun == null || btnDebug == null) return;
        if (on) {
            btnRun.setStyle("-fx-background-color: PURPLE; -fx-text-fill: white;");
            btnRun.setEffect(runGlowEffect);
            btnDebug.setStyle("-fx-background-color: PURPLE; -fx-text-fill: white;");
            btnDebug.setEffect(runGlowEffect);
        } else {
            btnRun.setStyle("");
            btnRun.setEffect(runBaseEffect);
            btnRun.setTextFill(runBaseTextFill);
            btnDebug.setStyle("");
            btnDebug.setEffect(debugBaseEffect);
            btnDebug.setTextFill(debugBaseTextFill);
        }
    }

    @FXML
    private void onRun(ActionEvent e) {
        setReadyToRunVisual(false);
        if (engine == null || !engine.hasProgramLoaded()) {
            alertInfo("No program loaded", "Use 'Load program XML' first.");
            return;
        }
        if (inputController == null) {
            alertError("Inputs not connected", "Inputs panel is not available.");
            return;
        }
        try {
            String currentProgram = resolveCurrentProgramName();
            Long[] inputs = inputController.collectAsLongsOrThrow();
            int max = (lastMaxDegree > 0) ? lastMaxDegree : 0;
            if (max == 0) { try { max = engine.programView(0).maxDegree(); } catch (Exception ignored) {} }
            int degree = Math.max(0, Math.min(currentDegree, max));
            String target = (selectedProgramSupplier != null) ? selectedProgramSupplier.get() : null;
            RunResult result = (target == null || target.isBlank()) ? engine.run(degree, inputs) : engine.run(target, degree, inputs);
            lastRunProgramName = (target == null || target.isBlank()) ? engine.programView(0).programName() : target;
            if (varsBoxController != null) {
                varsBoxController.renderFromRun(result, inputs);
                Map<String, Long> snap = varsBoxController.buildVarsMap(result, inputs);
                Map<String, String> asStrings = new LinkedHashMap<>();
                snap.forEach((k,v) -> asStrings.put(k, String.valueOf(v)));
                lastRunVarsSnapshot = asStrings;
            }
            updateStatisticsFromEngineHistory();
            var hist = (currentProgram == null || currentProgram.isBlank())
                    ? engine.history()
                    : engine.history(currentProgram);
            if (hist != null && !hist.isEmpty()) {
                int idx = hist.size() - 1;
                varsSnapshotsByProgram
                        .computeIfAbsent(canonicalProgramKey(currentProgram), k -> new HashMap<>())
                        .put(idx, lastRunVarsSnapshot);
            }
        } catch (Exception ex) {
            alertError("Run failed", friendlyMsg(ex));
        }
    }

    public void setCurrentDegree(int degree) { this.currentDegree = Math.max(0, degree); }

    public void setVarsBoxController(VariablesBoxController c) { this.varsBoxController = c; }

    public void setInputsBoxController(InputsBox.InputsBoxController c) {
        this.inputController = c;
        boolean inDebug = (debugState == DebugState.RUNNING || debugState == DebugState.PAUSED);
        if (inputController != null) {
            if (inDebug) inputController.lockInputs();
            else inputController.unlockInputs();
        }
    }

    private void updateStatisticsFromEngineHistory() {
        updateStatisticsFromEngineHistory(resolveCurrentProgramName());
    }

    private void updateStatisticsFromEngineHistory(String programName) {
        if (statisticsController == null || engine == null) return;
        List<RunRecord> history = (programName == null || programName.isBlank())
                ? engine.history()
                : engine.history(programName);
        Function<String, String> fmt = (inputsFormatter != null) ? inputsFormatter : (s -> s);
        Platform.runLater(() -> statisticsController.setHistory(history, fmt));
    }

    @FXML
    private void onDebug(ActionEvent e) {
        setReadyToRunVisual(false);
        if (engine == null || !engine.hasProgramLoaded()) {
            alertInfo("No program loaded", "Load a program first.");
            return;
        }
        try {
            Long[] inputs = (inputController != null) ? inputController.collectAsLongsOrThrow() : new Long[0];
            inputsAtDebugStart = Arrays.copyOf(inputs, inputs.length);
            debugProgramName = (selectedProgramSupplier != null) ? selectedProgramSupplier.get() : null;
            if (debugProgramName == null || debugProgramName.isBlank()) debugProgramName = engine.programView(0).programName();
            clearStatisticsIfProgramChanged(debugProgramName);
            int degree = currentDegree;
            debugSession = engine.debugger();
            DebugSnapshot first = debugSession.start(inputs, degree, debugProgramName);
            if (inputController != null) inputController.lockInputs();
            debugState = DebugState.PAUSED;
            applySnapshot(first);
        } catch (Exception ex) {
            alertError("Debug start failed", friendlyMsg(ex));
            debugState = DebugState.IDLE;
            if (inputController != null) inputController.unlockInputs();
        } finally {
            refreshButtonsEnabled();
        }
    }

    private void applySnapshot(DebugSnapshot snap) {
        if (snap == null) return;
        if (varsBoxController != null) varsBoxController.clearHighlight();
        int row = toDisplayRowFromInstructionIndex(snap.currentInstructionIndex());
        if (instructionsController != null) instructionsController.highlightRow(row);
        if (varsBoxController != null) {
            Map<String, String> vars = (snap.vars() == null) ? Map.of() : snap.vars();
            Set<String> changed = new HashSet<>();
            for (var e : vars.entrySet()) {
                String k = e.getKey();
                String newV = e.getValue();
                String oldV = lastVarsSnapshot.get(k);
                if (oldV != null && !Objects.equals(oldV, newV)) changed.add(k);
            }
            varsBoxController.renderAll(vars);
            varsBoxController.highlightVariables(changed);
            varsBoxController.setCycles(Math.max(0, snap.cycles()));
            lastVarsSnapshot = new HashMap<>(vars);
        }
        if (snap.finished()) {
            debugState = DebugState.STOPPED;
            onDebugFinishedUI();
            if (inputController != null) inputController.unlockInputs();
            postDebugCommitToHistory(snap);
        } else {
            debugState = DebugState.PAUSED;
        }
        refreshButtonsEnabled();
    }

    private int toDisplayRowFromInstructionIndex(int instrIndex) {
        try {
            String target = (selectedProgramSupplier != null) ? selectedProgramSupplier.get() : null;
            ProgramView pv = (target == null || target.isBlank()) ? engine.programView(currentDegree) : engine.programView(target, currentDegree);
            if (pv != null && pv.instructions() != null) {
                var list = pv.instructions();
                for (int i = 0; i < list.size(); i++) {
                    InstructionView iv = list.get(i);
                    if (iv != null && iv.index() == instrIndex) return i;
                }
                return Math.max(1, Math.min(instrIndex, list.size()));
            }
        } catch (Exception ignore) {}
        return Math.max(1, instrIndex);
    }

    private void onDebugFinishedUI() {
        if (instructionsController != null) {
            try { instructionsController.clearHighlight(); } catch (Exception ignore) {}
        }
    }

    private void postDebugCommitToHistory(DebugSnapshot snap) {
        String programName = resolveCurrentProgramName();
        updateStatisticsFromEngineHistory(programName);
        try {
            var hist = (programName == null || programName.isBlank())
                    ? engine.history()
                    : engine.history(programName);
            if (hist != null && !hist.isEmpty()) {
                int lastIdx = hist.size() - 1;
                Map<String, String> vars = (snap != null && snap.vars() != null) ? snap.vars() : Map.of();
                varsSnapshotsByProgram
                        .computeIfAbsent(canonicalProgramKey(programName), k -> new HashMap<>())
                        .put(lastIdx, vars);
            }
        } catch (Exception ignore) {}
    }

    @FXML
    private void onStop(ActionEvent e) {
        try {
            if (debugSession != null) {
                DebugSnapshot snap = debugSession.stop();
                if (snap != null) applySnapshot(snap);
            }
            updateStatisticsFromEngineHistory();
        } catch (Exception ex) {
            alertError("Stop failed", friendlyMsg(ex));
        } finally {
            debugState = DebugState.STOPPED;
            onDebugFinishedUI();
            if (inputController != null) inputController.unlockInputs();
            if (varsBoxController != null) varsBoxController.clearHighlight();
            refreshButtonsEnabled();
        }
    }

    @FXML
    private void onResume(ActionEvent e) {
        if (debugSession == null) return;
        debugState = DebugState.RUNNING;
        refreshButtonsEnabled();
        Thread t = new Thread(() -> {
            try {
                DebugSnapshot finalSnap = debugSession.resume();
                Platform.runLater(() -> applySnapshot(finalSnap));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    alertError("Resume failed", friendlyMsg(ex));
                    debugState = DebugState.PAUSED;
                    refreshButtonsEnabled();
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onStepOver(ActionEvent e) {
        if (debugSession == null) return;
        try {
            DebugSnapshot snap = debugSession.stepOver();
            applySnapshot(snap);
        } catch (Exception ex) {
            alertError("Step Over failed", friendlyMsg(ex));
        }
    }

    @FXML
    private void onStepBack(ActionEvent e) {
        alertInfo("Not implemented", "Step backward is not yet implemented in the UI.");
    }

    private void refreshButtonsEnabled() {
        boolean loaded = (engine != null && engine.hasProgramLoaded());
        boolean inDebug = (debugState == DebugState.RUNNING || debugState == DebugState.PAUSED);
        boolean paused = (debugState == DebugState.PAUSED);
        if (btnNewRun != null) btnNewRun.setDisable(!loaded || inDebug);
        if (btnRun != null) btnRun.setDisable(!loaded || inDebug);
        if (btnDebug != null) btnDebug.setDisable(!loaded || inDebug);
        if (btnStop != null) btnStop.setDisable(!inDebug);
        if (btnResume != null) btnResume.setDisable(!paused);
        if (btnStepOver != null) btnStepOver.setDisable(!paused);
        if (btnStepBack != null) btnStepBack.setDisable(true);
        if (inputController != null) {
            if (inDebug) inputController.lockInputs();
            else inputController.unlockInputs();
        }
        if (toolbarController != null) {
            toolbarController.setDegreeUiLocked(inDebug);
        }
    }

    private Optional<Integer> promptExpansionDegree(int min, int max) {
        List<Integer> choices = new ArrayList<>();
        for (int i = min; i <= max; i++) choices.add(i);
        ChoiceDialog<Integer> dlg = new ChoiceDialog<>(0, choices);
        dlg.setTitle("Choose Expansion Degree");
        dlg.setHeaderText("Select expansion degree (0 - " + max + ")");
        dlg.setContentText("Degree:");
        return dlg.showAndWait();
    }

    private Long[] promptInputsAsCsv(ProgramView pv) {
        String hint = "Enter inputs (comma-separated, e.g. 3,6,2):";
        try {
            var method = pv.getClass().getMethod("inputNames");
            Object namesObj = method.invoke(pv);
            if (namesObj instanceof List<?> names && !names.isEmpty()) {
                hint = "Inputs: " + String.join(", ", names.stream().map(Object::toString).toList())
                        + "\nEnter inputs (comma-separated, e.g. 3,6,2):";
            }
        } catch (ReflectiveOperationException ignore) {}
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Program Inputs");
        dlg.setHeaderText("Provide input values");
        dlg.setContentText(hint);
        Optional<String> ans = dlg.showAndWait();
        if (ans.isEmpty() || ans.get().isBlank()) {
            return new Long[0];
        }
        try {
            String[] toks = ans.get().split(",");
            Long[] out = new Long[toks.length];
            for (int i = 0; i < toks.length; i++) {
                out[i] = Long.parseLong(toks[i].trim());
            }
            return out;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid inputs. Please enter comma-separated integers.", nfe);
        }
    }

    private void alertInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void alertError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String friendlyMsg(Throwable t) {
        String m = (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        Throwable cause = t.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            m += " (cause: " + cause.getMessage() + ")";
        }
        return m;
    }

    private void clearStatisticsUI() {
        Platform.runLater(() -> {
            if (statisticsController != null) {
                try { statisticsController.clear(); } catch (Throwable ignore) {
                    Function<String,String> fmt = (inputsFormatter != null) ? inputsFormatter : (s -> s);
                    statisticsController.setHistory(List.of(), fmt);
                }
            }
            lastRunVarsSnapshot = Map.of();
        });
    }

    public void clearStoredStatistics() {
        varsSnapshotsByProgram.clear();
        lastRunVarsSnapshot = Map.of();
        lastRunProgramName = null;
        clearStatisticsUI();
    }

    private void clearStatisticsIfProgramChanged(String programName) {
        if (programName == null) return;
        String newKey = programName.trim().toLowerCase(Locale.ROOT);
        String oldKey = (lastRunProgramName == null) ? null : lastRunProgramName.trim().toLowerCase(Locale.ROOT);
        if (!Objects.equals(oldKey, newKey)) {
            lastRunProgramName = programName;
            updateStatisticsFromEngineHistory(programName);
        }
    }

    private String canonicalProgramKey(String programName) {
        if (programName == null) return "";
        return programName.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveCurrentProgramName() {
        String name = (selectedProgramSupplier != null) ? selectedProgramSupplier.get() : null;
        if (name == null || name.isBlank()) {
            try { name = engine.programView(0).programName(); } catch (Exception ignore) {}
        }
        return name == null ? "" : name.trim();
    }

    public void resetForNewRun() {
        try {
            debugState = DebugState.IDLE;
            lastVarsSnapshot.clear();
            if (inputController != null) inputController.unlockInputs();
            if (instructionsController != null) {
                try { instructionsController.clearHighlight(); } catch (Throwable ignore) {}
                try { instructionsController.clear(); } catch (Throwable ignore) {}
            }
            if (varsBoxController != null) {
                try { varsBoxController.clear(); } catch (Throwable ignore) {}
            }
            setReadyToRunVisual(true);
        } catch (Throwable ignore) {
        } finally {
            refreshButtonsEnabled();
        }
    }
}
