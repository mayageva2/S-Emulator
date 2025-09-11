package InputsBox;

import emulator.api.dto.ProgramView;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.scene.control.TextFormatter;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class InputsPrompt {
    private InputsPrompt() {}

    /** Shows a modal dialog asking for the program inputs, returns empty if user cancels. */
    public static Optional<Long[]> show(Window owner, ProgramView pv) {
        List<String> names = extractInputNames(pv);

        Dialog<Long[]> dlg = new Dialog<>();
        dlg.setTitle("Program Inputs");
        dlg.setHeaderText("Please enter input values");
        if (owner != null) dlg.initOwner(owner);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 12, 10, 12));

        List<TextField> fields = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            Label lbl = new Label(name + ":");

            TextField tf = new TextField();
            tf.setPromptText("0");
            // numeric-only formatter per field
            tf.setTextFormatter(new TextFormatter<>(longConverter(), null, numericLongFilter()));

            GridPane.setHgrow(tf, Priority.ALWAYS);
            grid.add(lbl, 0, i);
            grid.add(tf, 1, i);
            fields.add(tf);
        }

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(Math.min(300, Math.max(120, names.size() * 40)));
        dlg.getDialogPane().setContent(sp);

        // Disable OK if any field has non-numeric text (empty is allowed => treated as 0)
        Node ok = dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> fields.stream().anyMatch(tf -> !isNumericOrEmpty(tf.getText())),
                        fields.stream().map(TextField::textProperty).toArray(Observable[]::new)
                )
        );

        dlg.setResultConverter(new Callback<ButtonType, Long[]>() {
            @Override
            public Long[] call(ButtonType bt) {
                if (bt != ButtonType.OK) return null;
                Long[] out = new Long[fields.size()];
                for (int i = 0; i < fields.size(); i++) {
                    String raw = fields.get(i).getText().trim();
                    if (raw.isEmpty()) raw = "0";
                    out[i] = Long.parseLong(raw);
                }
                return out;
            }
        });

        return dlg.showAndWait();
    }

    // ------- helpers -------

    private static List<String> extractInputNames(ProgramView pv) {
        try {
            Method m = pv.getClass().getMethod("inputNames");
            Object obj = m.invoke(pv);
            if (obj instanceof List<?> list) {
                List<String> out = new ArrayList<>(list.size());
                for (Object o : list) out.add(String.valueOf(o));
                return out;
            }
        } catch (ReflectiveOperationException ignore) {}
        return Collections.emptyList();
    }

    private static boolean isNumericOrEmpty(String s) {
        if (s == null || s.isBlank()) return true;
        return s.matches("-?\\d+");
    }

    // FIX: return the correct type for TextFormatter's filter
    private static UnaryOperator<TextFormatter.Change> numericLongFilter() {
        Pattern p = Pattern.compile("-?\\d*");  // optional leading '-' + digits
        return change -> p.matcher(change.getControlNewText()).matches() ? change : null;
    }

    private static StringConverter<Long> longConverter() {
        return new StringConverter<>() {
            @Override public String toString(Long value) { return value == null ? "" : String.valueOf(value); }
            @Override public Long fromString(String s) {
                if (s == null || s.isBlank()) return 0L;
                return Long.parseLong(s.trim());
            }
        };
    }
}
