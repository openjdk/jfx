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
package com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.gradientpicker;

import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPickerController;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPickerController.Mode;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.colorpicker.ColorPicker;
import com.sun.javafx.Utils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;

/**
 * Controller class for the gradient editor stop.
 */
public class GradientPickerStop extends VBox {

    @FXML
    private Rectangle chip_rect;
    @FXML
    private ImageView indicator_image;
    @FXML
    private TextField offset_textfield;
    @FXML
    private ContextMenu context_menu;
    @FXML
    private CustomMenuItem custom_menu_item;
    @FXML
    private Button stop_button;

    private final double min;
    private final double max;
    private double offset;
    private Color color;
    private boolean isSelected;
    private double origX;
    private double startDragX;
    private double thumbWidth;
    private final double edgeMargin = 2.0;
    private final GradientPicker gradientPicker;

    public GradientPickerStop(GradientPicker ge, double mini, double maxi, double val, Color c) {
        gradientPicker = ge;
        min = mini;
        max = maxi;
        offset = val;
        color = c;
        initialize();
    }

    public void setOffset(double val) {
        offset = Utils.clamp(min, val, max);
        valueToPixels();
    }

    public double getOffset() {
        return offset;
    }

    public void setColor(Color c) {
        color = c;
        chip_rect.setFill(c);
    }

    public Color getColor() {
        return color;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
        if (selected) {
            indicator_image.setVisible(true);
        } else {
            indicator_image.setVisible(false);
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    private void initialize() {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(GradientPickerStop.class.getResource("GradientPickerStop.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GradientPicker.class.getName()).log(Level.SEVERE, null, ex);
        }

        assert offset_textfield != null;
        assert chip_rect != null;

        offset_textfield.setText("" + offset); //NOI18N

        chip_rect.setFill(color);
        gradientPicker.setSelectedStop(this);

        // when we detect a width change, we know node layout is resolved so we position stop in track
        widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                if (newValue.doubleValue() > 0) {
                    thumbWidth = newValue.doubleValue();
                    valueToPixels();
                }
            }
        });
    }

    @FXML
    void stopAction(ActionEvent event) {
        double val = Double.valueOf(offset_textfield.getText());
        setOffset(val);
        showHUD();
        // Called when moving a gradient stop :
        // - update gradient preview accordingly
        // - update model
        final PaintPickerController paintPicker
                = gradientPicker.getPaintPickerController();
        final Mode mode = paintPicker.getMode();
        final Paint value = gradientPicker.getValue(mode);
        gradientPicker.updatePreview(value);
        // Update model
        paintPicker.setPaintProperty(value);
    }

    @FXML
    void thumKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.BACK_SPACE) {
            gradientPicker.removeStop(this);
            // Called when removing a gradient stop :
            // - update gradient preview accordingly
            // - update model
            final PaintPickerController paintPicker
                    = gradientPicker.getPaintPickerController();
            final Mode mode = paintPicker.getMode();
            final Paint value = gradientPicker.getValue(mode);
            gradientPicker.updatePreview(value);
            // Update model
            paintPicker.setPaintProperty(value);
        }
    }

    @FXML
    void thumbMousePressed(MouseEvent event) {
        gradientPicker.setSelectedStop(this);
        startDragX = event.getSceneX();
        origX = getLayoutX();
        toFront(); // make sure this stop is in highest z-order
//        showHUD();
        pixelsToValue();
        // Called when selecting a gradient stop :
        // - update color preview accordingly
        // - do not update the model
        final PaintPickerController paintPicker
                = gradientPicker.getPaintPickerController();
        final ColorPicker colorPicker = paintPicker.getColorPicker();
        colorPicker.updateUI(color);
        stop_button.requestFocus();
    }

    @FXML
    void thumbMouseReleased() {
        pixelsToValue();
    }

    @FXML
    void thumbMouseDragged(MouseEvent event) {
        double dragValue = event.getSceneX() - startDragX;
        double deltaX = origX + dragValue;
        double trackWidth = getParent().getBoundsInLocal().getWidth();
        final Double newX = Utils.clamp(edgeMargin, deltaX, (trackWidth - (getWidth() + edgeMargin)));
        setLayoutX(newX);
//        showHUD();
        pixelsToValue();
        // Called when moving a gradient stop :
        // - update gradient preview accordingly
        // - update model
        final PaintPickerController paintPicker
                = gradientPicker.getPaintPickerController();
        final Mode mode = paintPicker.getMode();
        final Paint value = gradientPicker.getValue(mode);
        gradientPicker.updatePreview(value);
        // Update model
        paintPicker.setPaintProperty(value);
    }

    private void showHUD() {
        final DecimalFormat df = new DecimalFormat("0.00"); //NOI18N
        final String valueString = String.valueOf(df.format(offset));
        offset_textfield.setText(valueString);
        context_menu.show(this, Side.BOTTOM, 0, 5); // better way to center?
    }

    public void valueToPixels() {
        double stopValue = Utils.clamp(min, offset, max);
        double availablePixels = getParent().getLayoutBounds().getWidth() - (thumbWidth + edgeMargin);
        double range = max - min;
        double pixelPosition = ((availablePixels / range) * stopValue);
        setLayoutX(pixelPosition);
    }

    private void pixelsToValue() {
        double range = max - min;
        double availablePixels = getParent().getLayoutBounds().getWidth() - (thumbWidth + edgeMargin);
        setOffset(min + (getLayoutX() * (range / availablePixels)));
    }
}
