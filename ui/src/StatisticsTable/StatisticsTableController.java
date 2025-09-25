package StatisticsTable;

import InstructionsTable.InstructionRow;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyLongWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

// Adjust the import to your real RunRecord package
import emulator.api.dto.RunRecord;

public class StatisticsTableController {

    /** Row model matching your console columns. */
    public static final class HistoryRow {
        public final int run;
        public final int degree;
        public final String inputs; // prettified
        public final long y;
        public final int cycles;

        public HistoryRow(int run, int degree, String inputs, long y, int cycles) {
            this.run = run;
            this.degree = degree;
            this.inputs = inputs;
            this.y = y;
            this.cycles = cycles;
        }
    }

    @FXML private TableView<HistoryRow> table;
    @FXML private TableColumn<HistoryRow, Number> runCol;
    @FXML private TableColumn<HistoryRow, Number> degreeCol;
    @FXML private TableColumn<HistoryRow, String> inputsCol;
    @FXML private TableColumn<HistoryRow, Number> yCol;
    @FXML private TableColumn<HistoryRow, Number> cyclesCol;

    private List<RunRecord> currentHistory = List.of();

    @FXML
    private void initialize() {
        // Value factories
        runCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().run));
        degreeCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().degree));
        inputsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().inputs));
        yCol.setCellValueFactory(cd -> new ReadOnlyLongWrapper(cd.getValue().y));
        cyclesCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().cycles));

       // table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Right-align numeric cells
        TableCellAlign.right(runCol);
        TableCellAlign.right(degreeCol);
        TableCellAlign.right(yCol);
        TableCellAlign.right(cyclesCol);

        // Make Inputs the flexible column; keep the others tight.
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (var col : table.getColumns()) col.setSortable(false);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            table.getStylesheets().add(css.toExternalForm());
            table.getStyleClass().add("instructions");
        }
    }

    /** Fill the table from your engine history using your existing formatter. */
    public void setHistory(List<RunRecord> history, Function<String, String> formatInputsByPosition) {
        currentHistory = (history == null) ? List.of() : new ArrayList<>(history);
        if (history == null || history.isEmpty()) {
            table.getItems().clear();
            return;
        }
        List<HistoryRow> rows = new ArrayList<>(history.size());
        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            String prettyInputs = (formatInputsByPosition != null) ? formatInputsByPosition.apply(r.inputsCsv()) : r.inputsCsv();
            rows.add(new HistoryRow(
                    r.runNumber(),
                    r.degree(),
                    prettyInputs,
                    r.y(),
                    r.cycles()
            ));
        }
        table.getItems().setAll(rows);
    }

    /** Convenience if you already precomputed pretty inputs (keeps your old flow). */
    public void setHistory(List<RunRecord> history, List<String> prettyInputs) {
        currentHistory = (history == null) ? List.of() : new ArrayList<>(history);
        if (history == null || history.isEmpty()) {
            table.getItems().clear();
            return;
        }
        List<HistoryRow> rows = new ArrayList<>(history.size());
        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            String pretty = (prettyInputs != null && i < prettyInputs.size()) ? prettyInputs.get(i) : r.inputsCsv();
            rows.add(new HistoryRow(r.runNumber(), r.degree(), pretty, r.y(), r.cycles()));
        }
        table.getItems().setAll(rows);
    }

    public OptionalInt getSelectedHistoryIndex() {
        int row = table.getSelectionModel().getSelectedIndex();
        if (row < 0 || row >= currentHistory.size()) return OptionalInt.empty();
        return OptionalInt.of(row);
    }

    // ---- helpers ----

    private static final class TableCellAlign {
        static <S, T> void right(TableColumn<S, T> col) {
            col.setCellFactory(c -> {
                TableCell<S, T> cell = new TableCell<>() {
                    @Override protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : (item == null ? "" : item.toString()));
                    }
                };
                cell.setAlignment(Pos.CENTER_RIGHT);
                return cell;
            });
        }
    }
}
