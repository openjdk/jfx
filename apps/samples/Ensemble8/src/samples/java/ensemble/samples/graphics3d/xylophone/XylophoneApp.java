/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.EventHandler;
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
 * @see javafx.scene.shape.Box
 * @see javafx.scene.paint.PhongMaterial
 * @see javafx.scene.media.AudioClip
 * @see javafx.scene.PerspectiveCamera
 * @see javafx.scene.transform.Rotate
 * @see javafx.scene.transform.Scale
 * @see javafx.animation.Timeline
 * @see javafx.animation.KeyFrame
 * @see javafx.animation.KeyValue
 * @see javafx.animation.Interpolator
 * @see javafx.scene.input.MouseEvent
 * @see javafx.event.EventHandler
 * @see javafx.util.Duration
 * @see javafx.scene.Group
 * @see javafx.scene.SceneAntialiasing
 * @see javafx.scene.SubScene
 * @conditionalFeatures SCENE3D
 */
public class XylophoneApp extends Application {

    private Timeline animation;
    private Timeline animation2;

    public Parent createContent() {
        Xform sceneRoot = new Xform();
        sceneRoot.rx.setAngle(45.0);
        sceneRoot.ry.setAngle(30.0);
        sceneRoot.setScale(2 * 1.5);

        final AudioClip bar1Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note1.wav").toString());
        final AudioClip bar2Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note2.wav").toString());
        final AudioClip bar3Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note3.wav").toString());
        final AudioClip bar4Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note4.wav").toString());
        final AudioClip bar5Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note5.wav").toString());
        final AudioClip bar6Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note6.wav").toString());
        final AudioClip bar7Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note7.wav").toString());
        final AudioClip bar8Note =
                new AudioClip(XylophoneApp.class.getResource("/ensemble/samples/shared-resources/Note8.wav").toString());

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

        // Bar1
        Box bar1Cube = new Box(barWidth, barDepth, 100.0);
        bar1Cube.setMaterial(new PhongMaterial(Color.PURPLE));
        bar1Cube.setTranslateX(xStart + 1 * xOffset);
        bar1Cube.setTranslateZ(yPos);

        // Bar2
        Box bar2Cube = new Box(barWidth, barDepth, 95);
        bar2Cube.setMaterial(new PhongMaterial(Color.BLUEVIOLET));
        bar2Cube.setTranslateX(xStart + 2 * xOffset);
        bar2Cube.setTranslateZ(yPos);

        // Bar3
        Box bar3Cube = new Box(barWidth, barDepth, 90);
        bar3Cube.setMaterial(new PhongMaterial(Color.BLUE));
        bar3Cube.setTranslateX(xStart + 3 * xOffset);
        bar3Cube.setTranslateZ(yPos);

        // Bar4
        Box bar4Cube = new Box(barWidth, barDepth, 85);
        bar4Cube.setMaterial(new PhongMaterial(Color.GREEN));
        bar4Cube.setTranslateX(xStart + 4 * xOffset);
        bar4Cube.setTranslateZ(yPos);

        // Bar5
        Box bar5Cube = new Box(barWidth, barDepth, 80);
        bar5Cube.setMaterial(new PhongMaterial(Color.GREENYELLOW));
        bar5Cube.setTranslateX(xStart + 5 * xOffset);
        bar5Cube.setTranslateZ(yPos);

        // Bar6
        Box bar6Cube = new Box(barWidth, barDepth, 75);
        bar6Cube.setMaterial(new PhongMaterial(Color.YELLOW));
        bar6Cube.setTranslateX(xStart + 6 * xOffset);
        bar6Cube.setTranslateZ(yPos);

        // Bar7
        Box bar7Cube = new Box(barWidth, barDepth, 70);
        bar7Cube.setMaterial(new PhongMaterial(Color.ORANGE));
        bar7Cube.setTranslateX(xStart + 7 * xOffset);
        bar7Cube.setTranslateZ(yPos);

        // Bar8
        Box bar8Cube = new Box(barWidth, barDepth, 65);
        bar8Cube.setMaterial(new PhongMaterial(Color.RED));
        bar8Cube.setTranslateX(xStart + 8 * xOffset);
        bar8Cube.setTranslateZ(yPos);

        bar1Cube.setOnMousePressed((MouseEvent me) -> {
            bar1Note.play();
        });
        bar2Cube.setOnMousePressed((MouseEvent me) -> {
            bar2Note.play();
        });
        bar3Cube.setOnMousePressed((MouseEvent me) -> {
            bar3Note.play();
        });
        bar4Cube.setOnMousePressed((MouseEvent me) -> {
            bar4Note.play();
        });
        bar5Cube.setOnMousePressed((MouseEvent me) -> {
            bar5Note.play();
        });
        bar6Cube.setOnMousePressed((MouseEvent me) -> {
            bar6Note.play();
        });
        bar7Cube.setOnMousePressed((MouseEvent me) -> {
            bar7Note.play();
        });
        bar8Cube.setOnMousePressed((MouseEvent me) -> {
            bar8Note.play();
        });
        rectangleGroup.getChildren().addAll(base1Cube, base2Cube,
                bar1Cube, bar2Cube, bar3Cube,
                bar4Cube, bar5Cube, bar6Cube,
                bar7Cube, bar8Cube);
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

        SubScene subScene = new SubScene(sceneRoot, 780 * 1.5, 380 * 1.5, true, SceneAntialiasing.BALANCED);
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
