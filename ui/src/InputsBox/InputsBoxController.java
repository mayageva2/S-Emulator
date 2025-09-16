package InputsBox;

import emulator.api.dto.ProgramView;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.*;

public class InputsBoxController {

    @FXML private VBox inputsList;

    private final List<String> inputNames = new ArrayList<>();
    private final Map<String, TextField> fieldsByName = new LinkedHashMap<>();

    @FXML
    private void initialize() {}

    public void showForProgram(ProgramView pv) {
        showNames(Collections.emptyList()); // fallback – real names come from MainController
    }

    public void showNames(List<String> names) {
        if (names == null) names = Collections.emptyList();
        buildRows(names);
        System.out.println("InputsBox rows: " + inputNames);
    }

    public Long[] collectAsLongsOrThrow() {
        Long[] out = new Long[inputNames.size()];
        for (int i = 0; i < inputNames.size(); i++) {
            String name = inputNames.get(i);
            String raw = Optional.ofNullable(fieldsByName.get(name).getText()).orElse("").trim();
            long v = raw.isEmpty() ? 0L : Long.parseLong(raw); // may throw NumberFormatException
            if (v < 0) throw new IllegalArgumentException("Negative numbers are not allowed: " + v);
            out[i] = v;
        }
        return out;
    }


    private void buildRows(List<String> names) {
        inputsList.getChildren().clear();
        inputNames.clear();
        fieldsByName.clear();

        inputNames.addAll(names);

        if (names.isEmpty()) {
            Label note = new Label("No inputs in this program.");
            note.setStyle("-fx-opacity: 0.7;");
            inputsList.getChildren().add(note);
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setPadding(new Insets(0, 0, 0, 0));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(50);
        c0.setPrefWidth(80);
        c0.setHgrow(Priority.NEVER);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().setAll(c0, c1);

        for (int row = 0; row < names.size(); row++) {
            String name = names.get(row);

            Label nameLbl = new Label(names.get(row) + " =");
            TextField tf = new TextField();

            fieldsByName.put(name, tf);

            grid.add(nameLbl, 0, row);
            grid.add(tf,      1, row);
            GridPane.setHgrow(tf, Priority.ALWAYS);
        }

        inputsList.getChildren().add(grid);
    }
}
