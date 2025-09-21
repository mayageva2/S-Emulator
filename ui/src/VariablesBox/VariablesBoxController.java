package VariablesBox;

import cyclesLine.CyclesLineController;
import emulator.api.dto.RunResult;
import emulator.api.dto.VarType;
import emulator.api.dto.VariableView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class VariablesBoxController {

    @FXML private TableView<VarRow> varsTable;
    @FXML private TableColumn<VarRow, String> colName;
    @FXML private TableColumn<VarRow, String> colValue;
    @FXML private CyclesLineController cyclesLineController;
    private final Map<String, Label> valueLabels = new ConcurrentHashMap<>();
    private final ObservableList<VarRow> rows = FXCollections.observableArrayList();
    private final Map<String, VarRow> byName = new ConcurrentHashMap<>();

    @FXML
    private void initialize() {
        varsTable.setItems(rows);
        varsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colValue.setCellValueFactory(data -> data.getValue().valueProperty());
        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            varsTable.getStylesheets().add(css.toExternalForm());
            varsTable.getStyleClass().add("instructions");
        }
    }

    private Map<String, Long> buildVarsMapLikeConsole(RunResult result, Long[] inputs) {
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
        Map<String, Long> vars = buildVarsMapLikeConsole(result, inputs);
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
    private VariableView classifyVariables(RunResult result,
                                           Map<Integer, Long> xOut,
                                           Map<Integer, Long> zOut) {
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

            variables.forEach((k, v) -> {
                VarRow r = new VarRow(k, valueToString(v));
                rows.add(r);
                byName.put(k, r);
            });
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
                int idx = rows.indexOf(r);
                if (idx >= 0) {
                    rows.set(idx, r);
                }
            }
        });
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
        });
    }

    private HBox makeRow(String name, Object value) {
        Label nameLbl = new Label(name + " = ");
        nameLbl.getStyleClass().add("var-name");

        Label valueLbl = new Label(valueToString(value));
        valueLbl.getStyleClass().add("var-value");
        valueLabels.put(name, valueLbl);

        HBox row = new HBox(6, nameLbl, valueLbl);
        row.setPadding(new Insets(2, 10, 2, 10));
        HBox.setHgrow(valueLbl, Priority.ALWAYS);
        return row;
    }

    private String valueToString(Object v) {
        return (v == null) ? "—" : String.valueOf(v);
    }

    private Label faintLabel(String text) {
        Label l = new Label(text);
        l.setOpacity(0.6);
        l.setPadding(new Insets(6,10,6,10));
        return l;
    }

    private void flash(Label lbl) {
        lbl.setStyle("-fx-background-color: rgba(255,255,0,0.25); -fx-background-radius: 4;");
        new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> lbl.setStyle(null));
        }).start();
    }

    public void clearForNewRun() {
        clear();
        setCycles(0);
    }
}
