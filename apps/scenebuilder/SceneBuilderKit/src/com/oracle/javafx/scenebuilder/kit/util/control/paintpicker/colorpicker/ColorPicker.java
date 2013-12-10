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
package com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.colorpicker;

import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPickerController;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPickerController.Mode;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.gradientpicker.GradientPicker;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.gradientpicker.GradientPickerStop;
import com.sun.javafx.Utils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Circle;

/**
 * Controller class for the color part of the paint editor.
 */
public class ColorPicker extends VBox {

    @FXML
    private Region chip_region;
    @FXML
    private Region alpha_region;
    @FXML
    private ScrollPane picker_scrollpane;
    @FXML
    private Region picker_region;
    @FXML
    private StackPane picker_handle_stackpane;
    @FXML
    private Circle picker_handle_chip_circle;
    @FXML
    private Slider hue_slider;
    @FXML
    private Slider alpha_slider;
    @FXML
    private TextField hue_textfield;
    @FXML
    private TextField saturation_textfield;
    @FXML
    private TextField brightness_textfield;
    @FXML
    private TextField red_textfield;
    @FXML
    private TextField green_textfield;
    @FXML
    private TextField blue_textfield;
    @FXML
    private TextField alpha_textfield;
    @FXML
    private TextField hexa_textfield;

    private final PaintPickerController paintPickerController;
    private boolean updating = false;

    private final static Color DEFAULT = Color.BLACK;

    public ColorPicker(PaintPickerController pe) {
        paintPickerController = pe;
        initialize();
    }

    public void reset() {
    }

    public Color getValue() {
        double hue = Double.valueOf(hue_textfield.getText());
        double saturation = Double.valueOf(saturation_textfield.getText()) / 100.0;
        double brightness = Double.valueOf(brightness_textfield.getText()) / 100.0;
        double alpha = Double.valueOf(alpha_textfield.getText());
        return Color.hsb(hue, saturation, brightness, alpha);
    }

    public void updateUI(final Color color) {
        double hue = color.getHue();
        double saturation = color.getSaturation();
        double brightness = color.getBrightness();
        double alpha = color.getOpacity();
        updateUI(hue, saturation, brightness, alpha);
    }

