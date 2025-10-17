package Main;

import HeaderAndLoadButton.HeaderAndLoadButtonController;
import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main/main.fxml"));
        Parent root = loader.load();
        MainController main = loader.getController();
        main.initServerMode("http://localhost:8080/semulator/");

        Scene scene = new Scene(root);
        stage.setTitle("S-Emulator (Server Mode)");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
