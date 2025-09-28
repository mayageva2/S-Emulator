package SummaryLine;

import emulator.api.EmulatorEngine;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class SummaryLineController {
    @FXML private Label basicCount;
    @FXML private Label syntheticCount;
    @FXML private Label totalCount;
    @FXML private BorderPane root;

    private EmulatorEngine engine;

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

    public void setEngine(EmulatorEngine engine) { this.engine = engine; }

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

    public void refreshFromEngine(int degree) {
        if (engine == null) return;
        try {
            ProgramView pv = engine.programView(degree);
            update(pv);
        } catch (Exception e) {
            setCounts(0, 0, 0);
        }
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
}
