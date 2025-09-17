package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import ProgramToolBar.ProgramToolbarController;
import InstructionsTable.InstructionsTableController;
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

import java.util.*;
import java.util.function.Consumer;
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
    @FXML private StatisticsTable.StatisticsTableController statisticsTableController;

    @FXML private VBox contentBox;
    @FXML private VBox historyChainBox;
    @FXML private VBox statisticsBox;
    @FXML private HBox sidePanels;
    @FXML private BorderPane varsBox;
    @FXML private BorderPane inputsBox;

    @FXML private Node toolbar;
    @FXML private Region instructions;
    @FXML private Region summaryLine;
    @FXML private Region historyChain;
    @FXML private Region RunButtons;
    @FXML private TextArea centerOutput;

    private static final String MAIN_PSEUDO = "Main Program";

    private EmulatorEngine engine;
    private List<String> inputNames = java.util.Collections.emptyList();
    private int currentDegree = 0;
    private Consumer<String> onHighlightChanged;

    @FXML
    private void initialize() {
        headerController.setOnLoaded(this::onProgramLoaded);
        toolbarController.setOnExpand(this::onExpandOne);
        toolbarController.setOnCollapse(this::onCollapseOne);
        instructionsController.setOnRowSelected(this::onExpandedRowSelected);
        toolbarController.setOnJumpToDegree(this::onJumpToDegree);
        toolbarController.bindDegree(0, 0);
        toolbarController.setHighlightEnabled(false);
        toolbarController.setDegreeButtonEnabled(false);

        if (RunButtonsController != null && statisticsTableController != null) {
            RunButtonsController.setStatisticsTableController(statisticsTableController);
            RunButtonsController.setInputsFormatter(this::formatInputsByPosition);
        }

        Platform.runLater(() -> {
            // allow shrinking
            contentBox.setMinWidth(0);
            contentBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
            historyChainBox.setMinWidth(0);
            historyChainBox.setPrefWidth(Region.USE_COMPUTED_SIZE);

            varsBox.setMinWidth(0);
            inputsBox.setMinWidth(0);
            varsBox.setMaxWidth(Double.MAX_VALUE);
            inputsBox.setMaxWidth(Double.MAX_VALUE);

            // Track width of the toolbar (kept in case you need it later)
            DoubleBinding toolbarContentW = Bindings.createDoubleBinding(
                    () -> toolbar.getBoundsInParent().getWidth(),
                    toolbar.boundsInParentProperty()
            );

            // Children widths follow their parent containers
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
                historyChainBox.prefWidthProperty().bind(contentBox.widthProperty());
            }

            // Keep right column aligned with RunButtons width (preserves your separation)
            if (RunButtons != null && sidePanels != null) {
                sidePanels.setMinWidth(0);
                sidePanels.maxWidthProperty().bind(RunButtons.widthProperty());
            }
            if (RunButtons != null && statisticsBox != null) {
                statisticsBox.setMinWidth(0);
                statisticsBox.maxWidthProperty().bind(RunButtons.widthProperty());
                statisticsBox.prefWidthProperty().bind(sidePanels.widthProperty());
            }

            // Let both columns participate in resizing
            HBox.setHgrow(contentBox, Priority.ALWAYS);
            HBox.setHgrow(sidePanels, Priority.ALWAYS);

            // Inside the right column, allow both panels to grow/shrink
            HBox.setHgrow(varsBox, Priority.ALWAYS);
            HBox.setHgrow(inputsBox, Priority.ALWAYS);

            // Even 50/50 split inside sidePanels that updates on resize
            var half = sidePanels.widthProperty()
                    .subtract(sidePanels.getSpacing())
                    .divide(2);
            varsBox.prefWidthProperty().bind(half);
            inputsBox.prefWidthProperty().bind(half);
        });
    }

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        headerController.setEngine(engine);
        summaryLineController.setEngine(engine);

        if (RunButtonsController != null) {
            RunButtonsController.setEngine(engine);
            RunButtonsController.setVarsBoxController(varsBoxController);

            if (statisticsTableController != null) {
                RunButtonsController.setStatisticsTableController(statisticsTableController);
                RunButtonsController.setInputsFormatter(this::formatInputsByPosition);
            }
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
        toolbarController.setDegreeButtonEnabled(true);
        if (RunButtonsController != null) {
            RunButtonsController.setEngine(engine);
            RunButtonsController.setLastMaxDegree(ev.maxDegree());
            RunButtonsController.setCurrentDegree(0);
            if (inputsBoxController != null) {
                RunButtonsController.setInputsBoxController(inputsBoxController);
            }
        }

        // Build the Inputs UI immediately after load, using engine.extractInputVars(pv)
        Platform.runLater(() -> {
            try {
                ProgramView pv0 = engine.programView(0);
                List<String> quotedFns = extractQuotedFunctionNames(pv0);
                List<String> varNames = engine.extractInputVars(pv0);
                List<String> labels = pv0.instructions().stream()
                        .map(InstructionView::label)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList();

                this.inputNames = (varNames != null) ? varNames : Collections.emptyList();
                Set<String> available = new HashSet<>(engine.availablePrograms());
                quotedFns = quotedFns.stream().filter(available::contains).sorted(String.CASE_INSENSITIVE_ORDER).toList();
                List<String> targets = new ArrayList<>();
                targets.add(MAIN_PSEUDO);
                targets.addAll(quotedFns);

                List<String> choices = new ArrayList<>();
                if (varNames != null) choices.addAll(varNames);
                choices.addAll(labels);

                toolbarController.setPrograms(targets);
                toolbarController.setOnProgramSelected(this::onProgramPicked);
                toolbarController.setHighlightOptions(choices);
                toolbarController.setHighlightEnabled(true);

                toolbarController.setOnHighlightChanged(term -> {
                    if (instructionsController != null) {
                        instructionsController.setHighlightTerm(term);
                    }
                });

                if (inputsBoxController != null) {
                    inputsBoxController.showNames(varNames);
                    System.out.println("Main extracted inputs: " + varNames);
                }
            } catch (Exception ex) {
                System.err.println("Inputs/highlight setup failed: " + ex.getMessage());
                toolbarController.setHighlightOptions(Collections.emptyList());
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
            if (RunButtonsController != null) RunButtonsController.setCurrentDegree(currentDegree);
            render(currentDegree);
        }
    }

    private void onCollapseOne() {
        if (!isLoaded()) return;
        int max = engine.programView(0).maxDegree();
        if (currentDegree > 0) {
            currentDegree--;
            toolbarController.bindDegree(currentDegree, max);
            if (RunButtonsController != null) RunButtonsController.setCurrentDegree(currentDegree);
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

    private void onProgramPicked(String name) {
        if (name == null) return;
        try {
            if (MAIN_PSEUDO.equals(name)) {
                int max = engine.programView(0).maxDegree();
                currentDegree = Math.min(currentDegree, max);
                toolbarController.bindDegree(currentDegree, max);
                render(currentDegree);
            } else {
                // Show function view at the same degree (clamped to that function’s max)
                int fMax = engine.programView(name, 0).maxDegree();
                int deg = Math.min(currentDegree, fMax);
                toolbarController.bindDegree(deg, fMax);
                // re-render using the overload that targets a function:
                var pv = engine.programView(name, deg);
                instructionsController.update(pv);
                summaryLineController.update(pv);
                // provenance panel is only meaningful vs original program at deg>0:
                if (deg > 0) historyChainController.clear(); // or adapt if you keep provenance per function
            }
        } catch (Exception ex) {
            // fall back to main if anything goes wrong
            int max = engine.programView(0).maxDegree();
            currentDegree = Math.min(currentDegree, max);
            toolbarController.bindDegree(currentDegree, max);
            render(currentDegree);
        }
    }

    private void onJumpToDegree(Integer target) {
        if (target == null || !isLoaded()) return;

        int max = engine.programView(0).maxDegree();
        // clamp (just in case)
        int clamped = Math.max(0, Math.min(target, max));

        currentDegree = clamped;
        toolbarController.bindDegree(currentDegree, max);

        if (RunButtonsController != null) {
            RunButtonsController.setCurrentDegree(currentDegree);
            RunButtonsController.setLastMaxDegree(max);
        }

        render(currentDegree);
        // optional: refresh provenance panel if a row is selected
        if (currentDegree > 0 && instructionsController != null) {
            var sel = instructionsController.getTableView().getSelectionModel().getSelectedItem();
            if (sel != null && sel.sourceIv != null) {
                onExpandedRowSelected(sel.sourceIv);
            } else {
                historyChainController.clear();
            }
        } else {
            historyChainController.clear();
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

    private String formatInputsByPosition(String csv) {
        if (csv == null || csv.isBlank()) return "";
        String[] vals = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            String val = vals[i].trim();
            String name;
            if (inputNames != null && i < inputNames.size() && inputNames.get(i) != null && !inputNames.get(i).isBlank()) {
                name = inputNames.get(i);
            } else {
                name = "x" + (i + 1);
            }
            if (i > 0) sb.append('\n');
            sb.append(name).append(" = ").append(val);
        }
        return sb.toString();
    }

    private List<String> extractQuotedFunctionNames(ProgramView pv) {
        Set<String> out = new LinkedHashSet<>();
        for (var iv : pv.instructions()) {
            String op = String.valueOf(iv.opcode());
            if ("QUOTE".equals(op) || "JUMP_EQUAL_FUNCTION".equals(op)) {
                String fn = getArg(iv.args(), "functionName");
                if (fn != null && !fn.isBlank()) out.add(fn);
                // also scan functionArguments for heads like "(Name,...)" and "(Name,(...))"
                String fargs = getArg(iv.args(), "functionArguments");
                out.addAll(extractHeadsFromCallString(fargs));
            }
        }
        return new ArrayList<>(out);
    }

    private Set<String> extractHeadsFromCallString(String s) {
        Set<String> out = new LinkedHashSet<>();
        if (s == null || s.isBlank()) return out;
        int n = s.length(), i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '(') {
                i++;
                int start = i;
                while (i < n && s.charAt(i) != ',' && s.charAt(i) != ')') i++;
                String head = s.substring(start, i).trim();
                if (!head.isEmpty()) out.add(head);
            } else i++;
        }
        return out;
    }

    private String getArg(java.util.List<String> args, String key) {
        if (args == null) return null;
        for (String a : args) {
            if (a == null) continue;
            int eq = a.indexOf('=');
            if (eq > 0 && a.substring(0, eq).equals(key)) {
                return a.substring(eq + 1).trim();
            }
        }
        return null;
    }

    public void setOnHighlightChanged(Consumer<String> c) { this.onHighlightChanged = c; }

    private String nz(String s) { return s == null ? "" : s; }
}
