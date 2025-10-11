package cyclesLine;

import emulator.api.dto.RunResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

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

    public void renderFromResponse(Map<String, Object> responseJson) {
        if (responseJson == null) return;

        Object cyclesObj = responseJson.get("cycles");
        long cycles = 0;
        if (cyclesObj instanceof Number n) cycles = n.longValue();
        else if (cyclesObj instanceof String s) {
            try { cycles = Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }

        long finalCycles = cycles;
        Platform.runLater(() -> {
            if (cyclesValue != null) {
                cyclesValue.setText(Long.toString(finalCycles));
            }
            if (root != null) {
                root.setVisible(true);
            }
        });
    }

    public void setCycles(int cycles) {
        Platform.runLater(() -> cyclesValue.setText(String.valueOf(cycles)));
    }

    public void reset() {
        Platform.runLater(() -> {
            if (cyclesValue != null) cyclesValue.setText("0");
            if (root != null) {
                root.setVisible(true);
                root.setManaged(true);
            }
        });
    }
}
