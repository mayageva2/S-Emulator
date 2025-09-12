package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import ProgramToolBar.ProgramToolbarController;
import InstructionsTable.InstructionsTableController;
import StatisticsTable.StatisticsTableController;
import SummaryLine.SummaryLineController;
import VariablesBox.VariablesBoxController;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import emulator.api.dto.InstructionView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;

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
    @FXML private RunButtons.RunButtonsController RunButtonsController;
    @FXML private VariablesBoxController varsBoxController;
    @FXML private InputsBox.InputsBoxController inputsBoxController;
    @FXML private StatisticsTable.StatisticsTableController statisticsController;
    @FXML private VBox contentBox;
    @FXML private VBox historyChainBox;
    @FXML private VBox statisticsBox;
    @FXML private HBox sidePanels;
    @FXML private BorderPane varsBox;
    @FXML private Node toolbar;
    @FXML private Region instructions;
    @FXML private Region summaryLine;
    @FXML private Region historyChain;
    @FXML private Region RunButtons;
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
            // allow shrinking
            contentBox.setMinWidth(0);
            contentBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
            historyChainBox.setMinWidth(0);
            historyChainBox.setPrefWidth(Region.USE_COMPUTED_SIZE);

            // Track width of the toolbar
            DoubleBinding toolbarContentW = Bindings.createDoubleBinding(
                    () -> toolbar.getBoundsInParent().getWidth(),
                    toolbar.boundsInParentProperty()
            );

            // Match toolbar width EXACTLY
            contentBox.prefWidthProperty().bind(toolbarContentW);
            contentBox.maxWidthProperty().bind(toolbarContentW);
            historyChainBox.prefWidthProperty().bind(toolbarContentW);
            historyChainBox.maxWidthProperty().bind(toolbarContentW);

            if (instructions != null) {
                instructions.setMinWidth(0);
                instructions.prefWidthProperty().bind(contentBox.widthProperty());
                instructions.maxWidthProperty().bind(contentBox.widthProperty());
            }
            if (summaryLine != null) {
                summaryLine.setMinWidth(0);
                summaryLine.prefWidthProperty().bind(contentBox.widthProperty());
                summaryLine.maxWidthProperty().bind(contentBox.widthProperty());
            }
            if (historyChain != null) {
                historyChain.setMinWidth(0);
                historyChain.prefWidthProperty().bind(historyChainBox.widthProperty());
                historyChain.maxWidthProperty().bind(historyChainBox.widthProperty());
            }
            if (RunButtons != null && sidePanels != null) {
                sidePanels.setMinWidth(0);
                sidePanels.prefWidthProperty().bind(RunButtons.widthProperty());
                sidePanels.maxWidthProperty().bind(RunButtons.widthProperty());
            }
            if(RunButtons != null && statisticsBox != null) {
                statisticsBox.setMinWidth(0);
                statisticsBox.prefWidthProperty().bind(RunButtons.widthProperty());
                statisticsBox.maxWidthProperty().bind(RunButtons.widthProperty());
            }
            HBox.setHgrow(contentBox, Priority.ALWAYS);
            HBox.setHgrow(sidePanels, Priority.NEVER);
            VBox.setVgrow(statisticsBox, Priority.NEVER);
        });
    }

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        headerController.setEngine(engine);
        summaryLineController.setEngine(engine);

        if (RunButtonsController != null) {
            RunButtonsController.setEngine(engine);
            RunButtonsController.setVarsBoxController(varsBoxController);
        }

        Platform.runLater(() -> {
            if (RunButtonsController != null && inputsBoxController != null) {
                RunButtonsController.setInputsBoxController(inputsBoxController);
            }
        });
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
        if (RunButtonsController != null) {
            RunButtonsController.setEngine(engine);
            RunButtonsController.setLastMaxDegree(ev.maxDegree());
            if (inputsBoxController != null) {
                RunButtonsController.setInputsBoxController(inputsBoxController);
            }
        }

        // Build the Inputs UI immediately after load, using engine.extractInputVars(pv)
        Platform.runLater(() -> {
            try {
                ProgramView pv0 = engine.programView(0);
                if (inputsBoxController != null) {
                    // THIS mirrors your console code path:
                    java.util.List<String> names = engine.extractInputVars(pv0); // <- trusted API
                    inputsBoxController.showNames(names);                        // <- build rows now
                    System.out.println("Main extracted inputs: " + names);
                }
            } catch (Exception ex) {
                System.err.println("Inputs panel setup failed: " + ex.getMessage());
            }
        });

        render(0);
    }

    private static List<String> extractInputNames(ProgramView pv) {
        try {
            for (String mname : new String[]{"inputNames", "getInputNames", "inputs", "getInputs"}) {
                try {
                    var m = pv.getClass().getMethod(mname);
                    Object obj = m.invoke(pv);
                    if (obj instanceof java.util.List<?> list && !list.isEmpty()) {
                        java.util.List<String> out = new java.util.ArrayList<>(list.size());
                        for (Object o : list) out.add(String.valueOf(o));
                        return out;
                    }
                } catch (NoSuchMethodException ignore) { /* try next */ }
            }
        } catch (ReflectiveOperationException ex) {
            System.err.println("MainController.extractInputNames failed: " + ex);
        }
        return java.util.Collections.emptyList();
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
