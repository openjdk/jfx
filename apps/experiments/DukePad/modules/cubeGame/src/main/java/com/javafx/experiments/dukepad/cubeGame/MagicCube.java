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

package com.javafx.experiments.dukepad.cubeGame;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import static com.javafx.experiments.dukepad.cubeGame.FancyBox3D.Face.*;

public class MagicCube extends Group {

    public static final float SIZE = 40;
    public static final float EDGE = SIZE * 0.05f;
    public static final Point3D ZERO_POINT3D = new Point3D(0, 0, 0);
    public final FancyBox3D cubes[][][] = new FancyBox3D[3][3][3];
    private Runnable onRotateEnd;
    private PointLight selectionLight;
    private Point3D selPos = new Point3D(1, 1, 2);
    private Point3D selUp = new Point3D(0, 1, 0);
    private Point3D selRight = new Point3D(1, 0, 0);
    private Point3D selOut = new Point3D(0, 0, 1);
    private Point3D rotatingAxis = null;
    private boolean[] rotatingLayers = new boolean[3];
    private Group cubesParent;

    public MagicCube() {
        cubesParent = new Group();
        getChildren().add(cubesParent);
        for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 3; r++) {
                for (int d = 0; d < 3; d++) {
                    if (!(r == 1 && d == 1 && c == 1)) {
                        FancyBox3D cube3D = new FancyBox3D(SIZE, SIZE, SIZE, EDGE);
                        cubes[c][r][d] = cube3D;
                        if (r == 0) {
                            cube3D.setFace(BOTTOM, 0, 0, d, c);
                        }
                        if (r == 2) {
                            cube3D.setFace(TOP, 1, 0, c, d);
                        }
                        if (c == 0) {
                            cube3D.setFace(LEFT, 2, 0, 2 - d, r);
                        }
                        if (c == 2) {
                            cube3D.setFace(RIGHT, 0, 1, d, r);
                        }
                        if (d == 2) {
                            cube3D.setFace(FRONT, 1, 1, 2 - c, r);
                        }
                        if (d == 0) {
                            cube3D.setFace(BACK, 2, 1, c, r);
                        }
                        cube3D.setTranslateX((c - 1.5) * SIZE);
                        cube3D.setTranslateY((r - 1.5) * SIZE);
                        cube3D.setTranslateZ((d - 1.5) * SIZE);
                        cubesParent.getChildren().add(cube3D);
                    }
                }
            }
        }
        selectionLight = new PointLight(Color.WHITE);
//        getChildren().add(selectionLight);
        positionLight();
        fixLightScope();
        AmbientLight ambientLight = new AmbientLight(Color.GREY);
        ambientLight.getScope().addAll(cubesParent.getChildren());
