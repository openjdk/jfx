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
package com.oracle.javafx.scenebuilder.kit.util.control.painteditor.gradienteditor;

import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.PaintEditorController;
import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.PaintEditorController.Mode;
import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.rotateeditor.RotateEditor;
import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.slidereditor.SliderEditor;
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;

/**
 * Controller class for the gradient part of the paint editor.
 */
public class GradientEditor extends VBox {

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

    private final PaintEditorController paintEditor;

    private final RotateEditor focusAngleEditor
            = new RotateEditor("focusAngle"); //NOI18N
    private final SliderEditor focusDistanceEditor
            = new SliderEditor("focusDistance", -1.0, 1.0, 0.0); //NOI18N
    private final SliderEditor radiusEditor
            = new SliderEditor("radius", 0.0, 1.0, 0.5); //NOI18N
    private final List<GradientEditorStop> gradientEditorStops = new ArrayList<>();
    private GradientEditorStop selectedStop;
    private final int maxStops = 12; // the numbers of stops supported in platform

    private LinearGradient linearGradient
            = new LinearGradient(0.0, 0.0, 1.0, 1.0, true, CycleMethod.NO_CYCLE);
    private RadialGradient radialGradient
            = new RadialGradient(0.0, 0.0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE);

    private final ObjectProperty<Paint> paint = new SimpleObjectProperty<>();

    public GradientEditor(PaintEditorController pe) {
        paintEditor = pe;
        initialize();
    }

    public final PaintEditorController getPaintEditorController() {
        return paintEditor;
    }

    public final ObjectProperty<Paint> paintProperty() {
        return paint;
    }

    public final Paint getPaintProperty() {
        return paint.get();
    }

    public final void setPaintProperty(Paint value) {
        if (value instanceof LinearGradient) {
            linearGradient = (LinearGradient) value;
        } else {
            assert value instanceof RadialGradient;
            radialGradient = (RadialGradient) value;
        }
        paint.setValue(value);
        updateGradient();
    }

    public void setGradientMode(final Mode mode) {
        startX_slider.setVisible(mode == Mode.LINEAR);
        endX_slider.setVisible(mode == Mode.LINEAR);
        endX_slider.setManaged(mode == Mode.LINEAR);
        startY_slider.setVisible(mode == Mode.LINEAR);
        endY_slider.setVisible(mode == Mode.LINEAR);
        centerX_slider.setVisible(mode == Mode.RADIAL);
        centerY_slider.setVisible(mode == Mode.RADIAL);
        radial_container.setVisible(mode == Mode.RADIAL);
        radial_container.setManaged(mode == Mode.RADIAL);
        updateGradient();
    }

    private void addStop(double min, double max, double value, Color color) {
        if (gradientEditorStops.size() < maxStops) {
            final GradientEditorStop gradientEditorStop
                    = new GradientEditorStop(this, min, max, value, color);
            track_pane.getChildren().add(gradientEditorStop);
            gradientEditorStops.add(gradientEditorStop);
        }
    }

    public void removeStop(GradientEditorStop gradientEditorStop) {
        track_pane.getChildren().remove(gradientEditorStop);
        gradientEditorStops.remove(gradientEditorStop);
        updateGradient();
    }

    public void updateGradient() {
        // mode may be null during initialization
        if (paintEditor.getMode() == null) {
            return;
        }
        switch (paintEditor.getMode()) {
            case LINEAR:
                updateLinearGradient();
                paint.setValue(linearGradient);
                break;
            case RADIAL:
                updateRadialGradient();
                paint.setValue(radialGradient);
                break;
            default:
                break;
        }
    }

    public boolean isGradientEditorStopsEmpty() {
        return gradientEditorStops.isEmpty();
    }
    
    public List<GradientEditorStop> getGradientEditorStops() {
        return gradientEditorStops;
    }

    public void setSelectedStop(GradientEditorStop gradientStop) {
        for (GradientEditorStop gradientEditorStop : gradientEditorStops) {
            gradientEditorStop.setSelected(false); // turn them all false
        }
        if (gradientStop == null) {
            selectedStop = null;
        } else {
            selectedStop = gradientStop;
            gradientStop.setSelected(true);
        }
    }

