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
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.CubicCurve;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CubicCurveT3D extends Application {

    @Override public void start(Stage stage) {
        final double sceneWidth = 640;
        final double sceneHeight = 480;

        final CubicCurve cubiccurve = new CubicCurve();
        cubiccurve.setStartX(0);
        cubiccurve.setStartY(0);
        cubiccurve.setControlX1(150);
        cubiccurve.setControlY1(-300);
        cubiccurve.setControlX2(200);
        cubiccurve.setControlY2(300);
        cubiccurve.setEndX(300);
        cubiccurve.setEndY(0);
        cubiccurve.setStroke(Color.WHITE);
        cubiccurve.setStrokeWidth(5);
        cubiccurve.setFill(new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.LIME),
                new Stop(1, Color.GREEN)));
        cubiccurve.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    System.out.println("Mouse Clicked:" + e);
                }
            });
        cubiccurve.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    System.out.println("Mouse Entered");
                }
            });
        cubiccurve.setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    System.out.println("Mouse Exited");
                }
            });
        final Group group = new Group(cubiccurve);
        group.setRotate(30);
        group.setTranslateX((sceneWidth - 300) / 2);
        group.setTranslateY(sceneHeight / 2);
        group.setRotationAxis(Rotate.Y_AXIS);
        final Scene scene = new Scene(group, sceneWidth, sceneHeight);
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
