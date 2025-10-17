package ExecutionHeader;

import emulator.logic.user.UserManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.Timer;
import java.util.TimerTask;

public class ExecutionHeaderController {
    @FXML private Label lblUsername;
    @FXML private Label lblCredits;

    @FXML
    public void initialize() {
        updateUserHeader();

        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                javafx.application.Platform.runLater(ExecutionHeaderController.this::updateUserHeader);
            }
        }, 2000, 2000);
    }

    private void updateUserHeader() {
        UserManager.getCurrentUser().ifPresentOrElse(u -> {
            lblUsername.setText("User: " + u.getUsername());
            lblCredits.setText("Available Credits: " + u.getCredits());
        }, () -> {
            lblUsername.setText("User: (none)");
            lblCredits.setText("Available Credits: —");
        });
    }
}
