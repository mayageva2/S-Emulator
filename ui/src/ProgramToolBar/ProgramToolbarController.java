package ProgramToolBar;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ProgramToolbarController {

    @FXML private Button selectProgramButton, CollapseButton, ExpandButton;
    @FXML private Label CurrentOrMaxDegreeLabel;
    @FXML private ChoiceBox<String> HighlightChoices;

    private Runnable onSelectProgram, onCollapseClick, onExpandClick;
    private Consumer<String> onHighlightChanged;
    private int currentDegree = 0;
    private int maxDegree = 0;

    @FXML
    private void initialize() {
        CollapseButton.setOnAction(e -> { if (onCollapseClick != null) onCollapseClick.run(); });
        ExpandButton.setOnAction(e ->   { if (onExpandClick   != null) onExpandClick.run();   });
        updateButtons();

        if (HighlightChoices != null) {
            HighlightChoices.setItems(FXCollections.observableArrayList("None"));
            HighlightChoices.getSelectionModel().selectFirst();
            HighlightChoices.setDisable(true);

            HighlightChoices.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (onHighlightChanged != null) onHighlightChanged.accept(newV);
            });
        }
    }

    public void bindDegree(int current, int max) {
        this.currentDegree = current;
        this.maxDegree = max;
        CurrentOrMaxDegreeLabel.setText("Degree: " + current + "/" + max);
        updateButtons();
    }

    public void setOnExpand(Runnable r)   { this.onExpandClick = r; }
    public void setOnCollapse(Runnable r) { this.onCollapseClick = r; }
    public void setOnHighlightChanged(Consumer<String> c) { this.onHighlightChanged = c; }

    public int  getCurrent() { return currentDegree; }
    public int  getMax() { return maxDegree; }
    public void setCurrent(int c) { this.currentDegree = c; bindDegree(c, maxDegree); }

    private void updateButtons() {
        CollapseButton.setDisable(currentDegree <= 0);
        ExpandButton.setDisable(currentDegree >= maxDegree);
    }

    public void setHighlightOptions(List<String> options) {
        if (HighlightChoices == null) return;
        List<String> items = new ArrayList<>();
        items.add("None");
        if (options != null) {
            options.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(items::add);
        }
        HighlightChoices.setItems(FXCollections.observableArrayList(items));
        HighlightChoices.getSelectionModel().selectFirst();
    }

    public void setHighlightEnabled(boolean enabled) {
        if (HighlightChoices == null) return;
        HighlightChoices.setDisable(!enabled);
        if (!enabled) {
            if (HighlightChoices.getItems().isEmpty()
                    || !"None".equals(HighlightChoices.getItems().get(0))) {
                HighlightChoices.setItems(FXCollections.observableArrayList("None"));
            }
            HighlightChoices.getSelectionModel().selectFirst();
        }
    }
}
