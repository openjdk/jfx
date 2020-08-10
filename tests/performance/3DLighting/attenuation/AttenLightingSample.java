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

import javafx.beans.property.DoubleProperty;
import javafx.scene.PointLight;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;

/**
 * A {@code LightingSample} with additional controls for light attenuation.
 */
public class AttenLightingSample extends LightingSample {

    @Override
    protected VBox addLightControls(PointLight light) {
        var vbox = super.addLightControls(light);
        var range = createSliderControl("range", light.maxRangeProperty(), 0, 100, light.getMaxRange());
        var c = createSliderControl("constant", light.constantAttenuationProperty(), -1, 1, light.getConstantAttenuation());
        var lc = createSliderControl("linear", light.linearAttenuationProperty(), -1, 1, light.getLinearAttenuation());
        var qc = createSliderControl("quadratic", light.quadraticAttenuationProperty(), -1, 1, light.getQuadraticAttenuation());
        vbox.getChildren().addAll(range, c, lc, qc);
        return vbox;
    }

    private HBox createSliderControl(String name, DoubleProperty property, double min, double max, double start) {
        var slider = new Slider(min, max, start);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        property.bindBidirectional(slider.valueProperty());
        var tf = new TextField();
        tf.textProperty().bindBidirectional(slider.valueProperty(), new NumberStringConverter());
        tf.setMaxWidth(50);
        return new HBox(5, new Label(name), slider, tf);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
