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

package com.javafx.experiments.dukepad.compass.app;

import com.javafx.experiments.dukepad.core.BaseDukeApplication;
import com.javafx.experiments.dukepad.core.DukeApplication;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Compass App
 */
public class CompassApp extends BaseDukeApplication implements BundleActivator {
    private static final Image appIcon = new Image(CompassApp.class.getResource("/images/ico-compass.png").toExternalForm());
    private Stage stage;
    private CompassUI compassUI;
    private PerspectiveCamera camera;
    private final Rotate cameraXRotate = new Rotate(0,0,0,0,Rotate.X_AXIS);
    private final Rotate cameraYRotate = new Rotate(0,0,0,0,Rotate.Y_AXIS);
    private final Rotate cameraLookXRotate = new Rotate(-90,0,0,0,Rotate.X_AXIS);
    private final Rotate cameraLookZRotate = new Rotate(0,0,0,0,Rotate.Y_AXIS);
    private final Translate cameraPosition = new Translate(0,-20,0);

    /** Get name of application */
    @Override public String getName() {
        return "Compass";
    }

    /** Create icon instance */
    @Override public Node createHomeIcon() {
        return new ImageView(appIcon);
    }

    /** Create the UI, new UI chould be created each time and not held on to */
    @Override protected Node createUI() {
        return null;
//        // Rendering artifacts has been found while using depth buffer, that's why it was decided to start using
//        // a new Stage(window) to display the content, that is why this method returns an empty Node and a new Stage is
//        // being created in the overridden startApp method
//        final Node node = new HBox();
//        // This method is required to move new stage smoothly and synchronously with the application node
//        node.localToSceneTransformProperty().addListener(new ChangeListener<Transform>() {
//            @Override
//            public void changed(ObservableValue<? extends Transform> observableValue, Transform transform, Transform transform2) {
//                stage.setX(node.localToScene(0, 0).getX());
//            }
//        });
//        // This method is required to close the stage when application is closed
//        node.sceneProperty().addListener(new ChangeListener<Scene>() {
//            @Override
//            public void changed(ObservableValue<? extends Scene> observableValue, Scene scene, Scene scene2) {
//                if(scene2 == null) {
//                    stage.close();
//                    compassUI.exitMpu9150();
//                    switchI2C(false);
//                }
//            }
//        });
//        return node;
    }

    /** Called when app is loaded at platform startup */
    @Override public void start(BundleContext bundleContext) throws Exception {
        // Register application service
        bundleContext.registerService(DukeApplication.class,this,null);
    }

    /** Called when app is unloaded at platform shutdown */
    @Override public void stop(BundleContext bundleContext) throws Exception {}

//    @Override
//    public void startApp() {
//        super.startApp();
//        stage = new Stage();
//        stage.initStyle(StageStyle.TRANSPARENT);
//        if(switchI2C(true)) {
//            camera = new PerspectiveCamera(true);
//            camera.getTransforms().addAll(
//                    cameraXRotate,
//                    cameraYRotate,
//                    cameraPosition,
//                    cameraLookXRotate,
//                    cameraLookZRotate);
//            camera.setNearClip(0.1);
//            camera.setFarClip(100);
//
//            compassUI = new CompassUI();
//
//            final Scene scene = new Scene(compassUI, 1280, 800, true);
//            scene.setFill(null);
//            scene.setCamera(camera);
//            stage.setScene(scene);
//            stage.show();
//        }
//    }

    private boolean switchI2C(boolean enable) {
        boolean result = true;
        try {
            List<String> hipiCommandLine = new ArrayList<>();
            hipiCommandLine.add("hipi-i2c");
            hipiCommandLine.add("e");
            hipiCommandLine.add("0");
            hipiCommandLine.add(enable ? "1" : "0");
            ProcessBuilder hipiProcessBuilder = new ProcessBuilder(hipiCommandLine);
            hipiProcessBuilder.redirectErrorStream(true);
            System.out.println("[hipi] command to run: " + hipiCommandLine);
            Process hipiProcess = hipiProcessBuilder.start();
            hipiProcess.waitFor();
            System.out.println("[hipi] command exited with: " + hipiProcess.exitValue());
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            return result;
        }
    }
}
