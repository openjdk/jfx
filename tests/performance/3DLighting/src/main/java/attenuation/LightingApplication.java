/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import attenuation.Models.Model;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A utility application for testing 3D features, including lighting, materials, and performance.
 * <p>
 * <b>Important</b>: when measuring performance, make sure that no other application on your system is rendering heavy
 * graphics, like videos, to a screen, as this will corrupt the measurement.
 */
public class LightingApplication extends Application {

    private final Environment environment = new Environment();
    private final Benchmark benchmark = new Benchmark(environment);

    @Override
    public void start(Stage stage) throws Exception {
        Node perfControls = createPerformanceControls();
        Node modelsControls = createModelsControls();

        Node backgroundControls = environment.createBackgroundControls();
        Node screenshotControls = environment.createScreenshotControls();
        Node defaultLightControl = environment.createDefaultLightControl();
        Node lightsControls = environment.createLightsControls();

        var controls = new VBox(perfControls, modelsControls, backgroundControls, screenshotControls,
                defaultLightControl, lightsControls);

        var hBox = new HBox(new ScrollPane(controls), environment);
        HBox.setHgrow(environment, Priority.ALWAYS);

        stage.setScene(new Scene(hBox));
        stage.setTitle("3DLighting");
        stage.show();
    }

    private Node createPerformanceControls() {
        var playButton = benchmark.createPlayButton();
        var stopButton = benchmark.createStopButton();

        Node sphereControls = benchmark.createSphereControls();
        Node meshControls = benchmark.createMeshControls();

        var titlePane = new TitledPane("Performance", new VBox(sphereControls, meshControls));
        titlePane.setGraphic(new HBox(5, playButton, stopButton));
        titlePane.setContentDisplay(ContentDisplay.RIGHT);
        titlePane.setExpanded(false);
        return titlePane;
    }

    private Node createModelsControls() {
        var models = new ChoiceBox<Model>();
        models.getItems().addAll(Model.values());
        models.setValue(Model.NONE);
        models.setOnAction(e -> environment.switchTo(Models.createModel(models.getValue())));

        var titlePane = new TitledPane("Models", MaterialControls.create());
        titlePane.setGraphic(models);
        titlePane.setContentDisplay(ContentDisplay.RIGHT);
        titlePane.setExpanded(false);
        return titlePane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
