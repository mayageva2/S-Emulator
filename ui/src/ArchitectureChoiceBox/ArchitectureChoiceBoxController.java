package ArchitectureChoiceBox;

import Utils.ClientContext;
import Utils.HttpSessionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ArchitectureChoiceBoxController {

    @FXML private ChoiceBox<Architecture> architectureChoiceBox;

    private final Gson gson = new Gson();
    private HttpSessionClient httpClient;
    private String baseUrl;

    private Consumer<Architecture> onArchitectureSelected;
    public void setOnArchitectureSelected(Consumer<Architecture> listener) {
        this.onArchitectureSelected = listener;
    }

    private static final Architecture PLACEHOLDER =
            new Architecture("__PLACEHOLDER__", 0, "Select Architecture…");

    public void setHttpClient(HttpSessionClient client) {
        this.httpClient = client;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @FXML
    public void initialize() {
        this.httpClient = ClientContext.getHttpClient();
        this.baseUrl = ClientContext.getBaseUrl();

        architectureChoiceBox.setConverter(new StringConverter<>() {
            @Override public String toString(Architecture a) {
                if (a == null) return "";
                if (a == PLACEHOLDER) return a.description();
                return a.name() + " (" + a.cost() + " credits)";
            }
            @Override public Architecture fromString(String s) { return null; }
        });

        architectureChoiceBox.getItems().setAll(PLACEHOLDER);
        architectureChoiceBox.getSelectionModel().select(PLACEHOLDER);

        loadArchitecturesFromServer();
    }

    public void selectArchitectureByName(String name) {
        if (name == null || name.isBlank()) {
            System.out.println("selectArchitectureByName called with null/blank name");
            return;
        }

        Platform.runLater(() -> {
            for (Architecture a : architectureChoiceBox.getItems()) {
                if (a != null && a.name().equalsIgnoreCase(name.trim())) {
                    architectureChoiceBox.getSelectionModel().select(a);
                    System.out.println("Architecture auto-selected → " + a.name());
                    return;
                }
            }
            architectureChoiceBox.getSelectionModel().select(PLACEHOLDER);
            System.out.println("Architecture name not found → fallback to placeholder");
        });
    }

    private void loadArchitecturesFromServer() {
        new Thread(() -> {
            try {
                String url = baseUrl + "architectures";
                String response = httpClient.get(url);
                System.out.println("[architectures] response: " + response);

                List<Architecture> architectures = parseArchitecturesResponse(response);
                Platform.runLater(() -> setupChoiceBox(architectures));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> setupChoiceBox(List.of()));
            }
        }).start();
    }

    private List<Architecture> parseArchitecturesResponse(String response) {
        if (response == null || response.isBlank()) return List.of();
        try {
            Type wrapperType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> wrapper = gson.fromJson(response, wrapperType);
            Object arr = wrapper.get("architectures");
            if (arr != null) {
                String arrJson = gson.toJson(arr);
                Type listType = new TypeToken<List<Architecture>>() {}.getType();
                return gson.fromJson(arrJson, listType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    private void setupChoiceBox(List<Architecture> architectures) {
        List<Architecture> items = new ArrayList<>();
        items.add(PLACEHOLDER);
        if (architectures != null) items.addAll(architectures);

        architectureChoiceBox.getItems().setAll(items);
        architectureChoiceBox.getSelectionModel().select(PLACEHOLDER);

        architectureChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Architecture selected: " + (newVal != null ? newVal.name() : "null"));
            if (onArchitectureSelected != null && newVal != null && newVal != PLACEHOLDER) {
                onArchitectureSelected.accept(newVal);
            }
        });
    }

    public Architecture getSelectedArchitecture() {
        Architecture a = architectureChoiceBox.getValue();
        System.out.println("getSelectedArchitecture() → " + (a != null ? a.name() : "null"));
        return (a == PLACEHOLDER) ? null : a;
    }

    public boolean isSelectionValid() {
        return architectureChoiceBox.getValue() != null && architectureChoiceBox.getValue() != PLACEHOLDER;
    }

    public record Architecture(String name, int cost, String description) {}
}
