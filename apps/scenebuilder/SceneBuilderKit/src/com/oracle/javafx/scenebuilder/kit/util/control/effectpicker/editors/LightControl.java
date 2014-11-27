/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors;

import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.EffectPickerController;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.effect.Light;
import javafx.scene.effect.Light.Distant;
import javafx.scene.effect.Light.Point;
import javafx.scene.effect.Light.Spot;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class LightControl extends VBox {

    @FXML
    private Label lightLabel;
    @FXML
    private ChoiceBox<LightsEnum> lightChoiceBox;
    @FXML
    private VBox lightProperties;

    private final ObjectProperty<Light> value = new SimpleObjectProperty<>();
    private final EffectPickerController effectPickerController;

    // Common color property
    private PaintPicker colorPicker;

    private final Light defaultDistant;
    private final Light defaultPoint;
    private final Light defaultSpot;

    private final BooleanProperty liveUpdate = new SimpleBooleanProperty();

    private enum LightsEnum {

        DISTANT,
        POINT,
        SPOT,
        NONE
    }

    public LightControl(EffectPickerController effectPickerController,
            String label, Light initValue) {
        this.effectPickerController = effectPickerController;
        this.defaultDistant = new Distant();
        this.defaultPoint = new Point();
        this.defaultSpot = new Spot();
        initialize(label, initValue);
    }

    public ObjectProperty<Light> valueProperty() {
        return value;
    }

    public Light getValue() {
        return value.get();
    }

    public void setValue(Light v) {
        value.set(v);
    }

    public final BooleanProperty liveUpdateProperty() {
        return liveUpdate;
    }

    public boolean isLiveUpdate() {
        return liveUpdate.get();
    }

    public void setLiveUpdate(boolean value) {
        liveUpdate.setValue(value);
    }

    private void initialize(String label, Light initValue) {

        final URL layoutURL = EnumControl.class.getResource("LightControl.fxml"); //NOI18N
        try (InputStream is = layoutURL.openStream()) {
            final FXMLLoader loader = new FXMLLoader();
            loader.setController(this);
            loader.setRoot(this);
            loader.setLocation(layoutURL);
            final Parent p = (Parent) loader.load(is);
            assert p == this;
        } catch (IOException x) {
            throw new RuntimeException(x);
        }

        lightLabel.setText(label);
        lightChoiceBox.getItems().addAll(LightsEnum.values());

        setValue(initValue);
        if (initValue == null) {
            lightChoiceBox.setValue(LightsEnum.NONE);
        } else if (initValue instanceof Distant) {
            lightChoiceBox.setValue(LightsEnum.DISTANT);
        } else if (initValue instanceof Point) {
            lightChoiceBox.setValue(LightsEnum.POINT);
        } else {
            assert initValue instanceof Spot;
            lightChoiceBox.setValue(LightsEnum.SPOT);
        }

        lightChoiceBox.getSelectionModel().selectedItemProperty().addListener((ChangeListener<LightsEnum>) (ov, oldValue, newValue) -> {
            final Light light;
            switch (newValue) {
                case DISTANT:
                    light = defaultDistant;
                    break;
                case POINT:
                    light = defaultPoint;
                    break;
                case SPOT:
                    light = defaultSpot;
                    break;
                case NONE:
                    light = null;
                    break;
                default:
                    light = null;
                    assert false;
            }
            // First update the model with new light value
            setValue(light);
            // Then update the UI
            updateLightPropertiesUI();
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        });

        lightChoiceBox.addEventHandler(ActionEvent.ACTION, (Event event) -> {
            event.consume();
        });

        updateLightPropertiesUI();
    }

    private void updateLightPropertiesUI() {
        lightProperties.getChildren().clear();

        // Add specific properties
        if (getValue() == null) {
            // No property to add
        } else {
            // Add common color property
            lightProperties.getChildren().add(getColorPicker());
            colorPicker.setPaintProperty(getValue().getColor());

            if (getValue() instanceof Distant) {
                final Distant distant = (Distant) getValue();

                final SliderControl azimuthEditor = new SliderControl(
                        effectPickerController, "azimuth", 0, 360.0, distant.getAzimuth(), 1.0, false); //NOI18N
                distant.azimuthProperty().bind(azimuthEditor.valueProperty());
                lightProperties.getChildren().add(azimuthEditor);

                final SliderControl elevationEditor = new SliderControl(
                        effectPickerController, "elevation", 0, 360.0, distant.getElevation(), 1.0, false); //NOI18N
                distant.elevationProperty().bind(elevationEditor.valueProperty());
                lightProperties.getChildren().add(elevationEditor);

            } else {
                assert getValue() instanceof Point;
                final Point point = (Point) getValue();

                final DoubleTextFieldControl xEditor = new DoubleTextFieldControl(
                        effectPickerController, "x", -10.0, 10.0, point.getX(), 1.0); //NOI18N
                point.xProperty().bind(xEditor.valueProperty());
                lightProperties.getChildren().add(xEditor);

                final DoubleTextFieldControl yEditor = new DoubleTextFieldControl(
                        effectPickerController, "y", -10.0, 10.0, point.getY(), 1.0); //NOI18N
                point.yProperty().bind(yEditor.valueProperty());
                lightProperties.getChildren().add(yEditor);

                final DoubleTextFieldControl zEditor = new DoubleTextFieldControl(
                        effectPickerController, "z", -10.0, 10.0, point.getY(), 1.0); //NOI18N
                point.zProperty().bind(zEditor.valueProperty());
                lightProperties.getChildren().add(zEditor);

                if (point instanceof Spot) {
                    final Spot spot = (Spot) getValue();

                    final DoubleTextFieldControl pointsAtXEditor = new DoubleTextFieldControl(
                            effectPickerController, "pointsAtX", -10.0, 10.0, spot.getPointsAtX(), 1.0); //NOI18N
                    spot.pointsAtXProperty().bind(pointsAtXEditor.valueProperty());
                    lightProperties.getChildren().add(pointsAtXEditor);

                    final DoubleTextFieldControl pointsAtYEditor = new DoubleTextFieldControl(
                            effectPickerController, "pointsAtY", -10.0, 10.0, spot.getPointsAtY(), 1.0); //NOI18N
                    spot.pointsAtYProperty().bind(pointsAtYEditor.valueProperty());
                    lightProperties.getChildren().add(pointsAtYEditor);

                    final DoubleTextFieldControl pointsAtZEditor = new DoubleTextFieldControl(
                            effectPickerController, "pointsAtZ", -10.0, 10.0, spot.getPointsAtZ(), 1.0); //NOI18N
                    spot.pointsAtZProperty().bind(pointsAtZEditor.valueProperty());
                    lightProperties.getChildren().add(pointsAtZEditor);

                    final SliderControl specularExponentEditor = new SliderControl(
                            effectPickerController, "specularExponent", 0, 4.0, spot.getSpecularExponent(), 1.0, false); //NOI18N
                    spot.specularExponentProperty().bind(specularExponentEditor.valueProperty());
                    lightProperties.getChildren().add(specularExponentEditor);

                }
            }
        }
    }

    private PaintPicker getColorPicker() {
        if (colorPicker == null) {
            colorPicker = new PaintPicker(effectPickerController.getPaintPickerDelegate(), PaintPicker.Mode.COLOR);
            colorPicker.paintProperty().addListener((ChangeListener<Paint>) (ov, oldValue, newValue) -> {
                assert newValue instanceof Color;
                final Color color = (Color) newValue;
                getValue().setColor(color);
                // Then notify the controller a change occured
                effectPickerController.incrementRevision();
            });
            colorPicker.liveUpdateProperty().addListener((ChangeListener<Boolean>) (ov, oldValue, newValue) -> setLiveUpdate(newValue));
        }
        return colorPicker;
    }
}
