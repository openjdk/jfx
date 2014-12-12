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
package com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.gradientpicker;

import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker.Mode;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPickerController;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.rotator.RotatorControl;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.slider.SliderControl;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;

/**
 * Controller class for the gradient part of the paint editor.
 */
public class GradientPicker extends VBox {

    @FXML
    private Pane track_pane;
    @FXML
    private Label stop_label;
    @FXML
    private Rectangle preview_rect;
    @FXML
    private StackPane slider_container;
    @FXML
    private VBox radial_container;
    @FXML
    private VBox shared_container;
    @FXML
    private Slider startX_slider;
    @FXML
    private Slider endX_slider;
    @FXML
    private Slider startY_slider;
    @FXML
    private Slider endY_slider;
    @FXML
    private Slider centerX_slider;
    @FXML
    private Slider centerY_slider;
    @FXML
    private CheckBox proportional_checkbox;
    @FXML
    private ChoiceBox<CycleMethod> cycleMethod_choicebox;

    private final PaintPickerController paintPicker;

    private final RotatorControl focusAngleRotator
            = new RotatorControl("focusAngle"); //NOI18N
    private final SliderControl focusDistanceSlider
            = new SliderControl("focusDistance", -1.0, 1.0, 0.0); //NOI18N
    private final SliderControl radiusSlider
            = new SliderControl("radius", 0.0, 1.0, 0.5); //NOI18N
    private final List<GradientPickerStop> gradientPickerStops = new ArrayList<>();
    private final int maxStops = 12; // the numbers of stops supported in platform

    public GradientPicker(PaintPickerController pe) {
        paintPicker = pe;
        initialize();
    }

    public final PaintPickerController getPaintPickerController() {
        return paintPicker;
    }

    public Paint getValue(Mode mode) {
        final Paint paint;
        switch (mode) {
            case LINEAR:
                double startX = startX_slider.getValue();
                double startY = startY_slider.getValue();
                double endX = endX_slider.getValue();
                double endY = endY_slider.getValue();
                boolean linear_proportional = proportional_checkbox.isSelected();
                final CycleMethod linear_cycleMethod = cycleMethod_choicebox.getValue();
                paint = new LinearGradient(startX, startY, endX, endY,
                        linear_proportional, linear_cycleMethod, getStops());
                break;
            case RADIAL:
                double focusAngle = focusAngleRotator.getRotationProperty();
                double focusDistance = focusDistanceSlider.getSlider().getValue();
                double centerX = centerX_slider.getValue();
                double centerY = centerY_slider.getValue();
                double radius = radiusSlider.getSlider().getValue();
                boolean radial_proportional = proportional_checkbox.isSelected();
                final CycleMethod radial_cycleMethod = cycleMethod_choicebox.getValue();
                paint = new RadialGradient(focusAngle, focusDistance, centerX, centerY, radius,
                        radial_proportional, radial_cycleMethod, getStops());
                break;
            default:
                assert false;
                paint = null;
                break;
        }
        return paint;
    }

    public boolean isGradientStopsEmpty() {
        return gradientPickerStops.isEmpty();
    }

    public List<GradientPickerStop> getGradientStops() {
        return gradientPickerStops;
    }

    public GradientPickerStop getSelectedStop() {
        GradientPickerStop selectedThumb = null;
        for (GradientPickerStop gradientStopThumb : gradientPickerStops) {
            if (gradientStopThumb.isSelected()) {
                selectedThumb = gradientStopThumb;
            }
        }
        return selectedThumb;
    }

