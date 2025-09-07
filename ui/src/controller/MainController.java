package controller;

import console.ConsoleApp;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import javafx.fxml.FXML;

public class MainController {
    @FXML
    private HeaderAndLoadButtonController headerController;

    @FXML
    private void initialize() {

        EmulatorEngine engine = new EmulatorEngineImpl();   // Create the shared engine
        ConsoleApp consoleApp = new ConsoleApp(engine);
        headerController.setConsoleApp(consoleApp);
    }
}
