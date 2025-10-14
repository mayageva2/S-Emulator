package ProgramToolBar;

import emulator.api.dto.ProgramView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class ProgramToolbarController {

    @FXML private Button CollapseButton, ExpandButton, CurrentOrMaxDegreeButton;
    @FXML private ChoiceBox<String> HighlightChoices, selectProgramChoice;
    private Consumer<Integer> onJumpToDegree;
    private Consumer<String> onHighlightChanged;
    private Consumer<String> onProgramSelected;

    public void setOnJumpToDegree(Consumer<Integer> c) { this.onJumpToDegree = c; }
    public void setOnProgramSelected(java.util.function.Consumer<String> c) { this.onProgramSelected = c; }
    private Runnable onSelectProgram, onCollapseClick, onExpandClick;
    private int currentDegree = 0;
    private int maxDegree = 0;
    private boolean degreeUiLocked = false;

    public void setExpandEnabled(boolean enabled) {
        if (ExpandButton != null) ExpandButton.setDisable(!enabled);
    }

    public void setCollapseEnabled(boolean enabled) {
        if (CollapseButton != null) CollapseButton.setDisable(!enabled);
    }

    public String getSelectedProgramName() {
        return (selectProgramChoice != null) ? selectProgramChoice.getValue() : null;
    }

    @FXML
    private void initialize() {
        CollapseButton.setOnAction(e -> { if (onCollapseClick != null) onCollapseClick.run(); });
        ExpandButton.setOnAction(e -> { if (onExpandClick != null) onExpandClick.run(); });

        if (selectProgramChoice != null) {
            selectProgramChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (onProgramSelected == null) return;
                String oldKey = (oldV == null) ? null : oldV.trim().toLowerCase(java.util.Locale.ROOT);
                String newKey = (newV == null) ? null : newV.trim().toLowerCase(java.util.Locale.ROOT);
                if (newKey != null && !newKey.isBlank() && !java.util.Objects.equals(oldKey, newKey)) {
                    onProgramSelected.accept(newV.trim());
                }
            });
            selectProgramChoice.setDisable(true);// disabled until we load a program
        }

        CurrentOrMaxDegreeButton.setMnemonicParsing(false);
        CurrentOrMaxDegreeButton.setOnAction(e -> openDegreeDialog());
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
        CurrentOrMaxDegreeButton.setText("Degree: " + current + "/" + max);
        updateButtons();
    }

    public void setDegreeUiLocked(boolean locked) {
        this.degreeUiLocked = locked;
        updateButtons(); // re-apply disabled state consistently
    }

    public void setOnExpand(Runnable r)   { this.onExpandClick = r; }
    public void setOnCollapse(Runnable r) { this.onCollapseClick = r; }
    public void setOnHighlightChanged(Consumer<String> c) { this.onHighlightChanged = c; }

    public int  getCurrent() { return currentDegree; }
    public int  getMax() { return maxDegree; }
    public void setCurrent(int c) { this.currentDegree = c; bindDegree(c, maxDegree); }

    private void updateButtons() {
        boolean canCollapse = !degreeUiLocked && currentDegree > 0;
        boolean canExpand = !degreeUiLocked && currentDegree < maxDegree;
        boolean canJump = !degreeUiLocked && maxDegree >= 0;
        CollapseButton.setDisable(!canCollapse);
        ExpandButton.setDisable(!canExpand);
        CurrentOrMaxDegreeButton.setDisable(!canJump);
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

    private void openDegreeDialog() {
        if (degreeUiLocked) return;
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Go to Degree");
        dialog.setHeaderText("Enter a degree between 0 and " + maxDegree);
        dialog.initModality(Modality.WINDOW_MODAL);
        if (CurrentOrMaxDegreeButton.getScene() != null)
            dialog.initOwner(CurrentOrMaxDegreeButton.getScene().getWindow());

        ButtonType goType = new ButtonType("Expand", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(goType, ButtonType.CANCEL);

        // Content: Spinner (editable)
        Spinner<Integer> spin = new Spinner<>(0, Math.max(0, maxDegree), Math.min(currentDegree, maxDegree));
        spin.setEditable(true);
        spin.setPrefWidth(120);

        // Validate text edits in the spinner
        Node okBtn = dialog.getDialogPane().lookupButton(goType);
        okBtn.setDisable(false);

        // keep OK disabled on invalid input
        spin.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            okBtn.setDisable(!isValidDegree(newV));
        });

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.add(new Label("Degree:"), 0, 0);
        gp.add(spin, 1, 0);
        dialog.getDialogPane().setContent(gp);

        dialog.setResultConverter(btn -> {
            if (btn == goType) {
                // commit editor text to spinner value
                String txt = spin.getEditor().getText();
                if (isValidDegree(txt)) {
                    spin.getValueFactory().setValue(Integer.parseInt(txt));
                    return spin.getValue();
                }
            }
            return null;
        });

        Optional<Integer> res = dialog.showAndWait();
        res.ifPresent(target -> {
            // update the toolbar label immediately
            bindDegree(target, maxDegree);
            // let the app actually expand to that degree
            if (onJumpToDegree != null) onJumpToDegree.accept(target);
        });
    }

    public void setDegreeButtonEnabled(boolean enabled) {
        if (CurrentOrMaxDegreeButton != null) {
            CurrentOrMaxDegreeButton.setDisable(!enabled);
        }
    }

    public void setPrograms(java.util.List<String> names) {
        if (selectProgramChoice == null) return;
        var items = (names == null) ? List.<String>of() : names;
        selectProgramChoice.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) {
            selectProgramChoice.getSelectionModel().select(0);
            if (onProgramSelected != null) {
                String v = selectProgramChoice.getValue();
                if (v != null && !v.isBlank()) onProgramSelected.accept(v.trim());
            }
        }
        selectProgramChoice.setDisable(items.isEmpty());
    }

    public void setSelectedProgram(String name) {
        if (selectProgramChoice == null || name == null) return;
        selectProgramChoice.getSelectionModel().select(name);
    }

    private boolean isValidDegree(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return v >= 0 && v <= maxDegree;
        } catch (Exception e) {
            return false;
        }
    }

    public void reset() {
        try {
            this.onJumpToDegree   = null;
            this.onHighlightChanged = null;
            this.onProgramSelected  = null;
            this.onExpandClick    = null;
            this.onCollapseClick  = null;
            this.currentDegree = 0;
            this.maxDegree = 0;
            this.degreeUiLocked = false;
            if (CurrentOrMaxDegreeButton != null) {
                CurrentOrMaxDegreeButton.setText("Degree: 0/0");
                CurrentOrMaxDegreeButton.setDisable(false);
            }
            if (HighlightChoices != null) {
                HighlightChoices.setItems(FXCollections.observableArrayList("None"));
                HighlightChoices.getSelectionModel().selectFirst();
                HighlightChoices.setDisable(true);
            }
            if (selectProgramChoice != null) {
                selectProgramChoice.setItems(FXCollections.observableArrayList());
                selectProgramChoice.setDisable(true);
            }
            updateButtons();
        } catch (Throwable ignore) {}
    }
}
