package SummaryLine;

import emulator.api.EmulatorEngine;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.Map;

public class SummaryLineController {
    @FXML private Label basicCount;
    @FXML private Label syntheticCount;
    @FXML private Label totalCount;
    @FXML private BorderPane root;

    @FXML
    private void initialize() {
        var css = getClass().getResource("/SummaryLine/summary-line.css");
        if (css != null) {
            if (root != null) {
                root.getStylesheets().add(css.toExternalForm());
            } else {
                javafx.application.Platform.runLater(() -> {
                    var scene = totalCount.getScene();
                    if (scene != null) scene.getStylesheets().add(css.toExternalForm());
                });
            }
        }
    }

    //Updates the summary line using a ProgramView object fetched from the server
    public void refreshFromServer(ProgramView pv) {
        update(pv);
    }

    public void update(ProgramView pv) {
        if (pv == null) {
            setCounts(0, 0, 0);
            return;
        }
        List<InstructionView> list = pv.instructions();
        int total = (list == null) ? 0 : list.size();
        int basic = (list == null) ? 0 : (int) list.stream().filter(InstructionView::basic).count();
        int synthetic = Math.max(0, total - basic);
        setCounts(total, basic, synthetic);
    }

    private void setCounts(int total, int basic, int synthetic) {
        Runnable r = () -> {
            if (totalCount != null) totalCount.setText(Integer.toString(total));
            if (basicCount != null) basicCount.setText(Integer.toString(basic));
            if (syntheticCount != null) syntheticCount.setText(Integer.toString(synthetic));
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    public void clear() {
        setCounts(0, 0, 0);
    }

    public void bindTo(ProgramView pv) {
        update(pv);
    }

    public void updateFromJson(Map<String, Object> programJson) {
        if (programJson == null) {
            setCounts(0, 0, 0);
            return;
        }

        try {
            List<Map<String, Object>> instructions =
                    (List<Map<String, Object>>) programJson.get("instructions");
            int total = (instructions == null) ? 0 : instructions.size();

            // basic / synthetic
            int basic = 0;
            if (instructions != null) {
                for (Map<String, Object> ins : instructions) {
                    Object val = ins.get("basic");
                    if (val instanceof Boolean b && b) basic++;
                }
            }
            int synthetic = Math.max(0, total - basic);

            setCounts(total, basic, synthetic);

        } catch (Exception e) {
            System.err.println("Failed to update summary line from JSON: " + e.getMessage());
            setCounts(0, 0, 0);
        }
    }

}
