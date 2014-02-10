/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.util.control.paintpicker;

import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker.Mode;
import static com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker.Mode.COLOR;
import static com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker.Mode.LINEAR;
import static com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker.Mode.RADIAL;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.colorpicker.ColorPicker;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.gradientpicker.GradientPicker;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.stage.Window;

/**
 * Controller class for the paint editor.
 */
public class PaintPickerController {

    @FXML
    private VBox root_vbox;
    @FXML
    private ToggleButton colorToggleButton;
    @FXML
    private ToggleButton linearToggleButton;
    @FXML
    private ToggleButton radialToggleButton;

    private ColorPicker colorPicker;
    private GradientPicker gradientPicker;
    private PaintPicker.Delegate delegate;

    private final ObjectProperty<Paint> paint = new SimpleObjectProperty<>();

    public final static Color DEFAULT_COLOR = Color.BLACK;
    public final static LinearGradient DEFAULT_LINEAR
            = new LinearGradient(0.0, 0.0, 1.0, 1.0, true, CycleMethod.NO_CYCLE);
    public final static RadialGradient DEFAULT_RADIAL
            = new RadialGradient(0.0, 0.0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE);

    public final ObjectProperty<Paint> paintProperty() {
        return paint;
    }

    public final Paint getPaintProperty() {
        return paint.get();
    }

    public final void setPaintProperty(Paint value) {
        paint.setValue(value);
    }

    public VBox getRoot() {
        return root_vbox;
    }

    public ColorPicker getColorPicker() {
        return colorPicker;
    }

    public GradientPicker getGradientPicker() {
        return gradientPicker;
    }

    public PaintPicker.Delegate getDelegate() {
        return delegate;
    }
    
    /**
     * Simple utility function which clamps the given value to be strictly
     * between the min and max values.
     * @param min
     * @param value
     * @param max
     * @return 
     * @treatAsPrivate
     */
    public static double clamp(double min, double value, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    void setDelegate(PaintPicker.Delegate delegate) {
        this.delegate = delegate;
    }
    
    public Mode getMode() {
        final Mode mode;
        final Paint value = getPaintProperty();
        if (value instanceof Color) {
            mode = Mode.COLOR;
        } else if (value instanceof LinearGradient) {
            mode = Mode.LINEAR;
        } else {
            assert value instanceof RadialGradient;
            mode = Mode.RADIAL;
        }
        return mode;
    }

    public void updateUI(Paint value) {
        if (value != null) {
            setMode(value);
            if (value instanceof Color) {
                colorPicker.updateUI((Color) value);
            } else if (value instanceof LinearGradient) {
                gradientPicker.updateUI((LinearGradient) value);
            } else if (value instanceof RadialGradient) {
                gradientPicker.updateUI((RadialGradient) value);
            } else {
                // Case not yet handled
                assert value instanceof ImagePattern;
            }
        }
    }

    @FXML
    public void initialize() {
        assert root_vbox != null;
        assert colorToggleButton != null;
        assert linearToggleButton != null;
        assert radialToggleButton != null;

        colorPicker = new ColorPicker(this);
        gradientPicker = new GradientPicker(this);

        // Default value
        setPaintProperty(DEFAULT_COLOR);

        // Resize the window so it matches the selected editor size
        root_vbox.heightProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                final Window window = root_vbox.getScene().getWindow();
                window.sizeToScene();
            }
        });
        root_vbox.getChildren().add(colorPicker);
    }

    void setSingleMode(Mode mode) {
        // First disable toggle buttons so we cannot switch from 1 mode to another
        colorToggleButton.setManaged(false);
        linearToggleButton.setManaged(false);
        radialToggleButton.setManaged(false);

        final Paint value;
        switch (mode) {
            case COLOR:
                value = DEFAULT_COLOR;
                break;
            case LINEAR:
                value = DEFAULT_LINEAR;
                break;
            case RADIAL:
                value = DEFAULT_RADIAL;
                break;
            default:
                value = null;
                assert false;
                break;
        }
        // Update model
        setPaintProperty(value);
        // Update UI
        updateUI(value);
    }

    private void setMode(Paint value) {
        if (value instanceof Color) {
            // make sure that a second click doesn't deselect the button
            if (colorToggleButton.isSelected() == false) {
                colorToggleButton.setSelected(true);
            }
            root_vbox.getChildren().remove(gradientPicker);
        } else if (value instanceof LinearGradient) {
            // make sure that a second click doesn't deselect the button
            if (linearToggleButton.isSelected() == false) {
                linearToggleButton.setSelected(true);
            }
            if (!root_vbox.getChildren().contains(gradientPicker)) {
                root_vbox.getChildren().add(gradientPicker);
            }
        } else if (value instanceof RadialGradient) {
            // make sure that a second click doesn't deselect the button
            if (radialToggleButton.isSelected() == false) {
                radialToggleButton.setSelected(true);
            }
            if (!root_vbox.getChildren().contains(gradientPicker)) {
                root_vbox.getChildren().add(gradientPicker);
            }
        } else {
            // Case not yet handled
            assert value instanceof ImagePattern;
        }
    }

    @FXML
    void onColorButtonAction(ActionEvent event) {
        final ToggleButton tb = (ToggleButton) event.getTarget();
        assert tb == colorToggleButton;
        final Color value = colorPicker.getValue();
        // Update UI
        setMode(value);
        // Update model
        setPaintProperty(value);
        event.consume();
    }

    @FXML
    void onLinearButtonAction(ActionEvent event) {
        final ToggleButton tb = (ToggleButton) event.getTarget();
        assert tb == linearToggleButton;
        final Paint value = gradientPicker.getValue(Mode.LINEAR);
        assert value instanceof LinearGradient;
        // Update UI
        setMode(value);
        gradientPicker.setMode(value);
        gradientPicker.updatePreview(value);
        // Update model
        setPaintProperty(value);
        event.consume();
    }

    @FXML
    void onRadialButtonAction(ActionEvent event) {
        final ToggleButton tb = (ToggleButton) event.getTarget();
        assert tb == radialToggleButton;
        final Paint value = gradientPicker.getValue(Mode.RADIAL);
        assert value instanceof RadialGradient;
        // Update UI
        setMode(value);
        gradientPicker.setMode(value);
        gradientPicker.updatePreview(value);
        // Update model
        setPaintProperty(value);
        event.consume();
    }
}
