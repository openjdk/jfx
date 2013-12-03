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
package com.oracle.javafx.scenebuilder.kit.util.control.painteditor.coloreditor;

import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.PaintEditorController;
import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.PaintEditorController.Mode;
import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.gradienteditor.GradientEditor;
import com.sun.javafx.Utils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Controller class for the color part of the paint editor.
 */
public class ColorEditor extends VBox {

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

    private final PaintEditorController paintEditor;
    private boolean updating = false;

    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.WHITE);

    public ColorEditor(PaintEditorController pe) {
        paintEditor = pe;
        initialize();
    }

    public final ObjectProperty<Color> colorProperty() {
        return color;
    }

    public final Color getColorProperty() {
        return color.get();
    }

    public final void setColorProperty(Color value) {
        color.setValue(value);
        updateUI(value);
    }

    /**
     * Private
     */
    private void initialize() {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ColorEditor.class.getResource("ColorEditor.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(ColorEditor.class.getName()).log(Level.SEVERE, null, ex);
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

        picker_region.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> ov, Bounds oldb, Bounds newb) {
                picker_scrollpane.setHvalue(0.5);
                picker_scrollpane.setVvalue(0.5);
                // Init time only
                updateUI(getColorProperty());
            }
        });

        // Update on focus lost
        final ChangeListener<Boolean> hsbFocusedPropertyListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                if (newValue == false) {
                    updateHSB();
                }
            }
        };
        hue_textfield.focusedProperty().addListener(hsbFocusedPropertyListener);
        saturation_textfield.focusedProperty().addListener(hsbFocusedPropertyListener);
        brightness_textfield.focusedProperty().addListener(hsbFocusedPropertyListener);
        alpha_textfield.focusedProperty().addListener(hsbFocusedPropertyListener);

        final ChangeListener<Boolean> rgbFocusedPropertyListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                if (newValue == false) {
                    updateRGB();
                }
            }
        };
        red_textfield.focusedProperty().addListener(rgbFocusedPropertyListener);
        green_textfield.focusedProperty().addListener(rgbFocusedPropertyListener);
        blue_textfield.focusedProperty().addListener(rgbFocusedPropertyListener);

        // Update on slider value change
        hue_slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                if (updating == true) {
                    return;
                }
                double hue = newValue.doubleValue();
                double saturation = getColorProperty().getSaturation();
                double brightness = getColorProperty().getBrightness();
                double alpha = getColorProperty().getOpacity();
                final Color c = updateUI(hue, saturation, brightness, alpha);
                color.setValue(c);
            }
        });
        alpha_slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                if (updating == true) {
                    return;
                }
                double hue = getColorProperty().getHue();
                double saturation = getColorProperty().getSaturation();
                double brightness = getColorProperty().getBrightness();
                double alpha = newValue.doubleValue();
                final Color c = updateUI(hue, saturation, brightness, alpha);
                color.setValue(c);
            }
        });
    }

    @FXML
    void onHueKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            updateHSB();
        }
    }

    @FXML
    void onSaturationKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            updateHSB();
        }
    }

    @FXML
    void onBrightnessKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            updateHSB();
        }
    }

    @FXML
    void onAlphaKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            updateHSB();
        }
    }

    @FXML
    void onHexaKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            final String hexa = hexa_textfield.getText();
            double hue = getHueFromHexa(hexa);
            double saturation = getSaturationFromHexa(hexa);
            double brightness = getBrightnessFromHexa(hexa);
            double alpha = getColorProperty().getOpacity();
            final Color c = updateUI(hue, saturation, brightness, alpha);
            color.setValue(c);
        }
    }

    @FXML
    void onRedKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            updateRGB();
        }
    }

    @FXML
    void onGreenKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            updateRGB();
        }
    }

    @FXML
    void onBlueKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            updateRGB();
        }
    }

    @FXML
    void onPickerRegionPressed(MouseEvent e) {
        double mx = e.getX();
        double my = e.getY();
        handlePickerRegionMouseEvent(mx, my);
    }

    @FXML
    void onPickerRegionDragged(MouseEvent e) {
        double mx = e.getX();
        double my = e.getY();
        handlePickerRegionMouseEvent(mx, my);
    }

    private void handlePickerRegionMouseEvent(double x, double y) {
        double w = picker_region.getWidth();
        double h = picker_region.getHeight();
        double hue = getColorProperty().getHue();
        double saturation = x / w;
        double brightness = 1.0 - (y / h);
        double alpha = getColorProperty().getOpacity();
        final Color c = updateUI(hue, saturation, brightness, alpha);
        color.setValue(c);
    }

    private void updateRGB() {
        double red = Double.valueOf(red_textfield.getText());
        double green = Double.valueOf(green_textfield.getText());
        double blue = Double.valueOf(blue_textfield.getText());
        final Color c = updateUI(red, green, blue);
        color.setValue(c);
    }

    private void updateHSB() {
        double hue = Double.valueOf(hue_textfield.getText());
        double saturation = Double.valueOf(saturation_textfield.getText()) / 100.0;
        double brightness = Double.valueOf(brightness_textfield.getText()) / 100.0;
        double alpha = Double.valueOf(alpha_textfield.getText());
        final Color c = updateUI(hue, saturation, brightness, alpha);
        color.setValue(c);
    }

    private Color updateUI(final Color c) {
        double hue = c.getHue();
        double saturation = c.getSaturation();
        double brightness = c.getBrightness();
        double alpha = c.getOpacity();
        return updateUI(hue, saturation, brightness, alpha);
    }

    private Color updateUI(double red, double green, double blue) {
        double hue = getHueFromRGB(red, green, blue);
        double saturation = getSaturationFromRGB(red, green, blue);
        double brightness = getBrightnessFromRGB(red, green, blue);
        double alpha = getColorProperty().getOpacity();
        return updateUI(hue, saturation, brightness, alpha);
    }

    private Color updateUI(double hue, double saturation, double brightness, double alpha) {

        updating = true;

        // check the HSB values are within range
        hue = Utils.clamp(0, hue, 360);
        saturation = Utils.clamp(0, saturation, 1);
        brightness = Utils.clamp(0, brightness, 1);
        alpha = Utils.clamp(0, alpha, 1);
        // make an rgb color from the hsb
        final Color hsb = Color.hsb(hue, saturation, brightness, alpha);
        double[] rgb = Utils.HSBtoRGB(hsb.getHue(), hsb.getSaturation(), hsb.getBrightness());
        int red = (int) (rgb[0] * 255);
        int green = (int) (rgb[1] * 255);
        int blue = (int) (rgb[2] * 255);
        final String hexa = String.format("%02X%02X%02X", red, green, blue); //NOI18N

        final DecimalFormat df = new DecimalFormat("#.##"); //NOI18N

        // Set TextFields value
        hue_textfield.setText(Double.toString(hue));
        saturation_textfield.setText(Double.toString(saturation));
        brightness_textfield.setText(Double.toString(brightness));
        alpha_textfield.setText(Double.toString(alpha));
        red_textfield.setText(Double.toString(red));
        green_textfield.setText(Double.toString(green));
        blue_textfield.setText(Double.toString(blue));
        hexa_textfield.setText("#" + hexa); //NOI18N
        alpha_textfield.setText(df.format(alpha));

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

        // update the selected stop after a forest of ifs ! ;-)
        if (paintEditor.getMode()== Mode.LINEAR || paintEditor.getMode()== Mode.RADIAL) {
            final GradientEditor gradientEditor = paintEditor.getGradientEditor();
            assert gradientEditor != null;
            assert paintEditor.getRoot().getChildren().contains(gradientEditor);
            if (gradientEditor.isGradientEditorStopsEmpty() == false) {
                if (gradientEditor.getSelectedStop() != null) {
                    Color stopColor = Color.hsb(hue, saturation, brightness, alpha);
                    gradientEditor.getSelectedStop().setColor(stopColor);
                    gradientEditor.updateGradient();
                }
            }
        }

        return hsb;
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
