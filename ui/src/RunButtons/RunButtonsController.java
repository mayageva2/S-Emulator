package RunButtons;

import VariablesBox.VariablesBoxController;
import cyclesLine.CyclesLineController;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.event.ActionEvent;

import java.util.*;
import java.util.function.Function;

public class RunButtonsController {
    @FXML private Button btnRun;
    @FXML private Button btnDebug;
    @FXML private Button btnStop;
    @FXML private Button btnResume;
    @FXML private Button btnStepOver;
    @FXML private Button btnStepBack;

    private EmulatorEngine engine;
    private int currentDegree = 0;
    private int lastMaxDegree = 0;

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
    }

    @FXML
    private void onRun(ActionEvent e) {
        if (engine == null || !engine.hasProgramLoaded()) {
            alertInfo("No program loaded", "Use 'Load program XML' first.");
            return;
        }

        int max = (lastMaxDegree > 0) ? lastMaxDegree : 0;
        if (max == 0) {
            try { max = engine.programView(0).maxDegree(); } catch (Exception ignored) {}
        }
        int degree = Math.max(0, Math.min(currentDegree, max));

        try {
            ProgramView pv = engine.programView(degree);
            Long[] inputs = promptInputsAsCsv(pv);
            RunResult result = engine.run(degree, inputs);
            String rendered = (degree == 0 ? basicRenderer.apply(pv) : provenanceRenderer.apply(pv));

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

    private VariablesBoxController varsBoxController;
    public void setVarsBoxController(VariablesBoxController c) {
        this.varsBoxController = c;
    }

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
