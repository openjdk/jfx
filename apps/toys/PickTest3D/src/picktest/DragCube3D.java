/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package picktest;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class DragCube3D extends Application {

    @Override public void start(Stage stage) {

        final PhongMaterial green = new PhongMaterial();
        green.setSpecularColor(Color.GREEN);
        green.setDiffuseColor(Color.LIGHTGREEN);

        final PhongMaterial red = new PhongMaterial();
        red.setSpecularColor(Color.ORANGE);
        red.setDiffuseColor(Color.RED);

        final Group root = new Group();
        root.setTranslateZ(-50);

        final Scene scene = new Scene(root, 800, 600, true);
        final PerspectiveCamera cam = new PerspectiveCamera();
        cam.setFieldOfView(50);
        scene.setCamera(cam);

        final Box plane = new Box(400, 400, 1);
        plane.setTranslateX(400);
        plane.setTranslateY(300);
        plane.setMaterial(green);
        plane.setRotationAxis(Rotate.X_AXIS);
        plane.setRotate(-70);
        root.getChildren().add(plane);

        final Group corners = new Group(
                corner(-200, -200),
                corner(-200,  200),
                corner( 200, -200),
                corner( 200,  200));
        corners.setTranslateX(400);
        corners.setTranslateY(250);
        corners.setRotationAxis(Rotate.X_AXIS);
        corners.setRotate(-70);
        root.getChildren().add(corners);

        final Box cube = new Box(50, 50, 50);
        cube.setMaterial(red);
        cube.setTranslateX(400);
        cube.setTranslateY(250);
        cube.setRotationAxis(Rotate.X_AXIS);
        cube.setRotate(-70);
        root.getChildren().add(cube);

        final Rectangle mousePlane = new Rectangle(800, 800, Color.TRANSPARENT);
        mousePlane.setTranslateY(-150);
        mousePlane.setMouseTransparent(true);
        mousePlane.setRotationAxis(Rotate.X_AXIS);
        mousePlane.setRotate(-70);
        mousePlane.setDepthTest(DepthTest.DISABLE);
        root.getChildren().add(mousePlane);

        cube.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                cube.setMouseTransparent(true);
                mousePlane.setMouseTransparent(false);
                cube.startFullDrag();
            }
        });

        cube.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                cube.setMouseTransparent(false);
                mousePlane.setMouseTransparent(true);
            }
        });

        mousePlane.setOnMouseDragOver(new EventHandler<MouseDragEvent>() {
            @Override public void handle(MouseDragEvent event) {
                Point3D coords = event.getPickResult().getIntersectedPoint();

                double x = coords.getX();
                if (x < 230) x = 230;
                if (x > 570) x = 570;

                double y = coords.getY();
                if (y < 230) y = 230;
                if (y > 570) y = 570;

                coords = mousePlane.localToParent(new Point3D(x, y, coords.getZ()));

                cube.setTranslateX(coords.getX());
                cube.setTranslateY(coords.getY());
                cube.setTranslateZ(coords.getZ());
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    private Group corner(double x, double y) {

        final PhongMaterial green = new PhongMaterial();
        green.setSpecularColor(Color.GREEN);
        green.setDiffuseColor(Color.LIGHTGREEN);

        Box boxX = new Box(50, 1, 50);
        boxX.setMaterial(green);
        boxX.setTranslateX(x < 0 ? 25 : -25);
        Box boxY = new Box(1, 50, 50);
        boxY.setMaterial(green);
        boxY.setTranslateY(y < 0 ? 25 : -25);

        Group g = new Group(boxX, boxY);
        g.setTranslateX(x);
        g.setTranslateY(y);
        return g;
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
