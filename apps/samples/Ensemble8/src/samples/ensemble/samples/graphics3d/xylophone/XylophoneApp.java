/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
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

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A sample that demonstrates a xylophone made of 3D cubes. When the
 * application runs in standalone mode, the scene must be constructed with
 * the depthBuffer argument set to true, and the root node must have depthTest
 * set to true.
 *
 * @sampleName Xylophone
 * @preview preview.png
 * @see javafx.scene.transform.Rotate
 * @see javafx.scene.paint.Color
 * @see javafx.scene.shape.RectangleBuilder
 */
public class XylophoneApp extends Application {

    private Timeline animation;
    private Timeline animation2;

    public Parent createContent() {
        Xform sceneRoot = new Xform();
        sceneRoot.rx.setAngle(225.0);
        sceneRoot.ry.setAngle(30.0);
        sceneRoot.setScale(2.1);

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
        double zPos = 0.0;
        double barWidth = 22.0;
        double barDepth = 7.0;

        // Base1
        Cube base1Cube = new Cube(1.0, new Color(0.2, 0.12, 0.1, 1.0), 1.0);
        base1Cube.setTranslateX(xStart + 135);
        base1Cube.setTranslateZ(yPos + 20.0);
        base1Cube.setTranslateY(11.0);
        base1Cube.setScaleX(barWidth * 11.5);
        base1Cube.setScaleZ(10.0);
        base1Cube.setScaleY(barDepth * 2.0);

        // Base2
        Cube base2Cube = new Cube(1.0, new Color(0.2, 0.12, 0.1, 1.0), 1.0);
        base2Cube.setTranslateX(xStart + 135);
        base2Cube.setTranslateZ(yPos - 20.0);
        base2Cube.setTranslateY(11.0);
        base2Cube.setScaleX(barWidth * 11.5);
        base2Cube.setScaleZ(10.0);
        base2Cube.setScaleY(barDepth * 2.0);

        // Bar1
        Cube bar1Cube = new Cube(1.0, Color.PURPLE, 1.0);
        bar1Cube.setTranslateX(xStart + 1 * xOffset);
        bar1Cube.setTranslateZ(yPos);
        bar1Cube.setScaleX(barWidth);
        bar1Cube.setScaleZ(100.0);
        bar1Cube.setScaleY(barDepth);

        // Bar2
        Cube bar2Cube = new Cube(1.0, Color.BLUEVIOLET, 1.0);
        bar2Cube.setTranslateX(xStart + 2 * xOffset);
        bar2Cube.setTranslateZ(yPos);
        bar2Cube.setScaleX(barWidth);
        bar2Cube.setScaleZ(95.0);
        bar2Cube.setScaleY(barDepth);

        // Bar3
        Cube bar3Cube = new Cube(1.0, Color.BLUE, 1.0);
        bar3Cube.setTranslateX(xStart + 3 * xOffset);
        bar3Cube.setTranslateZ(yPos);
        bar3Cube.setScaleX(barWidth);
        bar3Cube.setScaleZ(90.0);
        bar3Cube.setScaleY(barDepth);

        // Bar4
        Cube bar4Cube = new Cube(1.0, Color.GREEN, 1.0);
        bar4Cube.setTranslateX(xStart + 4 * xOffset);
        bar4Cube.setTranslateZ(yPos);
        bar4Cube.setScaleX(barWidth);
        bar4Cube.setScaleZ(85.0);
        bar4Cube.setScaleY(barDepth);

        // Bar5
        Cube bar5Cube = new Cube(1.0, Color.GREENYELLOW, 1.0);
        bar5Cube.setTranslateX(xStart + 5 * xOffset);
        bar5Cube.setTranslateZ(yPos);
        bar5Cube.setScaleX(barWidth);
        bar5Cube.setScaleZ(80.0);
        bar5Cube.setScaleY(barDepth);

        // Bar6
        Cube bar6Cube = new Cube(1.0, Color.YELLOW, 1.0);
        bar6Cube.setTranslateX(xStart + 6 * xOffset);
        bar6Cube.setTranslateZ(yPos);
        bar6Cube.setScaleX(barWidth);
        bar6Cube.setScaleZ(75.0);
        bar6Cube.setScaleY(barDepth);

        // Bar7
        Cube bar7Cube = new Cube(1.0, Color.ORANGE, 1.0);
        bar7Cube.setTranslateX(xStart + 7 * xOffset);
        bar7Cube.setTranslateZ(yPos);
        bar7Cube.setScaleX(barWidth);
        bar7Cube.setScaleZ(70.0);
        bar7Cube.setScaleY(barDepth);

        // Bar8
        Cube bar8Cube = new Cube(1.0, Color.RED, 1.0);
        bar8Cube.setTranslateX(xStart + 8 * xOffset);
        bar8Cube.setTranslateZ(yPos);
        bar8Cube.setScaleX(barWidth);
        bar8Cube.setScaleZ(65.0);
        bar8Cube.setScaleY(barDepth);

        bar1Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                bar1Note.play();
            }
        });
        bar2Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                bar2Note.play();
            }
        });
        bar3Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                bar3Note.play();
            }
        });
        bar4Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                bar4Note.play();
            }
        });
        bar5Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                bar5Note.play();
            }
        });
        bar6Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                bar6Note.play();
            }
        });
        bar7Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                bar7Note.play();
            }
        });
        bar8Cube.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                bar8Note.play();
            }
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
                new KeyValue(sceneRoot.rx.angleProperty(), 200d,
                Interpolator.TANGENT(Duration.seconds(1.0), 200d,
                Duration.seconds(1.0), 200d))),
                new KeyFrame(Duration.seconds(4),
                new KeyValue(sceneRoot.rx.angleProperty(), 250d,
                Interpolator.TANGENT(Duration.seconds(1.0), 250d,
                Duration.seconds(1.0), 250d))),
                new KeyFrame(Duration.seconds(8),
                new KeyValue(sceneRoot.rx.angleProperty(), 200d,
                Interpolator.TANGENT(Duration.seconds(1.0), 200d,
                Duration.seconds(1.0), 200d))));
        animation2.setCycleCount(Animation.INDEFINITE);

        return sceneRoot;


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
        animation.play();
        animation2.play();
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
