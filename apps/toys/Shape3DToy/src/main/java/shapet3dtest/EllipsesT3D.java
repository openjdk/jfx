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
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EllipsesT3D extends Application {

    @Override public void start(Stage stage) {
        final double sceneWidth = 640;
        final double sceneHeight = 480;

        final Ellipse ellipse = new Ellipse(sceneWidth / 2, sceneHeight / 2, 200, 100);
        ellipse.setFill(Color.BLUE);
        ellipse.setTranslateX(-100);
        ellipse.setTranslateY(-100);
        ellipse.setTranslateZ(-100);
        final Ellipse ellipse2 = new Ellipse(sceneWidth / 2, sceneHeight / 2, 200, 100);
        ellipse2.setFill(Color.GREEN);
        final Ellipse ellipse3 = new Ellipse(sceneWidth / 2, sceneHeight / 2, 200, 100);
        ellipse3.setFill(Color.RED);
        ellipse3.setTranslateX(100);
        ellipse3.setTranslateY(100);
        ellipse3.setTranslateZ(100);
        final Group group = new Group(ellipse, ellipse2, ellipse3);
        group.setRotationAxis(Rotate.Y_AXIS);
        final Scene scene = new Scene(group, 640, 480, true);
        scene.setFill(Color.BLACK);
        scene.setCamera(new PerspectiveCamera());
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            System.out.println("*************************************************************");
            System.out.println("*    WARNING: common conditional SCENE3D isn't supported    *");
            System.out.println("*************************************************************");
        }

        final RotateTransition tx = new RotateTransition(Duration.seconds(20), group);
        tx.setToAngle(360);
        tx.setCycleCount(RotateTransition.INDEFINITE);
        tx.setInterpolator(Interpolator.LINEAR);
        tx.play();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
