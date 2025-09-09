package SelectedInstructionHistoryChainTable;

import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.util.*;

public class SelectedInstructionHistoryChainTableController {

    @FXML private BorderPane root;
    @FXML private TableView<Row> instructionsTable;
    @FXML private TableColumn<Row, Number> indexCol;
    @FXML private TableColumn<Row, String> typeCol;
    @FXML private TableColumn<Row, String> labelCol;
    @FXML private TableColumn<Row, Number> cyclesCol;
    @FXML private TableColumn<Row, String> InstructionsCol;

    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        instructionsTable.setItems(rows);
        instructionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            instructionsTable.getStylesheets().add(css.toExternalForm());
            instructionsTable.getStyleClass().add("instructions");
        }

        indexCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().iv.index()));
        typeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().iv.basic() ? "B" : "S"));
        labelCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(ns(cd.getValue().iv.label())));
        cyclesCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().iv.cycles()));
        InstructionsCol.setCellValueFactory(cd -> {
            Row r = cd.getValue();
            String pretty = prettyOpcode(r.iv.opcode(), r.iv.args());
            return new ReadOnlyStringWrapper("  ".repeat(Math.max(0, r.depth)) + pretty);
        });

        indexCol.setMinWidth(30);          indexCol.setMaxWidth(100);
        typeCol.setMinWidth(50);           typeCol.setMaxWidth(100);
        labelCol.setMinWidth(60);          labelCol.setMaxWidth(100);
        cyclesCol.setMinWidth(50);         cyclesCol.setMaxWidth(100);
        InstructionsCol.setMinWidth(90);   InstructionsCol.setResizable(true);


        indexCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setStyle("-fx-alignment: CENTER;");
        labelCol.setStyle("-fx-alignment: CENTER;");
        cyclesCol.setStyle("-fx-alignment: CENTER;");
        InstructionsCol.setStyle("-fx-alignment: CENTER-LEFT;");
    }

    /** Call this when a row is clicked in the expanded table. */
    public void showForSelected(InstructionView selected, ProgramView pvOriginal) {
        if (selected == null) { clear(); return; }
        List<InstructionView> chain = toChainRootToLeaf(selected, pvOriginal);
        Platform.runLater(() -> {
            rows.setAll(toRows(chain));
            instructionsTable.scrollTo(rows.size() - 1);
        });
    }

    public void clear() { rows.clear(); }

    // ---- chain building using your existing data ----

    /** Build chain: root -> ... -> selected. Uses createdFromViews() first, else createdFromChain(). */
    private List<InstructionView> toChainRootToLeaf(InstructionView selected, ProgramView pvOriginal) {
        // 1) If the expanded instruction carries full views:
        List<InstructionView> fromViews = selected.createdFromViews();
        if (fromViews != null && !fromViews.isEmpty()) {
            List<InstructionView> list = new ArrayList<>(fromViews);
            // If order looks reversed in your UI, uncomment the next line:
            // Collections.reverse(list);
            list.add(selected);  // ensure the clicked instruction is last
            return list;
        }

        // 2) Else use the index chain (origin indices from the original program)
        List<Integer> idxChain = selected.createdFromChain();
        if (idxChain != null && !idxChain.isEmpty()) {
            Map<Integer, InstructionView> originalByIndex = mapByIndex(pvOriginal);
            List<InstructionView> list = new ArrayList<>(idxChain.size() + 1);
            for (Integer idx : idxChain) {
                if (idx == null) continue;
                InstructionView origin = originalByIndex.get(idx);
                if (origin != null) list.add(origin);
            }
            list.add(selected);
            return list;
        }

        // 3) No provenance → just show the selected one
        return List.of(selected);
    }

    private Map<Integer, InstructionView> mapByIndex(ProgramView pvOriginal) {
        Map<Integer, InstructionView> m = new HashMap<>();
        for (InstructionView iv0 : pvOriginal.instructions()) m.put(iv0.index(), iv0);
        return m;
    }

    private List<Row> toRows(List<InstructionView> chain) {
        List<Row> out = new ArrayList<>(chain.size());
        for (int d = 0; d < chain.size(); d++) out.add(new Row(chain.get(d), d));
        return out;
    }

    private static String prettyOpcode(String opcode, List<String> args) {
        String op = ns(opcode);
        return (args == null || args.isEmpty()) ? op : op + " " + String.join(", ", args);
    }

    private static String ns(String s) { return (s == null) ? "" : s; }

    /** Row model = instruction + its depth (for indentation). */
    public static final class Row {
        final InstructionView iv;
        final int depth;
        Row(InstructionView iv, int depth) { this.iv = Objects.requireNonNull(iv); this.depth = depth; }
    }
}
