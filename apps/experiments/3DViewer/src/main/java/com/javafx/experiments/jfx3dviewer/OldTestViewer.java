/*
 * Copyright (c) 2010, 2013 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.jfx3dviewer;


// import testviewer.Frame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import com.javafx.experiments.exporters.fxml.FXMLExporter;
import com.javafx.experiments.importers.max.MaxLoader;
import com.javafx.experiments.importers.maya.MayaGroup;
import com.javafx.experiments.importers.maya.MayaImporter;
import com.javafx.experiments.importers.maya.Xform;
import com.javafx.experiments.importers.obj.ObjImporter;
import com.sun.javafx.geom.Vec3d;


/**
 *
 */
public class OldTestViewer extends Application {
    public static final String SESSION_PROPERTIES_FILENAME = "session.properties";
    public static final String PATH_PROPERTY = "file";
    
    final private PointLight pointLight = new PointLight();
    final private PointLight pointLight2 = new PointLight();
    final private PointLight pointLight3 = new PointLight();
    final private Sphere pointLight2Geo = new Sphere(0.1);
    final private Sphere pointLight3Geo = new Sphere(0.1);
    final PhongMaterial pointLight2Material = new PhongMaterial();
    final PhongMaterial pointLight3Material = new PhongMaterial();
        
    private Node loadedNode = null;
    private File loadedPath = null;
    private Xform vertexes;
    private BooleanProperty wireframe = new SimpleBooleanProperty(false);
    final PerspectiveCamera camera = new PerspectiveCamera(true);
    final Group root = new Group();
    final Group axisGroup = new Group();
    final Xform spheresGroup = new Xform();
    final Xform world = new Xform();
    final Xform cameraXform = new Xform();
    final Xform cameraXform2 = new Xform();
    final Xform cameraXform3 = new Xform();
    final double cameraDistance = 30;
    private Timeline timeline;
    boolean timelinePlaying = false;
    double ONE_FRAME = 1.0/24.0;
    double DELTA_MULTIPLIER = 200.0;
    double CONTROL_MULTIPLIER = 0.1;
    double SHIFT_MULTIPLIER = 0.1;
    double ALT_MULTIPLIER = 0.5;
    
    boolean enableSaveSession = true;
    
    double mousePosX;
    double mousePosY;
    double mouseOldX;
    double mouseOldY;
    double mouseDeltaX;
    double mouseDeltaY;

    // [*] root (Group)
    //     [*] world (Xform)
    //         [*] axisGroup
    //         [*] spheresGroup
    //     [*] pointLight
    //     [*] pointLight2
    //     [*] pointLight3
    //     [*] cameraXform
    //         [*] cameraXform2
    //             [*] cameraXform3
    //                 [*] camera

    private void buildCamera() {
        root.getChildren().add(cameraXform);
        cameraXform.getChildren().add(cameraXform2);
        cameraXform2.getChildren().add(cameraXform3);
        cameraXform3.getChildren().add(camera);
        cameraXform3.setRotateZ(180.0);

        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(-cameraDistance);
        cameraXform.ry.setAngle(180.0);
        cameraXform.rx.setAngle(20);
    }
    
