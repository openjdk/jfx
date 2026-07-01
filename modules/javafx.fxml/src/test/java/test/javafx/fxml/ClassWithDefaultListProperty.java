package test.javafx.fxml;

import javafx.beans.DefaultProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@DefaultProperty("items")
public class ClassWithDefaultListProperty {

    private final ObservableList<String> items = FXCollections.observableArrayList();

    // getter only — no setter → isReadOnly() returns true
    // ObservableList implements List → isAssignableFrom passes
    public ObservableList<String> getItems() {
        return items;
    }
}
