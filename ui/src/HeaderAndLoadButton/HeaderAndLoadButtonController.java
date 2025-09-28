package HeaderAndLoadButton;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.LoadResult;
import emulator.api.dto.ProgramView;
import emulator.api.dto.ProgressListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class HeaderAndLoadButtonController {
    @FXML private Button loadButton;
    @FXML private TextField xmlPathField;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progress;

    private EmulatorEngine engine;
    private Path lastXmlPath;
    private int lastMaxDegree = 0;
    private String lastProgramName;

    public record LoadedEvent(Path xmlPath, String programName, int maxDegree) {}
    private InputsBox.InputsBoxController inputController;
    private Consumer<LoadedEvent> onLoaded;

    @FXML
    private void initialize() {
        assert xmlPathField != null : "xmlPathField not injected";
        assert statusLabel  != null : "statusLabel not injected";
        assert loadButton   != null : "loadButton not injected";
    }

    public void setInputController(InputsBox.InputsBoxController c) { this.inputController = c; }
    public InputsBox.InputsBoxController getInputController() { return this.inputController; }
    public void setEngine(EmulatorEngine engine) { this.engine = Objects.requireNonNull(engine, "engine"); }
    public void setOnLoaded(Consumer<LoadedEvent> onLoaded) { this.onLoaded = onLoaded; }
    public Path getLastXmlPath() { return lastXmlPath; }
    public int getLastMaxDegree() { return lastMaxDegree; }
    public String getLastProgramName() { return lastProgramName; }
    private static String nz(String s) { return s == null ? "" : s; }

    @FXML
    private void handleLoadButtonClick() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open S-Program XML (Exercise 2)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml"));
        File file = fc.showOpenDialog(loadButton.getScene().getWindow());
        if (file == null) return;
        xmlPathField.setText(file.getAbsolutePath());
        runLoadTask(file.toPath());
    }

    private void runLoadTask(Path xmlPath) {
        Task<LoadResult> task = new Task<>() {
            @Override
            protected LoadResult call() throws Exception {
                ProgressListener cb = (stage, fraction) -> {
                    updateMessage(stage);
                    updateProgress(fraction, 1.0);
                };

                updateMessage("Starting...");
                updateProgress(0, 1.0);

                updateMessage("Parsing program...");
                updateProgress(0.05, 1);
                LoadResult res = engine.loadProgram(xmlPath, cb);
                Thread.sleep(150);

                updateMessage("Finishing...");
                updateProgress(0.90, 1);
                Thread.sleep(200);

                updateMessage("Done");
                updateProgress(1, 1);
                return res;
            }
        };

        progress.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        progress.setVisible(true);
        loadButton.setDisable(true);

        task.setOnSucceeded(e -> {
            progress.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progress.setVisible(false);
            loadButton.setDisable(false);

            LoadResult res = task.getValue();
            statusLabel.setText("Loaded: " + nz(res.programName()));
            if (onLoaded != null) {
                onLoaded.accept(new LoadedEvent(xmlPath, res.programName(), res.maxDegree()));
            }
        });

        task.setOnFailed(e -> {
            progress.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progress.setVisible(false);
            loadButton.setDisable(false);

            Throwable ex = task.getException();
            statusLabel.setText("Load failed");
            new Alert(Alert.AlertType.ERROR,
                    "File load failed:\n" + (ex != null ? ex.getMessage() : "Unknown error"),
                    ButtonType.OK).showAndWait();
        });
        new Thread(task, "load-xml-task").start();
    }

    private void openChooserAndLoad() {
        var owner = loadButton.getScene() != null ? loadButton.getScene().getWindow() : null;
        var fc = buildChooser();
        var file = fc.showOpenDialog(owner);
        if (file == null) { statusLabel.setText("Load cancelled."); return; }
        String abs = file.getAbsolutePath();
        xmlPathField.setText(abs);
        doLoad(abs, statusLabel::setText);
    }

    private FileChooser buildChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open S-Program XML");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml"));
        if (lastXmlPath != null && lastXmlPath.getParent() != null) {
            var dir = lastXmlPath.getParent().toFile();
            if (dir.isDirectory()) fc.setInitialDirectory(dir);
        }
        return fc;
    }

    //This func loads an XML file
    public void doLoad(String raw, Consumer<String> printer){
        final String input = (raw == null) ? "" : raw.trim();
        if (input.isEmpty()) { printer.accept("Load cancelled: empty path."); return; }

        final java.nio.file.Path path;
        try { path = java.nio.file.Paths.get(input); }
        catch (RuntimeException e) { printer.accept("Load failed: invalid path syntax."); return; }

        if (!java.nio.file.Files.exists(path)) { printer.accept("Load failed: file not found."); return; }
        if (!input.toLowerCase(java.util.Locale.ROOT).endsWith(".xml")) {
            printer.accept("Load failed: file must have .xml extension."); return;
        }

        try {
            var res = engine.loadProgram(path);
            lastProgramName = res.programName();
            lastMaxDegree = res.maxDegree();
            lastXmlPath = path;
            printer.accept("XML loaded: '" + res.programName() + "' (" +
                    res.instructionCount() + " instructions). Max degree: " + lastMaxDegree);
            if (onLoaded != null) { onLoaded.accept(new LoadedEvent( lastXmlPath, lastProgramName, lastMaxDegree)); }
        } catch (emulator.exception.XmlWrongExtensionException e) {
            printer.accept("Load failed: file must have .xml extension.");
        } catch (emulator.exception.XmlNotFoundException e) {
            printer.accept("Load failed: file not found.");
        } catch (emulator.exception.XmlReadException e) {
            printer.accept("Load failed: XML is malformed – " + e.getMessage());
        } catch (emulator.exception.XmlInvalidContentException e) {
            printer.accept("Load failed: invalid XML content – " + e.getMessage());
        } catch (emulator.exception.InvalidInstructionException e) {
            printer.accept("Load failed: invalid instruction – " + e.getMessage());
        } catch (emulator.exception.MissingLabelException e) {
            printer.accept("Load failed: missing label – " + e.getMessage());
        } catch (emulator.exception.ProgramException e) {
            printer.accept("Load failed: program error – " + e.getMessage());
        } catch (Exception e) {
            printer.accept("Load failed: unexpected error – " + e.getMessage());
        }
    }
}
