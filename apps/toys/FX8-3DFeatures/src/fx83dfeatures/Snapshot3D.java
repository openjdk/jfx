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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;

/**
 *
 * @author kcr
 */
public class Snapshot3D extends Application {
    private Sphere sphere;

    private Scene buildScene() {
        PointLight pointLight;
        PhongMaterial material;

        material = new PhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        material.setSpecularColor(null);
        sphere = new Sphere(150);
        sphere.setTranslateX(200);
        sphere.setTranslateY(200);
        sphere.setTranslateZ(10);
        sphere.setMaterial(material);

        sphere.setDrawMode(DrawMode.FILL);

        pointLight = new PointLight(Color.PALEGREEN);
        pointLight.setTranslateX(75);
        pointLight.setTranslateY(-50);
        pointLight.setTranslateZ(-200);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateX(400);
        camera.setTranslateY(200);
        camera.setTranslateZ(-750);
        camera.setFarClip(2000);

        Group cameraGroup = new Group(camera);
        Group root = new Group(sphere, pointLight, cameraGroup);

        Scene scene = new Scene(root, 800, 400, true);
        scene.setFill(Color.GRAY);
        scene.setCamera(camera);

        return scene;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Snapshot 3D");

        Scene scene = buildScene();
        primaryStage.setScene(scene);
        primaryStage.show();

        Group root = (Group)scene.getRoot();
        Image image = scene.snapshot(null);
        ImageView iv = new ImageView(image);
        iv.setLayoutX(400);
        root.getChildren().add(iv);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
