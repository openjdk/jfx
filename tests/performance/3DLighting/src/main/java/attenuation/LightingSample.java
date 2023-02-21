/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;

/**
 * A sample application for measuring FPS for various 3D nodes with environmental lighting.
 * <p>
 * <b>Important</b>: make sure that no other application on your system is rendering heavy graphics, like videos, to a screen,
 * as this will corrupt the measurement.
 */
public class LightingSample extends Application {

    private final Environment environment = new Environment();
    private final TranslateTransition animation = createAnimation();
    private final FPSCounter fpsCouner = new FPSCounter();

    @Override
    public void start(Stage stage) throws Exception {
        var sphereControls = createSphereControls();
        var meshControls = createMeshControls();
        var boxesControls = createBoxesControls();

        var playButton = new Button("Start");
        playButton.setOnAction(e -> startMeasurement());

        var stopButton = new Button("Stop");
        stopButton.setOnAction(e -> stopMeasurement());

        var animationControls = new HBox(5, playButton, stopButton);

        var defaultLightButton = new CheckBox("Force default light");
        defaultLightButton.setOnAction(e -> environment.forceDefaultLight(defaultLightButton.isSelected()));

        var controls = new VBox(sphereControls, meshControls, animationControls, boxesControls, defaultLightButton);

        environment.ambientLights.forEach(light -> controls.getChildren().add(Controls.createLightControls(light, null)));
        environment.pointLights.forEach(light -> controls.getChildren().add(Controls.addPointLightControls(light)));
        environment.spotLights.forEach(light -> controls.getChildren().add(Controls.addSpotLightControls(light)));
        environment.directionalLights.forEach(light -> controls.getChildren().add(Controls.addDirectionalLightControls(light)));

        var hBox = new HBox(new ScrollPane(controls), environment);
        HBox.setHgrow(environment, Priority.ALWAYS);
        stage.setScene(new Scene(hBox));
        stage.setWidth(1100);
        stage.setHeight(735);
        stage.show();
    }

    private HBox createMeshControls() {
        var quadSlider = new Slider(100, 5000, 1000);
        quadSlider.setMajorTickUnit(100);
        setupSlider(quadSlider);

        var quadLabel = new Label();
        quadLabel.textProperty().bindBidirectional(quadSlider.valueProperty(), new NumberStringConverter("#"));

        var mesh = new Button("Mesh");
        mesh.setOnAction(e -> switchTo(environment.createMeshView((int) quadSlider.getValue())));

        var meshBox = new HBox(mesh, quadSlider, quadLabel);
        return meshBox;
    }

    private HBox createSphereControls() {
        var subdivisionSlider = new Slider(10, 1000, 60);
        subdivisionSlider.setMajorTickUnit(50);
        setupSlider(subdivisionSlider);

        var subdivisionLabel = new Label();
        subdivisionLabel.textProperty().bindBidirectional(subdivisionSlider.valueProperty(), new NumberStringConverter("#"));

        var sphere = new Button("Sphere");
        sphere.setOnAction(e -> switchTo(environment.createSphere((int) subdivisionSlider.getValue())));

        var sphereBox = new HBox(sphere, subdivisionSlider, subdivisionLabel);
        return sphereBox;
    }

    private Node createBoxesControls() {
        var box = new Button("Create");

        var titlePane = new TitledPane("Boxes", new VBox(1, box, new HBox(Boxes.createBoxesControls())));

        titlePane.setExpanded(false);
        box.setOnAction(e -> switchTo(environment.createBoxes()));
        return titlePane;
    }

    private void setupSlider(Slider slider) {
        slider.setMinorTickCount(0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
    }

   private TranslateTransition createAnimation() {
        var anim = new TranslateTransition(Duration.seconds(2));
        anim.setAutoReverse(true);
        anim.setCycleCount(Animation.INDEFINITE);
        anim.setFromZ(150);
        anim.setToZ(0);
        return anim;
    }

    private void switchTo(Node node) {
        stopMeasurement();
        environment.switchTo(node);
        animation.setNode(node);
    }

    private void startMeasurement() {
        animation.playFromStart();
        fpsCouner.start();
    }

    private void stopMeasurement() {
        fpsCouner.stop();
        fpsCouner.reset();
        animation.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