    private void buildSpheres() {
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);

        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.DARKGREEN);
        greenMaterial.setSpecularColor(Color.GREEN);

        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.DARKBLUE);
        blueMaterial.setSpecularColor(Color.BLUE);
        /*
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.RED);
        redMaterial.setSpecularColor(Color.ORANGE);

        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.GREEN);
        greenMaterial.setSpecularColor(Color.YELLOW);

        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.BLUE);
        blueMaterial.setSpecularColor(Color.VIOLET);
*/
        
        // red
        final Box box1 = new Box(8, 8, 8);
        box1.setMaterial(redMaterial);
        final Box box2 = new Box();
        box2.setMaterial(redMaterial);

        box1.setTranslateX(-10);
        //box1.setTranslateY(10);

        //box1.setScaleX(0.10);
        //box1.setScaleY(0.04);
        //box1.setScaleZ(0.04);
        
        box2.setTranslateX(-12);
        //box2.setScaleX(0.04);
        //box2.setScaleY(0.10);
        //box2.setScaleZ(0.10);

        // green
        final Cylinder cylinder1 = new Cylinder(4, 8);
        cylinder1.setMaterial(greenMaterial);
        final Cylinder cylinder2 = new Cylinder();
        cylinder2.setMaterial(greenMaterial);

        cylinder1.setTranslateX(0);
        //cylinder1.setTranslateY(10);
        //cylinder1.setScaleX(0.04);
        //cylinder1.setScaleY(0.10);
        //cylinder1.setScaleZ(0.04);
        
        cylinder2.setTranslateY(-12);
        //cylinder2.setScaleX(0.10);
        //cylinder2.setScaleY(0.04);
        //cylinder2.setScaleZ(0.10);

        // blue
        final Sphere sphere1 = new Sphere(4.5);
        sphere1.setMaterial(blueMaterial);
        final Sphere sphere2 = new Sphere(4);
        sphere2.setMaterial(blueMaterial);

        sphere1.setTranslateX(10);
        //sphere1.setTranslateY(10);
        //sphere1.setScaleX(0.04);
        //sphere1.setScaleY(0.04);
        //sphere1.setScaleZ(0.10);
        
        sphere2.setTranslateZ(-12);
        //sphere2.setScaleX(0.10);
        //sphere2.setScaleY(0.10);
        //sphere2.setScaleZ(0.04);

        //spheresGroup.getChildren().addAll(box1, box2, cylinder1, cylinder2, sphere1, sphere2);
        spheresGroup.getChildren().addAll(box1, cylinder1, sphere1);
        spheresGroup.setRotateX(180.0);



        /*
        // red
        final Sphere redPos = new Sphere(4);
        redPos.setMaterial(redMaterial);
        final Sphere redNeg = new Sphere(4);
        redNeg.setMaterial(redMaterial);

        redPos.setTranslateX(12);
        redPos.setScaleX(0.10);
        redPos.setScaleY(0.04);
        redPos.setScaleZ(0.04);
        
        redNeg.setTranslateX(-12);
        redNeg.setScaleX(0.04);
        redNeg.setScaleY(0.10);
        redNeg.setScaleZ(0.10);

        // green
        final Sphere greenPos = new Sphere(4);
        greenPos.setMaterial(greenMaterial);
        final Sphere greenNeg = new Sphere(4);
        greenNeg.setMaterial(greenMaterial);

        greenPos.setTranslateY(12);
        greenPos.setScaleX(0.04);
        greenPos.setScaleY(0.10);
        greenPos.setScaleZ(0.04);
        
        greenNeg.setTranslateY(-12);
        greenNeg.setScaleX(0.10);
        greenNeg.setScaleY(0.04);
        greenNeg.setScaleZ(0.10);

        // blue
        final Sphere bluePos = new Sphere(4);
        bluePos.setMaterial(blueMaterial);
        final Sphere blueNeg = new Sphere(4);
        blueNeg.setMaterial(blueMaterial);

        bluePos.setTranslateZ(12);
        bluePos.setScaleX(0.04);
        bluePos.setScaleY(0.04);
        bluePos.setScaleZ(0.10);
        
        blueNeg.setTranslateZ(-12);
        blueNeg.setScaleX(0.10);
        blueNeg.setScaleY(0.10);
        blueNeg.setScaleZ(0.04);

        spheresGroup.getChildren().addAll(redPos, redNeg, greenPos, greenNeg, bluePos, blueNeg);
        */
        // Hide spheres by default for now
        spheresGroup.setVisible(false);

        world.getChildren().addAll(spheresGroup);
    }

    private void buildAxes() {
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);

        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.DARKGREEN);
        greenMaterial.setSpecularColor(Color.GREEN);

        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.DARKBLUE);
        blueMaterial.setSpecularColor(Color.BLUE);

        final Sphere red = new Sphere(50);
        red.setMaterial(redMaterial);

        final Sphere blue = new Sphere(50);
        blue.setMaterial(blueMaterial);

        final Box xAxis = new Box(24.0, 0.05, 0.05);
        final Box yAxis = new Box(0.05, 24.0, 0.05);
        final Box zAxis = new Box(0.05, 0.05, 24.0);
        
        xAxis.setMaterial(redMaterial);
        yAxis.setMaterial(greenMaterial);
        zAxis.setMaterial(blueMaterial);

        // blue.setTranslateZ(100);
        // red.setTranslateZ(-100);

        axisGroup.getChildren().addAll(xAxis, yAxis, zAxis);
        world.getChildren().addAll(axisGroup);
    }

    private void buildLights() {
        pointLight.setColor(Color.GREY);
        //pointLight.setColor(Color.RED);

        pointLight2.setColor(Color.LIGHTGREY);
        pointLight2.setTranslateX(-7.0);
        pointLight2.setTranslateY(7.0);
        pointLight2.setTranslateZ(7.0);
        
        pointLight3.setColor(Color.DARKGREY);
        pointLight3.setTranslateX(7.0);
        pointLight3.setTranslateY(7.0);
        pointLight3.setTranslateZ(-7.0);
        
        final PhongMaterial pointLight2Material = new PhongMaterial();
        pointLight2Material.setDiffuseColor(Color.LIGHTGREY);
        pointLight2Geo.setMaterial(pointLight2Material);

        final PhongMaterial pointLight3Material = new PhongMaterial();
        pointLight3Material.setDiffuseColor(Color.DARKGREY);
        pointLight3Geo.setMaterial(pointLight3Material);
        
        root.getChildren().add(pointLight);
        root.getChildren().add(pointLight2);
        root.getChildren().add(pointLight3);
        root.getChildren().add(pointLight2Geo);
        root.getChildren().add(pointLight3Geo);       
    }

    private void buildScene() {
        vertexes = new Xform();
        
        root.getChildren().add(world);
        world.getChildren().add(vertexes);
      
        world.setDepthTest(DepthTest.ENABLE);
        root.setDepthTest(DepthTest.ENABLE);
    }
    
    private void loadNewNode(File file) {
        if (!file.exists()) {
            return;
        }
        Node newNode = null;
        try {
            switch (getExtension(file)) {
                case "ma":
                    newNode = loadMayaFile(file);
                    break;
                case "ase":
                    newNode = loadMaxFile(file);
                    break;
                case "com/javafx/importers/obj":
                    newNode = loadObjFile(file);
                    break;
                case "fxml":
                    newNode = FXMLLoader.load(file.toURI().toURL());
                    break;
            }
        } catch (Exception ex) {
            Logger.getLogger(OldTestViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (newNode != null) {
            vertexes.getChildren().clear();
            world.getChildren().remove(loadedNode);
            loadedNode = newNode;
            loadedPath = file;
            applyRecursively(newNode, applyWireframe);
            applyRecursively(newNode, addToCollisionScene);
            world.getChildren().add(loadedNode);
        }
    }
    
    private Node loadMaxFile(File file) {
        MaxLoader loader = new MaxLoader();
        return loader.loadMaxFile(file);
    }
    
    private Node loadObjFile(File file) {
        try {
//            ObjImporter.setDebug(true);
            ObjImporter reader = new ObjImporter(file.getAbsolutePath());
            Group res = new Group();
            for (String key : reader.getMeshes()) {
                res.getChildren().add(reader.buildMeshView(key));
            }
            return res;
        } catch (IOException ex) {
            Logger.getLogger(OldTestViewer.class.getName()).log(Level.SEVERE, null, ex);
            return new Group();
        }
    }
    
    private String getExtension(File file) {
        String name = file.getName();

        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return file.getPath().toLowerCase();
        } else {
            return name.substring(dot + 1, name.length()).toLowerCase();
        }
    }
    
    private void onButtonLoad() {
        FileChooser chooser = new FileChooser();
        
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported files", "*.ma", "*.ase", "*.obj", "*.fxml"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        if (loadedPath != null) {
            chooser.setInitialDirectory(loadedPath.getAbsoluteFile().getParentFile());
        }
        chooser.setTitle("Select file to load");
        
        //probably not the best way to get window
        File newFile = chooser.showOpenDialog(world.getScene().getWindow());
        if (newFile != null) {
            loadNewNode(newFile);
        }
    }

    private MayaGroup loadMayaFile(File file) {
        MayaImporter mayaImporter = new MayaImporter();

        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException ex) {
            Logger.getLogger(OldTestViewer.class.getName()).log(Level.SEVERE, null, ex);
            return new MayaGroup();
        }
        mayaImporter.load(url.toString());
        timeline = mayaImporter.getTimeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        //timeline.setAutoReverse(true);
        //timeline.setRate(0.1);
        timeline.play();
        timelinePlaying = true;

        return mayaImporter.getRoot();
    }
    
    
    private void createStage2() 
    {
        final Stage stage = new Stage();
        BorderPane borderPane = new BorderPane();
        Scene scene2 = new Scene(borderPane, 310, 170, false);
        // Scene scene2 = new Scene(borderPane, 600, 200, true);  
        
        handleKeyboard(scene2, world);
        
        //stage.setTitle("Test Controls");
        scene2.setFill(Color.DARKGREY);
        stage.setScene(scene2);
        handleMouse(scene2, world);
        
        Label fovLabel;
        Label nearLabel;
        Label farLabel;
        Button resetFovButton;
        Button resetNearButton;
        Button resetFarButton;
                
        // light2
        Label light2Label;
        Label light2xyLabel;
        Label light2xzLabel;
        Label light2yzLabel;
        Button light2resetXYZButton;
        Button light2zeroXYZButton;
        Button light2show;
        Button light2hide;
        
        // light3
        Label light3Label;
        Label light3xyLabel;
        Label light3xzLabel;
        Label light3yzLabel;
        Button light3resetXYZButton;
        Button light3zeroXYZButton;
        Button light3show;
        Button light3hide;
        
        /*
        Button backButton;
        Button stopButton;
        Button playButton;
        Button pauseButton;
        Button forwardButton;
        final EventHandler<ActionEvent> backAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("backAction");
                Duration currentTime;
                currentTime = timeline.getCurrentTime();
                timeline.jumpTo(Duration.seconds(currentTime.toSeconds() - ONE_FRAME));
            }
        };
        final EventHandler<ActionEvent> stopAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("stopAction");
                timeline.stop();
            }
        };
        final EventHandler<ActionEvent> playAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("playAction");
                timeline.play();
            }
        };
        final EventHandler<ActionEvent> pauseAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("pauseAction");
                timeline.pause();
            }
        };
        final EventHandler<ActionEvent> forwardAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("forwardAction");                
                Duration currentTime;
                currentTime = timeline.getCurrentTime();
                timeline.jumpTo(Duration.seconds(currentTime.toSeconds() + ONE_FRAME));
            }
        };
        */

        //----------------------------------------
        // Light (Parented to Camera)

        final ColorPicker lightColorPicker = new ColorPicker(pointLight.getColor());
        lightColorPicker.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Color c = lightColorPicker.getValue();
                pointLight.setColor(c);
                //pointLightMaterial.setDiffuseColor(c);
                //pointLightGeo.setMaterial(pointLightMaterial);
            }
        });

        Color c = Color.WHITE;
        Scene scene = world.getScene();
        if (scene != null) {
            Paint fill = scene.getFill();
            if (fill instanceof Color) {
                c = (Color)fill;
            }
        }
        final ColorPicker backColorPicker = new ColorPicker(c);
        backColorPicker.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                Color c = backColorPicker.getValue();
                //probably not the best way to get scene
                if (world.getScene() != null) {
                    world.getScene().setFill(c);
                }
                
            }
        });

        final EventHandler<ActionEvent> resetFovAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("resetFovAction");                
                camera.setFieldOfView(30.0);
            }
        };
        final EventHandler<ActionEvent> resetNearAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("resetFovAction");                
                camera.setNearClip(0.1);
            }
        };
        final EventHandler<ActionEvent> resetFarAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("resetFovAction");                
                camera.setFarClip(10000.0);
            }
        };

        borderPane.setCenter(VBoxBuilder.create()
            .id("center")
            .spacing(0)
            .alignment(Pos.TOP_CENTER)
            .children(
                lightColorPicker,
                backColorPicker,
                fovLabel = LabelBuilder.create()
                    .id("fov-label")
                    .text("fov")
                    .build(),
                nearLabel = LabelBuilder.create()
                    .id("near-label")
                    .text("Near Clip")
                    .build(),
                farLabel = LabelBuilder.create()
                    .id("far-label")
                    .text("Far Clip")
                    .build(),
                resetFovButton = ButtonBuilder.create()
                    .id("resetFov-button")
                    .text("reset FOV")
                    .onAction(resetFovAction)
                    .build(),
                resetNearButton = ButtonBuilder.create()
                    .id("resetNear-button")
                    .text("reset near")
                    .onAction(resetNearAction)
                    .build(),
                resetFarButton = ButtonBuilder.create()
                    .id("resetFar-button")
                    .text("reset far")
                    .onAction(resetFarAction)
                    .build()
                )
            .build());
        
        handleMouse2(fovLabel);
        handleMouse2(nearLabel);
        handleMouse2(farLabel);
        
        //----------------------------------------
        // Light 2

        final EventHandler<ActionEvent> light2resetXYZAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light2resetXYZAction");                
                pointLight2.setTranslateX(-10.0);
                pointLight2.setTranslateY(10.0);
                pointLight2.setTranslateZ(0.0);
                pointLight2Geo.setTranslateX(pointLight2.getTranslateX());
                pointLight2Geo.setTranslateY(pointLight2.getTranslateY());
                pointLight2Geo.setTranslateZ(pointLight2.getTranslateZ());
            }
        };
        final EventHandler<ActionEvent> light2zeroXYZAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light2zeroXYZAction");                
                pointLight2.setTranslateX(0.0);
                pointLight2.setTranslateY(0.0);
                pointLight2.setTranslateZ(0.0);
                pointLight2Geo.setTranslateX(pointLight2.getTranslateX());
                pointLight2Geo.setTranslateY(pointLight2.getTranslateY());
                pointLight2Geo.setTranslateZ(pointLight2.getTranslateZ());
            }
        };
        final EventHandler<ActionEvent> light2showAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light2show");                
                pointLight2Geo.setVisible(true);
            }
        };
        final EventHandler<ActionEvent> light2hideAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light2hide");                
                pointLight2Geo.setVisible(false);
            }
        };
        
        final ColorPicker light2ColorPicker = new ColorPicker(pointLight2.getColor());
        light2ColorPicker.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Color c = light2ColorPicker.getValue();
                pointLight2.setColor(c);
                pointLight2Material.setDiffuseColor(c);
                pointLight2Geo.setMaterial(pointLight2Material);
            }
        });

        borderPane.setLeft(VBoxBuilder.create()
            .id("left")
            .spacing(0)
            .alignment(Pos.TOP_CENTER)
            .children(
                light2ColorPicker,
                light2Label = LabelBuilder.create()
                    .id("light2-label")
                    .text("light2")
                    .build(),
                light2xyLabel = LabelBuilder.create()
                    .id("light2xy-label")
                    .text("light2 XY")
                    .build(),
                light2xzLabel = LabelBuilder.create()
                    .id("light2xz-label")
                    .text("light2 XZ")
                    .build(),
                light2yzLabel = LabelBuilder.create()
                    .id("light2yz-label")
                    .text("light2 YZ")
                    .build(),
                light2resetXYZButton = ButtonBuilder.create()
                    .id("light2-resetXYZ")
                    .text("reset XYZ")
                    .onAction(light2resetXYZAction)
                    .build(),
                light2zeroXYZButton = ButtonBuilder.create()
                    .id("light2-zeroXYZ")
                    .text("zero XYZ")
                    .onAction(light2zeroXYZAction)
                    .build(),
                light2show = ButtonBuilder.create()
                    .id("light2-show")
                    .text("show")
                    .onAction(light2showAction)
                    .build(),
                light2hide = ButtonBuilder.create()
                    .id("light2-hide")
                    .text("hide")
                    .onAction(light2hideAction)
                    .build()
                )
            .build());

        handleMouse2(light2Label);
        handleMouse2(light2xyLabel);
        handleMouse2(light2xzLabel);
        handleMouse2(light2yzLabel);

        //----------------------------------------
        // Light 3

        final EventHandler<ActionEvent> light3resetXYZAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light3resetXYZAction");                
                pointLight3.setTranslateX(-10.0);
                pointLight3.setTranslateY(10.0);
                pointLight3.setTranslateZ(0.0);
                pointLight3Geo.setTranslateX(pointLight3.getTranslateX());
                pointLight3Geo.setTranslateY(pointLight3.getTranslateY());
                pointLight3Geo.setTranslateZ(pointLight3.getTranslateZ());
            }
        };
        final EventHandler<ActionEvent> light3zeroXYZAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light3zeroXYZAction");                
                pointLight3.setTranslateX(0.0);
                pointLight3.setTranslateY(0.0);
                pointLight3.setTranslateZ(0.0);
                pointLight3Geo.setTranslateX(pointLight3.getTranslateX());
                pointLight3Geo.setTranslateY(pointLight3.getTranslateY());
                pointLight3Geo.setTranslateZ(pointLight3.getTranslateZ());
            }
        };
        final EventHandler<ActionEvent> light3showAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light3show");                
                pointLight3Geo.setVisible(true);
            }
        };
        final EventHandler<ActionEvent> light3hideAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("light3hide");                
                pointLight3Geo.setVisible(false);
            }
        };
        
        final ColorPicker light3ColorPicker = new ColorPicker(pointLight3.getColor());
        light3ColorPicker.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Color c = light3ColorPicker.getValue();
                pointLight3.setColor(c);
                pointLight3Material.setDiffuseColor(c);
                pointLight3Geo.setMaterial(pointLight3Material);
            }
        });

        borderPane.setRight(VBoxBuilder.create()
            .id("right")
            .spacing(0)
            .alignment(Pos.TOP_CENTER)
            .children(
                light3ColorPicker,
                light3Label = LabelBuilder.create()
                    .id("light3-label")
                    .text("light3")
                    .build(),
                light3xyLabel = LabelBuilder.create()
                    .id("light3xy-label")
                    .text("light3 XY")
                    .build(),
                light3xzLabel = LabelBuilder.create()
                    .id("light3xz-label")
                    .text("light3 XZ")
                    .build(),
                light3yzLabel = LabelBuilder.create()
                    .id("light3yz-label")
                    .text("light3 YZ")
                    .build(),
                light3resetXYZButton = ButtonBuilder.create()
                    .id("light3-resetXYZ")
                    .text("reset XYZ")
                    .onAction(light3resetXYZAction)
                    .build(),
                light3zeroXYZButton = ButtonBuilder.create()
                    .id("light3-zeroXYZ")
                    .text("zero XYZ")
                    .onAction(light3zeroXYZAction)
                    .build(),
                light3show = ButtonBuilder.create()
                    .id("light3-show")
                    .text("show")
                    .onAction(light3showAction)
                    .build(),
                light3hide = ButtonBuilder.create()
                    .id("light3-hide")
                    .text("hide")
                    .onAction(light3hideAction)
                    .build()
                )
            .build());

        handleMouse2(light3Label);
        handleMouse2(light3xyLabel);
        handleMouse2(light3xzLabel);
        handleMouse2(light3yzLabel);



