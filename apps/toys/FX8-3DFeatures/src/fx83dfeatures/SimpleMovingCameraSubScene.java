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

import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SimpleMovingCameraSubScene extends Application {

    PointLight pointLight;
    Sphere sphere;
    PhongMaterial material;
    PerspectiveCamera camera;
    PerspectiveCamera subSceneCamera;
    Group cameraGroup;
    TranslateTransition transTrans;
    double fovValue;
    double rotateCamera = 0.0;
    double translateCamera = 0.0;
    
    private SubScene buildSubScene() {
        PhongMaterial ssMaterial = new PhongMaterial();
        ssMaterial.setDiffuseColor(Color.SILVER);
        ssMaterial.setSpecularColor(Color.rgb(30, 30, 30));
        Sphere ssSphere = new Sphere(300);
        ssSphere.setTranslateX(200);
        ssSphere.setTranslateY(100);
        ssSphere.setTranslateZ(1000);
        ssSphere.setMaterial(ssMaterial);

        ssSphere.setDrawMode(DrawMode.FILL);
        
        subSceneCamera = new PerspectiveCamera();

        Group root = new Group(ssSphere);

        SubScene subScene = new SubScene(root, 400, 200, true, true);
        subScene.setCamera(subSceneCamera);
        subScene.setFill(Color.BLACK);
        return subScene;
    }

    private Scene buildScene() {
        material = new PhongMaterial();
        material.setDiffuseColor(Color.GOLD);
        material.setSpecularColor(Color.rgb(30, 30, 30));
        sphere = new Sphere(300);
        sphere.setTranslateX(400);
        sphere.setTranslateY(400);
        sphere.setTranslateZ(20);
        sphere.setMaterial(material);

        sphere.setDrawMode(DrawMode.FILL);

        pointLight = new PointLight(Color.ANTIQUEWHITE);
        pointLight.setTranslateX(150);
        pointLight.setTranslateY(-100);
        pointLight.setTranslateZ(-1000);
        
        camera = createCamera();
        cameraGroup = new Group(camera);

        Group root = new Group(buildSubScene(), sphere, pointLight, cameraGroup);

        Scene scene = new Scene(root, 800, 800, true);
        scene.setFill(Color.GRAY);
        scene.setCamera(camera);
        System.err.println("Camera FOV = " + (fovValue = camera.getFieldOfView()));
        
        transTrans = new TranslateTransition(Duration.seconds(5), cameraGroup);
        transTrans.setAutoReverse(true);
        transTrans.setCycleCount(Timeline.INDEFINITE);
        transTrans.setByZ(-400);

        scene.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                switch (e.getCharacter()) {
                    case "[":
                        fovValue -= 2.0;
                        if (fovValue < 10.0) {
                            fovValue = 10.0;
                        }
                        subSceneCamera.setFieldOfView(fovValue);
                        break;
                    case "]":
                        fovValue += 2.0;
                        if (fovValue > 60.0) {
                            fovValue = 60.0;
                        }
                        subSceneCamera.setFieldOfView(fovValue);
                        break;
                    case "r":
                        rotateCamera += 5.0;
                        if (rotateCamera > 360.0) {
                            rotateCamera = 0.0;
                        }
                        subSceneCamera.setRotate(rotateCamera);
                        break;
                    case "t":
                        if (transTrans.getStatus() == Timeline.Status.RUNNING) {
                            transTrans.pause();
                        } else {
                            transTrans.play();
                        }
                        break;
                }
            }
        });
        return scene;
    }

    private PerspectiveCamera createCamera() {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera(true);
        perspectiveCamera.setTranslateX(400);
        perspectiveCamera.setTranslateY(400);
        perspectiveCamera.setTranslateZ(-1500);
        perspectiveCamera.setFarClip(2000);
        return perspectiveCamera;
    }

    @Override
    public void start(Stage primaryStage) {
        Scene scene = buildScene();
        primaryStage.setTitle("SimpleMovingCamera");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
