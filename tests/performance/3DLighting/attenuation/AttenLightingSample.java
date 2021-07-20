/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point3D;
import javafx.scene.PointLight;
import javafx.scene.SpotLight;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.util.converter.NumberStringConverter;

/**
 * A {@code LightingSample} with additional controls for light attenuation.
 */
public class AttenLightingSample extends LightingSample {

    @Override
    protected VBox addPointLightControls(PointLight light) {
        var vbox = super.addLightControls(light);
        var range = createSliderControl("range", light.maxRangeProperty(), 0, 500, 150);
        var c = createSliderControl("constant", light.constantAttenuationProperty(), -1, 1, light.getConstantAttenuation());
        var lc = createSliderControl("linear", light.linearAttenuationProperty(), -0.1, 0.1, light.getLinearAttenuation());
        var qc = createSliderControl("quadratic", light.quadraticAttenuationProperty(), -0.01, 0.01, light.getQuadraticAttenuation());
        vbox.getChildren().addAll(range, c, lc, qc);
        return vbox;
    }

    @Override
    protected VBox addSpotLightControls(SpotLight light) {
        var vbox = addPointLightControls(light);
        var ia = createSliderControl("inner", light.innerAngleProperty(), 0, 180, light.getInnerAngle());
        var oa = createSliderControl("outer", light.outerAngleProperty(), 0, 180, light.getOuterAngle());
        var fo = createSliderControl("falloff", light.falloffProperty(), -5, 5, light.getFalloff());

        var transX = new Rotate(0, Rotate.X_AXIS);
        var transY = new Rotate(0, Rotate.Y_AXIS);
        var transZ = new Rotate(0, Rotate.Z_AXIS);
        light.getTransforms().addAll(transX, transY, transZ);
        var rotX = createSliderControl("rot x", transX.angleProperty(), -180, 180, 0);
        var rotY = createSliderControl("rot y", transY.angleProperty(), -180, 180, 0);
        var rotZ = createSliderControl("rot z", transZ.angleProperty(), -180, 180, 0);

        var sliderX = createSlider(-5, 5, light.getDirection().getX());
        var sliderY = createSlider(-5, 5, light.getDirection().getY());
        var sliderZ = createSlider(-5, 5, light.getDirection().getZ());
        light.directionProperty().bind(Bindings.createObjectBinding(() ->
            new Point3D(sliderX.getValue(), sliderY.getValue(), sliderZ.getValue()),
            sliderX.valueProperty(), sliderY.valueProperty(), sliderZ.valueProperty()));
        var dirX = createSliderControl("dir x", sliderX);
        var dirY = createSliderControl("dir y", sliderY);
        var dirZ = createSliderControl("dir z", sliderZ);

        vbox.getChildren().addAll(ia, oa, fo, rotX, rotY, rotZ, dirX, dirY, dirZ);
        return vbox;
    }

    private HBox createSliderControl(String name, DoubleProperty property, double min, double max, double start) {
        var slider = createSlider(min, max, start);
        property.bind(slider.valueProperty());
        return createSliderControl(name, slider);
    }

    private HBox createSliderControl(String name, Slider slider) {
        var tf = createTextField(slider);
        return new HBox(5, new Label(name), slider, tf);
    }

    private TextField createTextField(Slider slider) {
        var tf = new TextField();
        tf.textProperty().bindBidirectional(slider.valueProperty(), new NumberStringConverter());
        tf.setMaxWidth(50);
        return tf;
    }

    private Slider createSlider(double min, double max, double start) {
        var slider = new Slider(min, max, start);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        return slider;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
