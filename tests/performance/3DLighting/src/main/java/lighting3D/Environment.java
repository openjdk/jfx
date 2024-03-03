/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SpotLight;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import lighting3D.CaptureUtils.Format;

/**
 * The 3D environment. Includes the lights and shapes.
 */
class Environment extends CameraScene3D {

    static final Image BACKGROUND_IMAGE = new Image(CameraScene3D.class.getResourceAsStream("background.jpg"));
    private static final double LIGHT_REP_RADIUS = 2;
    private static final double LIGHT_X_DIST = 50;
    static final double LIGHT_Z_DIST = 50;

    private final AmbientLight ambientLight1 = new AmbientLight(Color.WHITE);
    private final AmbientLight ambientLight2 = new AmbientLight(Color.RED);
    private final AmbientLight ambientLight3 = new AmbientLight(Color.BLACK);
    private final List<AmbientLight> ambientLights = List.of(ambientLight1, ambientLight2, ambientLight3);

    private final DirectionalLight directionalLight1 = new DirectionalLight(Color.RED);
    private final DirectionalLight directionalLight2 = new DirectionalLight(Color.BLUE);
    private final DirectionalLight directionalLight3 = new DirectionalLight(Color.MAGENTA);
    private final List<DirectionalLight> directionalLights = List.of(directionalLight1, directionalLight2, directionalLight3);

    private final PointLight pointLight1 = new PointLight(Color.RED);
    private final PointLight pointLight2 = new PointLight(Color.BLUE);
    private final PointLight pointLight3 = new PointLight(Color.MAGENTA);
    private final List<PointLight> pointLights = List.of(pointLight1, pointLight2, pointLight3);

    private final SpotLight spotLight1 = new SpotLight(Color.RED);
    private final SpotLight spotLight2 = new SpotLight(Color.BLUE);
    private final SpotLight spotLight3 = new SpotLight(Color.MAGENTA);
    private final List<SpotLight> spotLights = List.of(spotLight1, spotLight2, spotLight3);

    private final Group shapeGroup = new Group();
    private final Group lightsGroup = new Group();

    Environment() {
        setPrefWidth(BACKGROUND_IMAGE.getWidth() / 2.5);
        setPrefHeight(BACKGROUND_IMAGE.getHeight() / 2.5);

        farClip.set(1000);
        zoom.set(-570);

        ambientLights.forEach(this::addLight);
        directionalLights.forEach(this::addLight);
        pointLights.forEach(this::setupLight);
        spotLights.forEach(this::setupLight);

        pointLight1.setTranslateX(LIGHT_X_DIST);
        spotLight1.setTranslateX(LIGHT_X_DIST);
        pointLight2.setTranslateX(-LIGHT_X_DIST);
        spotLight2.setTranslateX(-LIGHT_X_DIST);

        directionalLight1.setDirection(new Point3D(-LIGHT_X_DIST, 0, LIGHT_Z_DIST));
        directionalLight2.setDirection(new Point3D(LIGHT_X_DIST, 0, LIGHT_Z_DIST));

        rootGroup.getChildren().addAll(lightsGroup, shapeGroup);
        rootGroup.setMouseTransparent(true);
    }

    private void setupLight(PointLight light) {
        light.setTranslateZ(-LIGHT_Z_DIST);
        addLight(light);

        var lightRep = new Sphere(LIGHT_REP_RADIUS);
        var lightRepMat = new PhongMaterial();
        lightRepMat.selfIlluminationMapProperty().bind(light.colorProperty().map(MaterialControls::imageOf));
        lightRep.setMaterial(lightRepMat);
        lightRep.translateXProperty().bind(light.translateXProperty());
        lightRep.translateYProperty().bind(light.translateYProperty());
        lightRep.translateZProperty().bind(light.translateZProperty());
        lightRep.visibleProperty().bind(light.lightOnProperty());
        rootGroup.getChildren().add(lightRep);
    }

    private void addLight(LightBase light) {
        light.getScope().add(shapeGroup);
        lightsGroup.getChildren().add(light);
    }

    Node createLightsControls() {
        var controls = new VBox();
        ambientLights.forEach(light -> controls.getChildren().add(Controls.createLightControls(light)));
        pointLights.forEach(light -> controls.getChildren().add(Controls.createLightControls(light)));
        spotLights.forEach(light -> controls.getChildren().add(Controls.createLightControls(light)));
        directionalLights.forEach(light -> controls.getChildren().add(Controls.createLightControls(light)));
        return controls;
    }

    Node createBackgroundControls() {
        var bgProp = new SimpleObjectProperty<Image>();
        var bgSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true);
        backgroundProperty().bind(bgProp.map(image -> new Background(new BackgroundImage(image, null, null, null, bgSize))));
        Node controls = MaterialControls.createMapControls(bgProp, MaterialControls.createFileChooser());
        return new HBox(new Label("Background"), controls);
    }

    Node createScreenshotControls() {
        var formats = new ChoiceBox<Format>();
        formats.getItems().addAll(Format.values());
        formats.setValue(Format.PNG);

        var graphic = new Text("📷");
        graphic.setBoundsType(TextBoundsType.VISUAL);
        graphic.setFont(Font.font(32));

        var screenshotButton = new Button("", graphic);
        screenshotButton.setPadding(new Insets(2));
        screenshotButton.setTooltip(new Tooltip("Capture screenshot"));
        screenshotButton.setOnAction(e -> CaptureUtils.capture(snapshot(null, null), formats.getValue()));

        return new HBox(2, screenshotButton, formats);
    }

    Node createDefaultLightControl() {
        var checkBox = new CheckBox("Force default light");
        checkBox.setOnAction(e -> {
            if (checkBox.isSelected()) {
                rootGroup.getChildren().remove(lightsGroup);
            } else {
                rootGroup.getChildren().add(lightsGroup);
            }
        });
        return checkBox;
    }

    void switchTo(Node node) {
        shapeGroup.getChildren().setAll(node);
    }
}