/*
        borderPane.setBottom(HBoxBuilder.create()
            .id("bottom")
            .spacing(0)
            .alignment(Pos.CENTER)
            .children(
                backButton = ButtonBuilder.create()
                    .id("back-button")
                    .text("<-")
                    .onAction(backAction)
                    .build(),
                stopButton = ButtonBuilder.create()
                    .id("stop-button")
                    .text("Stop")
                    .onAction(stopAction)
                    .build(),
                playButton = ButtonBuilder.create()
                    .id("play-button")
                    .text("Play")
                    .onAction(playAction)
                    .build(),
                pauseButton = ButtonBuilder.create()
                    .id("pause-button")
                    .text("Pause")
                    .onAction(pauseAction)
                    .build(),
                forwardButton = ButtonBuilder.create()
                    .id("forward-button")
                    .text("->")
                    .onAction(forwardAction)
                    .build()
                )
            .build());
            */
        
        stage.show();
    }
    
    private void handleMouse2(final Label label) {

        label.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                // System.out.println("________________________________");
                System.out.println("label.getId() = " + label.getId());
                // System.out.println("label.getText() = " + label.getText());
                // System.out.println("label.toString() = " + label.toString());
                
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseOldX = me.getSceneX();
                mouseOldY = me.getSceneY();
            }
        });
        label.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX); //*DELTA_MULTIPLIER;
                mouseDeltaY = (mousePosY - mouseOldY); //*DELTA_MULTIPLIER;
                double modifier = 1.0;
                double modifierFactor = 0.1;
                
                if (me.isControlDown()) {
                    modifier = 0.1;
                } 
                if (me.isShiftDown()) {
                    modifier = 10.0;
                }     
                /*
                    .id("fov-label")
                    .id("near-label")
                    .id("far-label")
*/
                if (label.getId().equalsIgnoreCase("fov-label")) {
                    if (me.isPrimaryButtonDown()) {
                        double fieldOfView = camera.getFieldOfView();
                        fieldOfView += mouseDeltaX*modifierFactor*modifier;
                        if (fieldOfView < 0.0001) { fieldOfView = 0.001; }
                        else if (fieldOfView > 179.99) { fieldOfView = 179.99; }
                        System.out.println("fieldOfView = " + fieldOfView);
                        camera.setFieldOfView(fieldOfView);
                    }
                }
                else if (label.getId().equalsIgnoreCase("near-label")) {
                    if (me.isPrimaryButtonDown()) {
                        double nearClip = camera.getNearClip();
                        nearClip += mouseDeltaX*modifierFactor*modifier;
                        if (nearClip < 0.00000000001) { nearClip = 0.00000000001; }
                        else if (nearClip > 10000.00) { nearClip = 10000.0; }
                        System.out.println("nearClip = " + nearClip);
                        camera.setNearClip(nearClip);
                    }
                }
                else if (label.getId().equalsIgnoreCase("far-label")) {
                    if (me.isPrimaryButtonDown()) {
                        double farClip = camera.getFarClip();
                        farClip += mouseDeltaX*modifierFactor*modifier*10.0;
                        if (farClip < 0.0001) { farClip = 0.001; }
                        else if (farClip > 100000000.00) { farClip = 1000000000.0; }
                        System.out.println("farClip = " + farClip);
                        camera.setFarClip(farClip);
                    }
                }

                else if (label.getId().equalsIgnoreCase("light2-label")) {
                    if (me.isPrimaryButtonDown()) {
                        pointLight2.setTranslateX(pointLight2.getTranslateX() + mouseDeltaX*modifierFactor*modifier);
                    }
                    else if (me.isMiddleButtonDown()) {
                        pointLight2.setTranslateY(pointLight2.getTranslateY() + mouseDeltaX*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        pointLight2.setTranslateZ(pointLight2.getTranslateZ() + mouseDeltaX*modifierFactor*modifier);
                    }
                    // System.out.println("light2 " + pointLight2.getTranslateX() + " " + pointLight2.getTranslateY() + " " + pointLight2.getTranslateZ());
                }
                else if (label.getId().equalsIgnoreCase("light2xy-label")) {
                    if (me.isPrimaryButtonDown() || me.isMiddleButtonDown()) {
                        pointLight2.setTranslateX(pointLight2.getTranslateX() + mouseDeltaX*modifierFactor*modifier);
                        pointLight2.setTranslateY(pointLight2.getTranslateY() - mouseDeltaY*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        pointLight2.setTranslateZ(pointLight2.getTranslateZ() + mouseDeltaX*modifierFactor*modifier);
                    }
                    // System.out.println("light2 " + pointLight2.getTranslateX() + " " + pointLight2.getTranslateY() + " " + pointLight2.getTranslateZ());
                }
                else if (label.getId().equalsIgnoreCase("light2xz-label")) {
                    if (me.isPrimaryButtonDown() || me.isMiddleButtonDown()) {
                        pointLight2.setTranslateX(pointLight2.getTranslateX() + mouseDeltaX*modifierFactor*modifier);
                        pointLight2.setTranslateZ(pointLight2.getTranslateZ() + mouseDeltaY*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        // pointLight2.setTranslateZ(pointLight2.getTranslateZ() + mouseDeltaX*0.1*modifier);
                    }
                    // System.out.println("light2 " + pointLight2.getTranslateX() + " " + pointLight2.getTranslateY() + " " + pointLight2.getTranslateZ());
                }
                else if (label.getId().equalsIgnoreCase("light2yz-label")) {
                    if (me.isPrimaryButtonDown() || me.isMiddleButtonDown()) {
                        pointLight2.setTranslateY(pointLight2.getTranslateY() - mouseDeltaY*modifierFactor*modifier);
                        pointLight2.setTranslateZ(pointLight2.getTranslateZ() + mouseDeltaX*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        // pointLight2.setTranslateZ(pointLight2.getTranslateZ() + mouseDeltaX*0.1*modifier);
                    }
                    // System.out.println("light2 " + pointLight2.getTranslateX() + " " + pointLight2.getTranslateY() + " " + pointLight2.getTranslateZ());
                }

                if (label.getId().equalsIgnoreCase("light3-label")) {
                    if (me.isPrimaryButtonDown()) {
                        pointLight3.setTranslateX(pointLight3.getTranslateX() + mouseDeltaX*modifierFactor*modifier);
                    }
                    else if (me.isMiddleButtonDown()) {
                        pointLight3.setTranslateY(pointLight3.getTranslateY() + mouseDeltaX*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        pointLight3.setTranslateZ(pointLight3.getTranslateZ() + mouseDeltaX*modifierFactor*modifier);
                    }
                    // System.out.println("light3 " + pointLight3.getTranslateX() + " " + pointLight3.getTranslateY() + " " + pointLight3.getTranslateZ());
                }
                else if (label.getId().equalsIgnoreCase("light3xy-label")) {
                    if (me.isPrimaryButtonDown() || me.isMiddleButtonDown()) {
                        pointLight3.setTranslateX(pointLight3.getTranslateX() + mouseDeltaX*modifierFactor*modifier);
                        pointLight3.setTranslateY(pointLight3.getTranslateY() - mouseDeltaY*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        pointLight3.setTranslateZ(pointLight3.getTranslateZ() + mouseDeltaX*modifierFactor*modifier);
                    }
                    // System.out.println("light3 " + pointLight3.getTranslateX() + " " + pointLight3.getTranslateY() + " " + pointLight3.getTranslateZ());
                }
                else if (label.getId().equalsIgnoreCase("light3xz-label")) {
                    if (me.isPrimaryButtonDown() || me.isMiddleButtonDown()) {
                        pointLight3.setTranslateX(pointLight3.getTranslateX() + mouseDeltaX*modifierFactor*modifier);
                        pointLight3.setTranslateZ(pointLight3.getTranslateZ() + mouseDeltaY*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        // pointLight3.setTranslateZ(pointLight3.getTranslateZ() + mouseDeltaX*0.1*modifier);
                    }
                    // System.out.println("light3 " + pointLight3.getTranslateX() + " " + pointLight3.getTranslateY() + " " + pointLight3.getTranslateZ());
                }
                else if (label.getId().equalsIgnoreCase("light3yz-label")) {
                    if (me.isPrimaryButtonDown() || me.isMiddleButtonDown()) {
                        pointLight3.setTranslateY(pointLight3.getTranslateY() - mouseDeltaY*modifierFactor*modifier);
                        pointLight3.setTranslateZ(pointLight3.getTranslateZ() + mouseDeltaX*modifierFactor*modifier);
                    }
                    else if (me.isSecondaryButtonDown()) {
                        // pointLight3.setTranslateZ(pointLight3.getTranslateZ() + mouseDeltaX*0.1*modifier);
                    }
                    // System.out.println("light3 " + pointLight3.getTranslateX() + " " + pointLight3.getTranslateY() + " " + pointLight3.getTranslateZ());
                }

                pointLight2Geo.setTranslateX(pointLight2.getTranslateX());
                pointLight2Geo.setTranslateY(pointLight2.getTranslateY());
                pointLight2Geo.setTranslateZ(pointLight2.getTranslateZ());

                pointLight3Geo.setTranslateX(pointLight3.getTranslateX());
                pointLight3Geo.setTranslateY(pointLight3.getTranslateY());
                pointLight3Geo.setTranslateZ(pointLight3.getTranslateZ());
            }
            

        });
    }
    
 
    
    //=============================================================================
    // start
    //=============================================================================
    @Override
    public void start(Stage primaryStage) {
        System.out.println("new File().getAbsolutePath() = " + new File("").getAbsolutePath());

        buildScene();
        buildLights();
        buildCamera();
        buildSpheres();
        buildAxes();
        
        Scene scene = new Scene(root, 1024, 768, true);
        scene.setFill(Color.GREY);
        handleKeyboard(scene, world);
        handleMouse(scene, world);
        
        primaryStage.setTitle("Test (Maya) Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                saveSession();
            }
        });
        
        loadSession();
        scene.setCamera(camera);
                
        // ScenicView.show(scene);
        
        // Eventually move the below to an AnimationTimer
        updateLight();
    }
    
    // Eventually move the below to an AnimationTimer
    // This causes the light to be updated properly
    // Currently there is a bug that doesn't update the lights that have parent transforms
    private void updateLight() {
        Transform transform = camera.getLocalToSceneTransform();
        double x = transform.getTx();
        double y = transform.getTy();
        double z = transform.getTz();
        // System.out.println("x = " + x + ", y = " + y + ", z = " + z);
        pointLight.setTranslateX(x);
        pointLight.setTranslateY(y);
        pointLight.setTranslateZ(z);
    }

    private void handleMouse(Scene scene, final Node root) {
        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseOldX = me.getSceneX();
                mouseOldY = me.getSceneY();
            }
        });
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent me) {
                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX); //*DELTA_MULTIPLIER;
                mouseDeltaY = (mousePosY - mouseOldY); //*DELTA_MULTIPLIER;
                
                double modifier = 1.0;
                double modifierFactor = 0.1;
                
                if (me.isControlDown()) {
                    modifier = 0.1;
                } 
                if (me.isShiftDown()) {
                    modifier = 10.0;
                }     
                if (me.isAltDown() && me.isPrimaryButtonDown()) {
//                    System.out.println("(MouseEvent.getX() = " + me.getSceneX() + ", MouseEvent.getY() = " + me.getSceneY() + ") (mouseDeltaX = " + mouseDeltaX + ", mouseDeltaY = " + mouseDeltaY + ")");
                    cameraXform.ry.setAngle(cameraXform.ry.getAngle() - mouseDeltaX*modifierFactor*modifier*2.0);  // +
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() + mouseDeltaY*modifierFactor*modifier*2.0);  // -
                    // Eventually move the below to an AnimationTimer
                    updateLight();
                }
                else if (me.isAltDown() && me.isSecondaryButtonDown()) {
                    double z = camera.getTranslateZ();
                    double newZ = z + mouseDeltaX*modifierFactor*modifier;
                    camera.setTranslateZ(newZ);
                    // Eventually move the below to an AnimationTimer
                    updateLight();
                }
                else if (me.isAltDown() && me.isMiddleButtonDown()) {
                    cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX*modifierFactor*modifier*0.3);  // -
                    cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY*modifierFactor*modifier*0.3);  // -
                    // System.out.println("cameraXform2.t : " + cameraXform2.t.getX() + ", " + cameraXform2.t.getY());
                    // Eventually move the below to an AnimationTimer
                    updateLight();
                }
            }
        });
    }
    
    private void handleKeyboard(Scene scene, final Node root) {
        //System.out.println("--> handleKeyboard");
        final boolean moveCamera = true;
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                Duration currentTime;
                //System.out.println("--> handleKeyboard>handle");
                switch (event.getCode()) {
                    case F:
                        if (event.isControlDown()) {
                            onButtonSave();
                        }
                        break;
                    case O:
                        if (event.isControlDown()) {
                            onButtonLoad();
                        }
                        break;
                    case Z:
                        if (event.isShiftDown()) {
                            cameraXform.ry.setAngle(0.0);
                            cameraXform.rx.setAngle(0.0);
                            camera.setTranslateZ(-300.0);
                        }   
                        cameraXform2.t.setX(0.0);
                        cameraXform2.t.setY(0.0);
                        break;
                    case X:
                        if (event.isControlDown()) {
                            if (axisGroup.isVisible()) {
                                axisGroup.setVisible(false);
                            }
                            else {
                                axisGroup.setVisible(true);
                            }
                        }   
                        break;
                    case W:
                        if (event.isControlDown()) {
                            if (loadedNode.isVisible()) {
                                loadedNode.setVisible(false);
                            }
                            else {
                                loadedNode.setVisible(true);
                            }
                        }   
                        break;
                    case S:
                        if (event.isControlDown()) {
                            if (spheresGroup.isVisible()) {
                                spheresGroup.setVisible(false);
                            }
                            else {
                                spheresGroup.setVisible(true);
                            }
                        }   
                        break;
                    case U:
                        createStage2();
                        break;
                    case SPACE:
                        if (timelinePlaying) {
                            timeline.pause();
                            timelinePlaying = false;
                        }
                        else {
                            timeline.play();
                            timelinePlaying = true;
                        }
                        break;
                        
                        /*
                         *     double CONTROL_MULTIPLIER = 0.1;
    double SHIFT_MULTIPLIER = 0.1;
    double ALT_MULTIPLIER = 0.5;
                         */
                    case UP:
                        if (event.isControlDown() && event.isShiftDown()) {
                            cameraXform2.t.setY(cameraXform2.t.getY() - 10.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }  
                        else if (event.isAltDown() && event.isShiftDown()) {
                            cameraXform.rx.setAngle(cameraXform.rx.getAngle() - 10.0*ALT_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isControlDown()) {
                            cameraXform2.t.setY(cameraXform2.t.getY() - 1.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isAltDown()) {
                            cameraXform.rx.setAngle(cameraXform.rx.getAngle() - 2.0*ALT_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isShiftDown()) {
                            double z = camera.getTranslateZ();
                            double newZ = z + 5.0*SHIFT_MULTIPLIER;
                            camera.setTranslateZ(newZ);
                            updateLight();
                        }
                        break;
                    case DOWN:
                        if (event.isControlDown() && event.isShiftDown()) {
                            cameraXform2.t.setY(cameraXform2.t.getY() + 10.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }  
                        else if (event.isAltDown() && event.isShiftDown()) {
                            cameraXform.rx.setAngle(cameraXform.rx.getAngle() + 10.0*ALT_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isControlDown()) {
                            cameraXform2.t.setY(cameraXform2.t.getY() + 1.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isAltDown()) {
                            cameraXform.rx.setAngle(cameraXform.rx.getAngle() + 2.0*ALT_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isShiftDown()) {
                            double z = camera.getTranslateZ();
                            double newZ = z - 5.0*SHIFT_MULTIPLIER;
                            camera.setTranslateZ(newZ);
                            updateLight();
                        }
                        break;
                    case RIGHT:
                        if (event.isControlDown() && event.isShiftDown()) {
                            cameraXform2.t.setX(cameraXform2.t.getX() + 10.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }  
                        else if (event.isAltDown() && event.isShiftDown()) {
                            cameraXform.ry.setAngle(cameraXform.ry.getAngle() - 10.0*ALT_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isControlDown()) {
                            cameraXform2.t.setX(cameraXform2.t.getX() + 1.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isShiftDown()) {
                            currentTime = timeline.getCurrentTime();
                            timeline.jumpTo(Frame.frame(Math.round(Frame.toFrame(currentTime)/10.0)*10 + 10));
                            // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() + ONE_FRAME));
                        }
                        else if (event.isAltDown()) {
                            cameraXform.ry.setAngle(cameraXform.ry.getAngle() - 2.0*ALT_MULTIPLIER);  
                            updateLight();
                        }
                        else {
                            currentTime = timeline.getCurrentTime();
                            timeline.jumpTo(Frame.frame(Frame.toFrame(currentTime) + 1));
                            // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() + ONE_FRAME));
                        }
                        break;
                    case LEFT:
                        if (event.isControlDown() && event.isShiftDown()) {
                            cameraXform2.t.setX(cameraXform2.t.getX() - 10.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }  
                        else if (event.isAltDown() && event.isShiftDown()) {
                            cameraXform.ry.setAngle(cameraXform.ry.getAngle() + 10.0*ALT_MULTIPLIER);  // -
                            updateLight();
                        }
                        else if (event.isControlDown()) {
                            cameraXform2.t.setX(cameraXform2.t.getX() - 1.0*CONTROL_MULTIPLIER);  
                            updateLight();
                        }
                        else if (event.isShiftDown()) {
                            currentTime = timeline.getCurrentTime();
                            timeline.jumpTo(Frame.frame(Math.round(Frame.toFrame(currentTime)/10.0)*10 - 10));
                            // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() - ONE_FRAME));
                        }
                        else if (event.isAltDown()) {
                            cameraXform.ry.setAngle(cameraXform.ry.getAngle() + 2.0*ALT_MULTIPLIER);  // -
                            updateLight();
                        }
                        else {
                            currentTime = timeline.getCurrentTime();
                            timeline.jumpTo(Frame.frame(Frame.toFrame(currentTime) - 1));
                            // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() - ONE_FRAME));
                        }
                        break;
                }
                //System.out.println(cameraXform.getTranslateX() + ", " + cameraXform.getTranslateY() + ", " + cameraXform.getTranslateZ());
            }

        });
    }
    
    private void onButtonSave() {
        new FXMLExporter("output.fxml").export(loadedNode);
    }

    private void loadSession() {
        Reader reader = null;
        try {
            Properties props = new Properties();
            reader = new FileReader(SESSION_PROPERTIES_FILENAME);
            props.load(reader);
            String path = props.getProperty(PATH_PROPERTY);
            if (path != null) {
                loadNewNode(new File(path));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OldTestViewer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OldTestViewer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(OldTestViewer.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void saveSession() {
        if (enableSaveSession) {
            Properties props = new Properties();
            if (loadedPath != null) {
                props.setProperty(PATH_PROPERTY, loadedPath.getAbsolutePath());
            }
            try {
                props.store(new FileWriter(SESSION_PROPERTIES_FILENAME), "Jfx3dViewerApp session properties");
            } catch (IOException ex) {
                Logger.getLogger(OldTestViewer.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
    }
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.setProperty("prism.dirtyopts", "false");
        //System.setProperty("prism.dirtyopts", "false");
        launch(args);
    }

    private void applyRecursively(Node node, Action a) {
        if (node instanceof MeshView) {
            a.apply((MeshView) node);
        }
        if (node instanceof Parent) {
            for (Node n : ((Parent) node).getChildrenUnmodifiable()) {
                applyRecursively(n, a);
            }
        }
    }

    private interface Action {
        void apply(MeshView mv);
    }
    
    private Action applyWireframe = new Action() {
        @Override
        public void apply(MeshView mv) {
            mv.drawModeProperty().bind(Bindings.when(wireframe).then(DrawMode.LINE).otherwise(DrawMode.FILL));
        }
    };

    private Action addToCollisionScene = new Action() {
        private Vec3d pointSize;
        private double size;
        private PhongMaterial material = new PhongMaterial(Color.RED);

        @Override
        public void apply(MeshView mv) {
        }
    };
    
}
