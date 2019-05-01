/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.graphics3d.xylophone;

import ensemble.samples.media.audioclip.AudioClipApp;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A sample that demonstrates a xylophone made of 3D cubes. It is animated and
 * plays sounds when clicked.
 *
 * @sampleName Xylophone
 * @preview preview.png
 * @docUrl http://docs.oracle.com/javase/8/javafx/graphics-tutorial/javafx-3d-graphics.htm#JFXGR256 JavaFX 3D Graphics
 * @see javafx.scene.PerspectiveCamera
 * @see javafx.scene.SceneAntialiasing
 * @see javafx.scene.SubScene
 * @see javafx.scene.input.MouseEvent
 * @see javafx.scene.media.AudioClip
 * @see javafx.scene.paint.PhongMaterial
 * @see javafx.scene.shape.Box
 * @see javafx.scene.transform.Rotate
 * @see javafx.scene.transform.Scale
 * @conditionalFeatures SCENE3D
 *
 * @related /Graphics 3d/3D Box
 * @related /Graphics 3d/3D Cubes
 * @related /Graphics 3d/3D Sphere
 * @related /Graphics 3d/3D Sphere System
 */
public class XylophoneApp extends Application {

    private Timeline animation;
    private Timeline animation2;

    /*
     * See JDK-8177428 for an explanation of why this is here.
     */
    private static AudioClip getNoteClip(String name) {
        // First look for the clips in a directory next to our jar file
        try {
            // Get a URI to this class file
            URI baseURI = XylophoneApp.class.getResource("XylophoneApp.class").toURI();

            // If we have a jar URL, get the embedded http or file URL
            // and trim off the internal jar path, this will leave us
            // with a URL to the jar file
            if (baseURI.getScheme().equals("jar")) {
                String basePath = baseURI.getSchemeSpecificPart();
                if (basePath.contains("!/")) {
                    basePath = basePath.substring(0, basePath.indexOf("!/"));
                }
                baseURI = new URI(basePath);
            }

            URL noteURL = baseURI.resolve("resources/"+name).toURL();

            // check if the resource exists, then try to load it
            if (noteURL.getProtocol().equals("http") || noteURL.getProtocol().equals("https")) {
                HttpURLConnection urlCon = (HttpURLConnection)noteURL.openConnection();
                urlCon.setRequestMethod("HEAD");
                urlCon.connect();
                if (urlCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    noteURL = null;
                }
                urlCon.disconnect();
            } else if (noteURL.getProtocol().equals("file")) {
                File f = new File(noteURL.getPath());
                if (!f.exists() || !f.isFile()) {
                    noteURL = null;
                }
            } else {
                // unsupported protocol
                noteURL = null;
            }
            if (noteURL != null) {
                return new AudioClip(noteURL.toExternalForm());
            }
        } catch (Exception e) {} // fail gracefully

        // Fall back on the embedded clips
        return new AudioClip(
                AudioClipApp.class.getResource("/ensemble/samples/shared-resources/"+name).toExternalForm());
    }

    public Parent createContent() {
        Xform sceneRoot = new Xform();
        sceneRoot.rx.setAngle(45.0);
        sceneRoot.ry.setAngle(30.0);
        sceneRoot.setScale(2 * 1.5);

        Group rectangleGroup = new Group();

        double xStart = -110.0;
        double xOffset = 30.0;
        double yPos = 25.0;
        double barWidth = 22.0;
        double barDepth = 7.0;

        // Base1
        Box base1Cube = new Box(barWidth * 11.5, barDepth * 2.0, 10.0);
        base1Cube.setMaterial(new PhongMaterial(new Color(0.2, 0.12, 0.1, 1.0)));
        base1Cube.setTranslateX(xStart + 128);
        base1Cube.setTranslateZ(yPos + 20.0);
        base1Cube.setTranslateY(11.0);

        // Base2
        Box base2Cube = new Box(barWidth * 11.5, barDepth * 2.0, 10.0);
        base2Cube.setMaterial(new PhongMaterial(new Color(0.2, 0.12, 0.1, 1.0)));
        base2Cube.setTranslateX(xStart + 128);
        base2Cube.setTranslateZ(yPos - 20.0);
        base2Cube.setTranslateY(11.0);

        Box[] barCubes = new Box[8];
        Color[] colors = {
            Color.PURPLE, Color.BLUEVIOLET, Color.BLUE, Color.GREEN,
            Color.GREENYELLOW, Color.YELLOW, Color.ORANGE, Color.RED
        };
        for (int i = 0; i < barCubes.length; i++) {
            final AudioClip barNote = getNoteClip("Note"+(i+1)+".wav");

            barCubes[i] = new Box(barWidth, barDepth, 100.0 - (i * 5.0));
            barCubes[i].setTranslateX(xStart + ((1 + i) * xOffset));
            barCubes[i].setTranslateZ(yPos);
            barCubes[i].setMaterial(new PhongMaterial(colors[i]));
            barCubes[i].setOnMousePressed((MouseEvent me) -> {
                barNote.play();
            });
        }

        rectangleGroup.getChildren().addAll(base1Cube, base2Cube);
        rectangleGroup.getChildren().addAll(java.util.Arrays.asList(barCubes));
        sceneRoot.getChildren().add(rectangleGroup);

        animation = new Timeline();
        animation.getKeyFrames().addAll(new KeyFrame(Duration.ZERO,
                new KeyValue(sceneRoot.ry.angleProperty(), 390d,
                Interpolator.TANGENT(Duration.seconds(0.5), 390d,
                Duration.seconds(0.5), 390d))),
                new KeyFrame(Duration.seconds(2),
                new KeyValue(sceneRoot.ry.angleProperty(), 30d,
                Interpolator.TANGENT(Duration.seconds(0.5), 30d,
                Duration.seconds(0.5), 30d))));

        animation2 = new Timeline();
        animation2.getKeyFrames().addAll(new KeyFrame(Duration.ZERO,
                new KeyValue(sceneRoot.rx.angleProperty(), 60d,
                Interpolator.TANGENT(Duration.seconds(1.0), 60d))),
                new KeyFrame(Duration.seconds(4),
                new KeyValue(sceneRoot.rx.angleProperty(), 80d,
                Interpolator.TANGENT(Duration.seconds(1.0), 80d))),
                new KeyFrame(Duration.seconds(8),
                new KeyValue(sceneRoot.rx.angleProperty(), 60d,
                Interpolator.TANGENT(Duration.seconds(1.0), 60d))));
        animation2.setCycleCount(Timeline.INDEFINITE);

        PerspectiveCamera camera = new PerspectiveCamera();

        SubScene subScene = new SubScene(sceneRoot, 780 * 1.5, 380 * 1.5,
                                         true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        sceneRoot.translateXProperty().bind(subScene.widthProperty().divide(2.2));
        sceneRoot.translateYProperty().bind(subScene.heightProperty().divide(1.6));

        return new Group(subScene);
    }

    public void play() {
        animation.play();
        animation2.play();
    }

    @Override
    public void stop() {
        animation.pause();
        animation2.pause();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
        play();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
