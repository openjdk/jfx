/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.util.control.painteditor;

import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.coloreditor.ColorEditor;
import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.gradienteditor.GradientEditor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;

/**
 * Controller class for the paint editor.
 */
public class PaintEditorController {

    public enum Mode {

        // What about ImagePattern ?
        COLOR, LINEAR, RADIAL
    }

    @FXML
    private VBox root_vbox;
    @FXML
    private ToggleButton colorToggleButton;
    @FXML
    private ToggleButton linearToggleButton;
    @FXML
    private ToggleButton radialToggleButton;

    private Mode mode;
    private ColorEditor colorEditor;
    private GradientEditor gradientEditor;

    private final ObjectProperty<Paint> paint = new SimpleObjectProperty<>();

    public final ObjectProperty<Paint> paintProperty() {
        return paint;
    }

    public final Paint getPaintProperty() {
        return paint.get();
    }

    public final void setPaintProperty(Paint value) {
        paint.setValue(value);
        if (value instanceof Color) {
            setMode(Mode.COLOR);
            colorEditor.setColorProperty((Color) value);
        } else if (value instanceof LinearGradient) {
            setMode(Mode.LINEAR);
            gradientEditor.setPaintProperty(value);
        } else if (value instanceof RadialGradient) {
            setMode(Mode.RADIAL);
            gradientEditor.setPaintProperty(value);
//        } else if (value instanceof ImagePattern) {
//            // This case not handled yet
        }
    }

    public final Mode getMode() {
        return mode;
    }

    private void setMode(Mode value) {
        mode = value;
        switch (mode) {
            case COLOR:
                // make sure that a second click doesn't deselect the button
                if (colorToggleButton.isSelected() == false) {
                    colorToggleButton.setSelected(true);
                }
                root_vbox.getChildren().remove(gradientEditor);
                break;
            case LINEAR:
                // make sure that a second click doesn't deselect the button
                if (linearToggleButton.isSelected() == false) {
                    linearToggleButton.setSelected(true);
                }
                gradientEditor.setGradientMode(Mode.LINEAR);
                if (!root_vbox.getChildren().contains(gradientEditor)) {
                    root_vbox.getChildren().add(gradientEditor);
                }
                break;
            case RADIAL:
                // make sure that a second click doesn't deselect the button
                if (radialToggleButton.isSelected() == false) {
                    radialToggleButton.setSelected(true);
                }
                gradientEditor.setGradientMode(Mode.RADIAL);
                if (!root_vbox.getChildren().contains(gradientEditor)) {
                    root_vbox.getChildren().add(gradientEditor);
                }
                break;
            default:
                assert false;
        }
    }

    @FXML
    public void initialize() {
        assert root_vbox != null;
        assert colorToggleButton != null;
        assert linearToggleButton != null;
        assert radialToggleButton != null;

        colorEditor = new ColorEditor(this);
        gradientEditor = new GradientEditor(this);

        colorEditor.colorProperty().addListener(new ChangeListener<Color>() {
            @Override
            public void changed(ObservableValue<? extends Color> ov, Color oldValue, Color newValue) {
                // The color editor is common to both color and gradient items.
                // If mode == COLOR, we update the paint property
                // If mode == LINEAR/RADIAL, the paint property is updated with 
                // the gradient editor value.
                if (mode == Mode.COLOR) {
                    paint.setValue(newValue);
                }
            }
        });
        gradientEditor.paintProperty().addListener(new ChangeListener<Paint>() {
            @Override
            public void changed(ObservableValue<? extends Paint> ov, Paint oldValue, Paint newValue) {
                paint.setValue(newValue);
            }
        });

        root_vbox.getChildren().add(colorEditor);
    }

    public VBox getRoot() {
        return root_vbox;
    }

    public ColorEditor getColorEditor() {
        return colorEditor;
    }

    public GradientEditor getGradientEditor() {
        return gradientEditor;
    }

    @FXML
    void onColorButtonAction(ActionEvent event) {
        final ToggleButton tb = (ToggleButton) event.getTarget();
        assert tb == colorToggleButton;
        setMode(Mode.COLOR);
        paint.setValue(colorEditor.getColorProperty());
        event.consume();
    }

    @FXML
    void onLinearButtonAction(ActionEvent event) {
        final ToggleButton tb = (ToggleButton) event.getTarget();
        assert tb == linearToggleButton;
        setMode(Mode.LINEAR);
        paint.setValue(gradientEditor.getPaintProperty());
        event.consume();
    }

    @FXML
    void onRadialButtonAction(ActionEvent event) {
        final ToggleButton tb = (ToggleButton) event.getTarget();
        assert tb == radialToggleButton;
        setMode(Mode.RADIAL);
        paint.setValue(gradientEditor.getPaintProperty());
        event.consume();
    }
}
