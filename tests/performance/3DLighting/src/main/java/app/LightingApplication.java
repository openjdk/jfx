/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
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

package app;

import app.Models.Model;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/// A utility application for testing 3D features.
///
/// ## Camera Movements
///
/// The camera can be position in the 3D environment by moving it with the following controls:
/// - Primary mouse button drag to pan
/// - Secondary mouse button to rotate
/// - Shift + secondary mouse button to swivel
/// - Mouse scrollwheel to zoom
///
/// ## Background
///
/// A background can be set to allow for better contrast or to test transparency. It can be a solid color or a selected
/// image. A background does not interact with lights.
///
/// ## Screenshots
///
/// The screenshot button will save an image of the current 3D environment (background included). These are written to
/// the `/screenshots` dir of this application. They are useful for demonstrations in issues, docs, or presentations.
///
/// ## Performance Measurements
///
/// The 'Performance' pane is used to measure the FPS of 3D setups. The 3D setup will be animated during the measurement
/// to keep the content dirty, otherwise no render request will be made. By default (see the Gradle build file), vsync
/// is turned off and the animation pulse is increased from 60 to 1000 to allow less capped measurements.
///
/// To measure performance:
/// 1. Create the setup:
///    - Add the 3D content
///    - Set up the lights
///    - Position the camera
///    - Resize the window to the desired size
/// 2. Press the start button. Measurements will appear in the header area and be printed to the console.
/// 3. Press the stop button. The measurements will stop and the application will be ready for the next measurement.
///
/// **Important**: when measuring performance, make sure that no other application on your system is rendering heavy
/// graphics, like videos, to a screen, as this will corrupt the measurement.
///
/// ## Models Inspection
///
/// The 'Models' pane is used to visually inspect the light-material behavior.
///
/// - Choose a model
/// - Set up its material (the maps can be a solid color or an image, like the background described above)
/// - Set up the lights
///
/// ## Running
///
/// Run with Gradle from the jfx root with: `gradlew -p tests\performance\3DLighting run`. This reuses the parent Gradle
/// wrapper.
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

        var controls = new VBox(5, perfControls, modelsControls, backgroundControls, screenshotControls,
                defaultLightControl, lightsControls);

        var hBox = new HBox(new ScrollPane(controls), environment);
        HBox.setHgrow(environment, Priority.ALWAYS);

        stage.setScene(new Scene(hBox));
        environment.boundsInLocalProperty().subscribe(bounds -> {
            String dims = Math.round(bounds.getWidth()) + "x" + Math.round(bounds.getHeight());
            IO.println(dims); // print for ease of copy paste
            stage.setTitle("3DLighting " + dims);
        });
        stage.show();
    }

    private Node createPerformanceControls() {
        var playButton = benchmark.createPlayButton();
        var stopButton = benchmark.createStopButton();

        Node sphereControls = benchmark.createSphereControls();
        Node stackedQuadsControls = benchmark.createStackedQuadsControls();
        Node spreadQuadsControls = benchmark.createSpreadQuadsControls();
        Node stackedMeshesControls = benchmark.createStackedMeshesControls();
        Node spreadMeshesControls = benchmark.createSpreadMeshesControls();
        Node spreadMeshesAnimControls = benchmark.createSpreadMeshesAnimControls();

        var fpsLabel = new Label();
        fpsLabel.textProperty().bind(benchmark.instantFps.asString("%.2f")
                .concat(" / avg: ").concat(benchmark.averageFps.asString("%.2f")));
        var titlePane = new TitledPane("Performance", new VBox(sphereControls, stackedQuadsControls,
                spreadQuadsControls, stackedMeshesControls, spreadMeshesControls, spreadMeshesAnimControls));
        titlePane.setGraphic(new HBox(5, playButton, stopButton, fpsLabel));
        titlePane.setContentDisplay(ContentDisplay.RIGHT);
        titlePane.setExpanded(false);
        return titlePane;
    }

    private Node createModelsControls() {
        var models = new ChoiceBox<Model>();
        models.getItems().addAll(Model.values());
        models.setValue(Model.NONE);
        models.setOnAction(_ -> environment.switchTo(Models.createModel(models.getValue())));

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
