package test.com.sun.javafx.fxml.builder;

import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.util.HashSet;
import java.util.Set;

public class ClassWithReadOnlySet {

    public final String label;
    private final Set<String> properties = new HashSet<>();
    private final ObservableSet<String> observableProperties = FXCollections.observableSet();

    public ClassWithReadOnlySet() {
        this.label = null;
    }

    public ClassWithReadOnlySet(@NamedArg("label") String label) {
        this.label = label;
    }

    /** Read-only map property: getter only, no setter. */
    public Set<String> getProperties() {
        return properties;
    }

    public ObservableSet<String> getObservableProperties() {
        return observableProperties;
    }

}
