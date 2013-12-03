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
package com.oracle.javafx.scenebuilder.kit.util.control.painteditor.rotateeditor;

import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.gradienteditor.GradientEditor;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

public class RotateEditor extends GridPane {

    @FXML
    private TextField editor_textfield;
    @FXML
    private Button rotator_dial;
    @FXML
    private Button rotator_handle;
    @FXML
    private Label editor_label;

    private final DoubleProperty rotation = new SimpleDoubleProperty();

    public RotateEditor(String text) {
        initialize(text);
    }

    public final DoubleProperty rotationProperty() {
        return rotation;
    }

    public final double getRotationProperty() {
        return rotation.get();
    }

    public final void setRotationProperty(double value) {
        rotation.set(value);

    }

    /**
     * Private
     */
    private void initialize(String text) {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(RotateEditor.class.getResource("RotateEditor.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GradientEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        assert editor_label != null;
        editor_label.setText(text);
    }

    @FXML
    void rotatorTyped(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            rotate(Double.valueOf(editor_textfield.getText()));
            editor_textfield.selectAll();
        }
    }

    @FXML
    void rotatorPressed(MouseEvent e) {
        rotatorDragged(e);
    }

    @FXML
    void rotatorDragged(MouseEvent e) {
        final Parent p = rotator_dial.getParent();
        final Bounds b = rotator_dial.getLayoutBounds();
        final Double centerX = b.getMinX() + (b.getWidth() / 2);
        final Double centerY = b.getMinY() + (b.getHeight() / 2);
        final Point2D center = p.localToParent(centerX, centerY);
        final Point2D mouse = p.localToParent(e.getX(), e.getY());
        final Double deltaX = mouse.getX() - center.getX();
        final Double deltaY = mouse.getY() - center.getY();
        final Double radians = Math.atan2(deltaY, deltaX);
        rotate(Math.toDegrees(radians));
    }

    private void rotate(Double degrees) {
        rotation.set(degrees);
        rotator_handle.setRotate(degrees);
        rotatePrint(degrees);
    }

    private void rotatePrint(Double r) {
        final DecimalFormat df = new DecimalFormat("#.#"); //NOI18N
        editor_textfield.setText(String.valueOf(df.format(r)));
    }
}
