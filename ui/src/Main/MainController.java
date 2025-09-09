package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import ProgramToolBar.ProgramToolbarController;
import InstructionsTable.InstructionsTableController;
import SummaryLine.SummaryLineController;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import emulator.api.dto.InstructionView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainController {
    @FXML private HeaderAndLoadButtonController headerController;
    @FXML private ProgramToolbarController toolbarController;
    @FXML private InstructionsTableController instructionsController;
    @FXML private SummaryLineController summaryLineController;
    @FXML private SelectedInstructionHistoryChainTable.SelectedInstructionHistoryChainTableController historyChainController;
    @FXML private VBox contentBox;
    @FXML private Node toolbar;
    @FXML private TextArea centerOutput;

    private EmulatorEngine engine;
    private int currentDegree = 0;

    @FXML
    private void initialize() {
        headerController.setOnLoaded(this::onProgramLoaded);
        toolbarController.setOnExpand(this::onExpandOne);
        toolbarController.setOnCollapse(this::onCollapseOne);
        instructionsController.setOnRowSelected(this::onExpandedRowSelected);
        toolbarController.bindDegree(0, 0);

        Platform.runLater(() -> {
            contentBox.setMinWidth(0);                         // allow shrinking
            contentBox.setPrefWidth(Region.USE_COMPUTED_SIZE);

            // Track width of the toolbar
            DoubleBinding toolbarContentW = Bindings.createDoubleBinding(
                    () -> toolbar.getBoundsInParent().getWidth(),
                    toolbar.boundsInParentProperty()
            );

            // Match toolbar width EXACTLY
            contentBox.prefWidthProperty().bind(toolbarContentW);
            contentBox.maxWidthProperty().bind(toolbarContentW);
        });
    }

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        headerController.setEngine(engine);
        summaryLineController.setEngine(engine);
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

    private void onExpandedRowSelected(emulator.api.dto.InstructionView selected) {
        try {
            if (currentDegree <= 0 || selected == null) { historyChainController.clear(); return; }
            ProgramView pvOriginal = engine.programView(0);
            historyChainController.showForSelected(selected, pvOriginal);
        } catch (Exception e) {
            historyChainController.clear();
        }
    }

    private void render(int degree) {
        try {
            var pv = engine.programView(degree);
            instructionsController.update(pv);
            summaryLineController.update(pv);
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
