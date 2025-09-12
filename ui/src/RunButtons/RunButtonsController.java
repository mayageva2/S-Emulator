package RunButtons;

import InputsBox.InputsBoxController;
import VariablesBox.VariablesBoxController;
import cyclesLine.CyclesLineController;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunResult;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.event.ActionEvent;
import javafx.scene.layout.HBox;

import java.util.*;
import java.util.function.Function;

public class RunButtonsController {
    @FXML private Button btnRun, btnDebug, btnStop, btnResume, btnStepOver, btnStepBack;
    @FXML private HBox runButtonsHBox;

    private EmulatorEngine engine;
    private int currentDegree = 0;
    private int lastMaxDegree = 0;
    private VariablesBoxController varsBoxController;
    private InputsBoxController inputController;


    private Function<ProgramView, String> basicRenderer = pv -> pv != null ? pv.toString() : "";
    private Function<ProgramView, String> provenanceRenderer = pv -> pv != null ? pv.toString() : "";

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        refreshButtonsEnabled();
    }

    public void setBasicRenderer(Function<ProgramView, String> renderer) {
        if (renderer != null) this.basicRenderer = renderer;
    }

    public void setProvenanceRenderer(Function<ProgramView, String> renderer) {
        if (renderer != null) this.provenanceRenderer = renderer;
    }

    public void setLastMaxDegree(int maxDegree) {
        this.lastMaxDegree = Math.max(0, maxDegree);
    }

    @FXML
    private void initialize() {
        // Optional: tooltips
        if (btnRun != null) btnRun.setTooltip(new Tooltip("Run"));
        if (btnDebug != null) btnDebug.setTooltip(new Tooltip("Debug"));
        if (btnStop != null) btnStop.setTooltip(new Tooltip("Stop"));
        if (btnResume != null) btnResume.setTooltip(new Tooltip("Resume"));
        if (btnStepBack != null) btnStepBack.setTooltip(new Tooltip("Step backward"));
        if (btnStepOver != null) btnStepOver.setTooltip(new Tooltip("Step over"));
        refreshButtonsEnabled();

        final double MIN_GAP = 2;
        final double MAX_GAP = 15;
        final int GAPS = 5;

        var totalButtonsWidthBinding = Bindings.createDoubleBinding(
                () -> get(btnRun) + get(btnDebug) + get(btnStop) + get(btnResume) + get(btnStepBack) + get(btnStepOver),
                btnRun.widthProperty(), btnDebug.widthProperty(), btnStop.widthProperty(),
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
    private void onRun(ActionEvent e) {
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
            ProgramView pv = engine.programView(degree);
            Long[] inputs = inputController.collectAsLongsOrThrow();
            RunResult result = engine.run(degree, inputs);

            if (varsBoxController != null) {
                varsBoxController.renderFromRun(result, inputs);
            }

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

    @FXML
    private void onDebug(ActionEvent e) {
        // TODO: hook to your engine's debug-start API if available
        alertInfo("Not implemented", "Debug start is not yet implemented in the UI.");
    }

    @FXML
    private void onStop(ActionEvent e) {
        // TODO: hook to your engine's stop API if available
        alertInfo("Not implemented", "Stop is not yet implemented in the UI.");
    }

    @FXML
    private void onResume(ActionEvent e) {
        // TODO: hook to your engine's resume API if available
        alertInfo("Not implemented", "Resume is not yet implemented in the UI.");
    }

    @FXML
    private void onStepOver(ActionEvent e) {
        // TODO: hook to your engine's step-over API if available
        alertInfo("Not implemented", "Step over is not yet implemented in the UI.");
    }

    @FXML
    private void onStepBack(ActionEvent e) {
        // TODO: hook to your engine's step-back API if available
        alertInfo("Not implemented", "Step backward is not yet implemented in the UI.");
    }

    private void refreshButtonsEnabled() {
        boolean loaded = (engine != null && engine.hasProgramLoaded());
        if (btnRun != null) btnRun.setDisable(!loaded);
        if (btnDebug != null) btnDebug.setDisable(!loaded);
        if (btnStop != null) btnStop.setDisable(!loaded);
        if (btnResume != null) btnResume.setDisable(!loaded);
        if (btnStepOver != null) btnStepOver.setDisable(!loaded);
        if (btnStepBack != null) btnStepBack.setDisable(!loaded);
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
}
