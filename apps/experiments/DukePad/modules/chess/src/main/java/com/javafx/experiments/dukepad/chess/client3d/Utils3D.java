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

package com.javafx.experiments.dukepad.chess.client3d;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author akouznet
 */
public class Utils3D {

    public static void attachToRight(Node node, Node base) {
        node.setTranslateX(base.getBoundsInParent().getMaxX() - node.getBoundsInParent().getMinX());
    }

    public static void attachToBack(Node node, Node base) {
        node.setTranslateZ(base.getBoundsInParent().getMaxZ() - node.getBoundsInParent().getMinZ());
    }

    public static void attachToFront(Node node, Node base) {
        node.setTranslateZ(base.getBoundsInParent().getMinZ() - node.getBoundsInParent().getMaxZ());
    }

    public static void alignYControlPoints(Node node, double nodeY, Node base, double baseY) {
        node.setTranslateY(baseY - nodeY);
    }

    public static void centerOnTop(Node node, Node base) {
        putOnTop(node, base);
        centerXZ(node, base);
    }

    public static void putOnTop(Node node, Node base) {
        node.setTranslateY(node.getTranslateY() + base.getBoundsInParent().getMaxY() - node.getBoundsInParent().getMinY());
    }

    public static void centerXZ(Node node, Node base) {
        centerX(node, base);
        centerZ(node, base);
    }

    public static void centerX(Node node, Node base) {
        node.setTranslateX(node.getTranslateX() + middleX(base.getBoundsInParent()) - middleX(node.getBoundsInParent()));
    }

    public static void centerZ(Node node, Node base) {
        node.setTranslateZ(node.getTranslateZ() + middleZ(base.getBoundsInParent()) - middleZ(node.getBoundsInParent()));
    }

    public static double middleX(Bounds b) {
        return (b.getMinX() + b.getMaxX()) / 2;
    }

    public static double middleY(Bounds b) {
        return (b.getMinY() + b.getMaxY()) / 2;
    }

    public static double middleZ(Bounds b) {
        return (b.getMinZ() + b.getMaxZ()) / 2;
    }

    public static Point3D convertPoint(Point3D p, Node node, Node target) {
        return target.sceneToLocal(node.localToScene(p));
    }
    public static Bounds convertBounds(Node node, Node target) {
        return target.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
    }

    public static class ReparentableGroup extends Group {

        private Transform lastParentToScene;
        private ObjectProperty<Transform> adjustmentTransform = new SimpleObjectProperty<Transform>(new Affine());

        {
            adjustmentTransform.addListener(new ChangeListener<Transform>() {

                @Override
                public void changed(ObservableValue<? extends Transform> observable, Transform oldValue, Transform newValue) {
                    if (oldValue != null) {
                        getTransforms().remove(oldValue);
                    }
                    if (newValue != null) {
                        getTransforms().add(0, newValue);
                    }
                }
            });
            parentProperty().addListener(new ChangeListener<Parent>() {
                @Override
                public void changed(ObservableValue<? extends Parent> observable, Parent oldValue, Parent newValue) {
                    if (oldValue != null) {
                        lastParentToScene = oldValue.getLocalToSceneTransform();
                    }
                    if (newValue != null && lastParentToScene != null) {
                        Transform parentToScene = newValue.getLocalToSceneTransform();

                        try {
                            // TODO: This approach won't work if setRotate is applied to this node
                            adjustmentTransform.set(
                                    parentToScene.createInverse()
                                    .createConcatenation(lastParentToScene)
                                    .createConcatenation(adjustmentTransform.get()));
                        } catch (NonInvertibleTransformException ex) {
                            Logger.getLogger(ReparentableGroup.class.getName()).
                                    log(Level.SEVERE, null, ex);
                        }
                        lastParentToScene = null;
                    }
                }
            });
        }

        public ReparentableGroup(Node... nodes) {
            super(nodes);
        }

        public ReparentableGroup(Collection<Node> clctn) {
            super(clctn);
        }
    }
}
