package ConnectedUsersTable;

import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConnectedUsersTableController {

    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> colUsername;
    @FXML private TableColumn<UserRow, Integer> colMainPrograms;
    @FXML private TableColumn<UserRow, Integer> colFunctions;
    @FXML private TableColumn<UserRow, Integer> colCredits;
    @FXML private TableColumn<UserRow, Integer> colUsedCredits;
    @FXML private TableColumn<UserRow, Integer> colRuns;

    private final Gson gson = new Gson();
    private String baseUrl = "http://localhost:8080/semulator/";
    public void setBaseUrl(String baseUrl) {this.baseUrl = baseUrl;}

    @FXML
    private void initialize() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colMainPrograms.setCellValueFactory(new PropertyValueFactory<>("mainPrograms"));
        colFunctions.setCellValueFactory(new PropertyValueFactory<>("functions"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
        colUsedCredits.setCellValueFactory(new PropertyValueFactory<>("usedCredits"));
        colRuns.setCellValueFactory(new PropertyValueFactory<>("runs"));

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            usersTable.getStylesheets().add(css.toExternalForm());
            usersTable.getStyleClass().add("instructions");
        }

        refreshUsers();
    }

    public void refreshUsers() {
        try {
            String json = HttpSessionClient.get(baseUrl + "user/list");
            Map<String, Object> response = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            if (!"success".equals(response.get("status"))) return;

            List<Map<String, Object>> users = (List<Map<String, Object>>) response.get("users");
            List<UserRow> rows = new ArrayList<>();
            for (Map<String, Object> u : users) {
                rows.add(new UserRow(
                        (String) u.get("username"),
                        ((Number) u.get("mainPrograms")).intValue(),
                        ((Number) u.get("functions")).intValue(),
                        ((Number) u.get("credits")).intValue(),
                        ((Number) u.get("usedCredits")).intValue(),
                        ((Number) u.get("runs")).intValue()
                ));
            }

            usersTable.getItems().setAll(rows);

        } catch (Exception e) {
            System.err.println("Failed to refresh users: " + e.getMessage());
        }
    }

    public TableView<UserRow> getUsersTable() {return usersTable;}
}
