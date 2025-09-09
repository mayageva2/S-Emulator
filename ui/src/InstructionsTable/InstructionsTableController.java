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
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InstructionsTableController {

    @FXML private BorderPane root;
    @FXML private TableView<InstructionView> instructionsTable;

    @FXML private TableColumn<InstructionView, Number> indexCol;
    @FXML private TableColumn<InstructionView, String> typeCol;
    @FXML private TableColumn<InstructionView, String> labelCol;
    @FXML private TableColumn<InstructionView, Number> cyclesCol;
    @FXML private TableColumn<InstructionView, String> InstructionsCol;

    private int currentDegree = 0;
    private Consumer<InstructionView> onRowSelected;

    @FXML
    private void initialize() {
        assert instructionsTable != null : "instructionsTable not injected";
        assert indexCol  != null && typeCol != null && cyclesCol != null && InstructionsCol != null;
        instructionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            instructionsTable.getStylesheets().add(css.toExternalForm());
            instructionsTable.getStyleClass().add("instructions");
        }

        instructionsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null && onRowSelected != null) {
                onRowSelected.accept(sel);  // notify MainController
            }
        });

        // Value factories
        indexCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().index()));
        typeCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().basic() ? "B" : "S"));
        labelCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().label()));
        cyclesCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cycles()));
        InstructionsCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(prettyOpcode(c.getValue().opcode(), c.getValue().args())));

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

    public void update(ProgramView pv) {
        currentDegree = pv.degree();
        instructionsTable.setItems(FXCollections.observableArrayList(pv.instructions()));
        instructionsTable.refresh();
    }

    private static String prettyOpcode(String opcode, java.util.List<String> args) {
        return (args == null || args.isEmpty()) ? nz(opcode) : nz(opcode) + " " + String.join(", ", args);
    }

    public void setOnRowSelected(java.util.function.Consumer<InstructionView> cb) {
        this.onRowSelected = cb;
    }

    private static String nz(String s) { return (s == null) ? "" : s; }
}
