package StatisticsTable;

import InstructionsTable.InstructionRow;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyLongWrapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

// Adjust the import to your real RunRecord package
import emulator.api.dto.RunRecord;

public class StatisticsTableController {

    // Row model matching your console columns
    public static final class HistoryRow {
        public final int runNumber;
        public final String type;
        public final String program;
        public final String arch;
        public final int degree;
        public final long y;
        public final int cycles;

        public HistoryRow(int runNumber, String type, String program, String arch, int degree, long y, int cycles) {
            this.runNumber = runNumber;
            this.type = type;
            this.program = program;
            this.arch = arch;
            this.degree = degree;
            this.y = y;
            this.cycles = cycles;
        }
    }

    @FXML private TableColumn<HistoryRow, Number> runCol;
    @FXML private TableColumn<HistoryRow, String> typeCol;
    @FXML private TableColumn<HistoryRow, String> programCol;
    @FXML private TableColumn<HistoryRow, String> archCol;
    @FXML private TableColumn<HistoryRow, Number> degreeCol;
    @FXML private TableColumn<HistoryRow, Number> yCol;
    @FXML private TableColumn<HistoryRow, Number> cyclesCol;
    @FXML private TableView<HistoryRow> table;

    private List<RunRecord> currentHistory = List.of();
    public List<RunRecord> getCurrentHistory() { return currentHistory; }
    public TableView<?> getTableView() { return table; }

    @FXML
    private void initialize() {
        // Value factories
        runCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().runNumber));
        typeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().type));
        programCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().program));
        archCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().arch));
        degreeCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().degree));
        yCol.setCellValueFactory(cd -> new ReadOnlyLongWrapper(cd.getValue().y));
        cyclesCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().cycles));

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

    // Fill the table from your engine history using your existing formatter
    public void setHistory(List<RunRecord> history, Function<String, String> formatInputsByPosition) {
        currentHistory = (history == null) ? List.of() : new ArrayList<>(history);
        if (history == null || history.isEmpty()) {
            table.getItems().clear();
            return;
        }
        List<HistoryRow> rows = new ArrayList<>(history.size());
        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            String prettyInputs = (formatInputsByPosition != null)
                    ? formatInputsByPosition.apply(r.inputsCsv())
                    : r.inputsCsv();

            rows.add(new HistoryRow(
                    r.runNumber(),
                    r.getType() != null ? r.getType() : "PROGRAM",
                    r.programName(),
                    "I",
                    r.degree(),
                    r.y(),
                    r.cycles()
            ));
        }
        table.getItems().setAll(rows);
    }

    // Convenience if you already precomputed pretty inputs
    public void setHistory(List<RunRecord> history, List<String> prettyInputs) {
        currentHistory = (history == null) ? List.of() : new ArrayList<>(history);
        if (history == null || history.isEmpty()) {
            table.getItems().clear();
            return;
        }
        List<HistoryRow> rows = new ArrayList<>(history.size());
        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            String pretty = (prettyInputs != null && i < prettyInputs.size())
                    ? prettyInputs.get(i)
                    : r.inputsCsv();

            rows.add(new HistoryRow(
                    r.runNumber(),
                    "Main",
                    r.programName(),
                    "I",
                    r.degree(),
                    r.y(),
                    r.cycles()
            ));
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

    public void clearSelection() {
        try {
            if (table != null) {
                table.getSelectionModel().clearSelection();
            }
        } catch (Throwable ignore) {}
    }

    public void clear() {
        try {
            if (table != null) {
                table.getItems().clear();
                table.getSelectionModel().clearSelection();
            }
            currentHistory = List.of();
        } catch (Throwable ignore) {}
    }

    public Optional<RunRecord> getSelectedRunRecord() {
        OptionalInt idx = getSelectedHistoryIndex();
        if (idx.isEmpty()) return Optional.empty();
        int i = idx.getAsInt();
        if (i < 0 || i >= currentHistory.size()) return Optional.empty();
        return Optional.of(currentHistory.get(i));
    }

    public void loadUserHistory(String baseUrl, String username) {
        try {
            String urlStr = baseUrl + "user/history?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
            String json = HttpSessionClient.get(urlStr);
            Map<String, Object> resp = new Gson().fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

            if (!"success".equals(resp.get("status"))) return;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> runs = (List<Map<String, Object>>) resp.get("runs");

            List<RunRecord> hist = new ArrayList<>();
            List<HistoryRow> rows = new ArrayList<>();

            for (Map<String, Object> r : runs) {
                HistoryRow row = new HistoryRow(
                        ((Number) r.get("runNumber")).intValue(),
                        Objects.toString(r.get("type")),
                        Objects.toString(r.get("program"), "Unknown"),
                        Objects.toString(r.get("arch"), "I"),
                        ((Number) r.get("degree")).intValue(),
                        ((Number) r.get("y")).longValue(),
                        ((Number) r.get("cycles")).intValue()
                );
                rows.add(row);

                List<Long> inputs = new ArrayList<>();
                Object inputsObj = r.get("inputs");
                if (inputsObj instanceof List<?> list) {
                    for (Object v : list) {
                        try {
                            if (v instanceof Number num) {
                                inputs.add(num.longValue());
                            } else {
                                inputs.add(Long.parseLong(v.toString().replace(".0", "")));
                            }
                        } catch (Exception ignored) {}
                    }
                } else if (inputsObj != null) {
                    for (String s : inputsObj.toString().split(",")) {
                        try { inputs.add(Long.parseLong(s.trim())); } catch (Exception ignored) {}
                    }
                }

                Map<String, Long> varsSnapshot = new LinkedHashMap<>();
                Object varsObj = r.get("varsSnapshot");
                if (varsObj instanceof Map<?,?> map) {
                    for (var e : map.entrySet()) {
                        try {
                            varsSnapshot.put(e.getKey().toString(), Long.parseLong(e.getValue().toString()));
                        } catch (Exception ignored) {}
                    }
                }

                hist.add(new RunRecord(
                        username,
                        row.program,
                        row.runNumber,
                        row.degree,
                        inputs,
                        row.y,
                        row.cycles,
                        varsSnapshot,
                        row.type
                ));
            }

            this.currentHistory = hist;
            table.getItems().setAll(rows);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
