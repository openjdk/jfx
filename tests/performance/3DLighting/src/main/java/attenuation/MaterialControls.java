/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Utility class for creating material adjustment controls.
 */
final class MaterialControls {

    static final PhongMaterial MATERIAL = new PhongMaterial();

    static Pane create() {
        var diffColorOn = new CheckBox("Diff Color");
        diffColorOn.setSelected(true);
        var diffColorPicker = new ColorPicker(Color.WHITE);
        MATERIAL.diffuseColorProperty().bind(diffColorOn.selectedProperty().flatMap(s -> s ? diffColorPicker.valueProperty() : null));

        var specColorOn = new CheckBox("Spec Color");
        var specColorPicker = new ColorPicker(Color.BLACK);
        MATERIAL.specularColorProperty().bind(specColorOn.selectedProperty().flatMap(s -> s ? specColorPicker.valueProperty() : null));

        var specPower = Controls.createSliderControl(MATERIAL.specularPowerProperty(), 0, 400, MATERIAL.getSpecularPower());

        var chooser = createFileChooser();
        var diffMapControls = createMapControls(MATERIAL.diffuseMapProperty(), chooser);
        var specMapControls = createMapControls(MATERIAL.specularMapProperty(), chooser);
        var selfIllumMapControls = createMapControls(MATERIAL.selfIlluminationMapProperty(), chooser);
        var bumpMapControls = createMapControls(MATERIAL.bumpMapProperty(), chooser);

        var gridPane = new GridPane();
        gridPane.addRow(gridPane.getRowCount(), diffColorOn, diffColorPicker);
        gridPane.addRow(gridPane.getRowCount(), specColorOn, specColorPicker);
        gridPane.add(new HBox(createLabel("Spec power"), specPower), 0, gridPane.getRowCount(), 2, 1);
        gridPane.addRow(gridPane.getRowCount(), createLabel("Diff Map"), diffMapControls);
        gridPane.addRow(gridPane.getRowCount(), createLabel("Spec Map"), specMapControls);
        gridPane.addRow(gridPane.getRowCount(), createLabel("Self-illum Map"), selfIllumMapControls);
        gridPane.addRow(gridPane.getRowCount(), createLabel("Bump Map"), bumpMapControls);
        return gridPane;
    }

    static FileChooser createFileChooser() {
        var chooser = new FileChooser();
        chooser.setTitle("Select image");
        chooser.setSelectedExtensionFilter(new ExtensionFilter("Image", ".bmp", ".jpg", ".jpeg", ".gif", ".png", ".tif", ".tiff"));
        return chooser;
    }

    private static Node createLabel(String text) {
        var label = new Label(text);
        GridPane.setValignment(label, VPos.TOP);
        return label;
    }

    static Node createMapControls(ObjectProperty<Image> mapProp, FileChooser chooser) {
        var noneButton = new RadioButton("None");
        noneButton.setOnAction(e -> mapProp.set(null));
        noneButton.setSelected(true);

        var imageButton = createImageButton(mapProp, chooser);
        var colorButton = createColorButton(mapProp);

        var toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(noneButton, colorButton, imageButton);

        return new VBox(1, noneButton, colorButton, imageButton);
    }

    private static RadioButton createColorButton(ObjectProperty<Image> mapProp) {
        var picker = new ColorPicker(Color.BLACK);

        var colorButton = new RadioButton(" ");
        colorButton.setContentDisplay(ContentDisplay.CENTER);
        colorButton.setGraphicTextGap(0);
        colorButton.setGraphic(picker);
        colorButton.selectedProperty().subscribe(selected -> {
            if (selected) {
                mapProp.bind(picker.valueProperty().map(MaterialControls::imageOf));
            } else {
                mapProp.unbind();
            }
        });
        return colorButton;
    }

    private static RadioButton createImageButton(ObjectProperty<Image> mapProp, FileChooser chooser) {
        var chosenImage = new SimpleObjectProperty<>(Environment.BACKGROUND_IMAGE);

        var graphic = new Text("📂");
        graphic.setBoundsType(TextBoundsType.VISUAL);
        graphic.setFill(Color.rgb(235, 163, 0));
        graphic.setFont(Font.font(20));

        var openButton = new Button("", graphic);
        openButton.setPadding(new Insets(2, 2, 3, 3));
        openButton.setOnAction(e -> {
            Optional.ofNullable(chooser.showOpenDialog(null)).ifPresent(file -> {
                try {
                    chosenImage.setValue(new Image(file.toURI().toURL().toExternalForm()));
                    chooser.setInitialDirectory(file.getParentFile());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
        });

        var imageButton = new RadioButton(" ");
        imageButton.setGraphic(openButton);
        imageButton.setContentDisplay(ContentDisplay.CENTER);
        imageButton.setGraphicTextGap(0);
        imageButton.selectedProperty().subscribe(selected -> {
            if (selected) {
                mapProp.bind(chosenImage);
            } else {
                mapProp.unbind();
            }
        });
        return imageButton;
    }

    static Image imageOf(Color color) {
        var image = new WritableImage(1, 1);
        image.getPixelWriter().setColor(0, 0, color);
        return image;
    }
}
