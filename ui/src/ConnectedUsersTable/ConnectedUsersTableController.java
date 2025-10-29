package ConnectedUsersTable;

import Main.Dashboard.mainDashboardController;
import Utils.ClientContext;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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
    private HttpSessionClient httpClient;
    private String baseUrl;
    private mainDashboardController dashboardController;

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setDashboardController(mainDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        this.httpClient = ClientContext.getHttpClient();
        this.baseUrl = ClientContext.getBaseUrl();

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colMainPrograms.setCellValueFactory(new PropertyValueFactory<>("mainPrograms"));
        colFunctions.setCellValueFactory(new PropertyValueFactory<>("functions"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
        colUsedCredits.setCellValueFactory(new PropertyValueFactory<>("usedCredits"));
        colRuns.setCellValueFactory(new PropertyValueFactory<>("runs"));

        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (var col : usersTable.getColumns()) col.setSortable(false);
        usersTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            usersTable.getStylesheets().add(css.toExternalForm());
            usersTable.getStyleClass().add("instructions");
        }

        refreshUsers();
    }

    public void refreshUsers() {
        try {
            String json = httpClient.get(baseUrl + "user/list");
            Map<String, Object> response = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            if (!"success".equals(response.get("status"))) return;

            List<Map<String, Object>> users = (List<Map<String, Object>>) response.get("users");
            List<UserRow> rows = new ArrayList<>();
            for (Map<String, Object> u : users) {
                rows.add(new UserRow(
                        (String) u.getOrDefault("username", "Unknown"),
                        toInt(u.get("mainPrograms")),
                        toInt(u.get("functions")),
                        toInt(u.get("credits")),
                        toInt(u.get("usedCredits")),
                        toInt(u.get("runs"))
                ));
            }

            usersTable.getItems().setAll(rows);

        } catch (Exception e) {
            System.err.println("Failed to refresh users: " + e.getMessage());
        }
    }

    @FXML
    private void onUnselectUserClicked() {
        usersTable.getSelectionModel().clearSelection();

        if (dashboardController != null) {
            String currentUser = dashboardController.getCurrentUsername();
            if (currentUser != null && !currentUser.isBlank()) {
                dashboardController.getStatisticsController().loadUserHistory(baseUrl, currentUser);
            }
        }
    }

    public TableView<UserRow> getUsersTable() {
        return usersTable;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
}
