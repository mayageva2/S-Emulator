package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import emulator.api.EmulatorEngineImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Main.fxml"));
        Scene scene = new Scene(root);

        stage.setTitle("S-Emulator");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
