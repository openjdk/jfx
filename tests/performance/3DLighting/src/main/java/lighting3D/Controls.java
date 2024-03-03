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

package lighting3D;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.AmbientLight;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.util.converter.NumberStringConverter;

/**
 * Utility class for creating adjustment controls.
 */
final class Controls {

    static Node createLightControls(AmbientLight light) {
        return createTitlePane(light, null);
    }

    static Node createLightControls(PointLight light) {
        var controls = createPointLightControls(light);
        return createTitlePane(light, controls);
    }

    static Node createLightControls(SpotLight light) {
        var controls = createSpotLightControls(light);
        return createTitlePane(light, controls);
    }

    static Node createLightControls(DirectionalLight light) {
        var controls = createDirectionControls(light.getTransforms(), light.directionProperty());
        return createTitlePane(light, controls);
    }

    private static Pane createSpotLightControls(SpotLight light) {
        GridPane gridPane = createPointLightControls(light);

        var ia = createSliderControl(light.innerAngleProperty(), 0, 180, light.getInnerAngle());
        var oa = createSliderControl(light.outerAngleProperty(), 0, 180, light.getOuterAngle());
        var fo = createSliderControl(light.falloffProperty(), -5, 5, light.getFalloff());
        gridPane.addRow(gridPane.getRowCount(), createLabel("inner", "Inner angle"), ia);
        gridPane.addRow(gridPane.getRowCount(), createLabel("outer", "Outer angle"), oa);
        gridPane.addRow(gridPane.getRowCount(), createLabel("falloff", "Falloff factor"), fo);

        GridPane directionControls = createDirectionControls(light.getTransforms(), light.directionProperty());
        var children = List.copyOf(directionControls.getChildren());
        int rowCount = gridPane.getRowCount();
        for (var child : children) {
            gridPane.add(child, GridPane.getColumnIndex(child), GridPane.getRowIndex(child) + rowCount);
        }
        return gridPane;
    }

    private static GridPane createPointLightControls(PointLight light) {
        var gridPane = new GridPane();

        var x = createSliderControl(light.translateXProperty(), -100, 100, light.getTranslateX());
        var y = createSliderControl(light.translateYProperty(), -100, 100, light.getTranslateY());
        var z = createSliderControl(light.translateZProperty(), -100, 100, light.getTranslateZ());
        gridPane.addRow(gridPane.getRowCount(), createLabel("x", "Translate x"), x);
        gridPane.addRow(gridPane.getRowCount(), createLabel("y", "Translate y"), y);
        gridPane.addRow(gridPane.getRowCount(), createLabel("z", "Translate z"), z);

        var range = createSliderControl(light.maxRangeProperty(), 0, 500, 150);
        var ca = createSliderControl(light.constantAttenuationProperty(), -1, 1, light.getConstantAttenuation());
        var la = createSliderControl(light.linearAttenuationProperty(), -0.1, 0.1, light.getLinearAttenuation());
        var qa = createSliderControl(light.quadraticAttenuationProperty(), -0.01, 0.01, light.getQuadraticAttenuation());
        gridPane.addRow(gridPane.getRowCount(), createLabel("range", "Range"), range);
        gridPane.addRow(gridPane.getRowCount(), createLabel("const", "Constant attenuation factor"), ca);
        gridPane.addRow(gridPane.getRowCount(), createLabel("linear", "Linear attenuation factor"), la);
        gridPane.addRow(gridPane.getRowCount(), createLabel("quad", "Quadratic attenuation factor"), qa);

        return gridPane;
    }

    private static GridPane createDirectionControls(ObservableList<Transform> transforms, ObjectProperty<Point3D> dirProp) {
        var gridPane = new GridPane();

        var transX = new Rotate(0, Rotate.X_AXIS);
        var transY = new Rotate(0, Rotate.Y_AXIS);
        var transZ = new Rotate(0, Rotate.Z_AXIS);
        transforms.addAll(transX, transY, transZ);
        var rotX = createSliderControl(transX.angleProperty(), -180, 180, 0);
        var rotY = createSliderControl(transY.angleProperty(), -180, 180, 0);
        var rotZ = createSliderControl(transZ.angleProperty(), -180, 180, 0);
        gridPane.addRow(gridPane.getRowCount(), createLabel("rot x", "Rotate x"), rotX);
        gridPane.addRow(gridPane.getRowCount(), createLabel("rot y", "Rotate y"), rotY);
        gridPane.addRow(gridPane.getRowCount(), createLabel("rot z", "Rotate z"), rotZ);

        var sliderX = createSlider(-5, 5, dirProp.get().getX());
        var sliderY = createSlider(-5, 5, dirProp.get().getY());
        var sliderZ = createSlider(-5, 5, dirProp.get().getZ());
        dirProp.bind(Bindings.createObjectBinding(() ->
            new Point3D(sliderX.getValue(), sliderY.getValue(), sliderZ.getValue()),
            sliderX.valueProperty(), sliderY.valueProperty(), sliderZ.valueProperty()));
        var dirX = createSliderControl(sliderX);
        var dirY = createSliderControl(sliderY);
        var dirZ = createSliderControl(sliderZ);
        gridPane.addRow(gridPane.getRowCount(), createLabel("dir x", "Direction x"), dirX);
        gridPane.addRow(gridPane.getRowCount(), createLabel("dir y", "Direction y"), dirY);
        gridPane.addRow(gridPane.getRowCount(), createLabel("dir z", "Direction z"), dirZ);

        return gridPane;
    }

    private static TitledPane createTitlePane(LightBase light, Pane content) {
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

    private static Label createLabel(String name, String tooltipText) {
        var label = new Label(name);
        label.setTooltip(new Tooltip(tooltipText));
        GridPane.setValignment(label, VPos.TOP);
        return label;
    }

    static HBox createSliderControl(DoubleProperty property, double min, double max, double start) {
        var slider = createSlider(min, max, start);
        property.bind(slider.valueProperty());
        return createSliderControl(slider);
    }

    private static HBox createSliderControl(Slider slider) {
        return new HBox(slider, createTextField(slider));
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
