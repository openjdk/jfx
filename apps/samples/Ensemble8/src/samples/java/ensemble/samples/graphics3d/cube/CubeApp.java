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
package ensemble.samples.graphics3d.cube;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A sample that demonstrates an animated rotation of 3D cubes.
 *
 * @sampleName 3D Cubes
 * @preview preview.png
 * @see javafx.scene.shape.Box
 * @see javafx.scene.paint.PhongMaterial
 * @see javafx.scene.PerspectiveCamera
 * @see javafx.scene.SubScene
 * @see javafx.scene.SceneAntialiasing
 * @see javafx.scene.paint.Color
 * @see javafx.scene.transform.Rotate
 * @see javafx.scene.transform.Translate
 * @see javafx.animation.KeyFrame
 * @see javafx.animation.KeyValue
 * @see javafx.animation.Timeline
 * @see javafx.util.Duration
 * @conditionalFeatures SCENE3D
 */
public class CubeApp extends Application {

    private Timeline animation;

    public Parent createContent() {
        Cube c = new Cube(1, Color.RED);
        c.rx.setAngle(45);
        c.ry.setAngle(45);
        Cube c2 = new Cube(1, Color.GREEN);
        c2.setTranslateX(2);
        c2.rx.setAngle(45);
        c2.ry.setAngle(45);

        Cube c3 = new Cube(1, Color.ORANGE);
        c3.setTranslateX(-2);
        c3.rx.setAngle(45);
        c3.ry.setAngle(45);

        animation = new Timeline();
        animation.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO,
                new KeyValue(c.ry.angleProperty(), 0d),
                new KeyValue(c2.rx.angleProperty(), 0d),
                new KeyValue(c3.rz.angleProperty(), 0d)),
                new KeyFrame(Duration.seconds(1),
                new KeyValue(c.ry.angleProperty(), 360d),
                new KeyValue(c2.rx.angleProperty(), 360d),
                new KeyValue(c3.rz.angleProperty(), 360d)));
        animation.setCycleCount(Timeline.INDEFINITE);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.getTransforms().add(new Translate(0, 0, -10));

        Group root = new Group();
        root.getChildren().addAll(c, c2, c3);

        SubScene subScene = new SubScene(root, 640, 480, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        return new Group(subScene);
    }

    public void play() {
        animation.play();
    }

    @Override
    public void stop() {
        animation.pause();
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
