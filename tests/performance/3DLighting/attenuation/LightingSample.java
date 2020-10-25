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

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SpotLight;
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
        environment.setStyle("-fx-background-color: teal");

        var subdivisionSlider = new Slider(10, 1000, 60);
        subdivisionSlider.setMajorTickUnit(50);
        setupSlier(subdivisionSlider);

        var subdivisionLabel = new Label();
        subdivisionLabel.textProperty().bindBidirectional(subdivisionSlider.valueProperty(), new NumberStringConverter("#"));

        var sphere = new Button("Sphere");
        sphere.setOnAction(e -> switchTo(environment.createSphere((int) subdivisionSlider.getValue())));

        var sphereBox = new HBox(sphere, subdivisionSlider, subdivisionLabel);


        var quadSlider = new Slider(100, 5000, 1000);
        quadSlider.setMajorTickUnit(200);
        setupSlier(quadSlider);

        var quadLabel = new Label();
        quadLabel.textProperty().bindBidirectional(quadSlider.valueProperty(), new NumberStringConverter("#"));

        var mesh = new Button("Mesh");
        mesh.setOnAction(e -> switchTo(environment.createMeshView((int) quadSlider.getValue())));

        var meshBox = new HBox(mesh, quadSlider, quadLabel);


        var box = new Button("Boxes (static)");
        box.setOnAction(e -> switchTo(environment.createBoxes()));


        var playButton = new Button("Start");
        playButton.setOnAction(e -> startMeasurement());
        
        var stopButton = new Button("Stop");
        stopButton.setOnAction(e -> stopMeasurement());

        var controls = new VBox(3, sphereBox, meshBox, box, new HBox(5, playButton, stopButton));
        for (var light : environment.lights) {
            VBox vBox = null;
            if (light instanceof PointLight) {
                vBox = addPointLightControls((PointLight) light);
            } else if (light instanceof SpotLight) {
                vBox = addSpotLightControls((SpotLight) light);
            }
            controls.getChildren().add(new TitledPane(light.getUserData() + " " + light.getClass().getSimpleName(), vBox));
        }

        var hBox = new HBox(new ScrollPane(controls), environment);
        HBox.setHgrow(environment, Priority.ALWAYS);
        stage.setScene(new Scene(hBox));
        stage.setWidth(1100);
        stage.setHeight(735);
        stage.show();
    }

    private void setupSlier(Slider slider) {
        slider.setMinorTickCount(0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
    }

    protected VBox addPointLightControls(PointLight light) {
        return addLightControls(light);
    }
    
    protected VBox addSpotLightControls(SpotLight light) {
        return addLightControls(light);
    }

    protected VBox addLightControls(LightBase light) {
        var lightOn = new CheckBox("On/Off");
        lightOn.setSelected(true);
        light.lightOnProperty().bind(lightOn.selectedProperty());
        return new VBox(lightOn);
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
