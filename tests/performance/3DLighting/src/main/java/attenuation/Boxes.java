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

import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Group;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

class Boxes extends Group {

    private static final PhongMaterial MATERIAL = new PhongMaterial();

    /**
     * Creates a box-like structure with 3 edges.
     *
     * @param size distance from the center of the box to an edge
     */
    Boxes(double size) {
        var back = createBox(size);
        back.setTranslateZ(size);
        var right = createBox(size);
        right.setRotationAxis(Rotate.Y_AXIS);
        right.setRotate(90);
        right.setTranslateX(size * 2);
        right.setTranslateZ(-size);
        var left = createBox(size);
        left.setRotationAxis(Rotate.Y_AXIS);
        left.setRotate(90);
        left.setTranslateX(-size * 2);
        left.setTranslateZ(-size);
        getChildren().addAll(left, back, right);
    }

    private Box createBox(double size) {
        var box = new Box(size * 4, size * 4, 1);
        box.setMaterial(MATERIAL);
        return box;
    }

    static Pane createBoxesControls() {
        var diffColorOn = new CheckBox("Diff Color");
        diffColorOn.setSelected(true);
        var diffColorPicker = new ColorPicker(Color.WHITE);
        MATERIAL.diffuseColorProperty().bind(new When(diffColorOn.selectedProperty())
                .then(diffColorPicker.valueProperty()).otherwise((Color) null));

        var specColorOn = new CheckBox("Spec Color");
        var specColorPicker = new ColorPicker(Color.BLACK);
        MATERIAL.specularColorProperty().bind(new When(specColorOn.selectedProperty())
                .then(specColorPicker.valueProperty()).otherwise((Color) null));

        var specPower = Controls.createSliderControl("Spec Power", MATERIAL.specularPowerProperty(), 0, 400, MATERIAL.getSpecularPower());

        var diffMapOn = new CheckBox("Diff Map");
        var diffMapPicker = new ColorPicker(Color.BLACK);
        setupMapBindings(MATERIAL.diffuseMapProperty(), diffMapPicker.valueProperty(), diffMapOn.selectedProperty());

        var specMapOn = new CheckBox("Spec Map");
        var specMapPicker = new ColorPicker(Color.BLACK);
        setupMapBindings(MATERIAL.specularMapProperty(), specMapPicker.valueProperty(), specMapOn.selectedProperty());

        var selfIllumMapOn = new CheckBox("SelfIllum Map");
        var selfIllumMapPicker = new ColorPicker(Color.BLACK);
        setupMapBindings(MATERIAL.selfIlluminationMapProperty(), selfIllumMapPicker.valueProperty(), selfIllumMapOn.selectedProperty());

        var gridPane = new GridPane();
        int row = 0;
        gridPane.add(diffColorOn, 0, row);
        gridPane.add(diffColorPicker, 1, row);
        row++;
        gridPane.add(specColorOn, 0, row);
        gridPane.add(specColorPicker, 1, row);
        row++;
        gridPane.add(specPower, 0, row, 2, 1);
        row++;
        gridPane.add(diffMapOn, 0, row);
        gridPane.add(diffMapPicker, 1, row);
        row++;
        gridPane.add(specMapOn, 0, row);
        gridPane.add(specMapPicker, 1, row);
        row++;
        gridPane.add(selfIllumMapOn, 0, row);
        gridPane.add(selfIllumMapPicker, 1, row);
        return gridPane;
    }

    private static void setupMapBindings(ObjectProperty<Image> map, ObjectProperty<Color> colorProp, BooleanProperty on) {
        var image = createMapImage(colorProp);
        map.bind(new When(on).then(image).otherwise((WritableImage) null));
    }

    static Image createMapImage(ObjectProperty<Color> colorProp) {
        var image = new WritableImage(1, 1);
        image.getPixelWriter().setColor(0, 0, colorProp.get());
        colorProp.addListener((obs, ov, nv) -> image.getPixelWriter().setColor(0, 0, nv));
        return image;
    }
}