//        getChildren().add(ambientLight);
    }

    public boolean rotate(final Point3D axis, final int layer, int rotate) {
        if (!(rotatingAxis == null || (rotatingAxis == axis && !rotatingLayers[layer]))) {
            return false;
        }
        rotatingAxis = axis;
        fixLightScope();
        rotatingLayers[layer] = true;
        final int r4 = r4(rotate);
        final FancyBox3D _cubes[][] = new FancyBox3D[3][3];
        final Group rotation = new Group();
        selectionLight.getScope().add(rotation);
        cubesParent.getChildren().add(rotation);
        if (axis == Rotate.X_AXIS) {
            Rotate rot = new Rotate(0, axis);
            rotation.getTransforms().setAll(rot);
            final int c = layer;
            for (int r = 0; r < 3; r++) {
                for (int d = 0; d < 3; d++) {
                    if (c == 1 && r == 1 && d == 1) {
                        continue;
                    }
                    _cubes[r][d] = cubes[c][r][d];
                    rotation.getChildren().add(cubes[c][r][d]);
                }
            }
            Timeline animation = new Timeline();
            animation.getKeyFrames().setAll(new KeyFrame(Duration.millis(400), t -> {
                for (int r = 0; r < 3; r++) {
                    for (int d = 0; d < 3; d++) {
                        if (c == 1 && r == 1 && d == 1) {
                            continue;
                        }
                        int oldR = oldX(r, d, r4), oldD = oldY(r, d, r4);
                        cubes[c][r][d] = _cubes[oldR][oldD];
                        cubes[c][r][d].setTranslateX((c - 1.5) * SIZE);
                        cubes[c][r][d].setTranslateY((r - 1.5) * SIZE);
                        cubes[c][r][d].setTranslateZ((d - 1.5) * SIZE);
                        cubes[c][r][d].getTransforms().add(0, new Rotate(r4 * 90, SIZE / 2, SIZE / 2, SIZE / 2, axis));
                        cubesParent.getChildren().add(cubes[c][r][d]);
                    }
                }
                cubesParent.getChildren().remove(rotation);
                endRotating(layer);
                fixLightScope();
            }, new KeyValue(rot.angleProperty(), 90 * rotate)));
            animation.play();
        } else if (axis == Rotate.Y_AXIS) {
            Rotate rot = new Rotate(0, axis);
            rotation.getTransforms().setAll(rot);
            final int r = layer;
            for (int c = 0; c < 3; c++) {
                for (int d = 0; d < 3; d++) {
                    if (c == 1 && r == 1 && d == 1) {
                        continue;
                    }
                    _cubes[c][d] = cubes[c][r][d];
                    rotation.getChildren().add(cubes[c][r][d]);
                }
            }
            Timeline animation = new Timeline();
            animation.getKeyFrames().setAll(new KeyFrame(Duration.millis(400), t -> {
                for (int c = 0; c < 3; c++) {
                    for (int d = 0; d < 3; d++) {
                        if (c == 1 && r == 1 && d == 1) {
                            continue;
                        }
                        int oldC = oldY(d, c, r4), oldD = oldX(d, c, r4);
                        cubes[c][r][d] = _cubes[oldC][oldD];
                        cubes[c][r][d].setTranslateX((c - 1.5) * SIZE);
                        cubes[c][r][d].setTranslateY((r - 1.5) * SIZE);
                        cubes[c][r][d].setTranslateZ((d - 1.5) * SIZE);
                        cubes[c][r][d].getTransforms().add(0, new Rotate(r4 * 90, SIZE / 2, SIZE / 2, SIZE / 2, axis));
                        cubesParent.getChildren().add(cubes[c][r][d]);
                    }
                }
                cubesParent.getChildren().remove(rotation);
                endRotating(layer);
                fixLightScope();
            }, new KeyValue(rot.angleProperty(), 90 * rotate)));
            animation.play();
        } else if (axis == Rotate.Z_AXIS) {
            Rotate rot = new Rotate(0, axis);
            rotation.getTransforms().setAll(rot);
            final int d = layer;
            for (int c = 0; c < 3; c++) {
                for (int r = 0; r < 3; r++) {
                    if (c == 1 && r == 1 && d == 1) {
                        continue;
                    }
                    _cubes[c][r] = cubes[c][r][d];
                    rotation.getChildren().add(cubes[c][r][d]);
                }
            }
            Timeline animation = new Timeline();
            animation.getKeyFrames().setAll(new KeyFrame(Duration.millis(400), new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    for (int c = 0; c < 3; c++) {
                        for (int r = 0; r < 3; r++) {
                            if (c == 1 && r == 1 && d == 1) {
                                continue;
                            }
                            int oldC = oldX(c, r, r4), oldR = oldY(c, r, r4);
                            cubes[c][r][d] = _cubes[oldC][oldR];
                            cubes[c][r][d].setTranslateX((c - 1.5) * SIZE);
                            cubes[c][r][d].setTranslateY((r - 1.5) * SIZE);
                            cubes[c][r][d].setTranslateZ((d - 1.5) * SIZE);
                            cubes[c][r][d].getTransforms().add(0, new Rotate(r4 * 90, SIZE / 2, SIZE / 2, SIZE / 2, axis));
                            cubesParent.getChildren().add(cubes[c][r][d]);
                        }
                    }
                    getChildren().remove(rotation);
                    endRotating(layer);
                    fixLightScope();
                }
            }, new KeyValue(rot.angleProperty(), 90 * rotate)));
            animation.play();
        }
        return true;
    }

    private void endRotating(int layer) {
        rotatingLayers[layer] = false;
        boolean stillRotating = false;
        for (boolean rl : rotatingLayers) {
            if (rl) {
                stillRotating = true;
                break;
            }
        }
        if (!stillRotating) {
            rotatingAxis = null;
        }
        if (onRotateEnd != null) {
            onRotateEnd.run();
        }
    }

    public void setOnRotateEnd(Runnable onRotateEnd) {
        this.onRotateEnd = onRotateEnd;
    }

    private int oldX(int x, int y, int r4) {
        switch (r4) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return 2 - x;
            case 3:
                return 2 - y;
            default:
                throw new IllegalArgumentException("r4 is expected to be 0, 1, 2 or 3");
        }
    }

    private int oldY(int x, int y, int r4) {
        switch (r4) {
            case 0:
                return y;
            case 1:
                return 2 - x;
            case 2:
                return 2 - y;
            case 3:
                return x;
            default:
                throw new IllegalArgumentException("r4 is expected to be 0, 1, 2 or 3");
        }
    }

    public int r4(int rotate) {
        while (rotate < 0) {
            rotate += 10000;
        }
        return rotate %= 4;
    }

    public void arrow(int byX, int byY) {
        if (!inSelectDirectionMode) {
            moveSelection(byX, byY);
        } else {
            Point3D inDirection = selRight.multiply(byX).add(selUp.multiply(byY));
            Point3D axis = selOut.crossProduct(inDirection).normalize();
            int layer;
            if (Math.abs(axis.getX()) > 0.5) {
                axis = Rotate.X_AXIS;
                layer = (int) selPos.getX();
            } else if (Math.abs(axis.getY()) > 0.5) {
                axis = Rotate.Y_AXIS;
                layer = (int) selPos.getY();
            } else if (Math.abs(axis.getZ()) > 0.5) {
                axis = Rotate.Z_AXIS;
                layer = (int) selPos.getZ();
            } else {
                return;
            }
            rotate(axis, layer, (int) Math.signum(
                    axis.crossProduct(selOut).dotProduct(inDirection)));
        }
    }

    public void moveSelection(int byX, int byY) {
        if (byX != 0) {
            byY = 0;
            byX = (byX > 0) ? 1 : -1;
        } else {
            byY = (byY > 0) ? 1 : -1;
        }
        Point3D tarPos = selPos.add(selRight.multiply(byX)).add(selUp.multiply(byY));
        if (tarPos.getX() < 0 || tarPos.getX() > 2
                || tarPos.getY() < 0 || tarPos.getY() > 2
                || tarPos.getZ() < 0 || tarPos.getZ() > 2) {
            if (byX != 0) {
                selRight = selUp.crossProduct(selRight).multiply(byX);
            } else {
                selUp = selRight.crossProduct(selUp).multiply(-byY);
            }
            selOut = selUp.crossProduct(selRight).multiply(-1);
            tarPos = selPos;
        }
        animateSelection(tarPos);
    }

    private Point3D getLightPosition() {
        return selPos.add(selOut).add(-1.5 + 0.5, -1.5 + 0.5, -1.5 + 0.5).multiply(SIZE);
    }

    private void fixLightScope() {
        selectionLight.getScope().setAll(getCube(selPos));
        if (inSelectDirectionMode && rotatingAxis == null) {
            addCubeToScopeIfNotNull(selPos.add(selUp));
            addCubeToScopeIfNotNull(selPos.subtract(selUp));
            addCubeToScopeIfNotNull(selPos.add(selRight));
            addCubeToScopeIfNotNull(selPos.subtract(selRight));
        }
    }

    private FancyBox3D getCube(Point3D p) {
        int c = (int) (p.getX() + 0.5);
        int r = (int) (p.getY() + 0.5);
        int d = (int) (p.getZ() + 0.5);
        return cubes[c][r][d];
    }

    private void addCubeToScopeIfNotNull(Point3D p) {
        int c = (int) (p.getX() + 0.5);
        int r = (int) (p.getY() + 0.5);
        int d = (int) (p.getZ() + 0.5);
        if (c >= 0 && c < 3 && r >= 0 && r < 3 && d >= 0 && d < 3) {
            selectionLight.getScope().add(cubes[c][r][d]);
        }
    }

    private Timeline selAnimation;

    private void animateSelection(final Point3D tarPos) {
        selPos = tarPos;
        final Point3D pos = getLightPosition();
        if (selAnimation != null) {
            selAnimation.stop();
        }

        Bounds cubeBounds = cubesParent.getBoundsInLocal();
        Point3D cubeCenter = cubesParent.localToParent(new Point3D(
                (cubeBounds.getMinX() + cubeBounds.getMaxX()) / 2,
                (cubeBounds.getMinY() + cubeBounds.getMaxY()) / 2,
                (cubeBounds.getMinZ() + cubeBounds.getMaxZ()) / 2));
        Point3D lightPos = pos;
        Point3D newViewDirection = cubeCenter.subtract(lightPos);
        Point3D oldViewDirection = sceneToLocal(new Point3D(0, 0, 1))
                .subtract(sceneToLocal(ZERO_POINT3D));
        Point3D rotateAxis = newViewDirection.crossProduct(oldViewDirection);
        double rotateAngle = newViewDirection.angle(oldViewDirection);
        Rotate rotateCube = new Rotate(rotateAngle, cubeCenter.getX(), cubeCenter.getY(), cubeCenter.getZ(), rotateAxis);
        getTransforms().addAll(rotateCube);

        Point3D oldUpTmp = sceneToLocal(new Point3D(0, -1, 0)).subtract(sceneToLocal(ZERO_POINT3D)).normalize();

        Point3D oldUpDirection = oldUpTmp.crossProduct(newViewDirection).normalize();
        Point3D newUpDirection = selUp.crossProduct(newViewDirection).normalize();

        double direction = newUpDirection.crossProduct(oldUpDirection).dotProduct(newViewDirection);
        double rotateAngle2 = Math.signum(direction) * newUpDirection.angle(oldUpDirection);
        if (Double.isNaN(rotateAngle2)) rotateAngle2 = 0;
        rotateCube.setAngle(0);
        Rotate rotateCube2 = new Rotate(0, cubeCenter.getX(), cubeCenter.getY(), cubeCenter.getZ(), newViewDirection);
        getTransforms().addAll(rotateCube2);

        selAnimation = new Timeline(
                new KeyFrame(Duration.millis(200), new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent t) {
                        fixLightScope();
                    }
                },
                new KeyValue(selectionLight.translateXProperty(), pos.getX(), Interpolator.EASE_OUT),
                new KeyValue(selectionLight.translateYProperty(), pos.getY(), Interpolator.EASE_OUT),
                new KeyValue(selectionLight.translateZProperty(), pos.getZ(), Interpolator.EASE_OUT),
                new KeyValue(rotateCube.angleProperty(), rotateAngle, Interpolator.EASE_OUT),
                new KeyValue(rotateCube2.angleProperty(), rotateAngle2, Interpolator.EASE_OUT)));
        selectionLight.getScope().add(getCube(tarPos));
        selAnimation.play();
    }

    private void positionLight() {
        Point3D pos = getLightPosition();
        selectionLight.setTranslateX(pos.getX());
        selectionLight.setTranslateY(pos.getY());
        selectionLight.setTranslateZ(pos.getZ());
    }

    private boolean inSelectDirectionMode = false;

    public void enter() {
        inSelectDirectionMode = !inSelectDirectionMode;
        fixLightScope();
    }
}
