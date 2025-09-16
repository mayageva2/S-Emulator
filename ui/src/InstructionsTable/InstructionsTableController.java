package InstructionsTable;

import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class InstructionsTableController {

    @FXML private TableView<InstructionRow> table;
    @FXML private TableColumn<InstructionRow, Number> indexCol;
    @FXML private TableColumn<InstructionRow, String> typeCol;
    @FXML private TableColumn<InstructionRow, String> labelCol;
    @FXML private TableColumn<InstructionRow, Number> cyclesCol;
    @FXML private TableColumn<InstructionRow, String> instructionCol;

    private Consumer<InstructionView> onRowSelected;
    private String highlightTerm = null;
    @FXML
    private void initialize() {
        indexCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().index));
        typeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().basic ? "B" : "S"));
        labelCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(ns(cd.getValue().label)));
        cyclesCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().cycles));
        instructionCol.setCellValueFactory(cd -> {
            var r = cd.getValue();
            String text = pretty(r.opcode, r.args);
            return new ReadOnlyStringWrapper("  ".repeat(Math.max(0, r.depth)) + text);
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> new TableRow<InstructionRow>() {
            @Override protected void updateItem(InstructionRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null || isBlank(highlightTerm)) {
                    setStyle("");
                } else if (matches(row, highlightTerm)) {
                    setStyle("-fx-background-color: #fff3cd;"); // soft yellow
                } else {
                    setStyle("");
                }
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (onRowSelected != null && row != null && row.sourceIv != null) {
                onRowSelected.accept(row.sourceIv);
            }
        });

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            table.getStylesheets().add(css.toExternalForm());
            table.getStyleClass().add("instructions");
        }
    }

    public void setHighlightTerm(String term) {
        this.highlightTerm = isNone(term) ? null : term;
        table.refresh();
    }

    private static boolean isNone(String s) {
        return s == null || s.isBlank() || "— None —".equals(s);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static boolean matches(InstructionRow r, String term) {
        String t = term.toLowerCase();
        if (r.label != null && r.label.equalsIgnoreCase(t)) return true;
        if (r.opcode != null && r.opcode.toLowerCase().contains(t)) return true;
        if (r.args != null) {
            for (String a : r.args) {
                if (a != null && a.toLowerCase().contains(t)) return true;
            }
        }
        return false;
    }

    public void setItems(List<InstructionRow> items) {
        table.getItems().setAll(items);
    }

    public void clear() {
        table.getItems().clear();
    }

    public void scrollToEnd() {
        var n = table.getItems().size();
        if (n > 0) table.scrollTo(n - 1);
    }

    public TableView<InstructionRow> getTableView() { return table; }

    public void update(ProgramView pv) {
        Objects.requireNonNull(pv, "pv");
        List<InstructionRow> rows = new ArrayList<>(pv.instructions().size());
        for (InstructionView iv : pv.instructions()) {
            rows.add(new InstructionRow(
                    iv.index(),
                    iv.basic(),
                    ns(iv.label()),
                    iv.cycles(),
                    ns(iv.opcode()),
                    iv.args(),
                    0,            // depth 0 in the main table
                    iv            // keep the source for selection callback
            ));
        }
        setItems(rows);
    }

    public void setOnRowSelected(Consumer<InstructionView> handler) {
        this.onRowSelected = handler;
    }
    private static String ns(String s) { return s == null ? "" : s; }
    private static String pretty(String op, List<String> args) {
        op = ns(op);
        return (args == null || args.isEmpty()) ? op : op + " " + String.join(", ", args);
    }
}