    public GradientEditorStop getSelectedStop() {
        GradientEditorStop selectedThumb = null;
        for (GradientEditorStop gradientStopThumb : gradientEditorStops) {
            if (gradientStopThumb.getSelected()) {
                selectedThumb = gradientStopThumb;
            }
        }
        return selectedThumb;
    }

    /**
     * Private
     */
    private void initialize() {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(GradientEditor.class.getResource("GradientEditor.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GradientEditor.class.getName()).log(Level.SEVERE, null, ex);
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
        addStop(0.0, 1.0, 0.0, Color.BLACK);
        addStop(0.0, 1.0, 1.0, Color.WHITE);
        setSelectedStop(null); // start with no selected stops

        proportional_checkbox.setSelected(true);
        proportional_checkbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                updateGradient();
            }
        });

        cycleMethod_choicebox.setItems(FXCollections.observableArrayList(CycleMethod.values()));
        cycleMethod_choicebox.getSelectionModel().selectFirst();
        cycleMethod_choicebox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<CycleMethod>() {
            @Override
            public void changed(ObservableValue<? extends CycleMethod> ov, CycleMethod oldValue, CycleMethod newValue) {
                updateGradient();
            }
        });

        final ChangeListener<Number> numberListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                updateGradient();
            }
        };
        focusAngleEditor.rotationProperty().addListener(numberListener);
        focusDistanceEditor.getSlider().valueProperty().addListener(numberListener);
        radiusEditor.getSlider().valueProperty().addListener(numberListener);

        // tweak - popup on sliders ?
//        startX_slider.setOnMousePressed(new EventHandler<MouseEvent>() {
//
//            @Override
//            public void handle(MouseEvent event) {
//                final Slider s = (Slider) event.getTarget();
//                s.getContextMenu().show(s, Side.BOTTOM, 0, 5);
//                event.consume();
//            }
//        });
        startX_slider.valueProperty().addListener(numberListener);
        startY_slider.valueProperty().addListener(numberListener);
        endX_slider.valueProperty().addListener(numberListener);
        endY_slider.valueProperty().addListener(numberListener);
        centerX_slider.valueProperty().addListener(numberListener);
        centerY_slider.valueProperty().addListener(numberListener);

        radial_container.getChildren().addAll(radiusEditor, focusDistanceEditor, focusAngleEditor);
        radial_container.setVisible(false);
        radial_container.setManaged(false);
    }

    @FXML
    void sliderPressed(MouseEvent event) {
        double percentH = ((100.0 / track_pane.getWidth()) * event.getX()) / 100;
        final Color color = paintEditor.getColorEditor().getColorProperty();
        addStop(0.0, 1.0, percentH, color);
        selectedStop.thumbPressed(event);
    }

    @FXML
    void sliderDragged(MouseEvent event) {
        selectedStop.thumbDragged(event);
    }

    private void updateLinearGradient() {
        double startX = startX_slider.getValue();
        double startY = startY_slider.getValue();
        double endX = endX_slider.getValue();
        double endY = endY_slider.getValue();
        boolean proportional = proportional_checkbox.isSelected();
        final CycleMethod cycleMethod = cycleMethod_choicebox.getValue();
        linearGradient = new LinearGradient(startX, startY, endX, endY,
                proportional, cycleMethod, getStops());
        preview_rect.setFill(linearGradient);
    }

    private void updateRadialGradient() {
        double focusAngle = focusAngleEditor.getRotationProperty();
        double focusDistance = focusDistanceEditor.getSlider().getValue();
        double centerX = centerX_slider.getValue();
        double centerY = centerY_slider.getValue();
        double radius = radiusEditor.getSlider().getValue();
        boolean proportional = proportional_checkbox.isSelected();
        final CycleMethod cycleMethod = cycleMethod_choicebox.getValue();
        radialGradient = new RadialGradient(focusAngle, focusDistance,
                centerX, centerY, radius, proportional, cycleMethod, getStops());
        preview_rect.setFill(radialGradient);
    }

    private List<Stop> getStops() {
        final List<Stop> stops = new ArrayList<>();
        for (GradientEditorStop ges : getGradientEditorStops()) {
            final Stop stop = new Stop(ges.getValue(), ges.getColor());
            stops.add(stop);
        }
        return stops;
    }
}
