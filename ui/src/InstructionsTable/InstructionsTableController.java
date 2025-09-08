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
    @FXML private TableColumn<InstructionView, Number> cyclesCol;
    @FXML private TableColumn<InstructionView, String> InstructionsCol;

    private int currentDegree = 0;

    @FXML
    private void initialize() {
        assert instructionsTable != null : "instructionsTable not injected";
        assert indexCol  != null && typeCol != null && cyclesCol != null && InstructionsCol != null;

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            instructionsTable.getStylesheets().add(css.toExternalForm());
            instructionsTable.getStyleClass().add("instructions");
        }

        // Value factories
        indexCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().index()));
        typeCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().basic() ? "B" : "S"));
        cyclesCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cycles()));
        InstructionsCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(prettyOpcode(c.getValue().opcode(), c.getValue().args())));

        instructionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        indexCol.setMinWidth(40);         indexCol.setPrefWidth(60);
        typeCol.setMinWidth(70);          typeCol.setPrefWidth(100);
        cyclesCol.setMinWidth(60);        cyclesCol.setPrefWidth(80);
        InstructionsCol.setMinWidth(160); InstructionsCol.setPrefWidth(400);

        indexCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setStyle("-fx-alignment: CENTER;");
        cyclesCol.setStyle("-fx-alignment: CENTER;");
        InstructionsCol.setStyle("-fx-alignment: CENTER-LEFT;");
    }

    public void update(ProgramView pv) {
        currentDegree = pv.degree();
        instructionsTable.setItems(FXCollections.observableArrayList(pv.instructions()));
        instructionsTable.refresh();
    }

    private static String prettyOpcode(String opcode, java.util.List<String> args) {
        return (args == null || args.isEmpty()) ? nz(opcode) : nz(opcode) + " " + String.join(", ", args);
    }

    private static String nz(String s) { return (s == null) ? "" : s; }
}
