/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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

import javafx.animation.Timeline;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import com.javafx.experiments.shape3d.PolygonMeshView;
import com.javafx.experiments.shape3d.SubdivisionMesh;

import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.util.Duration;

/**
 * 3D Content Model for Viewer App. Contains the 3D scene and everything related to it: light, cameras etc.
 */
public class ContentModel {
    private final SimpleObjectProperty<SubScene> subScene = new SimpleObjectProperty<>();
    private final Group root3D = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate cameraXRotate = new Rotate(-20,0,0,0,Rotate.X_AXIS);
    private final Rotate cameraYRotate = new Rotate(-20,0,0,0,Rotate.Y_AXIS);
    private final Rotate cameraLookXRotate = new Rotate(0,0,0,0,Rotate.X_AXIS);
    private final Rotate cameraLookZRotate = new Rotate(0,0,0,0,Rotate.Z_AXIS);
    private final Translate cameraPosition = new Translate(0,0,0);
    private final Xform cameraXform = new Xform();
    private final Xform cameraXform2 = new Xform();
    private final Xform cameraXform3 = new Xform();
    private final double cameraDistance = 200;
    private double dragStartX, dragStartY, dragStartRotateX, dragStartRotateY;
    private ObjectProperty<Node> content = new SimpleObjectProperty<>();
    private AutoScalingGroup autoScalingGroup = new AutoScalingGroup(2);
    private Box xAxis, yAxis, zAxis;
    private Sphere xSphere, ySphere, zSphere;
    private AmbientLight ambientLight = new AmbientLight(Color.DARKGREY);
    private PointLight light1 = new PointLight(Color.WHITE);
    private PointLight light2 = new PointLight(Color.ANTIQUEWHITE);
    private PointLight light3 = new PointLight(Color.ALICEBLUE);
    private final SimpleObjectProperty<Timeline> timeline = new SimpleObjectProperty<>();
    public Timeline getTimeline() { return timeline.get(); }
    public SimpleObjectProperty<Timeline> timelineProperty() { return timeline; }
    public void setTimeline(Timeline timeline) { this.timeline.set(timeline); }
    private SimpleBooleanProperty ambientLightEnabled = new SimpleBooleanProperty(false){
        @Override protected void invalidated() {
            if (get()) {
                root3D.getChildren().add(ambientLight);
            } else {
                root3D.getChildren().remove(ambientLight);
            }
        }
    };
    private SimpleBooleanProperty light1Enabled = new SimpleBooleanProperty(false){
        @Override protected void invalidated() {
            if (get()) {
                root3D.getChildren().add(light1);
            } else {
                root3D.getChildren().remove(light1);
            }
        }
    };
    private SimpleBooleanProperty light2Enabled = new SimpleBooleanProperty(false){
        @Override protected void invalidated() {
            if (get()) {
                root3D.getChildren().add(light2);
            } else {
                root3D.getChildren().remove(light2);
            }
        }
    };
    private SimpleBooleanProperty light3Enabled = new SimpleBooleanProperty(false){
        @Override protected void invalidated() {
            if (get()) {
                root3D.getChildren().add(light3);
            } else {
                root3D.getChildren().remove(light3);
            }
        }
    };
    private SimpleBooleanProperty showAxis = new SimpleBooleanProperty(false){
        @Override protected void invalidated() {
            if (get()) {
                if (xAxis == null) createAxes();
                autoScalingGroup.getChildren().addAll(xAxis, yAxis, zAxis);
                autoScalingGroup.getChildren().addAll(xSphere, ySphere, zSphere);
                //root3D.getChildren().addAll(xAxis, yAxis, zAxis);
                //root3D.getChildren().addAll(xSphere, ySphere, zSphere);
            } else if (xAxis != null) {
                autoScalingGroup.getChildren().removeAll(xAxis, yAxis, zAxis);
                autoScalingGroup.getChildren().removeAll(xSphere, ySphere, zSphere);
                //root3D.getChildren().removeAll(xAxis, yAxis, zAxis);
                //root3D.getChildren().removeAll(xSphere, ySphere, zSphere);
            }
        }
    };
    private Rotate yUpRotate = new Rotate(0,0,0,0,Rotate.X_AXIS);
    private SimpleBooleanProperty yUp = new SimpleBooleanProperty(false){
        @Override protected void invalidated() {
            if (get()) {
                yUpRotate.setAngle(180);
                //cameraPosition.setZ(cameraDistance);
                // camera.setTranslateZ(cameraDistance);
            } else {
                yUpRotate.setAngle(0);
                //cameraPosition.setZ(-cameraDistance);
                // camera.setTranslateZ(-cameraDistance);
            }
        }
    };
    private SimpleBooleanProperty msaa = new SimpleBooleanProperty(){
        @Override protected void invalidated() {
            rebuildSubScene();
        }
    };
    private boolean wireframe = false;
    private int subdivisionLevel = 0;
    private SubdivisionMesh.BoundaryMode boundaryMode = SubdivisionMesh.BoundaryMode.CREASE_EDGES;
    private SubdivisionMesh.MapBorderMode mapBorderMode = SubdivisionMesh.MapBorderMode.NOT_SMOOTH;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    private final EventHandler<MouseEvent> mouseEventHandler = event -> {
        // System.out.println("MouseEvent ...");

        double yFlip = 1.0;
        if (getYUp()) {
            yFlip = 1.0;
        }
        else {
            yFlip = -1.0;
        }
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            dragStartRotateX = cameraXRotate.getAngle();
            dragStartRotateY = cameraYRotate.getAngle();
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();

        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            double xDelta = event.getSceneX() -  dragStartX;
            double yDelta = event.getSceneY() -  dragStartY;
            //cameraXRotate.setAngle(dragStartRotateX - (yDelta*0.7));
            //cameraYRotate.setAngle(dragStartRotateY + (xDelta*0.7));

            double modifier = 1.0;
            double modifierFactor = 0.3;

            if (event.isControlDown()) {
                modifier = 0.1;
            }
            if (event.isShiftDown()) {
                modifier = 10.0;
            }

            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX); //*DELTA_MULTIPLIER;
            mouseDeltaY = (mousePosY - mouseOldY); //*DELTA_MULTIPLIER;

