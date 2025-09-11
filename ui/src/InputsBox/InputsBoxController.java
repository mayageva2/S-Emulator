package InputsBox;

import emulator.api.dto.ProgramView;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class InputsBoxController {

    @FXML private VBox inputsList;

    private final List<String> inputNames = new ArrayList<>();
    private final Map<String, TextField> fieldsByName = new LinkedHashMap<>();

    @FXML
    private void initialize() {}

    public void showForProgram(ProgramView pv) {
        List<String> names = extractInputNames(pv);
        buildRows(names);
    }

    public String[] collectAsStrings() {
        String[] out = new String[inputNames.size()];
        for (int i = 0; i < inputNames.size(); i++) {
            String name = inputNames.get(i);
            TextField tf = fieldsByName.get(name);
            out[i] = tf.getText();
        }
        return out;
    }

    public Long[] collectAsLongsOrThrow() {
        Long[] out = new Long[inputNames.size()];
        for (int i = 0; i < inputNames.size(); i++) {
            String name = inputNames.get(i);
            String raw = Optional.ofNullable(fieldsByName.get(name).getText()).orElse("").trim();
            if (raw.isEmpty()) {
                out[i] = 0L;
            } else {
                try {
                    out[i] = Long.parseLong(raw);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Input '" + name + "' must be an integer.");
                }
            }
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
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(0));

        for (int row = 0; row < names.size(); row++) {
            String name = names.get(row);

            Label nameLbl = new Label(name);
            Label eq = new Label(" = ");
            TextField tf = new TextField();
            tf.setPromptText(""); // free text (string)

            fieldsByName.put(name, tf);

            grid.add(nameLbl, 0, row);
            grid.add(eq,      1, row);
            grid.add(tf,      2, row);

            // make the field stretch horizontally
            GridPane.setHgrow(tf, javafx.scene.layout.Priority.ALWAYS);
        }
        inputsList.getChildren().add(grid);
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractInputNames(ProgramView pv) {
        try {
            Method m = pv.getClass().getMethod("inputNames");
            Object obj = m.invoke(pv);
            if (obj instanceof List<?> list && !list.isEmpty()) {
                List<String> out = new ArrayList<>(list.size());
                for (Object o : list) out.add(String.valueOf(o));
                return out;
            }
        } catch (ReflectiveOperationException ignore) {}
        return Collections.emptyList();
    }
}
