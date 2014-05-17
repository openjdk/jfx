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
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PathT3D extends Application {
    @Override public void start(Stage stage) {
        Scene scene = new Scene(new Group(), 400.0f, 300.0f);
        scene.setCamera(new PerspectiveCamera());
        Group group = new Group();
        group.setRotate(30.0F);
        group.setRotationAxis(Rotate.Y_AXIS);
        Path path = new Path();
        ArcTo arcto = new ArcTo();
        arcto.setX(10.0F);
        arcto.setY(50.0F);
        arcto.setRadiusX(100.0F);
        arcto.setRadiusY(100.0F);
        arcto.setSweepFlag(true);
        path.getElements().clear();
        path.getElements().addAll(new MoveTo(10.0F, 50.0F), new HLineTo(70.0F), new QuadCurveTo(100.0F, 0.0F, 120.0F, 60.0F), new LineTo(175.0F, 55.0F), arcto);
        path.setFill(Color.BLUE);
        path.setOnMouseClicked(e -> System.out.println("Mouse Clicked:" + e));
        path.setOnMouseEntered(e -> System.out.println("Mouse Entered"));
        path.setOnMouseExited(e -> System.out.println("Mouse Exited"));
        group.getChildren().addAll(path);
        ((Group)scene.getRoot()).getChildren().addAll(group);
        stage.setScene(scene);
        stage.sizeToScene();
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            System.out.println("*************************************************************");
            System.out.println("*    WARNING: common conditional SCENE3D isn\'t supported    *");
            System.out.println("*************************************************************");
        }
        stage.show();

        RotateTransition tx = new RotateTransition(Duration.seconds(20), group);
        tx.setToAngle(360);
        tx.setCycleCount(RotateTransition.INDEFINITE);
        tx.setInterpolator(Interpolator.LINEAR);
        tx.play();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
