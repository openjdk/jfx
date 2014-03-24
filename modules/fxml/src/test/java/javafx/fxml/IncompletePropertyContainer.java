package javafx.fxml;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
* Created by msladecek on 3/24/14.
*/
public class IncompletePropertyContainer {

    private StringProperty prop = new SimpleStringProperty("");

    public String getProp() {
        return prop.get();
    }

    public void setProp(String s) {
        prop.set(s);
    }

    public StringProperty propProperty() {
        return prop;
    }
}
