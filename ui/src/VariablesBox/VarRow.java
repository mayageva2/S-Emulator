package VariablesBox;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class VarRow {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty value = new SimpleStringProperty();

    public VarRow(String name, String value) {
        this.name.set(name);
        this.value.set(value);
    }

    public StringProperty nameProperty()  { return name; }
    public StringProperty valueProperty() { return value; }


}
