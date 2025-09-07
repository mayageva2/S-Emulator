package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

public class ProgramToolbarController {

    @FXML private Button selectProgramButton;
    @FXML private Button CollapseButton;
    @FXML private Button ExpandButton;
    @FXML private Label CurrentOrMaxDegreeLabel;
    @FXML private ChoiceBox<String> HighlightChoices;

    private Runnable onSelectProgram;
    private Runnable onCollapse;
    private Runnable onExpand;
    private int currentDegree = 0;
    private int maxDegree = 0;

    @FXML
    private void initialize() {
        CurrentOrMaxDegreeLabel.setText("Degree: 0/0");

        selectProgramButton.setOnAction(e -> { if (onSelectProgram != null) onSelectProgram.run(); });
        CollapseButton.setOnAction(e -> { if (onCollapse != null) onCollapse.run(); });
        ExpandButton.setOnAction(e -> { if (onExpand != null) onExpand.run(); });

        updateButtons();
    }

    public void setProgramName(String name) { CurrentOrMaxDegreeLabel.setText(name == null ? "" : name); }
    public void setCollapseEnabled(boolean on) { CollapseButton.setDisable(!on); }
    public void setExpandEnabled(boolean on)   { ExpandButton.setDisable(!on);   }

    public void setDegree(int current, int max) {
        this.currentDegree = current;
        this.maxDegree = max;
        CurrentOrMaxDegreeLabel.setText("Degree: " + current + "/" + max);
        updateButtons();
    }

    private void updateButtons() {
        CollapseButton.setDisable(currentDegree <= 0);
        ExpandButton.setDisable(currentDegree >= maxDegree);
    }
}
