package controller;

import console.ConsoleApp;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HeaderAndLoadButtonController {
    @FXML private Button LoadButton;
    @FXML private TextField xmlPathField;
    @FXML private Label statusLabel;

    private ConsoleApp consoleApp;

    public void setConsoleApp(ConsoleApp app) {
        this.consoleApp = app;
    }

    @FXML
    private void handleLoadButtonClick() {
        String raw = (xmlPathField.getText() == null) ? "" : xmlPathField.getText().trim();
        statusLabel.setText(raw.isEmpty() ? "Empty path" : "Loading: " + raw);
        consoleApp.doLoad(raw, msg -> statusLabel.setText(msg));
    }

    @FXML
    private void initialize() {
        assert xmlPathField != null : "xmlPathField not injected";
        assert statusLabel  != null : "statusLabel not injected";
        assert LoadButton   != null : "LoadButton not injected";
    }

}
