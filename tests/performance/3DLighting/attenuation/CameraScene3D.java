/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package attenuation;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

class CameraScene3D extends Pane {

    public DoubleProperty xPan = new SimpleDoubleProperty();
    public DoubleProperty yPan = new SimpleDoubleProperty(-10);
    public DoubleProperty zoom = new SimpleDoubleProperty();
    public DoubleProperty zAngle = new SimpleDoubleProperty();
    public DoubleProperty isometricAngle = new SimpleDoubleProperty();

    public DoubleProperty panSensitivity = new SimpleDoubleProperty(1);
    public DoubleProperty zoomSensitivity = new SimpleDoubleProperty(1);
    public DoubleProperty zRotationSensitivity = new SimpleDoubleProperty(1);
    public BooleanProperty isZoomTotal = new SimpleBooleanProperty();

    protected PerspectiveCamera camera = new PerspectiveCamera(true);

    public DoubleProperty farClip = new SimpleDoubleProperty(camera.getFarClip());
    public DoubleProperty nearClip = new SimpleDoubleProperty(camera.getNearClip());
    public DoubleProperty fieldOfView = new SimpleDoubleProperty(camera.getFieldOfView());
    public BooleanProperty verticalFOV = new SimpleBooleanProperty(camera.isVerticalFieldOfView());

    protected PointLight camLight = new PointLight();
    public BooleanProperty camLightOn = new SimpleBooleanProperty(camLight.isLightOn());

    public Group rootGroup = new Group();

    public CameraScene3D() {
        setupCamera();
        createScenes();
        setUIBindings();
    }

    private void setupCamera() {
        Translate panTranslation = new Translate();
        panTranslation.xProperty().bind(xPan);
        panTranslation.yProperty().bind(yPan);

        Translate zoomTranslation = new Translate();
        zoomTranslation.zProperty().bind(zoom);

        Rotate zRotation = new Rotate(0, Rotate.Y_AXIS);
        zRotation.angleProperty().bind(zAngle);

        Rotate isometricRotation = new Rotate(0, Rotate.X_AXIS);
        isometricRotation.angleProperty().bind(isometricAngle);

        camera.farClipProperty().bind(farClip);
        camera.nearClipProperty().bind(nearClip);
        camera.fieldOfViewProperty().bind(fieldOfView);
        camera.verticalFieldOfViewProperty().bind(verticalFOV);

        camera.getTransforms().addAll(panTranslation, zRotation, isometricRotation, zoomTranslation);

        camLight.lightOnProperty().bind(camLightOn);
        camLight.getTransforms().addAll(camera.getTransforms());
        camLightOn.set(false);

        rootGroup.getTransforms().addAll();
        rootGroup.getChildren().add(camLight);
        rootGroup.setId("root group");
    }

    private void createScenes() {
        var aaScene = new SubScene(rootGroup, 0, 0, true, SceneAntialiasing.BALANCED);
        aaScene.setCamera(camera);
        aaScene.widthProperty().bind(widthProperty());
        aaScene.heightProperty().bind(heightProperty());
        aaScene.setOnMouseEntered(e -> aaScene.requestFocus());
        getChildren().setAll(aaScene);
    }

    private double startX, startY, curX, curY;

    private final void setUIBindings() {
        setOnRotate(e -> rotate(e.getAngle()));
        setOnZoom(e -> zoom(isZoomTotal.get() ? e.getTotalZoomFactor() : e.getZoomFactor()));
        setOnScroll(e -> {
            // touch scroll for moving the board
            if (e.getEventType() == ScrollEvent.SCROLL_STARTED)
                pan(e.getDeltaX(), e.getDeltaY());
            // mouse scroll for zoom
            else
                zoom(e.getDeltaY());
        });

        setOnMousePressed(e -> {
            startX = curX = e.getX();
            startY = curY = e.getY();
        });

        setOnMouseDragged(e -> {
            startX = curX;
            startY = curY;
            curX = e.getX();
            curY = e.getY();
            double deltaX = curX - startX;
            double deltaY = curY - startY;
            if (e.getButton() == MouseButton.PRIMARY) {
                pan(deltaX, deltaY);
            } else if (e.getButton() == MouseButton.SECONDARY) {
                boolean positiveX = curX > getWidth() / 2;
                boolean positiveY = curY > getHeight() / 2;
                deltaX = positiveY ? -deltaX : deltaX;
                deltaY = positiveX ? deltaY : -deltaY;
                rotate((deltaX + deltaY)/2);
            }
        });
    }

    private final double scaleFactor = 500;

    private void pan(double deltaX, double deltaY) {
        double angle = Math.toRadians(zAngle.get());
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        double rotatedDeltaX = deltaX *  cosA + deltaY * sinA;
        double rotatedDeltaY = deltaX * -sinA + deltaY * cosA;
        double panFactor = panSensitivity.get() * zoom.get() / scaleFactor;
        double newX = xPan.get() + rotatedDeltaX * panFactor;
        double newY = yPan.get() + rotatedDeltaY * panFactor;
        xPan.set(newX);
        yPan.set(newY);
    }

    private void zoom(double amount) {
        zoom.set(zoom.get() - amount * zoomSensitivity.get() * zoom.get() / scaleFactor);
    }

    private void rotate(double amount) {
        zAngle.set(zAngle.get() - amount * zRotationSensitivity.get());
    }
}
