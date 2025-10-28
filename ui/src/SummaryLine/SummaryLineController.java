package SummaryLine;

import Utils.HttpSessionClient;
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
    @FXML private Label arch1Count;
    @FXML private Label arch2Count;
    @FXML private Label arch3Count;
    @FXML private Label arch4Count;
    @FXML private BorderPane root;

    private HttpSessionClient httpClient;
    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    @FXML
    private void initialize() {
        var css = getClass().getResource("/SummaryLine/summary-line.css");
        if (css != null) {
            if (root != null) {
                root.getStylesheets().add(css.toExternalForm());
            } else {
                Platform.runLater(() -> {
                    var scene = totalCount.getScene();
                    if (scene != null) scene.getStylesheets().add(css.toExternalForm());
                });
            }
        }
    }

    public void refreshFromServer(ProgramView pv) { update(pv); }

    public void update(ProgramView pv) {
        if (pv == null) {
            setCounts(0, 0, 0, 0, 0, 0, 0);
            return;
        }

        List<InstructionView> list = pv.instructions();
        int total = (list == null) ? 0 : list.size();
        int basic = (int) list.stream().filter(InstructionView::basic).count();
        int synthetic = Math.max(0, total - basic);

        long archI = list.stream().filter(iv -> "I".equalsIgnoreCase(iv.architecture())).count();
        long archII = list.stream().filter(iv -> "II".equalsIgnoreCase(iv.architecture())).count();
        long archIII = list.stream().filter(iv -> "III".equalsIgnoreCase(iv.architecture())).count();
        long archIV = list.stream().filter(iv -> "IV".equalsIgnoreCase(iv.architecture())).count();

        setCounts(total, basic, synthetic, (int) archI, (int) archII, (int) archIII, (int) archIV);
    }

    private void setCounts(int total, int basic, int synthetic,
                           int arch1, int arch2, int arch3, int arch4) {
        Runnable r = () -> {
            if (totalCount != null) totalCount.setText(Integer.toString(total));
            if (basicCount != null) basicCount.setText(Integer.toString(basic));
            if (syntheticCount != null) syntheticCount.setText(Integer.toString(synthetic));
            if (arch1Count != null) arch1Count.setText(Integer.toString(arch1));
            if (arch2Count != null) arch2Count.setText(Integer.toString(arch2));
            if (arch3Count != null) arch3Count.setText(Integer.toString(arch3));
            if (arch4Count != null) arch4Count.setText(Integer.toString(arch4));
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    public void clear() {
        setCounts(0, 0, 0, 0, 0, 0, 0);
    }

    public void bindTo(ProgramView pv) { update(pv); }

    public void updateFromJson(Map<String, Object> programJson) {
        if (programJson == null) {
            setCounts(0, 0, 0, 0, 0, 0, 0);
            return;
        }

        try {
            List<Map<String, Object>> instructions =
                    (List<Map<String, Object>>) programJson.get("instructions");
            int total = (instructions == null) ? 0 : instructions.size();

            int basic = 0, arch1 = 0, arch2 = 0, arch3 = 0, arch4 = 0;
            if (instructions != null) {
                for (Map<String, Object> ins : instructions) {
                    if (Boolean.TRUE.equals(ins.get("basic"))) basic++;
                    String arch = String.valueOf(ins.getOrDefault("architecture", "?"));
                    switch (arch) {
                        case "I" -> arch1++;
                        case "II" -> arch2++;
                        case "III" -> arch3++;
                        case "IV" -> arch4++;
                    }
                }
            }
            int synthetic = Math.max(0, total - basic);

            setCounts(total, basic, synthetic, arch1, arch2, arch3, arch4);

        } catch (Exception e) {
            System.err.println("Failed to update summary line from JSON: " + e.getMessage());
            setCounts(0, 0, 0, 0, 0, 0, 0);
        }
    }
}
