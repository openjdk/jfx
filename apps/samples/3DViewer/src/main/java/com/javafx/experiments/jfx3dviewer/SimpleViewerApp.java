/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates.
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
package com.javafx.experiments.jfx3dviewer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import com.javafx.experiments.importers.Importer3D;

/**
 * JavaFX 3D Viewer Application
 */
public class SimpleViewerApp extends Application {
    private final Group root3D = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate cameraXRotate = new Rotate(-20,0,0,0,Rotate.X_AXIS);
    private final Rotate cameraYRotate = new Rotate(-20,0,0,0,Rotate.Y_AXIS);
    private final Rotate cameraLookXRotate = new Rotate(0,0,0,0,Rotate.X_AXIS);
    private final Rotate cameraLookZRotate = new Rotate(0,0,0,0,Rotate.Z_AXIS);
    private final Translate cameraPosition = new Translate(0,0,-7);
    private AutoScalingGroup autoScalingGroup = new AutoScalingGroup(2);

    @Override public void start(Stage stage) throws Exception {
        List<String> args = getParameters().getRaw();
        final Scene scene = new Scene(root3D,1920,1080,true);
        scene.setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);

        // CAMERA
        camera.getTransforms().addAll(
                cameraXRotate,
                cameraYRotate,
                cameraPosition,
                cameraLookXRotate,
                cameraLookZRotate);
        camera.setNearClip(0.1);
        camera.setFarClip(100);
        scene.setCamera(camera);
        root3D.getChildren().addAll(camera, autoScalingGroup);

        // LOAD DROP HERE MODEL
        try {
            Node content;
            if (args.isEmpty()) {
                content = Importer3D.load(ContentModel.class.getResource("drop-here.obj").toExternalForm());
            } else {
                content = Importer3D.load(new File(args.get(0)).toURI().toURL().toExternalForm());
            }
            autoScalingGroup.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(cameraYRotate.angleProperty(),0)),
                new KeyFrame(Duration.seconds(4), new KeyValue(cameraYRotate.angleProperty(),360))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        stage.setScene(scene);
        stage.show();

        // MEASURE FPS
        //TODO: RT-40270 - Public PerformanceTracker support should be added
//        Timeline fpsTimeline = new Timeline(new KeyFrame(Duration.seconds(2), t ->
//                System.out.println("fps = " + PerformanceTracker.getSceneTracker(scene).getInstantFPS())));
//        fpsTimeline.setCycleCount(Timeline.INDEFINITE);
//        fpsTimeline.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
