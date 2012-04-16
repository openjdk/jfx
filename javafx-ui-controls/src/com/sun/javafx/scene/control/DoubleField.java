package com.sun.javafx.scene.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 *
 */
public class DoubleField extends InputField {
    /**
     * The value of the DoubleField. If null, the value will be treated as "0", but
     * will still actually be null.
     */
    private DoubleProperty value = new SimpleDoubleProperty(this, "value");
    public final double getValue() { return value.get(); }
    public final void setValue(double value) { this.value.set(value); }
    public final DoubleProperty valueProperty() { return value; }

    /**
     * Creates a new DoubleField. The style class is set to "money-field".
     */
    public DoubleField() {
        getStyleClass().setAll("double-field");
    }
}