    public void updateUI(Paint value) {
        assert value instanceof LinearGradient || value instanceof RadialGradient;
        if (value instanceof LinearGradient) {
            final LinearGradient linear = (LinearGradient) value;
            startX_slider.setValue(linear.getStartX());
            startY_slider.setValue(linear.getStartY());
            endX_slider.setValue(linear.getEndX());
            endY_slider.setValue(linear.getEndY());
            proportional_checkbox.setSelected(linear.isProportional());
            cycleMethod_choicebox.setValue(linear.getCycleMethod());
            // clear first
            removeAllStops();
            for (Stop stop : linear.getStops()) {
                // Update stops
                addStop(0.0, 1.0, stop.getOffset(), stop.getColor());
            }

        } else {
            assert value instanceof RadialGradient;
            final RadialGradient radial = (RadialGradient) value;
            centerX_slider.setValue(radial.getCenterX());
            centerY_slider.setValue(radial.getCenterY());
            focusAngleRotator.setRotationProperty(radial.getFocusAngle());
            focusDistanceSlider.getSlider().setValue(radial.getFocusDistance());
            radiusSlider.getSlider().setValue(radial.getRadius());
            proportional_checkbox.setSelected(radial.isProportional());
            cycleMethod_choicebox.setValue(radial.getCycleMethod());
            // clear first
            removeAllStops();
            for (Stop stop : radial.getStops()) {
                // Update stops
                addStop(0.0, 1.0, stop.getOffset(), stop.getColor());
            }
        }
        setMode(value);
        updatePreview(value);
    }

    public void updatePreview(Paint value) {
        preview_rect.setFill(value);
    }

    public void setMode(Paint value) {
        final Mode mode;
        if (value instanceof LinearGradient) {
            mode = Mode.LINEAR;
        } else {
            assert value instanceof RadialGradient;
            mode = Mode.RADIAL;
        }
        startX_slider.setVisible(mode == Mode.LINEAR);
        startY_slider.setVisible(mode == Mode.LINEAR);
        endX_slider.setVisible(mode == Mode.LINEAR);
        endY_slider.setVisible(mode == Mode.LINEAR);
        centerX_slider.setVisible(mode == Mode.RADIAL);
        centerY_slider.setVisible(mode == Mode.RADIAL);
        radial_container.setVisible(mode == Mode.RADIAL);
        radial_container.setManaged(mode == Mode.RADIAL);
    }

