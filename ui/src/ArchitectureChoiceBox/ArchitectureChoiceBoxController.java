package ArchitectureChoiceBox;

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

public class ArchitectureChoiceBoxController {

    @FXML private ChoiceBox<Architecture> architectureChoiceBox;

    private static final String BASE_URL = "http://localhost:8080/semulator/";
    private static final Gson gson = new Gson();

    private static final Architecture PLACEHOLDER =
            new Architecture("__PLACEHOLDER__", 0, "Select Architecture…");

    @FXML
    public void initialize() {
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

    private void loadArchitecturesFromServer() {
        new Thread(() -> {
            try {
                String url = BASE_URL + "architectures";
                String response = Utils.HttpSessionClient.get(url);
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
        if (architectures != null && !architectures.isEmpty()) {
            architectureChoiceBox.getSelectionModel().select(architectures.get(0));
        } else {
            architectureChoiceBox.getSelectionModel().select(PLACEHOLDER);
        }
        architectureChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Architecture selected: " + (newVal != null ? newVal.name() : "null"));
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
