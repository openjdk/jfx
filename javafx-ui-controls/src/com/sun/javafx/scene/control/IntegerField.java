package com.sun.javafx.scene.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 *
 */
public class IntegerField extends InputField {
    /**
     * The value of the IntegerField. If null, the value will be treated as "0", but
     * will still actually be null.
     */
    private IntegerProperty value = new SimpleIntegerProperty(this, "value");
    public final int getValue() { return value.get(); }
    public final void setValue(int value) { this.value.set(value); }
    public final IntegerProperty valueProperty() { return value; }

    /**
     * Creates a new IntegerField. The style class is set to "money-field".
     */
    public IntegerField() {
        getStyleClass().setAll("integer-field");
    }
}