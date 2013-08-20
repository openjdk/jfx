/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package fx83dfeatures;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ScrollBar;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;

public class SpecularColorTestApp extends Application {

    private ScrollBar specularPowerScroll;
    private ColorPicker specularColorPicker;
    private CheckBox specularMapCheckBox;

    @Override
    public void start(Stage stage) throws Exception {
        final Image diffuseMap = new Image("resources/cup_diffuseMap_1024.png");
        final PhongMaterial material = new PhongMaterial(Color.ANTIQUEWHITE);

        final Sphere s = new Sphere();
        s.setScaleX(100);
        s.setScaleY(100);
        s.setScaleZ(100);
        s.setMaterial(material);
        s.setTranslateX(150);
        s.setTranslateY(250);

        final Sphere s1 = new Sphere(2);
        s1.setScaleX(100);
        s1.setScaleY(100);
        s1.setScaleZ(100);
        s1.setMaterial(material);
        s1.setTranslateX(500);
        s1.setTranslateY(250);

        Group root1 = new Group(s, s1);
        specularPowerScroll = new ScrollBar();
        specularPowerScroll.setValue(material.getSpecularPower());
        specularPowerScroll.setMin(0);
        specularPowerScroll.setMax(1);
        specularPowerScroll.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                material.setSpecularPower(t1.doubleValue());
                System.out.println("power changed " + t1.doubleValue());
            }
        });

        specularColorPicker = new ColorPicker(material.getSpecularColor());
        specularColorPicker.valueProperty().addListener(new ChangeListener<Color>() {
            @Override
            public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                material.setSpecularColor(t1);
                System.out.println("color changed " + t1);
            }
        });

        specularMapCheckBox = new CheckBox("Specular Map");
        specularMapCheckBox.setSelected(false);
        specularMapCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                material.setSpecularMap(t1 ? diffuseMap : null);
            }
        });
        VBox controls = new VBox(15);
        controls.getChildren().addAll(specularColorPicker, specularPowerScroll, specularMapCheckBox);

        Group root = new Group(root1, controls);
        Scene scene = new Scene(root, 800, 500, true);

        scene.setCamera(new PerspectiveCamera());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
