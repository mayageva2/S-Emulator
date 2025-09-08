package InstructionsTable;

import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.stream.Collectors;

public class InstructionsTableController {

    @FXML private BorderPane root;
    @FXML private TableView<InstructionView> instructionsTable;
    @FXML private TableColumn<InstructionView, Number> indexCol;
    @FXML private TableColumn<InstructionView, String> typeCol;
    @FXML private TableColumn<InstructionView, String> InstructionsCol;
    @FXML private TableColumn<InstructionView, Number> cyclesCol;

    private int currentDegree = 0;

    @FXML
    private void initialize() {
        // column factories (once)
        indexCol .setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().index()));
        typeCol  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().basic() ? "B" : "S"));
        cyclesCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cycles()));
        InstructionsCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(prettyOpcode(c.getValue().opcode(), c.getValue().args())));

        // optional niceties
        instructionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        indexCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        cyclesCol.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    public void update(ProgramView pv) {
        currentDegree = pv.degree();
        instructionsTable.setItems(FXCollections.observableArrayList(pv.instructions()));
    }

    private static String prettyOpcode(String opcode, java.util.List<String> args) {
        return (args == null || args.isEmpty()) ? nz(opcode) : nz(opcode) + " " + String.join(", ", args);
    }

    private static String formatChain(List<Integer> chain, int degree) {
        if (degree > 0 && chain != null && !chain.isEmpty()) {
            return chain.stream().map(Object::toString).collect(Collectors.joining(" -> "));
        }
        return "";
    }

    private static String nz(String s) { return (s == null) ? "" : s; }
}
