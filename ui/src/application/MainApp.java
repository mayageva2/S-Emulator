package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/components/headerAndLoadButton.fxml"));
        Parent root = loader.load();

        var engine = new emulator.api.EmulatorEngineImpl();
        var consoleApp = new console.ConsoleApp(engine);

        controller.HeaderAndLoadButtonController ctrl = loader.getController();
        if (ctrl == null) throw new IllegalStateException("Controller is null. Check fx:controller in FXML.");
        ctrl.setConsoleApp(consoleApp);

        stage.setScene(new Scene(root));
        stage.setTitle("S-Emulator");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
