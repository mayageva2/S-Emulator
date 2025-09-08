package ProgramToolBar;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

public class ProgramToolbarController {

    @FXML private Button selectProgramButton, CollapseButton, ExpandButton;
    @FXML private Label CurrentOrMaxDegreeLabel;
    @FXML private ChoiceBox<String> HighlightChoices;

    private Runnable onSelectProgram, onCollapseClick, onExpandClick;
    private int currentDegree = 0;
    private int maxDegree = 0;

    @FXML
    private void initialize() {
        CollapseButton.setOnAction(e -> { if (onCollapseClick != null) onCollapseClick.run(); });
        ExpandButton.setOnAction(e ->   { if (onExpandClick   != null) onExpandClick.run();   });
        updateButtons();
    }

    public void bindDegree(int current, int max) {
        this.currentDegree = current;
        this.maxDegree = max;
        CurrentOrMaxDegreeLabel.setText("Degree: " + current + "/" + max);
        updateButtons();
    }

    public void setOnExpand(Runnable r)   { this.onExpandClick = r; }
    public void setOnCollapse(Runnable r) { this.onCollapseClick = r; }

    public int  getCurrent() { return currentDegree; }
    public int  getMax() { return maxDegree; }
    public void setCurrent(int c) { this.currentDegree = c; bindDegree(c, maxDegree); }

    private void updateButtons() {
        CollapseButton.setDisable(currentDegree <= 0);
        ExpandButton.setDisable(currentDegree >= maxDegree);
    }
}
