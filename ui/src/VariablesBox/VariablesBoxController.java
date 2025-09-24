package VariablesBox;

import cyclesLine.CyclesLineController;
import emulator.api.dto.RunResult;
import emulator.api.dto.VarType;
import emulator.api.dto.VariableView;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VariablesBoxController {

    @FXML private TableView<VarRow> varsTable;
    @FXML private TableColumn<VarRow, String> colName;
    @FXML private TableColumn<VarRow, String> colValue;
    @FXML private CyclesLineController cyclesLineController;

    private final ObservableList<VarRow> rows = FXCollections.observableArrayList();
    private final Map<String, VarRow> byName = new ConcurrentHashMap<>();
    private final ObservableSet<String> changedNow = FXCollections.observableSet();
    private static final String CHANGED_CSS_CLASS = "current-instruction";

    @FXML
    private void initialize() {
        varsTable.setItems(rows);
        varsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colValue.setCellValueFactory(data -> data.getValue().valueProperty());

        colValue.setCellFactory(col -> new TableCell<VarRow, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    getStyleClass().remove(CHANGED_CSS_CLASS);
                    return;
                }
                setText(item);
                VarRow rowItem = (getTableRow() == null) ? null : getTableRow().getItem();
                String varName = (rowItem == null) ? null : rowItem.nameProperty().get();
                boolean isChanged = varName != null && changedNow.contains(varName);
                if (isChanged) {
                    if (!getStyleClass().contains(CHANGED_CSS_CLASS)) {
                        getStyleClass().add(CHANGED_CSS_CLASS);
                    }
                } else {
                    getStyleClass().remove(CHANGED_CSS_CLASS);
                }
            }
        });
        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            varsTable.getStylesheets().add(css.toExternalForm());
            varsTable.getStyleClass().add("instructions");
        }
    }

    public Map<String, Long> buildVarsMap(RunResult result, Long[] inputs) {
        var xValues = new java.util.TreeMap<Integer, Long>();
        var zValues = new java.util.TreeMap<Integer, Long>();
        var yVar = classifyVariables(result, xValues, zValues);
        fillMissingXFromInputs(xValues, inputs);
        var out = new java.util.LinkedHashMap<String, Long>();
        out.put("y", (yVar != null ? yVar.value() : result.y()));
        xValues.forEach((i,v) -> out.put("x"+i, v));
        zValues.forEach((i,v) -> out.put("z"+i, v));
        return out;
    }

    public void renderFromRun(RunResult result, Long[] inputs) {
        Map<String, Long> vars = buildVarsMap(result, inputs);
        renderAll(vars);
        if (cyclesLineController != null) {
            cyclesLineController.renderFromRun(result, inputs);
        }
    }

    //This func fills inputs
    private void fillMissingXFromInputs(Map<Integer, Long> xValues, Long[] inputs) {
        if (inputs == null) return;
        for (int i = 0; i < inputs.length; i++) {
            int idx = i + 1;
            xValues.putIfAbsent(idx, inputs[i]);
        }
    }

    //This func classifies RESULT variables
    private VariableView classifyVariables(RunResult result, Map<Integer, Long> xOut, Map<Integer, Long> zOut) {
        List<VariableView> vars = (result == null || result.vars() == null) ? List.of() : result.vars();
        VariableView yVar = null;

        for (VariableView v : vars) {
            if (v == null) continue;
            String name = v.name() == null ? "" : v.name().toLowerCase(Locale.ROOT);

            if (v.type() == VarType.RESULT || "y".equals(name)) {
                yVar = v;
            } else if (v.type() == VarType.INPUT || name.startsWith("x")) {
                xOut.put(v.number(), v.value());
            } else if (v.type() == VarType.WORK || name.startsWith("z")) {
                zOut.put(v.number(), v.value());
            }
        }
        return yVar;
    }

    public void renderAll(Map<String, ?> variables) {
        Platform.runLater(() -> {
            rows.clear();
            byName.clear();
            if (variables == null || variables.isEmpty()) return;

            List<String> keys = new ArrayList<>(variables.keySet());
            keys.sort((a,b) -> {
                int ra = rank(a), rb = rank(b);
                if (ra != rb) return Integer.compare(ra, rb);
                if (ra == 1 || ra == 2) {
                    return Integer.compare(numSuffix(a), numSuffix(b));
                }
                return a.compareToIgnoreCase(b);
            });

            for (String k : keys) {
                Object v = variables.get(k);
                VarRow r = new VarRow(k, valueToString(v));
                rows.add(r);
                byName.put(k, r);
            }
        });
    }

    public void addOrUpdate(String name, Object value) {
        Platform.runLater(() -> {
            VarRow r = byName.get(name);
            String newText = valueToString(value);
            if (r == null) {
                r = new VarRow(name, newText);
                rows.add(r);
                byName.put(name, r);
            } else {
                r.valueProperty().set(newText);
            }
        });
    }

    private int rank(String k) {
        String s = (k == null) ? "" : k.trim().toLowerCase(Locale.ROOT);
        if (s.equals("y")) return 0;
        if (s.startsWith("x")) return 1;
        if (s.startsWith("z")) return 2;
        return 3;
    }

    private int numSuffix(String k) {
        String s = (k == null) ? "" : k.trim().toLowerCase(Locale.ROOT);
        int i = 0; while (i < s.length() && !Character.isDigit(s.charAt(i))) i++;
        if (i >= s.length()) return Integer.MAX_VALUE;
        try { return Integer.parseInt(s.substring(i)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    public void setCycles(int cycles) {
        if (cyclesLineController != null) {
            cyclesLineController.setCycles(cycles);
        }
    }

    public void clear() {
        Platform.runLater(() -> {
            rows.clear();
            byName.clear();
            changedNow.clear();
        });
    }

    private String valueToString(Object v) {
        return (v == null) ? "—" : String.valueOf(v);
    }

    public void clearForNewRun() {
        clear();
        setCycles(0);
    }

    public void highlightVariables(Set<String> names) {
        if (names == null) names = Set.of();
        final Set<String> copy = new HashSet<>(names);
        Platform.runLater(() -> {
            changedNow.clear();
            changedNow.addAll(copy);
            varsTable.refresh();
        });
    }

    public void clearHighlight() {
        Platform.runLater(() -> {
            changedNow.clear();
            varsTable.refresh();
        });
    }
}
