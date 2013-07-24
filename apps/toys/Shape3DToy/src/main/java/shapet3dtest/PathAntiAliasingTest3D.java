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

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.VLineTo;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class PathAntiAliasingTest3D extends Application {
    @Override public void start(Stage stage) {
        stage.setTitle("Path Anti-Aliasing Test");
        Scene scene = new Scene(new Group(), 800.0f, 800.0f);
        scene.setCamera(new PerspectiveCamera());
        scene.setFill(Color.BEIGE);
        Path path = new Path();
        path.setRotate(40.0F);
        path.setRotationAxis(Rotate.Y_AXIS);
        path.setStroke(Color.RED);
        path.setStrokeWidth(8.0F);
        path.getElements().clear();
        path.getElements().addAll(new MoveTo(100.0F, 600.0F),
                                  new LineTo(100.0F, 550.0F),
                                  new CubicCurveTo(100.0F, 450.0F, 600.0F, 600.0F, 600.0F, 300.0F),
                                  new VLineTo(150.0F),
                                  new CubicCurveTo(600.0F, 40.0F, 700.0F, 80.0F, 700.0F, 200.0F),
                                  new VLineTo(450.0F),
                                  new QuadCurveTo(700.0F, 650.0F, 600.0F, 650.0F),
                                  new HLineTo(150.0F),
                                  new QuadCurveTo(100.0F, 650.0F, 100.0F, 600.0F),
                                  new ClosePath());
        ((Group)scene.getRoot()).getChildren().addAll(path);
        stage.setScene(scene);
        stage.sizeToScene();
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            System.out.println("*************************************************************");
            System.out.println("*    WARNING: common conditional SCENE3D isn\'t supported    *");
            System.out.println("*************************************************************");
        }
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
