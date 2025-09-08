package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import ProgramToolBar.ProgramToolbarController;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import emulator.api.dto.InstructionView;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainController {
    @FXML private HeaderAndLoadButtonController headerController;
    @FXML private ProgramToolbarController toolbarController;
    @FXML private InstructionsTable.InstructionsTableController instructionsController;
    @FXML private TextArea centerOutput;

    private EmulatorEngine engine;
    private int currentDegree = 0;

    @FXML
    private void initialize() {
        headerController.setOnLoaded(this::onProgramLoaded);
        toolbarController.setOnExpand(this::onExpandOne);
        toolbarController.setOnCollapse(this::onCollapseOne);
        toolbarController.bindDegree(0, 0);
    }

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        headerController.setEngine(engine);
    }

    private boolean isLoaded() {
        try {
            engine.programView(0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void onProgramLoaded(HeaderAndLoadButtonController.LoadedEvent ev) {
        currentDegree = 0;
        toolbarController.bindDegree(0, ev.maxDegree());
        render(0);
    }

    private void onExpandOne() {
        if (!isLoaded()) return;
        int max = engine.programView(0).maxDegree();
        if (currentDegree < max) {
            currentDegree++;
            toolbarController.bindDegree(currentDegree, max);
            render(currentDegree);
        }
    }

    private void onCollapseOne() {
        if (!isLoaded()) return;
        int max = engine.programView(0).maxDegree();
        if (currentDegree > 0) {
            currentDegree--;
            toolbarController.bindDegree(currentDegree, max);
            render(currentDegree);
        }
    }

    private void render(int degree) {
        try {
            ProgramView pv = engine.programView(degree);
            instructionsController.update(pv);
        } catch (Exception e) {
            if (centerOutput != null) centerOutput.setText("Render failed: " + e.getMessage());
        }
    }

    private String formatProgramView(ProgramView pv) {
        StringBuilder sb = new StringBuilder();
        sb.append("Program: ").append(nz(pv.programName())).append('\n');
        sb.append("Degree: ").append(pv.degree()).append(" / ").append(pv.maxDegree()).append('\n');
        sb.append("Instructions: ").append(pv.instructions().size()).append('\n');
        sb.append('\n');

        for (InstructionView iv : pv.instructions()) {
            sb.append(String.format(Locale.ROOT,
                    "#%-3d (%s) [%-4s]  %s  (%d)",
                    iv.index(),
                    iv.basic() ? "B" : "S",
                    nz(iv.label()),
                    prettyOpcode(iv.opcode(), iv.args()),
                    iv.cycles()
            ));

            List<Integer> chain = iv.createdFromChain();
            if (pv.degree() > 0 && chain != null && !chain.isEmpty()) {
                sb.append("  <<<  ").append(chain.stream().map(Object::toString).collect(Collectors.joining(" -> ")));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String prettyOpcode(String opcode, List<String> args) {
        if (args == null || args.isEmpty()) return nz(opcode);
        return nz(opcode) + " " + String.join(", ", args);
    }

    private String nz(String s) { return s == null ? "" : s; }
}