    /**
     * Private
     */
    private void initialize() {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(GradientPicker.class.getResource("GradientPicker.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GradientPicker.class.getName()).log(Level.SEVERE, null, ex);
        }

        assert proportional_checkbox != null;
        assert cycleMethod_choicebox != null;
        assert startX_slider != null;
        assert endX_slider != null;
        assert startY_slider != null;
        assert endY_slider != null;
        assert centerX_slider != null;
        assert centerY_slider != null;
        assert radial_container != null;

        // Add two default stops
        final GradientPickerStop black = addStop(0.0, 1.0, 0.0, Color.BLACK);
        addStop(0.0, 1.0, 1.0, Color.WHITE);
        // Select first default stop
        setSelectedStop(black);
        proportional_checkbox.setSelected(true);
        proportional_checkbox.selectedProperty().addListener((ChangeListener<Boolean>) (ov, oldValue, newValue) -> {
            final Mode mode = paintPicker.getMode();
            final Paint value = getValue(mode);
            // Update UI
            preview_rect.setFill(value);
            // Update model
            paintPicker.setPaintProperty(value);
        });
        proportional_checkbox.setOnAction((ActionEvent event) -> {
            event.consume();
        });

        cycleMethod_choicebox.setItems(FXCollections.observableArrayList(CycleMethod.values()));
        cycleMethod_choicebox.getSelectionModel().selectFirst();
        cycleMethod_choicebox.getSelectionModel().selectedItemProperty().addListener((ChangeListener<CycleMethod>) (ov, oldValue, newValue) -> {
            final Mode mode = paintPicker.getMode();
            final Paint value = getValue(mode);
            // Update UI
            preview_rect.setFill(value);
            // Update model
            paintPicker.setPaintProperty(value);
        });
        cycleMethod_choicebox.addEventHandler(ActionEvent.ACTION, (Event event) -> {
            event.consume();
        });

        final ChangeListener<Number> onValueChange = (ov, oldValue, newValue) -> {
            final Mode mode = paintPicker.getMode();
            final Paint value = getValue(mode);
            // Update UI
            preview_rect.setFill(value);
            // Update model
            paintPicker.setPaintProperty(value);
        };
        startX_slider.valueProperty().addListener(onValueChange);
        startY_slider.valueProperty().addListener(onValueChange);
        endX_slider.valueProperty().addListener(onValueChange);
        endY_slider.valueProperty().addListener(onValueChange);

        centerX_slider.valueProperty().addListener(onValueChange);
        centerY_slider.valueProperty().addListener(onValueChange);
        focusAngleRotator.rotationProperty().addListener(onValueChange);
        focusDistanceSlider.getSlider().valueProperty().addListener(onValueChange);
        radiusSlider.getSlider().valueProperty().addListener(onValueChange);

        radial_container.getChildren().addAll(radiusSlider, focusDistanceSlider, focusAngleRotator);
        radial_container.setVisible(false);
        radial_container.setManaged(false);

        final ChangeListener<Boolean> liveUpdateListener = (ov, oldValue, newValue) -> paintPicker.setLiveUpdate(newValue);
        startX_slider.pressedProperty().addListener(liveUpdateListener);
        startY_slider.pressedProperty().addListener(liveUpdateListener);
        endX_slider.pressedProperty().addListener(liveUpdateListener);
        endY_slider.pressedProperty().addListener(liveUpdateListener);
        centerX_slider.pressedProperty().addListener(liveUpdateListener);
        centerY_slider.pressedProperty().addListener(liveUpdateListener);
        radiusSlider.pressedProperty().addListener(liveUpdateListener);
        focusDistanceSlider.pressedProperty().addListener(liveUpdateListener);
        focusAngleRotator.pressedProperty().addListener(liveUpdateListener);
        slider_container.pressedProperty().addListener(liveUpdateListener);
    }
    
    @FXML
    void sliderPressed(MouseEvent event) {
        double percentH = ((100.0 / track_pane.getWidth()) * event.getX()) / 100;
        final Color color = paintPicker.getColorPicker().getValue();
        addStop(0.0, 1.0, percentH, color);
        final Mode mode = paintPicker.getMode();
        final Paint value = getValue(mode);
        // Update UI
        preview_rect.setFill(value);
        // Update model
        paintPicker.setPaintProperty(value);
    }

    @FXML
    void sliderDragged(MouseEvent event) {
        final Mode mode = paintPicker.getMode();
        final Paint value = getValue(mode);
        // Update UI
        preview_rect.setFill(value);
        // Update model
        paintPicker.setPaintProperty(value);
    }

    GradientPickerStop addStop(double min, double max, double value, Color color) {
        if (gradientPickerStops.size() < maxStops) {
            final GradientPickerStop gradientStop
                    = new GradientPickerStop(this, min, max, value, color);
            track_pane.getChildren().add(gradientStop);
            gradientPickerStops.add(gradientStop);
            return gradientStop;
        }
        return null;
    }

    void removeStop(GradientPickerStop gradientStop) {
        track_pane.getChildren().remove(gradientStop);
        gradientPickerStops.remove(gradientStop);
    }

    void removeAllStops() {
        track_pane.getChildren().clear();
        gradientPickerStops.clear();
    }

    public void setSelectedStop(GradientPickerStop gradientStop) {
        for (GradientPickerStop stop : gradientPickerStops) {
            stop.setSelected(false); // turn them all false
        }
        if (gradientStop != null) {
            gradientStop.setSelected(true);
        }
    }

    private List<Stop> getStops() {
        final List<Stop> stops = new ArrayList<>();
        for (GradientPickerStop ges : getGradientStops()) {
            final Stop stop = new Stop(ges.getOffset(), ges.getColor());
            stops.add(stop);
        }
        return stops;
    }
}
