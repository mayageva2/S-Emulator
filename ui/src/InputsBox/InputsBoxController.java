package InputsBox;

import Utils.ClientContext;
import Utils.HttpSessionClient;
import emulator.api.dto.ProgramView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.*;

import java.util.*;

public class InputsBoxController {

    @FXML private VBox inputsList;

    private final List<String> inputNames = new ArrayList<>();
    private final Map<String, TextField> fieldsByName = new LinkedHashMap<>();
    private final BooleanProperty locked = new SimpleBooleanProperty(false);

    private HttpSessionClient httpClient;

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    @FXML
    private void initialize() {
        this.httpClient = ClientContext.getHttpClient();
    }

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
        if (pv == null) {
            showNames(Collections.emptyList());
            return;
        }
        List<String> inputs = pv.inputs();
        showNames(inputs == null ? Collections.emptyList() : inputs);
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
            String raw = Optional.ofNullable(fieldsByName.get(name).getText()).orElse("").trim();
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
            grid.add(tf, 1, row);
            GridPane.setHgrow(tf, Priority.ALWAYS);
        }

        inputsList.getChildren().add(grid);
        applyLockToFields();
    }

    private void attachGuards(TextField tf) {
        tf.setTextFormatter(new TextFormatter<String>(change -> locked.get() ? null : change));
    }

    public void clearInputs() {
        fieldsByName.values().forEach(tf -> tf.setText(""));
    }

    public List<String> getInputNames() {
        return new ArrayList<>(inputNames);
    }

    public void setInputs(List<Long> inputs) {
        if (inputs == null) inputs = List.of();

        for (var entry : fieldsByName.entrySet()) {
            String name = entry.getKey();
            TextField tf = entry.getValue();
            if (tf == null) continue;

            int idx = parseXIndex(name);
            String val = "";
            if (idx > 0) {
                int arrIdx = idx - 1;
                if (arrIdx < inputs.size() && inputs.get(arrIdx) != null) {
                    val = String.valueOf(inputs.get(arrIdx));
                }
            }
            tf.setText(val);
        }
    }

    public String collectInputsAsJson() {
        Long[] values = collectAsLongsOrThrow();
        return "[" + String.join(",", Arrays.stream(values).map(String::valueOf).toList()) + "]";
    }

    public Map<String, Long> collectInputsAsMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String name : inputNames) {
            String raw = fieldsByName.get(name).getText().trim();
            long value = raw.isEmpty() ? 0L : Long.parseLong(raw);
            map.put(name, value);
        }
        return map;
    }

    public List<String> getCurrentInputValues() {
        List<String> values = new ArrayList<>();
        for (String name : inputNames) {
            TextField field = fieldsByName.get(name);
            values.add(field != null ? field.getText() : "");
        }
        return values;
    }

    public void restoreInputValues(List<String> prevValues) {
        int i = 0;
        for (String name : inputNames) {
            if (i >= prevValues.size()) break;
            TextField field = fieldsByName.get(name);
            if (field != null) field.setText(prevValues.get(i));
            i++;
        }
    }

    public void fillFromCsv(String csv) {
        if (csv == null || csv.isBlank()) return;

        String[] parts = csv.split(",");
        if (inputNames.isEmpty() || inputNames.size() < parts.length) {
            inputNames.clear();
            for (int j = 0; j < parts.length; j++) {
                inputNames.add("x" + (j + 1));
            }
        }

        for (String name : inputNames) {
            fieldsByName.computeIfAbsent(name, n -> new TextField());
        }

        for (int i = 0; i < inputNames.size() && i < parts.length; i++) {
            String name = inputNames.get(i);
            TextField field = fieldsByName.get(name);
            if (field != null) {
                field.setText(parts[i].trim());
            }
        }
    }
}
