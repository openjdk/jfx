package test.javafx.fxml;

import javafx.beans.DefaultProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

@DefaultProperty("items")
public class ClassWithDefaultSetProperty {

    private final ObservableSet<String> items = FXCollections.observableSet();

    // getter only — no setter → isReadOnly() returns true
    // ObservableSet implements Set → isAssignableFrom passes
    public ObservableSet<String> getItems() {
        return items;
    }
}
