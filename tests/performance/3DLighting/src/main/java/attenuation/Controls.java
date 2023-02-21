/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package attenuation;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.DirectionalLight;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SpotLight;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.util.converter.NumberStringConverter;

final class Controls {

    static TitledPane addPointLightControls(PointLight light) {
        var controls = createPointLightControls(light);
        return createLightControls(light, controls);
    }

    static TitledPane addSpotLightControls(SpotLight light) {
        var ia = createSliderControl("inner", light.innerAngleProperty(), 0, 180, light.getInnerAngle());
        var oa = createSliderControl("outer", light.outerAngleProperty(), 0, 180, light.getOuterAngle());
        var fo = createSliderControl("falloff", light.falloffProperty(), -5, 5, light.getFalloff());
        VBox controls = createPointLightControls(light);
        controls.getChildren().addAll(ia, oa, fo);

        List<Node> directionControls = createDirectionControls(light.getTransforms(), light.directionProperty());
        controls.getChildren().addAll(directionControls);
        return createLightControls(light, controls);
    }

    private static VBox createPointLightControls(PointLight light) {
        var range = createSliderControl("range", light.maxRangeProperty(), 0, 500, 150);
        var c = createSliderControl("constant", light.constantAttenuationProperty(), -1, 1, light.getConstantAttenuation());
        var lc = createSliderControl("linear", light.linearAttenuationProperty(), -0.1, 0.1, light.getLinearAttenuation());
        var qc = createSliderControl("quadratic", light.quadraticAttenuationProperty(), -0.01, 0.01, light.getQuadraticAttenuation());
        return new VBox(range, c, lc, qc);
    }

    static TitledPane addDirectionalLightControls(DirectionalLight light) {
        List<Node> directionControls = createDirectionControls(light.getTransforms(), light.directionProperty());
        var controls = new VBox(directionControls.toArray(new Node[0]));
        return createLightControls(light, controls);
    }

    static TitledPane createLightControls(LightBase light, Pane content) {
        var lightOn = new CheckBox(light.getClass().getSimpleName());
        light.lightOnProperty().bind(lightOn.selectedProperty());
        var colorPicker = new ColorPicker(light.getColor());
        light.colorProperty().bind(colorPicker.valueProperty());
        var titleControls = new HBox(5, lightOn, colorPicker);
        titleControls.setAlignment(Pos.CENTER_LEFT);

        var titlePane = new TitledPane("", content);
        titlePane.setGraphic(titleControls);
        titlePane.setExpanded(false);
        return titlePane;
    }

    private static List<Node> createDirectionControls(ObservableList<Transform> transforms, ObjectProperty<Point3D> directionProperty) {
        var transX = new Rotate(0, Rotate.X_AXIS);
        var transY = new Rotate(0, Rotate.Y_AXIS);
        var transZ = new Rotate(0, Rotate.Z_AXIS);
        transforms.addAll(transX, transY, transZ);
        var rotX = createSliderControl("rot x", transX.angleProperty(), -180, 180, 0);
        var rotY = createSliderControl("rot y", transY.angleProperty(), -180, 180, 0);
        var rotZ = createSliderControl("rot z", transZ.angleProperty(), -180, 180, 0);

        var sliderX = createSlider(-5, 5, directionProperty.get().getX());
        var sliderY = createSlider(-5, 5, directionProperty.get().getY());
        var sliderZ = createSlider(-5, 5, directionProperty.get().getZ());
        directionProperty.bind(Bindings.createObjectBinding(() ->
            new Point3D(sliderX.getValue(), sliderY.getValue(), sliderZ.getValue()),
            sliderX.valueProperty(), sliderY.valueProperty(), sliderZ.valueProperty()));
        var dirX = createSliderControl("dir x", sliderX);
        var dirY = createSliderControl("dir y", sliderY);
        var dirZ = createSliderControl("dir z", sliderZ);

        return List.of(rotX, rotY, rotZ, dirX, dirY, dirZ);
    }

    static HBox createSliderControl(String name, DoubleProperty property, double min, double max, double start) {
        var slider = createSlider(min, max, start);
        property.bind(slider.valueProperty());
        return createSliderControl(name, slider);
    }

    private static HBox createSliderControl(String name, Slider slider) {
        var tf = createTextField(slider);
        return new HBox(5, new Label(name), slider, tf);
    }

    private static TextField createTextField(Slider slider) {
        var tf = new TextField();
        tf.textProperty().bindBidirectional(slider.valueProperty(), new NumberStringConverter());
        tf.setMaxWidth(50);
        return tf;
    }

    private static Slider createSlider(double min, double max, double start) {
        var slider = new Slider(min, max, start);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        return slider;
    }
}
