package com.sun.javafx.scene.control.skin;

import com.sun.javafx.scene.control.skin.InputField;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;

/**
 *
 */
class WebColorField extends InputField {
    /**
     * The value of the WebColorField. If null, the value will be treated as "#000000" black, but
     * will still actually be null.
     */
    private ObjectProperty<Color> value = new SimpleObjectProperty<Color>(this, "value");
    public final Color getValue() { return value.get(); }
    public final void setValue(Color value) { this.value.set(value); }
    public final ObjectProperty<Color> valueProperty() { return value; }

    /**
     * Creates a new WebColorField. The style class is set to "webcolor-field".
     */
    public WebColorField() {
        getStyleClass().setAll("webcolor-field");
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new WebColorFieldSkin(this);
    }
}