    /**
     * Private
     */
    private void initialize() {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ColorPicker.class.getResource("ColorPicker.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(ColorPicker.class.getName()).log(Level.SEVERE, null, ex);
        }

        assert hue_slider != null;
        assert picker_region != null;
        assert hue_textfield != null;
        assert saturation_textfield != null;
        assert brightness_textfield != null;
        assert alpha_textfield != null;
        assert red_textfield != null;
        assert green_textfield != null;
        assert blue_textfield != null;
        assert alpha_slider != null;

        hue_slider.setStyle(makeHueSliderCSS()); // Make the grad for hue slider

        // Investigate why height + width listeners do not work
        // Indeed, the picker_handle_stackpane bounds may still be null at this point
        // UPDATE BELOW TO BE CALLED ONCE ONLY AT DISPLAY TIME
        picker_region.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> ov, Bounds oldb, Bounds newb) {
                picker_scrollpane.setHvalue(0.5);
                picker_scrollpane.setVvalue(0.5);
                // Init time only
                final Paint paint = paintPickerController.getPaintProperty();
                if (paint instanceof Color) {
                    updateUI((Color) paint);
                } else if (paint instanceof LinearGradient
                        || paint instanceof RadialGradient) {
                    final GradientPicker gradientPicker = paintPickerController.getGradientPicker();
                    final GradientPickerStop gradientPickerStop = gradientPicker.getSelectedStop();
                    // Update the color preview with the color of the selected stop
                    if (gradientPickerStop != null) {
                        updateUI(gradientPickerStop.getColor());
                    }
                }
            }
        });

        final ChangeListener<Boolean> onHSBFocusedChange = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                if (newValue == false) {
                    // Update UI
                    final Color color = updateUI_OnHSBChange();
                    // Update model
                    setPaintProperty(color);
                }
            }
        };
        final ChangeListener<Boolean> onRGBFocusedChange = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                if (newValue == false) {
                    // Update UI
                    final Color color = updateUI_OnRGBChange();
                    // Update model
                    setPaintProperty(color);
                }
            }
        };
        final ChangeListener<Boolean> onHexaFocusedChange = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                if (newValue == false) {
                    // Update UI
                    final Color color = updateUI_OnHexaChange();
                    // Update model
                    setPaintProperty(color);
                }
            }
        };

        // TextField ON FOCUS LOST event handler
        hue_textfield.focusedProperty().addListener(onHSBFocusedChange);
        saturation_textfield.focusedProperty().addListener(onHSBFocusedChange);
        brightness_textfield.focusedProperty().addListener(onHSBFocusedChange);
        alpha_textfield.focusedProperty().addListener(onHSBFocusedChange);
        red_textfield.focusedProperty().addListener(onRGBFocusedChange);
        green_textfield.focusedProperty().addListener(onRGBFocusedChange);
        blue_textfield.focusedProperty().addListener(onRGBFocusedChange);
        hexa_textfield.focusedProperty().addListener(onHexaFocusedChange);

        // Slider ON VALUE CHANGE event handler
        hue_slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                if (updating == true) {
                    return;
                }
                double hue = newValue.doubleValue();
                // retrieve HSB TextFields values
                double saturation = Double.valueOf(saturation_textfield.getText()) / 100.0;
                double brightness = Double.valueOf(brightness_textfield.getText()) / 100.0;
                double alpha = Double.valueOf(alpha_textfield.getText());
                // Update UI
                final Color color = updateUI(hue, saturation, brightness, alpha);
                // Update model
                setPaintProperty(color);
            }
        });
        alpha_slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                if (updating == true) {
                    return;
                }
                double alpha = newValue.doubleValue();
                // retrieve HSB TextFields values
                double hue = Double.valueOf(hue_textfield.getText());
                double saturation = Double.valueOf(saturation_textfield.getText()) / 100.0;
                double brightness = Double.valueOf(brightness_textfield.getText()) / 100.0;
                // Update UI
                final Color color = updateUI(hue, saturation, brightness, alpha);
            // Update model
                setPaintProperty(color);
            }
        });
    }

    /**
     * When updating the color picker, we may update :
     * - either the color of the paint picker itself (Color mode)
     * - or the color of the selected stop (LinearGradient or RadialGradient mode)
     *
     * @param color
     */
    private void setPaintProperty(Color color) {
        final Mode mode = paintPickerController.getMode();
        final Paint paint;
        switch (mode) {
            case COLOR:
                paint = color;
                break;
            case LINEAR:
            case RADIAL:
                final GradientPicker gradientPicker = paintPickerController.getGradientPicker();
                final GradientPickerStop gradientPickerStop = gradientPicker.getSelectedStop();
                // Set the color of the selected stop
                if (gradientPickerStop != null) {
                    gradientPickerStop.setColor(color);
                }
                // Update gradient preview
                paint = gradientPicker.getValue(mode);
                gradientPicker.updatePreview(paint);
                break;
            default:
                paint = null;
                break;
        }
        paintPickerController.setPaintProperty(paint);
    }

    @FXML
    void onActionHue(ActionEvent event) {
        onHSBChange(event);
    }

    @FXML
    void onActionSaturation(ActionEvent event) {
        onHSBChange(event);
    }

    @FXML
    void onActionBrightness(ActionEvent event) {
        onHSBChange(event);
    }

    @FXML
    void onActionAlpha(ActionEvent event) {
        onHSBChange(event);
    }

    @FXML
    void onActionRed(ActionEvent event) {
        onRGBChange(event);
    }

    @FXML
    void onActionGreen(ActionEvent event) {
        onRGBChange(event);
    }

    @FXML
    void onActionBlue(ActionEvent event) {
        onRGBChange(event);
    }

    @FXML
    void onActionHexa(ActionEvent event) {
        onHexaChange(event);
    }

    private void onHSBChange(ActionEvent event) {
        // Update UI
        final Color color = updateUI_OnHSBChange();
        final Object source = event.getSource();
        assert source instanceof TextField;
        ((TextField) source).selectAll();
        // Update model
        setPaintProperty(color);
    }

    private void onRGBChange(ActionEvent event) {
        // Update UI
        final Color color = updateUI_OnRGBChange();
        final Object source = event.getSource();
        assert source instanceof TextField;
        ((TextField) source).selectAll();
        // Update model
        setPaintProperty(color);
    }

    private void onHexaChange(ActionEvent event) {
        // Update UI
        final Color color = updateUI_OnHexaChange();
        final Object source = event.getSource();
        assert source instanceof TextField;
        ((TextField) source).selectAll();
        // Update model
        setPaintProperty(color);
    }

    @FXML
    void onPickerRegionPressed(MouseEvent e) {
        double mx = e.getX();
        double my = e.getY();
        // Update UI
        final Color color = updateUI_OnPickerChange(mx, my);
        // Update model
        setPaintProperty(color);
    }

    @FXML
    void onPickerRegionDragged(MouseEvent e) {
        double mx = e.getX();
        double my = e.getY();
        // Update UI
        final Color color = updateUI_OnPickerChange(mx, my);
        // Update model
        setPaintProperty(color);
    }

    private Color updateUI_OnPickerChange(double x, double y) {
        double w = picker_region.getWidth();
        double h = picker_region.getHeight();
        double hue = Double.valueOf(hue_textfield.getText());
        double saturation = x / w;
        double brightness = 1.0 - (y / h);
        double alpha = Double.valueOf(alpha_textfield.getText());
        return updateUI(hue, saturation, brightness, alpha);
    }

    private Color updateUI_OnHSBChange() {
        // retrieve HSB TextFields values
        double hue = Double.valueOf(hue_textfield.getText());
        double saturation = Double.valueOf(saturation_textfield.getText()) / 100.0;
        double brightness = Double.valueOf(brightness_textfield.getText()) / 100.0;
        double alpha = Double.valueOf(alpha_textfield.getText());
        return updateUI(hue, saturation, brightness, alpha);
    }

    private Color updateUI_OnRGBChange() {
        // retrieve RGB TextFields values
        double red = Double.valueOf(red_textfield.getText());
        double green = Double.valueOf(green_textfield.getText());
        double blue = Double.valueOf(blue_textfield.getText());
        // retrieve HSB values from RGB values
        double hue = getHueFromRGB(red, green, blue);
        double saturation = getSaturationFromRGB(red, green, blue);
        double brightness = getBrightnessFromRGB(red, green, blue);
        double alpha = Double.valueOf(alpha_textfield.getText());
        return updateUI(hue, saturation, brightness, alpha);
    }

    private Color updateUI_OnHexaChange() {
        // retrieve Hexa TextField value
        final String hexa = hexa_textfield.getText();
        // retrieve HSB values from RGB values
        double hue = getHueFromHexa(hexa);
        double saturation = getSaturationFromHexa(hexa);
        double brightness = getBrightnessFromHexa(hexa);
        double alpha = Double.valueOf(alpha_textfield.getText());
        return updateUI(hue, saturation, brightness, alpha);
    }

    private Color updateUI(double hue, double saturation, double brightness, double alpha) {

        updating = true;

        // update the HSB values so they are within range
        hue = Utils.clamp(0, hue, 360);
        saturation = Utils.clamp(0, saturation, 1);
        brightness = Utils.clamp(0, brightness, 1);
        alpha = Utils.clamp(0, alpha, 1);
        // make an rgb color from the hsb
        final Color color = Color.hsb(hue, saturation, brightness, alpha);
        double[] rgb = Utils.HSBtoRGB(color.getHue(), color.getSaturation(), color.getBrightness());
        int red = (int) (rgb[0] * 255);
        int green = (int) (rgb[1] * 255);
        int blue = (int) (rgb[2] * 255);
        final String hexa = String.format("%02X%02X%02X", red, green, blue); //NOI18N
        final DecimalFormat df = new DecimalFormat("0.0#"); //NOI18N

        // Set TextFields value
        hue_textfield.setText(String.valueOf((int) hue));
        saturation_textfield.setText(String.valueOf((int) (saturation * 100)));
        brightness_textfield.setText(String.valueOf((int) (brightness * 100)));
        alpha_textfield.setText(df.format(alpha));
        red_textfield.setText(Integer.toString(red));
        green_textfield.setText(Integer.toString(green));
        blue_textfield.setText(Integer.toString(blue));
        hexa_textfield.setText("#" + hexa); //NOI18N

        // Set the background color of the chips
        final StringBuilder sb = new StringBuilder();
        sb.append("hsb("); //NOI18N
        sb.append(hue);
        sb.append(", "); //NOI18N
        sb.append(saturation * 100);
        sb.append("%, "); //NOI18N
        sb.append(brightness * 100);
        sb.append("%, "); //NOI18N
        sb.append(alpha);
        sb.append(")"); //NOI18N
        final String hsbCssValue = sb.toString();
        final String chipStyle = "-fx-background-color: " + hsbCssValue; //NOI18N
        chip_region.setStyle(chipStyle);
        picker_handle_chip_circle.setFill(Color.rgb(red, green, blue));
        final String alphaChipStyle = "-fx-background-color: " //NOI18N
                + "linear-gradient(to right, transparent, " + hsbCssValue + ")"; //NOI18N
        alpha_region.setStyle(alphaChipStyle);

        // Set the background color of the picker region
        // (force saturation and brightness to 100% - don't add opacity)
        final String pickerRegionStyle = "-fx-background-color: hsb(" //NOI18N
                + hue + ", 100%, 100%, 1.0);"; //NOI18N
        picker_region.setStyle(pickerRegionStyle);

        // Position the picker dot
        double xSat = picker_region.getWidth() * saturation; // Saturation is on x axis
        double yBri = picker_region.getHeight() * (1.0 - brightness); // Brightness is on y axis (reversed as white is top)
        double xPos = (picker_region.getBoundsInParent().getMinX() + xSat) - picker_handle_stackpane.getWidth() / 2;
        double yPos = (picker_region.getBoundsInParent().getMinY() + yBri) - picker_handle_stackpane.getHeight() / 2;
        picker_handle_stackpane.setLayoutX(xPos);
        picker_handle_stackpane.setLayoutY(yPos);

        // Set the Sliders value
        hue_slider.adjustValue(hue);
        alpha_slider.adjustValue(alpha);

        updating = false;
        return color;
    }

    private String makeHueSliderCSS() {
        final StringBuilder sb = new StringBuilder();
        sb.append("-fx-background-color: linear-gradient(to right "); //NOI18N
        for (int i = 0; i < 12; i++) { // max 12 gradient stops
            sb.append(", hsb("); //NOI18N
            sb.append(i * (360 / 11));
            sb.append(", 100%, 100%)"); //NOI18N
        }
        sb.append(");"); //NOI18N
        return sb.toString();
    }

    private double getHueFromRGB(double red, double green, double blue) {
        double[] hsb = Utils.RGBtoHSB(red, green, blue);
        return hsb[0];
    }

    private double getSaturationFromRGB(double red, double green, double blue) {
        double[] hsb = Utils.RGBtoHSB(red, green, blue);
        return hsb[1];
    }

    private double getBrightnessFromRGB(double red, double green, double blue) {
        double[] hsb = Utils.RGBtoHSB(red, green, blue);
        return hsb[2];
    }

    private double getHueFromHexa(String hexa) {
        final Color c = Color.web(hexa);
        return c.getHue();
    }

    private double getSaturationFromHexa(String hexa) {
        final Color c = Color.web(hexa);
        return c.getSaturation();
    }

    private double getBrightnessFromHexa(String hexa) {
        final Color c = Color.web(hexa);
        return c.getBrightness();
    }
}
