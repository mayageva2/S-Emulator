package cyclesLine;

import emulator.api.dto.RunResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.lang.reflect.Method;
import java.util.Collection;

public class CyclesLineController {
    @FXML private Region root;
    @FXML private Label cyclesValue;

    @FXML
    private void initialize() {
        if (cyclesValue != null) cyclesValue.setText("");
    }

    public void bindWidthTo(Region varsRoot) {
        if (varsRoot != null && root != null) {
            root.minWidthProperty().bind(varsRoot.widthProperty());
            root.prefWidthProperty().bind(varsRoot.widthProperty());
            root.maxWidthProperty().bind(varsRoot.widthProperty());
        }
    }

    public void renderFromRun(RunResult result, Long[] inputs) {
        long cycles = extractCycles(result);
        Platform.runLater(() -> {
            if (cyclesValue != null) {
                cyclesValue.setText(Long.toString(cycles));
            }
            if (root != null) {
                root.setVisible(true);
            }
        });
    }
    public void setCycles(int cycles) {
        Platform.runLater(() -> cyclesValue.setText(String.valueOf(cycles)));
    }

    private long extractCycles(RunResult result) {
        if (result == null) return 0L;

        for (String m : new String[]{"getTotalCycles", "totalCycles", "getCycles", "cycles"}) {
            Long v = tryNumberGetter(result, m);
            if (v != null) return v;
        }
        Integer sz = tryCollectionSizeGetter(result, "getTrace", "trace", "getSteps", "steps");
        return (sz != null) ? sz : 0L;
    }

    private Long tryNumberGetter(Object obj, String name) {
        try {
            Method m = obj.getClass().getMethod(name);
            Object v = m.invoke(obj);
            if (v instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return null;
    }

    private Integer tryCollectionSizeGetter(Object obj, String... names) {
        for (String name : names) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object v = m.invoke(obj);
                if (v instanceof Collection<?> c) return c.size();
            } catch (Exception ignored) {}
        }
        return null;
    }
}
