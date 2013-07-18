/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package shapet3dtest;

import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CircleT3D extends Application {

    @Override public void start(Stage stage) {
        final double sceneWidth = 640;
        final double sceneHeight = 480;

        final Circle circle = new Circle(sceneWidth / 2, sceneHeight / 2, 120);
        circle.setFill(new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.LIME),
                new Stop(1, Color.GREEN)));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(5);
        circle.setRotationAxis(Rotate.Y_AXIS);

        final Scene scene = new Scene(new Group(circle), sceneWidth, sceneHeight);
        scene.setCamera(new PerspectiveCamera());
        scene.setFill(Color.BLACK);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            System.out.println("*************************************************************");
            System.out.println("*    WARNING: common conditional SCENE3D isn't supported    *");
            System.out.println("*************************************************************");
        }

        final RotateTransition tx = new RotateTransition(Duration.seconds(2), circle);
        tx.setToAngle(360);
        tx.setCycleCount(RotateTransition.INDEFINITE);
        tx.setInterpolator(Interpolator.LINEAR);

        PauseTransition ptx = new PauseTransition(Duration.seconds(5));
        SequentialTransition stx = new SequentialTransition(ptx, tx);
        stx.play();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
