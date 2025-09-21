package RunButtons;

import InputsBox.InputsBoxController;
import VariablesBox.VariablesBoxController;
import emulator.api.EmulatorEngine;
import emulator.api.debug.DebugSnapshot;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunResult;
import emulator.api.debug.DebugSession;
import emulator.logic.debug.EngineDebugAdapter;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.event.ActionEvent;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Function;

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
    private Function<String, String> inputsFormatter;
    private Paint  runBaseTextFill;
    private Effect runBaseEffect;
    private DropShadow runGlowEffect;
    private Timeline runGlowPulse;
    private PauseTransition runGlowStopTimer;

    // -- Debug --//
    private DebugSession debugSession;
    private enum DebugState { IDLE, RUNNING, PAUSED, STOPPED }
    private DebugState debugState = DebugState.IDLE;
    private Map<String, String> lastVarsSnapshot = new HashMap<>();

    private Function<ProgramView, String> basicRenderer = pv -> pv != null ? pv.toString() : "";
    private Function<ProgramView, String> provenanceRenderer = pv -> pv != null ? pv.toString() : "";

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        refreshButtonsEnabled();
    }

    public void setInstructionsController(InstructionsTable.InstructionsTableController c) {
        this.instructionsController = c;
    }

    public void setBasicRenderer(Function<ProgramView, String> renderer) {
        if (renderer != null) this.basicRenderer = renderer;
    }

    public void setProvenanceRenderer(Function<ProgramView, String> renderer) {
        if (renderer != null) this.provenanceRenderer = renderer;
    }

    public void setStatisticsTableController(StatisticsTable.StatisticsTableController c) {
        this.statisticsController = c;
    }

    public void setInputsFormatter(java.util.function.Function<String, String> f) {
        this.inputsFormatter = f;
    }

    public void setLastMaxDegree(int maxDegree) {
        this.lastMaxDegree = Math.max(0, maxDegree);
    }

    @FXML
    private void initialize() {
        // Optional: tooltips
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
            runGlowEffect = new DropShadow();
            runGlowEffect.setBlurType(BlurType.GAUSSIAN);
            runGlowEffect.setColor(Color.web("#cf94d4"));
            runGlowEffect.setRadius(30);
            runGlowEffect.setSpread(0.55);

            runGlowPulse = new Timeline(
                    new KeyFrame(Duration.ZERO,           new KeyValue(runGlowEffect.radiusProperty(), 14)),
                    new KeyFrame(Duration.millis(700),    new KeyValue(runGlowEffect.radiusProperty(), 22)),
                    new KeyFrame(Duration.millis(1400),   new KeyValue(runGlowEffect.radiusProperty(), 14))
            );
            runGlowPulse.setAutoReverse(true);
            runGlowPulse.setCycleCount(Timeline.INDEFINITE);
        }
        refreshButtonsEnabled();

        final double MIN_GAP = 2;
        final double MAX_GAP = 15;
        final int GAPS = 6;

        var totalButtonsWidthBinding = Bindings.createDoubleBinding(
                () -> get(btnNewRun) + get(btnRun) + get(btnDebug) + get(btnStop) + get(btnResume) + get(btnStepBack) + get(btnStepOver),
                btnNewRun.widthProperty(), btnRun.widthProperty(), btnDebug.widthProperty(), btnStop.widthProperty(),
                btnResume.widthProperty(), btnStepBack.widthProperty(), btnStepOver.widthProperty()
        );

        runButtonsHBox.spacingProperty().bind(Bindings.createDoubleBinding(() -> {
            double contentW   = runButtonsHBox.getWidth();
            double buttonsW   = totalButtonsWidthBinding.get();
            double paddingW   = runButtonsHBox.getPadding() == null ? 0
                    : runButtonsHBox.getPadding().getLeft() + runButtonsHBox.getPadding().getRight();
            double availForGaps = Math.max(0, contentW - buttonsW - paddingW);
            double gap = (GAPS > 0) ? availForGaps / GAPS : 0;
            if (Double.isNaN(gap) || Double.isInfinite(gap)) gap = MIN_GAP;
            return Math.max(MIN_GAP, Math.min(MAX_GAP, gap));
        }, runButtonsHBox.widthProperty(), totalButtonsWidthBinding));
    }

    private static double get(Control c) { return (c == null) ? 0 : c.getWidth(); }

    @FXML
    private void onNewRun(javafx.event.ActionEvent e) {
        if (inputController != null) inputController.clearInputs();
        if (varsBoxController != null) {
            try { varsBoxController.clearForNewRun(); }
            catch (Exception ex) { varsBoxController.clear(); }
        }
        blinkRunEmphasisTwoSeconds();
    }

    private void setReadyToRunVisual(boolean on) {
        if (btnRun == null) return;
        var cls = "run-ready";
        if (on) {
            btnRun.setTextFill(Color.WHITE);
            btnRun.setEffect(runGlowEffect);
            if (runGlowPulse != null && runGlowPulse.getStatus() != Timeline.Status.RUNNING) {
                runGlowPulse.playFromStart();
            }
        } else {
            if (runGlowPulse != null) runGlowPulse.stop();
            btnRun.setTextFill(runBaseTextFill);
            btnRun.setEffect(runBaseEffect);
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
            alertError("Inputs not connected",
                    "Inputs panel is not available. Make sure MainController calls setInputsBoxController(...).");
            return;
        }

        int max = (lastMaxDegree > 0) ? lastMaxDegree : 0;
        if (max == 0) { try { max = engine.programView(0).maxDegree(); } catch (Exception ignored) {} }
        int degree = Math.max(0, Math.min(currentDegree, max));

        try {
            Long[] inputs = inputController.collectAsLongsOrThrow();
            RunResult result = engine.run(degree, inputs);

            if (varsBoxController != null) {
                varsBoxController.renderFromRun(result, inputs);
            }

            updateStatisticsFromEngineHistory();

        } catch (Exception ex) {
            alertError("Run failed", friendlyMsg(ex));
        }
    }

    public void setCurrentDegree(int degree) {
        this.currentDegree = Math.max(0, degree);
    }

    public void setVarsBoxController(VariablesBoxController c) {
        this.varsBoxController = c;
    }

    public void setInputsBoxController(InputsBox.InputsBoxController c) { this.inputController = c; }

    private void updateStatisticsFromEngineHistory() {
        if (statisticsController == null || engine == null) return;
        var history = engine.history();
        Function<String, String> fmt = (inputsFormatter != null) ? inputsFormatter : (s -> s);
        Platform.runLater(() -> statisticsController.setHistory(history, fmt));
    }

    @FXML
    private void onDebug(ActionEvent e) {
        if (engine == null || !engine.hasProgramLoaded()) {
            alertInfo("No program loaded", "Load a program first.");
            return;
        }
        try {
            Long[] inputs = (inputController != null) ? inputController.collectAsLongsOrThrow() : new Long[0];
            int degree = currentDegree;
            debugSession = new EngineDebugAdapter(engine);
            DebugSnapshot first = debugSession.start(inputs, degree);
            debugState = DebugState.PAUSED;
            applySnapshot(first);
        } catch (UnsupportedOperationException uoe) {
            alertInfo("Debug not wired", "Connect EngineDebugAdapter to your EmulatorEngine stepping API.");
            debugState = DebugState.IDLE;
        } catch (Exception ex) {
            alertError("Debug start failed", friendlyMsg(ex));
            debugState = DebugState.IDLE;
        } finally {
            refreshButtonsEnabled();
        }
    }

    private void applySnapshot(DebugSnapshot snap) {
        if (snap == null) return;
        if (instructionsController != null) {
            instructionsController.highlightRow(snap.currentInstructionIndex());
        }
        if (varsBoxController != null) {
            Map<String, String> vars = (snap.vars() == null) ? Map.of() : snap.vars();
            Set<String> changed = new HashSet<>();
            for (var e : vars.entrySet()) {
                String prev = lastVarsSnapshot.get(e.getKey());
                if (!Objects.equals(prev, e.getValue())) changed.add(e.getKey());
            }
            varsBoxController.renderAll(vars);
            try {
                varsBoxController.highlightVariables(changed);
            } catch (NoSuchMethodError | Exception ignore) {
            }
            varsBoxController.setCycles(Math.max(0, snap.cycles()));

            lastVarsSnapshot.clear();
            lastVarsSnapshot.putAll(vars);
        }
        if (snap.finished()) {
            debugState = DebugState.STOPPED;
            onDebugFinishedUI();
        } else {
            debugState = DebugState.PAUSED;
        }
        refreshButtonsEnabled();
    }

    private void onDebugFinishedUI() {
        if (instructionsController != null) {
            try { instructionsController.clearHighlight(); } catch (Exception ignore) {}
        }
    }


    @FXML
    private void onStop(ActionEvent e) {
        try {
            if (debugSession != null) debugSession.stop();
        } catch (UnsupportedOperationException ex) {
        } catch (Exception ex) {
            alertError("Stop failed", friendlyMsg(ex));
        } finally {
            debugState = DebugState.STOPPED;
            onDebugFinishedUI();
            refreshButtonsEnabled();
        }
    }

    @FXML
    private void onResume(ActionEvent e) {
        if (debugSession == null) return;
        try {
            debugState = DebugState.RUNNING;
            refreshButtonsEnabled();
            DebugSnapshot snap = debugSession.resume();
            applySnapshot(snap); // יגדיר PAUSED/STOPPED בהתאם ל-finished
        } catch (UnsupportedOperationException ex) {
            alertInfo("Resume not wired", "Implement resume() in EngineDebugAdapter.");
            debugState = DebugState.PAUSED;
            refreshButtonsEnabled();
        } catch (Exception ex) {
            alertError("Resume failed", friendlyMsg(ex));
            debugState = DebugState.PAUSED;
            refreshButtonsEnabled();
        }
    }

    @FXML
    private void onStepOver(ActionEvent e) {
        if (debugSession == null) return;
        try {
            DebugSnapshot snap = debugSession.stepOver();
            applySnapshot(snap);
        } catch (UnsupportedOperationException ex) {
            alertInfo("Step Over not wired", "Implement stepOver() in EngineDebugAdapter.");
        } catch (Exception ex) {
            alertError("Step Over failed", friendlyMsg(ex));
        }
    }

    @FXML
    private void onStepBack(ActionEvent e) {
        // TODO: hook to your engine's step-back API if available
        alertInfo("Not implemented", "Step backward is not yet implemented in the UI.");
    }

    private void refreshButtonsEnabled() {
        boolean loaded = (engine != null && engine.hasProgramLoaded());
        boolean inDebug = (debugState == DebugState.RUNNING || debugState == DebugState.PAUSED);
        boolean paused  = (debugState == DebugState.PAUSED);
        if (btnNewRun != null) btnNewRun.setDisable(!loaded || inDebug);
        if (btnRun != null) btnRun.setDisable(!loaded || inDebug);
        if (btnDebug != null) btnDebug.setDisable(!loaded || inDebug);
        if (btnStop     != null) btnStop.setDisable(!inDebug);
        if (btnResume   != null) btnResume.setDisable(!paused);
        if (btnStepOver != null) btnStepOver.setDisable(!paused);
        if (btnStepBack != null) btnStepBack.setDisable(true);

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

    private void blinkRunEmphasisTwoSeconds() {
        setReadyToRunVisual(true);
        if (runGlowStopTimer != null) runGlowStopTimer.stop();
        runGlowStopTimer = new PauseTransition(Duration.seconds(2));
        runGlowStopTimer.setOnFinished(e -> setReadyToRunVisual(false));
        runGlowStopTimer.playFromStart();
    }
}
