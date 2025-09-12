package InstructionsTable;

import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InstructionsTableController {

    @FXML private TableView<InstructionRow> table;
    @FXML private TableColumn<InstructionRow, Number> indexCol;
    @FXML private TableColumn<InstructionRow, String> typeCol;
    @FXML private TableColumn<InstructionRow, String> labelCol;
    @FXML private TableColumn<InstructionRow, Number> cyclesCol;
    @FXML private TableColumn<InstructionRow, String> instructionCol;

    private Consumer<InstructionView> onRowSelected;

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
