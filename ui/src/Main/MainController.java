package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import ProgramToolBar.ProgramToolbarController;
import InstructionsTable.InstructionsTableController;
import StatisticsCommands.RerunSpec;
import SummaryLine.SummaryLineController;
import VariablesBox.VariablesBoxController;
import emulator.api.EmulatorEngine;
import emulator.api.dto.ProgramView;
import emulator.api.dto.InstructionView;
import emulator.api.dto.RunRecord;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
    @FXML private StatisticsCommands.StatisticsCommandsController statisticsCommandsController;

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
    @FXML private Region RunButtons;
    @FXML private TextArea centerOutput;

    private static final String MAIN_PSEUDO = "Main Program";
    private final Map<String,String> programDisplayToInternal = new LinkedHashMap<>();
    private String currentTargetProgramInternal = null;

    private EmulatorEngine engine;
    private List<String> inputNames = Collections.emptyList();
    private int currentDegree = 0;
    private Consumer<String> onHighlightChanged;

    private String getCurrentTargetProgramInternal() {
        return currentTargetProgramInternal;
    }

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
            sidePanels.setMinWidth(0);
            historyChainBox.setMinWidth(0);
            statisticsBox.setMinWidth(0);
            HBox.setHgrow(contentBox, Priority.ALWAYS);
            HBox.setHgrow(sidePanels, Priority.ALWAYS);
            HBox.setHgrow(historyChainBox, Priority.ALWAYS);
            HBox.setHgrow(statisticsBox, Priority.ALWAYS);

            varsBox.setMinWidth(0);
            inputsBox.setMinWidth(0);
            varsBox.setMaxWidth(Double.MAX_VALUE);
            inputsBox.setMaxWidth(Double.MAX_VALUE);

            // Children widths follow their parent containers
            if (instructions != null) {
                instructions.setMinWidth(0);
                instructions.prefWidthProperty().bind(leftCol.widthProperty());
                instructions.maxWidthProperty().bind(leftCol.widthProperty());
            }
            if (summaryLine != null) {
                summaryLine.setMinWidth(0);
                summaryLine.prefWidthProperty().bind(leftCol.widthProperty());
                summaryLine.maxWidthProperty().bind(leftCol.widthProperty());
            }
            if (historyChain != null) {
                historyChain.setMinWidth(0);
                historyChain.prefWidthProperty().bind(leftCol.widthProperty());
                historyChain.maxWidthProperty().bind(leftCol.widthProperty());
            }

            sidePanels.prefWidthProperty().bind(rightCol.widthProperty());
            statisticsBox.prefWidthProperty().bind(rightCol.widthProperty());

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

        if (instructionsController != null) {
            instructionsController.setFunctionNameResolver(this::displayForProgram);
        }
    }

    public void setEngine(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        headerController.setEngine(engine);
        summaryLineController.setEngine(engine);
        wireStatisticsCommands();

        if (RunButtonsController != null) {
            RunButtonsController.setEngine(engine);
            RunButtonsController.setVarsBoxController(varsBoxController);
            RunButtonsController.setInstructionsController(instructionsController);
            RunButtonsController.setProgramToolbarController(toolbarController);
            RunButtonsController.setSelectedProgramSupplier(this::getCurrentTargetProgramInternal);

            if (statisticsTableController != null) {
                RunButtonsController.setStatisticsTableController(statisticsTableController);
                RunButtonsController.setInputsFormatter(this::formatInputsByPosition);
            }
        }

        if (statisticsCommandsController != null) {
            statisticsCommandsController.setStatusSupplier(() -> {
                if (statisticsTableController != null && RunButtonsController != null) {
                    var idxOpt = statisticsTableController.getSelectedHistoryIndex();
                    if (idxOpt.isPresent()) {
                        Map<String, String> m = RunButtonsController.getVarsSnapshotForIndex(idxOpt.getAsInt());
                        if (m != null && !m.isEmpty()) return m;
                    }
                }
                if (RunButtonsController != null) {
                    Map<String, String> last = RunButtonsController.getLastRunVarsSnapshot();
                    if (last != null && !last.isEmpty()) return last;
                }
                if (engine != null) {
                    Map<String, Long> lm = engine.lastRunVars();
                    if (lm != null && !lm.isEmpty()) {
                        Map<String, String> asStrings = new LinkedHashMap<>();
                        lm.forEach((k,v) -> asStrings.put(k, String.valueOf(v)));
                        return asStrings;
                    }
                }
                return Collections.emptyMap();
            });
        }

        Platform.runLater(() -> {
            if (RunButtonsController != null && inputsBoxController != null) {
                RunButtonsController.setInputsBoxController(inputsBoxController);
            }
            if (headerController != null && inputsBoxController != null) {
                headerController.setInputController(inputsBoxController);
            }
        });
    }

    private int safeDegreeForProgram(String programName, int requested) {
        try {
            var pv0 = engine.programView(programName, 0);
            int max = pv0.maxDegree();
            return Math.max(0, Math.min(requested, max));
        } catch (Exception e) {
            var pv0 = engine.programView(0);
            int max = pv0.maxDegree();
            return Math.max(0, Math.min(requested, max));
        }
    }

    private void resetForNewRunUI() {
        try {
            if (instructionsController != null) {
                try { instructionsController.clearHighlight(); } catch (Throwable ignore) {}
                try { instructionsController.clearSelection(); } catch (Throwable ignore) {}
            }
            if (historyChainController != null) {
                try { historyChainController.clear(); } catch (Throwable ignore) {}
            }
            if (varsBoxController != null) {
                try { varsBoxController.clearForNewRun(); } catch (Throwable ignore) {}
            }
            if (centerOutput != null) {
                centerOutput.clear();
            }
            if (statisticsTableController != null) {
                try { statisticsTableController.clearSelection(); } catch (Throwable ignore) {}
            }
            if (RunButtonsController != null) {
                try { RunButtonsController.resetForNewRun(); } catch (Throwable ignore) {}
            }
            if (summaryLineController != null) {
                try { summaryLineController.clear(); } catch (Throwable ignore) {}
            }
        } catch (Exception ignored) {}
    }

    private void showProgramView(ProgramView pv) {
        if (instructionsController != null) {
            instructionsController.setProgramView(pv);
        }
        if (summaryLineController != null) {
            summaryLineController.bindTo(pv);
        }
    }
    private void prefillInputs(List<Long> inputs) {
        try {
            var inputCtl = headerController.getInputController();
            if (inputCtl != null) {
                inputCtl.setInputs(inputs);
            }
        } catch (Exception ignored) {}
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
        resetForNewLoadUI();
        currentTargetProgramInternal = null;
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
                List<String> XVars = engine.extractInputVars(pv0);
                List<String> labels = pv0.instructions().stream()
                        .map(InstructionView::label)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList();
                VarScan scan = scanZsAndY(pv0);

                this.inputNames = (XVars != null) ? XVars : Collections.emptyList();

                Set<String> available = new HashSet<>(engine.availablePrograms());
                Set<String> allFns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                allFns.addAll(available);
                allFns.addAll(quotedFns);

                try {
                    var qrObj = engine.getClass().getMethod("getQuotationRegistry").invoke(engine);
                    if (qrObj instanceof emulator.logic.instruction.quote.QuotationRegistry reg) {
                        allFns.addAll(reg.allNames());
                    }
                } catch (Exception ignore) {}

                programDisplayToInternal.clear();
                List<String> displays = new ArrayList<>();
                displays.add(MAIN_PSEUDO);
                Set<String> usedDisplays = new HashSet<>();

                for (String internal : allFns) {
                    String disp = displayForProgram(internal);
                    if (disp == null || disp.isBlank()) continue;

                    if (!usedDisplays.add(disp)) continue;
                    programDisplayToInternal.put(disp, internal);
                    displays.add(disp);

                    try {
                        var pv = engine.programView(internal, 0);
                        String us = null;
                        for (String mname : List.of("userString", "user-string", "getUserString", "programUserString")) {
                            try {
                                var m = pv.getClass().getMethod(mname);
                                Object v = m.invoke(pv);
                                if (v != null && !String.valueOf(v).isBlank()) {
                                    us = String.valueOf(v).trim();
                                    break;
                                }
                            } catch (NoSuchMethodException ignored) {}
                        }

                        if (us != null && !us.isBlank() && !usedDisplays.contains(us)) {
                            programDisplayToInternal.put(us, internal);
                            displays.add(us);
                            usedDisplays.add(us);
                        }
                    } catch (Exception ignored) {}
                }


                List<String> choices = new ArrayList<>();
                if (XVars != null) choices.addAll(XVars);
                if (scan.hasY) choices.add("y");
                choices.addAll(scan.zs);
                choices.addAll(labels);

                toolbarController.setPrograms(displays);
                toolbarController.setOnProgramSelected(this::onProgramPicked);
                toolbarController.setHighlightOptions(choices);
                toolbarController.setHighlightEnabled(true);

                toolbarController.setOnHighlightChanged(term -> {
                    if (instructionsController != null) {
                        instructionsController.setHighlightTerm(term);
                    }
                });

                if (inputsBoxController != null) {
                    inputsBoxController.showNames(XVars);
                    System.out.println("Main extracted inputs: " + XVars);
                }
            } catch (Exception ex) {
                System.err.println("Inputs/highlight setup failed: " + ex.getMessage());
                toolbarController.setHighlightOptions(Collections.emptyList());
            }
        });

        render(0);
    }

    private static final Pattern Z_TOKEN = Pattern.compile("\\bz(?:[1-9]\\d*)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern Y_TOKEN = Pattern.compile("\\by\\b", Pattern.CASE_INSENSITIVE);

    private static class VarScan {
        final List<String> zs;
        final boolean hasY;
        VarScan(List<String> zs, boolean hasY) { this.zs = zs; this.hasY = hasY; }
    }

    private VarScan scanZsAndY(ProgramView pv) {
        // preserve insertion order + uniqueness
        Set<String> zs = new LinkedHashSet<>();
        boolean hasY = false;

        for (InstructionView iv : pv.instructions()) {
            List<String> args = iv.args();
            if (args == null) continue;
            for (String a : args) {
                if (a == null) continue;

                // collect z and zN
                var mz = Z_TOKEN.matcher(a);
                while (mz.find()) zs.add(mz.group().toLowerCase());

                // detect a plain 'y'
                if (!hasY && Y_TOKEN.matcher(a).find()) hasY = true;
            }
        }

        // sort z, z1, z2, z10… (bare z before numbered ones)
        List<String> sortedZs = zs.stream()
                .sorted((a, b) -> {
                    int na = (a.equalsIgnoreCase("z")) ? 0 : numSuffix(a);
                    int nb = (b.equalsIgnoreCase("z")) ? 0 : numSuffix(b);
                    return Integer.compare(na, nb);
                })
                .toList();

        return new VarScan(sortedZs, hasY);
    }

    private static List<Long> parseCsvAsLongs(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        String[] toks = csv.split(",");
        List<Long> out = new ArrayList<>(toks.length);
        for (String t : toks) {
            String s = t == null ? "" : t.trim();
            if (s.isEmpty()) out.add(0L);
            else try { out.add(Long.parseLong(s)); } catch (NumberFormatException nfe) { out.add(0L); }
        }
        return out;
    }

    private static boolean hasInputsList(RunRecord r) {
        try { r.getClass().getMethod("inputs"); return true; } catch (NoSuchMethodException e) { return false; }
    }

    @SuppressWarnings("unchecked")
    private static List<Long> getInputsList(RunRecord r) {
        try { return (List<Long>) r.getClass().getMethod("inputs").invoke(r); }
        catch (Exception e) { return List.of(); }
    }

    private int maxDegreeForCurrentSelection() {
        try {
            if (currentTargetProgramInternal == null) {
                return engine.programView(0).maxDegree();
            } else {
                return engine.programView(currentTargetProgramInternal, 0).maxDegree();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static int numSuffix(String s) {
        // assumes s starts with 'z' or 'Z'
        if (s.length() == 1) return 0;
        try { return Integer.parseInt(s.substring(1)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static List<String> extractInputNames(ProgramView pv) {
        try {
            for (String mname : new String[]{"inputNames", "getInputNames", "inputs", "getInputs"}) {
                try {
                    var m = pv.getClass().getMethod(mname);
                    Object obj = m.invoke(pv);
                    if (obj instanceof List<?> list && !list.isEmpty()) {
                        List<String> out = new ArrayList<>(list.size());
                        for (Object o : list) out.add(String.valueOf(o));
                        return out;
                    }
                } catch (NoSuchMethodException ignore) { /* try next */ }
            }
        } catch (ReflectiveOperationException ex) {
            System.err.println("MainController.extractInputNames failed: " + ex);
        }
        return Collections.emptyList();
    }

    private void onExpandOne() {
        if (!isLoaded()) return;
        int max = maxDegreeForCurrentSelection();
        if (currentDegree < max) {
            currentDegree++;
            toolbarController.bindDegree(currentDegree, max);
            if (RunButtonsController != null) RunButtonsController.setCurrentDegree(currentDegree);
            render(currentDegree);
        }
    }

    private void onCollapseOne() {
        if (!isLoaded()) return;
        int max = maxDegreeForCurrentSelection();
        if (currentDegree > 0) {
            currentDegree--;
            toolbarController.bindDegree(currentDegree, max);
            if (RunButtonsController != null) RunButtonsController.setCurrentDegree(currentDegree);
            render(currentDegree);
        }
    }

    private void onExpandedRowSelected(InstructionView selected) {
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
            if (currentTargetProgramInternal == null) {
                var pv = engine.programView(degree);
                instructionsController.update(pv);
                summaryLineController.update(pv);
                refreshHighlightOptions(pv);
            } else {
                    var pv = engine.programView(currentTargetProgramInternal, degree);
                    instructionsController.update(pv);
                    summaryLineController.update(pv);
                    refreshHighlightOptions(pv);
            }
        } catch (Exception e) {
            if (centerOutput != null) centerOutput.setText("Render failed: " + e.getMessage());
        }
    }

    private void onProgramPicked(String pickedDisplay) {
        if (pickedDisplay == null) return;
        String prevProgram = currentTargetProgramInternal;
        try {
            if (MAIN_PSEUDO.equals(pickedDisplay)) {
                currentTargetProgramInternal = null;
            } else {
                String internal = programDisplayToInternal.getOrDefault(pickedDisplay, pickedDisplay);
                if (internal == null) {
                    for (var e : programDisplayToInternal.entrySet()) {
                        if (pickedDisplay.equalsIgnoreCase(e.getKey())) {
                            internal = e.getValue();
                            break;
                        }
                    }
                }
                currentTargetProgramInternal = (internal != null) ? internal : pickedDisplay;
            }

            int max = maxDegreeForCurrentSelection();
            currentDegree = 0;
            toolbarController.bindDegree(currentDegree, max);
            if (RunButtonsController != null) {
                RunButtonsController.setCurrentDegree(0);
                RunButtonsController.setLastMaxDegree(max);
            }

            ProgramView pv = (currentTargetProgramInternal == null)
                    ? engine.programView(currentDegree)
                    : engine.programView(currentTargetProgramInternal, currentDegree);

            if (instructionsController != null) {
                instructionsController.setProgramView(pv);
            }
            if (summaryLineController != null) {
                summaryLineController.update(pv);
            }

            refreshHighlightOptions(pv);
            boolean programChanged = !Objects.equals(prevProgram, currentTargetProgramInternal);
            if (programChanged && historyChainController != null) {
                historyChainController.clear();
            }
            if (RunButtonsController != null) {
                String notifyProgram = (currentTargetProgramInternal == null)
                        ? engine.programView(0).programName()
                        : currentTargetProgramInternal;
                RunButtonsController.notifyProgramSelection(notifyProgram);
            }
        } catch (Exception ex) {
            currentTargetProgramInternal = null;
            int max = maxDegreeForCurrentSelection();
            currentDegree = Math.min(currentDegree, max);
            toolbarController.bindDegree(currentDegree, max);

            try {
                ProgramView pv = engine.programView(currentDegree);
                if (instructionsController != null) instructionsController.forceReload(pv);
                if (summaryLineController != null) summaryLineController.update(pv);
                refreshHighlightOptions(pv);
            } catch (Exception innerEx) {
                if (centerOutput != null)
                    centerOutput.setText("Failed to switch program: " + innerEx.getMessage());
            }
        }
    }

    private void onJumpToDegree(Integer target) {
        if (target == null || !isLoaded()) return;

        int max = maxDegreeForCurrentSelection();
        int clamped = Math.max(0, Math.min(target, max));
        if (clamped == currentDegree) return;
        currentDegree = clamped;
        toolbarController.bindDegree(currentDegree, max);

        if (RunButtonsController != null) {
            RunButtonsController.setCurrentDegree(currentDegree);
            RunButtonsController.setLastMaxDegree(max);
        }

        render(currentDegree);

        if (instructionsController != null) {
            var sel = instructionsController.getTableView().getSelectionModel().getSelectedItem();
            if (sel != null && sel.sourceIv != null) {
                try {
                    ProgramView pvOriginal = engine.programView(0);
                    historyChainController.showForSelected(sel.sourceIv, pvOriginal);
                } catch (Exception e) {
                }
            }
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
        String[] vals = Arrays.stream(csv.split(",")).map(String::trim).toArray(String[]::new);
        List<String> names = (inputsBoxController != null) ? inputsBoxController.getInputNames() : Collections.emptyList();

        if (names.isEmpty()) {  return String.join(", ", vals); }
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            int idx = parseXIndex(name);
            if (idx < 1) continue;
            String value = (idx - 1 < vals.length && !vals[idx - 1].isBlank()) ? vals[idx - 1] : "0";
            if (sb.length() > 0) sb.append('\n');
            sb.append(name).append(" = ").append(value);
        }
        return sb.toString();
    }

    private static int parseXIndex(String name) {
        if (name == null) return -1;
        String s = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (!s.startsWith("x")) return -1;
        try { return Integer.parseInt(s.substring(1)); }
        catch (Exception e) { return -1; }
    }

    private List<String> extractQuotedFunctionNames(ProgramView pv) {
        Set<String> out = new LinkedHashSet<>();
        for (var iv : pv.instructions()) {
            String op = String.valueOf(iv.opcode());
            if ("QUOTE".equals(op) || "JUMP_EQUAL_FUNCTION".equals(op)) {
                String fn = getArg(iv.args(), "functionName");
                if (fn != null && !fn.isBlank()) out.add(fn);
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

    private void refreshHighlightOptions(ProgramView pv) {
        // x* from engine (still OK to use engine helper)
        List<String> xs = engine.extractInputVars(pv);

        // labels from this view
        List<String> labels = pv.instructions().stream()
                .map(InstructionView::label)
                .filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty()).distinct().toList();

        // z / zN and a single y from this view (expanded degree!)
        VarScan scan = scanZsAndY(pv);

        // assemble list in your order
        List<String> choices = new ArrayList<>();
        if (xs != null) choices.addAll(xs);
        if (scan.hasY)  choices.add("y");
        choices.addAll(scan.zs);
        choices.addAll(labels);

        // keep previous selection if still present
        String prev = null; // add getters in toolbar if you want to preserve selection
        // prev = toolbarController.getSelectedHighlight();

        // de-dup while preserving order
        choices = new ArrayList<>(new LinkedHashSet<>(choices));

        toolbarController.setHighlightOptions(choices);
        // if (prev != null && choices.contains(prev)) toolbarController.setSelectedHighlight(prev);
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

    private String programUserString(String internalName) {
        if (internalName == null) return "";
        for (String mname : List.of(
                "user-string",
                "functionUserString",
                "programUserString",
                "userStringFor",
                "userString",
                "getUserString",
                "displayName"
        )) {
            try {
                var m = engine.getClass().getMethod(mname, String.class);
                Object v = m.invoke(engine, internalName);
                if (v != null) return String.valueOf(v);
            } catch (NoSuchMethodException ignore) {} catch (Exception ignore) {}
        }

        try {
            var pv = engine.programView(internalName, 0);
            for (String mname : List.of(
                    "user-string",
                    "programUserString",
                    "userString",
                    "getProgramUserString",
                    "displayName"
            )) {
                try {
                    var m = pv.getClass().getMethod(mname);
                    Object v = m.invoke(pv);
                    if (v != null) return String.valueOf(v);
                } catch (NoSuchMethodException ignore) {}
            }

            try {
                var m = pv.getClass().getMethod("programName");
                Object v = m.invoke(pv);
                if (v != null) {
                    String pn = String.valueOf(v);
                    if (!pn.isBlank() && !pn.equals(internalName)) {
                        return pn;
                    }
                }
            } catch (NoSuchMethodException ignore) {}
        } catch (Exception ignore) {}

        return internalName;
    }

    private String displayForProgram(String internalName) {
        String us = programUserString(internalName);
        return (us == null || us.isBlank()) ? internalName : us;
    }

    private void resetForNewLoadUI() {
        resetForNewRunUI();
        currentTargetProgramInternal = null;
        currentDegree = 0;
        programDisplayToInternal.clear();
        inputNames = Collections.emptyList();

        try { if (centerOutput != null) centerOutput.clear(); } catch (Throwable ignore) {}
        if (instructionsController != null) {
            try { instructionsController.clearHighlight(); } catch (Throwable ignore) {}
            try { instructionsController.clearSelection(); } catch (Throwable ignore) {}
            try { instructionsController.clear(); } catch (Throwable ignore) {}
        }
        if (summaryLineController != null) {
            try { summaryLineController.clear(); } catch (Throwable ignore) {}
        }
        if (historyChainController != null) {
            try { historyChainController.clear(); } catch (Throwable ignore) {}
        }
        if (varsBoxController != null) {
            try { varsBoxController.clearForNewRun(); } catch (Throwable ignore) {}
        }
        if (inputsBoxController != null) {
            try { inputsBoxController.showNames(Collections.emptyList()); } catch (Throwable ignore) {}
            try { inputsBoxController.setInputs(Collections.emptyList()); } catch (Throwable ignore) {}
        } else if (headerController != null && headerController.getInputController() != null) {
            try {
                headerController.getInputController().showNames(Collections.emptyList());
                headerController.getInputController().setInputs(Collections.emptyList());
            } catch (Throwable ignore) {}
        }
        if (statisticsTableController != null) {
            try { statisticsTableController.clearSelection(); } catch (Throwable ignore) {}
            try { statisticsTableController.clear(); } catch (Throwable ignore) {}
        }
        if (RunButtonsController != null) {
            try {
                RunButtonsController.clearStoredStatistics();
                RunButtonsController.resetForNewRun();
            } catch (Throwable ignore) {}
        }
        if (toolbarController != null) {
            try {
                toolbarController.setPrograms(List.of(MAIN_PSEUDO));
                toolbarController.setSelectedProgram(MAIN_PSEUDO);
                toolbarController.bindDegree(0, 0);
                toolbarController.setDegreeButtonEnabled(false);
                toolbarController.setHighlightOptions(Collections.emptyList());
                toolbarController.setHighlightEnabled(false);
            } catch (Throwable ignore) {}
        }
    }

    private void wireStatisticsCommands() {
        if (statisticsCommandsController == null) return;

        statisticsCommandsController.setStatusSupplier(() -> engine.lastRunVars()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, e -> String.valueOf(e.getValue()),
                        (a, b) -> b, LinkedHashMap::new)));

        statisticsCommandsController.setRerunSupplier(() -> {
            String selectedInternal = getCurrentTargetProgramInternal();
            String targetProgram = (selectedInternal != null) ? selectedInternal : safeMainName();

            try {
                if (statisticsTableController != null) {
                    var idxOpt = statisticsTableController.getSelectedHistoryIndex();
                    if (idxOpt.isPresent()) {
                        int idx = idxOpt.getAsInt();
                        var hist = engine.history(targetProgram);
                        if (idx >= 0 && idx < hist.size()) {
                            var r = hist.get(idx);
                            int deg = r.degree();
                            List<Long> inputs = (hasInputsList(r) ? getInputsList(r)
                                    : parseCsvAsLongs(r.inputsCsv()));
                            return new RerunSpec(targetProgram, deg, inputs);
                        }
                    }
                }
            } catch (Exception ignore) {}

            int deg = currentDegree;
            try {
                int max = (selectedInternal != null)
                        ? engine.programView(selectedInternal, 0).maxDegree()
                        : engine.programView(0).maxDegree();
                deg = Math.max(0, Math.min(deg, max));
            } catch (Exception ignore) {}

            List<Long> inputs = Optional.ofNullable(engine.lastRunInputs()).orElse(List.of());
            return new RerunSpec(targetProgram, deg, inputs);
        });

        statisticsCommandsController.setRerunPreparer(spec -> {
            int degree = safeDegreeForProgram(spec.programName(), spec.degree());
            var pv = engine.programView(spec.programName(), degree);
            resetForNewRunUI();
            Platform.runLater(() -> {
                showProgramView(pv);
                try {
                    int fMax = engine.programView(spec.programName(), 0).maxDegree();
                    currentDegree = degree;

                    if (toolbarController != null) {
                        toolbarController.bindDegree(degree, fMax);
                        String mainName = engine.programView(0).programName();
                        if (spec.programName().equalsIgnoreCase(mainName)) {
                            currentTargetProgramInternal = null;
                            toolbarController.setSelectedProgram(MAIN_PSEUDO);
                        } else {
                            currentTargetProgramInternal = spec.programName();
                            toolbarController.setSelectedProgram(displayForProgram(spec.programName()));
                        }
                        toolbarController.setHighlightEnabled(true);
                    }
                    if (RunButtonsController != null) {
                        RunButtonsController.setCurrentDegree(degree);
                        RunButtonsController.setLastMaxDegree(fMax);
                    }
                } catch (Exception ignore) {}
            });

            Platform.runLater(() -> {
                try {
                    if (inputsBoxController != null) inputsBoxController.setInputs(spec.inputs());
                    else if (headerController != null && headerController.getInputController() != null)
                        headerController.getInputController().setInputs(spec.inputs());
                } catch (Exception ignored) {}
            });
        });
    }

    private String safeMainName() {
        try {
            String n = engine.programView(0).programName();
            return (n == null || n.isBlank()) ? MAIN_PSEUDO : n;
        } catch (Exception e) {
            String n = engine.lastRunProgramName();
            return (n == null || n.isBlank()) ? MAIN_PSEUDO : n;
        }
    }


    public void setOnHighlightChanged(Consumer<String> c) { this.onHighlightChanged = c; }

    private String nz(String s) { return s == null ? "" : s; }
}