            double flip = -1.0;

            boolean alt = (true || event.isAltDown());  // For now, don't require ALT to be pressed
            if (alt && (event.isMiddleButtonDown() || (event.isPrimaryButtonDown() && event.isSecondaryButtonDown()))) {
                cameraXform2.t.setX(cameraXform2.t.getX() + flip*mouseDeltaX*modifierFactor*modifier*0.3);  // -
                cameraXform2.t.setY(cameraXform2.t.getY() + yFlip*mouseDeltaY*modifierFactor*modifier*0.3);  // -
            }
            else if (alt && event.isPrimaryButtonDown()) {
                cameraXform.ry.setAngle(cameraXform.ry.getAngle() - yFlip*mouseDeltaX*modifierFactor*modifier*2.0);  // +
                cameraXform.rx.setAngle(cameraXform.rx.getAngle() + flip*mouseDeltaY*modifierFactor*modifier*2.0);  // -
            }
            else if (alt && event.isSecondaryButtonDown()) {
                double z = cameraPosition.getZ();
                // double z = camera.getTranslateZ();
                // double newZ = z + yFlip*flip*mouseDeltaX*modifierFactor*modifier;
                double newZ = z - flip*(mouseDeltaX+mouseDeltaY)*modifierFactor*modifier;
                System.out.println("newZ = " + newZ);
                cameraPosition.setZ(newZ);
                // camera.setTranslateZ(newZ);
            }

        }
    };
    private final EventHandler<ScrollEvent> scrollEventHandler = event -> {
        if (event.getTouchCount() > 0) { // touch pad scroll
            cameraXform2.t.setX(cameraXform2.t.getX() - (0.01*event.getDeltaX()));  // -
            cameraXform2.t.setY(cameraXform2.t.getY() + (0.01*event.getDeltaY()));  // -
        } else {
            double z = cameraPosition.getZ()-(event.getDeltaY()*0.2);
            z = Math.max(z,-1000);
            z = Math.min(z,0);
            cameraPosition.setZ(z);
        }
    };
    private final EventHandler<ZoomEvent> zoomEventHandler = event -> {
        if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8 && event.getZoomFactor() < 1.2) {
            double z = cameraPosition.getZ()/event.getZoomFactor();
            z = Math.max(z,-1000);
            z = Math.min(z,0);
            cameraPosition.setZ(z);
        }
    };
    private final EventHandler<KeyEvent> keyEventHandler = event -> {
        /*
        if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8 && event.getZoomFactor() < 1.2) {
            double z = cameraPosition.getZ()/event.getZoomFactor();
            z = Math.max(z,-1000);
            z = Math.min(z,0);
            cameraPosition.setZ(z);
        }
        */
        System.out.println("KeyEvent ...");
        Timeline timeline = getTimeline();
        Duration currentTime;
        double CONTROL_MULTIPLIER = 0.1;
        double SHIFT_MULTIPLIER = 0.1;
        double ALT_MULTIPLIER = 0.5;
        //System.out.println("--> handleKeyboard>handle");

        // event.getEventType();

        switch (event.getCode()) {
            case F:
                if (event.isControlDown()) {
                    //onButtonSave();
                }
                break;
            case O:
                if (event.isControlDown()) {
                    //onButtonLoad();
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
            /*
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
            */
            case UP:
                if (event.isControlDown() && event.isShiftDown()) {
                    cameraXform2.t.setY(cameraXform2.t.getY() - 10.0*CONTROL_MULTIPLIER);
                }
                else if (event.isAltDown() && event.isShiftDown()) {
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() - 10.0*ALT_MULTIPLIER);
                }
                else if (event.isControlDown()) {
                    cameraXform2.t.setY(cameraXform2.t.getY() - 1.0*CONTROL_MULTIPLIER);
                }
                else if (event.isAltDown()) {
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() - 2.0*ALT_MULTIPLIER);
                }
                else if (event.isShiftDown()) {
                    double z = camera.getTranslateZ();
                    double newZ = z + 5.0*SHIFT_MULTIPLIER;
                    camera.setTranslateZ(newZ);
                }
                break;
            case DOWN:
                if (event.isControlDown() && event.isShiftDown()) {
                    cameraXform2.t.setY(cameraXform2.t.getY() + 10.0*CONTROL_MULTIPLIER);
                }
                else if (event.isAltDown() && event.isShiftDown()) {
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() + 10.0*ALT_MULTIPLIER);
                }
                else if (event.isControlDown()) {
                    cameraXform2.t.setY(cameraXform2.t.getY() + 1.0*CONTROL_MULTIPLIER);
                }
                else if (event.isAltDown()) {
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() + 2.0*ALT_MULTIPLIER);
                }
                else if (event.isShiftDown()) {
                    double z = camera.getTranslateZ();
                    double newZ = z - 5.0*SHIFT_MULTIPLIER;
                    camera.setTranslateZ(newZ);
                }
                break;
            case RIGHT:
                if (event.isControlDown() && event.isShiftDown()) {
                    cameraXform2.t.setX(cameraXform2.t.getX() + 10.0*CONTROL_MULTIPLIER);
                }
                else if (event.isAltDown() && event.isShiftDown()) {
                    cameraXform.ry.setAngle(cameraXform.ry.getAngle() - 10.0*ALT_MULTIPLIER);
                }
                else if (event.isControlDown()) {
                    cameraXform2.t.setX(cameraXform2.t.getX() + 1.0*CONTROL_MULTIPLIER);
                }
                else if (event.isShiftDown()) {
                    currentTime = timeline.getCurrentTime();
                    timeline.jumpTo(Frame.frame(Math.round(Frame.toFrame(currentTime)/10.0)*10 + 10));
                    // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() + ONE_FRAME));
                }
                else if (event.isAltDown()) {
                    cameraXform.ry.setAngle(cameraXform.ry.getAngle() - 2.0*ALT_MULTIPLIER);
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
                }
                else if (event.isAltDown() && event.isShiftDown()) {
                    cameraXform.ry.setAngle(cameraXform.ry.getAngle() + 10.0*ALT_MULTIPLIER);  // -
                }
                else if (event.isControlDown()) {
                    cameraXform2.t.setX(cameraXform2.t.getX() - 1.0*CONTROL_MULTIPLIER);
                }
                else if (event.isShiftDown()) {
                    currentTime = timeline.getCurrentTime();
                    timeline.jumpTo(Frame.frame(Math.round(Frame.toFrame(currentTime)/10.0)*10 - 10));
                    // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() - ONE_FRAME));
                }
                else if (event.isAltDown()) {
                    cameraXform.ry.setAngle(cameraXform.ry.getAngle() + 2.0*ALT_MULTIPLIER);  // -
                }
                else {
                    currentTime = timeline.getCurrentTime();
                    timeline.jumpTo(Frame.frame(Frame.toFrame(currentTime) - 1));
                    // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() - ONE_FRAME));
                }
                break;
        }
        //System.out.println(cameraXform.getTranslateX() + ", " + cameraXform.getTranslateY() + ", " + cameraXform.getTranslateZ());


    };

    public ContentModel() {
        // CAMERA
        camera.setNearClip(1.0); // TODO: Workaround as per RT-31255
        camera.setFarClip(10000.0); // TODO: Workaround as per RT-31255

        camera.getTransforms().addAll(
                yUpRotate,
                //cameraXRotate,
                //cameraYRotate,
                cameraPosition,
                cameraLookXRotate,
                cameraLookZRotate);
        //root3D.getChildren().add(camera);
        root3D.getChildren().add(cameraXform);
        cameraXform.getChildren().add(cameraXform2);
        cameraXform2.getChildren().add(cameraXform3);
        cameraXform3.getChildren().add(camera);
        cameraPosition.setZ(-cameraDistance);
        // camera.setTranslateZ(-cameraDistance);
        root3D.getChildren().add(autoScalingGroup);

        SessionManager sessionManager = SessionManager.getSessionManager();
        sessionManager.bind(cameraLookXRotate.angleProperty(), "cameraLookXRotate");
        sessionManager.bind(cameraLookZRotate.angleProperty(), "cameraLookZRotate");
        sessionManager.bind(cameraPosition.xProperty(), "cameraPosition.x");
        sessionManager.bind(cameraPosition.yProperty(), "cameraPosition.y");
        sessionManager.bind(cameraPosition.zProperty(), "cameraPosition.z");
        sessionManager.bind(cameraXRotate.angleProperty(), "cameraXRotate");
        sessionManager.bind(cameraYRotate.angleProperty(), "cameraYRotate");
        sessionManager.bind(camera.nearClipProperty(), "cameraNearClip");
        sessionManager.bind(camera.farClipProperty(), "cameraFarClip");

        // Build SubScene
        rebuildSubScene();
    }

    private void rebuildSubScene() {
        SubScene oldSubScene = this.subScene.get();
        if (oldSubScene != null) {
            oldSubScene.setRoot(new Region());
            oldSubScene.setCamera(null);
            oldSubScene.removeEventHandler(MouseEvent.ANY, mouseEventHandler);
            oldSubScene.removeEventHandler(KeyEvent.ANY, keyEventHandler);
            oldSubScene.removeEventHandler(ScrollEvent.ANY, scrollEventHandler);
        }

        javafx.scene.SceneAntialiasing aaVal = msaa.get() ?
                javafx.scene.SceneAntialiasing.BALANCED :
                javafx.scene.SceneAntialiasing.DISABLED;
        SubScene subScene = new SubScene(root3D,400,400,true,aaVal);
        this.subScene.set(subScene);
        subScene.setFill(Color.ALICEBLUE);
        subScene.setCamera(camera);
        // SCENE EVENT HANDLING FOR CAMERA NAV
        subScene.addEventHandler(MouseEvent.ANY, mouseEventHandler);
        subScene.addEventHandler(KeyEvent.ANY, keyEventHandler);
        // subScene.addEventFilter(KeyEvent.ANY, keyEventHandler);
        subScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler);
        subScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler);

        // Scene scene = subScene.getScene();
        // scene.addEventFilter(KeyEvent.ANY, keyEventHandler);

        /*
        subScene.sceneProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object t, Object t1) {
                System.out.println("hello world");
            }
        });
        */
    }

    public boolean getAmbientLightEnabled() {
        return ambientLightEnabled.get();
    }

    public SimpleBooleanProperty ambientLightEnabledProperty() {
        return ambientLightEnabled;
    }

    public void setAmbientLightEnabled(boolean ambientLightEnabled) {
        this.ambientLightEnabled.set(ambientLightEnabled);
    }

    public boolean getLight1Enabled() {
        return light1Enabled.get();
    }

    public SimpleBooleanProperty light1EnabledProperty() {
        return light1Enabled;
    }

    public void setLight1Enabled(boolean light1Enabled) {
        this.light1Enabled.set(light1Enabled);
    }

    public boolean getLight2Enabled() {
        return light2Enabled.get();
    }

    public SimpleBooleanProperty light2EnabledProperty() {
        return light2Enabled;
    }

    public void setLight2Enabled(boolean light2Enabled) {
        this.light2Enabled.set(light2Enabled);
    }

    public boolean getLight3Enabled() {
        return light3Enabled.get();
    }

    public SimpleBooleanProperty light3EnabledProperty() {
        return light3Enabled;
    }

    public void setLight3Enabled(boolean light3Enabled) {
        this.light3Enabled.set(light3Enabled);
    }

    public AmbientLight getAmbientLight() {
        return ambientLight;
    }

    public PointLight getLight1() {
        return light1;
    }

    public PointLight getLight2() {
        return light2;
    }

    public PointLight getLight3() {
        return light3;
    }

    public boolean getYUp() {
        return yUp.get();
    }

    public SimpleBooleanProperty yUpProperty() {
        return yUp;
    }

    public void setYUp(boolean yUp) {
        this.yUp.set(yUp);
    }

    public boolean getShowAxis() {
        return showAxis.get();
    }

    public SimpleBooleanProperty showAxisProperty() {
        return showAxis;
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis.set(showAxis);
    }

    public AutoScalingGroup getAutoScalingGroup() {
        return autoScalingGroup;
    }

    public ObjectProperty<Node> contentProperty() { return content; }
    public Node getContent() { return content.get(); }
    public void setContent(Node content) { this.content.set(content); }

    {
        contentProperty().addListener((ov, oldContent, newContent) -> {
            autoScalingGroup.getChildren().remove(oldContent);
            autoScalingGroup.getChildren().add(newContent);
            setWireFrame(newContent,wireframe);
            // TODO mesh is updated each time these are called even if no rendering needs to happen
            setSubdivisionLevel(newContent, subdivisionLevel);
            setBoundaryMode(newContent, boundaryMode);
            setMapBorderMode(newContent, mapBorderMode);
        });
    }

    public boolean getMsaa() {
        return msaa.get();
    }

    public SimpleBooleanProperty msaaProperty() {
        return msaa;
    }

    public void setMsaa(boolean msaa) {
        this.msaa.set(msaa);
    }

    public SubScene getSubScene() {
        return subScene.get();
    }

    public SimpleObjectProperty<SubScene> subSceneProperty() {
        return subScene;
    }

    public Group getRoot3D() {
        return root3D;
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    public Rotate getCameraXRotate() {
        return cameraXRotate;
    }

    public Rotate getCameraYRotate() {
        return cameraYRotate;
    }

    public Translate getCameraPosition() {
        return cameraPosition;
    }

    public Rotate getCameraLookXRotate() {
        return cameraLookXRotate;
    }

    public Rotate getCameraLookZRotate() {
        return cameraLookZRotate;
    }

    public void setWireFrame(boolean wireframe) {
        this.wireframe = wireframe;
        setWireFrame(root3D,wireframe);
    }

    public boolean isWireframe() {
        return wireframe;
    }

    private void setWireFrame(Node node, boolean wireframe) {
        if (node instanceof PolygonMeshView) {
            ((PolygonMeshView)node).setDrawMode(wireframe ? DrawMode.LINE: DrawMode.FILL);
        } else if (node instanceof MeshView) {
            ((MeshView)node).setDrawMode(wireframe ? DrawMode.LINE: DrawMode.FILL);
        } else if (node instanceof Parent) {
            for (Node child: ((Parent)node).getChildrenUnmodifiable()) setWireFrame(child,wireframe);
        }
    }

    public SubdivisionMesh.BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    public void setBoundaryMode(SubdivisionMesh.BoundaryMode boundaryMode) {
        this.boundaryMode = boundaryMode;
        setBoundaryMode(root3D, boundaryMode);
    }
    private void setBoundaryMode(Node node, SubdivisionMesh.BoundaryMode boundaryMode) {
        if (node instanceof PolygonMeshView) {
            ((PolygonMeshView)node).setBoundaryMode(boundaryMode);
        } else if (node instanceof Parent) {
            for (Node child: ((Parent)node).getChildrenUnmodifiable()) setBoundaryMode(child, boundaryMode);
        }
    }

    public SubdivisionMesh.MapBorderMode getMapBorderMode() {
        return mapBorderMode;
    }
    public void setMapBorderMode(SubdivisionMesh.MapBorderMode mapBorderMode) {
        this.mapBorderMode = mapBorderMode;
        setMapBorderMode(root3D, mapBorderMode);
    }
    private void setMapBorderMode(Node node, SubdivisionMesh.MapBorderMode mapBorderMode) {
        if (node instanceof PolygonMeshView) {
            ((PolygonMeshView)node).setMapBorderMode(mapBorderMode);
        } else if (node instanceof Parent) {
            for (Node child: ((Parent)node).getChildrenUnmodifiable()) setMapBorderMode(child, mapBorderMode);
        }
    }

    public int getSubdivisionLevel() {
        return subdivisionLevel;
    }
    public void setSubdivisionLevel(int subdivisionLevel) {
        this.subdivisionLevel = subdivisionLevel;
        setSubdivisionLevel(root3D, subdivisionLevel);
    }
    private void setSubdivisionLevel(Node node, int subdivisionLevel) {
        if (node instanceof PolygonMeshView) {
            ((PolygonMeshView)node).setSubdivisionLevel(subdivisionLevel);
        } else if (node instanceof Parent) {
            for (Node child: ((Parent)node).getChildrenUnmodifiable()) setSubdivisionLevel(child, subdivisionLevel);
        }
    }

    private void createAxes() {
        double length = 200.0;
        double width = 1.0;
        double radius = 2.0;
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);
        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.DARKGREEN);
        greenMaterial.setSpecularColor(Color.GREEN);
        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.DARKBLUE);
        blueMaterial.setSpecularColor(Color.BLUE);

        xSphere = new Sphere(radius);
        ySphere = new Sphere(radius);
        zSphere = new Sphere(radius);
        xSphere.setMaterial(redMaterial);
        ySphere.setMaterial(greenMaterial);
        zSphere.setMaterial(blueMaterial);

        xSphere.setTranslateX(100.0);
        ySphere.setTranslateY(100.0);
        zSphere.setTranslateZ(100.0);

        xAxis = new Box(length, width, width);
        yAxis = new Box(width, length, width);
        zAxis = new Box(width, width, length);
        xAxis.setMaterial(redMaterial);
        yAxis.setMaterial(greenMaterial);
        zAxis.setMaterial(blueMaterial);
    }
}
