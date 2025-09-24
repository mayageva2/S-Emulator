package InputsBox;

import emulator.api.dto.ProgramView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.*;

import java.util.*;

public class InputsBoxController {

    @FXML private VBox inputsList;

    private final List<String> inputNames = new ArrayList<>();
    private final Map<String, TextField> fieldsByName = new LinkedHashMap<>();
    private final BooleanProperty locked = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {}

    public void lockInputs()   { locked.set(true);  applyLockToFields(); }
    public void unlockInputs() { locked.set(false); applyLockToFields(); }

    private void applyLockToFields() {
        boolean isLocked = locked.get();
        for (TextField tf : fieldsByName.values()) {
            tf.setEditable(!isLocked);
            tf.setDisable(false);
            tf.setFocusTraversable(true);
        }
    }

    public void showForProgram(ProgramView pv) {
        showNames(Collections.emptyList()); // fallback – real names come from MainController
    }

    public void showNames(List<String> names) {
        if (names == null) names = Collections.emptyList();
        buildRows(names);
        System.out.println("InputsBox rows: " + inputNames);
    }

    public Long[] collectAsLongsOrThrow() {
        int maxIdx = 0;
        for (String name : inputNames) {
            int idx = parseXIndex(name);
            if (idx > maxIdx) maxIdx = idx;
        }
        if (maxIdx == 0) {
            return new Long[0];
        }

        Long[] out = new Long[maxIdx];
        Arrays.fill(out, 0L);
        for (String name : inputNames) {
            int idx = parseXIndex(name);
            if (idx <= 0) continue;
            String raw = java.util.Optional.ofNullable(fieldsByName.get(name).getText()).orElse("").trim();
            long v = raw.isEmpty() ? 0L : Long.parseLong(raw);
            out[idx - 1] = v;
        }
        return out;
    }

    private static int parseXIndex(String name) {
        if (name == null) return 0;
        String s = name.trim().toLowerCase(Locale.ROOT);
        if (!s.startsWith("x")) return 0;
        try {
            int idx = Integer.parseInt(s.substring(1));
            return idx > 0 ? idx : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
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
            attachGuards(tf);
            fieldsByName.put(name, tf);

            grid.add(nameLbl, 0, row);
            grid.add(tf,      1, row);
            GridPane.setHgrow(tf, Priority.ALWAYS);
        }

        inputsList.getChildren().add(grid);
        applyLockToFields();
    }

    private void attachGuards(TextField tf) {
        // This formatter allows edits only when locked=false
        tf.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change -> {
            return locked.get() ? null : change;
        }));
    }

    private void makeEditable(TextField tf) {
        tf.setDisable(false);
        tf.setEditable(true);
        tf.setTextFormatter(null);
    }

    public void clearInputs() {
        fieldsByName.values().forEach(tf -> tf.setText(""));
    }

    public List<String> getInputNames() {
        return new ArrayList<>(inputNames);
    }
}
