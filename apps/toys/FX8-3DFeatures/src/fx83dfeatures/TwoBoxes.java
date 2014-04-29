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
package fx83dfeatures;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class TwoBoxes extends Application {

    double anchorX, anchorY, anchorAngle;

    private PerspectiveCamera addCamera(Scene scene) {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera(false);
        scene.setCamera(perspectiveCamera);
        return perspectiveCamera;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("javafx.runtime.version: " + System.getProperties().get("javafx.runtime.version"));
        primaryStage.setTitle("2 Boxes");

        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setSpecularColor(Color.ORANGE);
        redMaterial.setDiffuseColor(Color.RED);

        final Box box1 = new Box(400, 400, 400);
        box1.setMaterial(redMaterial);

        final Box box2 = new Box(400, 400, 400);

        box2.setMaterial(redMaterial);

        box2.setTranslateX(250);
        box2.setTranslateY(250);
        box2.setTranslateZ(50);
        box1.setTranslateX(250);
        box1.setTranslateY(250);
        box1.setTranslateZ(450);

        final Group parent = new Group(box1, box2);
        parent.setTranslateZ(500);
        parent.setRotationAxis(Rotate.Y_AXIS);


        final Group root = new Group(parent);

        final Scene scene = new Scene(root, 500, 500, true);

        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngle = parent.getRotate();
        });

        scene.setOnMouseDragged(event -> parent.setRotate(anchorAngle + anchorX - event.getSceneX()));

        PointLight pointLight = new PointLight(Color.ANTIQUEWHITE);
        pointLight.setTranslateX(15);
        pointLight.setTranslateY(-10);
        pointLight.setTranslateZ(-100);

        root.getChildren().add(pointLight);

        addCamera(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
