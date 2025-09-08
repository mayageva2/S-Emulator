package HeaderAndLoadButton;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public class HeaderAndLoadButtonController {
    @FXML private Button loadButton;
    @FXML private TextField xmlPathField;
    @FXML private Label statusLabel;

    private EmulatorEngine engine;
    private Path lastXmlPath;
    private int lastMaxDegree = 0;
    private String lastProgramName;

    public record LoadedEvent(Path xmlPath, String programName, int maxDegree) {}
    private Consumer<LoadedEvent> onLoaded;

    @FXML
    private void initialize() {
        assert xmlPathField != null : "xmlPathField not injected";
        assert statusLabel  != null : "statusLabel not injected";
        assert loadButton   != null : "loadButton not injected";
    }

    public void setEngine(EmulatorEngine engine) { this.engine = Objects.requireNonNull(engine, "engine"); }
    public void setOnLoaded(Consumer<LoadedEvent> onLoaded) { this.onLoaded = onLoaded; }
    public Path getLastXmlPath() { return lastXmlPath; }
    public int getLastMaxDegree() { return lastMaxDegree; }
    public String getLastProgramName() { return lastProgramName; }

    @FXML
    private void handleLoadButtonClick() {
        String raw = (xmlPathField.getText() == null) ? "" : xmlPathField.getText().trim();
        statusLabel.setText(raw.isEmpty() ? "Empty path" : "Loading: " + raw);
        doLoad(raw, msg -> statusLabel.setText(msg));
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
            if (onLoaded != null) {
                onLoaded.accept(new LoadedEvent(lastXmlPath, lastProgramName, lastMaxDegree));
            }
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
