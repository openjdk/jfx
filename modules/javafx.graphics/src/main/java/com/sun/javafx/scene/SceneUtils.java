/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.SubScene;

/**
 * Utility class for helper methods needed by scene.
 */
public class SceneUtils {

    /**
     * Translates point from inner subScene coordinates to scene coordinates.
     */
    public static Point3D subSceneToScene(SubScene subScene, Point3D point) {

        Node n = subScene;
        while(n != null) {
            // flatten the coords - project them by subScene's camera
            final Point2D projection = CameraHelper.project(
                    SubSceneHelper.getEffectiveCamera(subScene), point);
            // transform to scene/outer-subScene coords
            point = n.localToScene(projection.getX(), projection.getY(), 0.0);
            n = NodeHelper.getSubScene(n);
        }

        return point;
    }

    /**
     * Translates point from scene coordinates to subScene local coordinates.
     */
    public static Point2D sceneToSubScenePlane(SubScene subScene, Point2D point) {

        // compute pick ray intersection with the subScene, recursively
        // over all parent subScenes
        point = computeSubSceneCoordinates(point.getX(), point.getY(), subScene);

        return point;
    }

    /**
     * Computes subScene local intersection point from the given scene mouse
     * coordinates. Works recursively over all outer subScenes.
     */
    private static Point2D computeSubSceneCoordinates(
            double x, double y, SubScene subScene) {
        SubScene outer = NodeHelper.getSubScene(subScene);

        if (outer == null) {
            return CameraHelper.pickNodeXYPlane(
                    SceneHelper.getEffectiveCamera(subScene.getScene()),
                    subScene, x, y);
        } else {
            Point2D coords = computeSubSceneCoordinates(x, y, outer);
            if (coords != null) {
                coords = CameraHelper.pickNodeXYPlane(
                        SubSceneHelper.getEffectiveCamera(outer),
                        subScene, coords.getX(), coords.getY());
            }
            return coords;
        }
    }

// This method is currently not used, but its code may be useful
// for the future introducion of public 3D picking and in particular
// 3D screenToLocal functionality.
//    /**
//     * Translates 2D scene coordinates to 3D local coordinates of a node.
//     *
//     * If the node contains (is rendered at) the given flat coordinates,
//     * the returned point represents the intersection of the node and a
//     * ray cast by the given coordinates; otherwise it represents
//     * the intersection point of the ray and the projection plane.
//     */
//    public Point3D sceneToLocal3D(Node n, double sceneX, double sceneY) {
//        Scene scene = n.getScene();
//        if (scene == null) {
//            return null;
//        }
//
//        Point2D pt = new Point2D(screenX, screenY);
//        final SubScene subScene = NodeHelper.getSubScene(n);
//        if (subScene != null) {
//            pt = SceneUtils.sceneToSubScenePlane(subScene, pt);
//            if (pt == null) {
//                return null;
//            }
//        }
//
//        // compute pick ray
//        final Camera cam = subScene != null
//                ? SubSceneHelper.getEffectiveCamera(subScene)
//                : SceneHelper.getEffectiveCamera(scene);
//        final PickRay pickRay = cam.computePickRay(pt.getX(), pt.getY(), null);
//
//        // convert it to node's local pickRay
//        final Affine3D localToSceneTx = new Affine3D();
//        TransformHelper.apply(n.getLocalToSceneTransform(), localToSceneTx);
//        try {
//            Vec3d origin = pickRay.getOriginNoClone();
//            Vec3d direction = pickRay.getDirectionNoClone();
//            localToSceneTx.inverseTransform(origin, origin);
//            localToSceneTx.inverseDeltaTransform(direction, direction);
//        } catch (NoninvertibleTransformException e) {
//            return null;
//        }
//
//        // compute the intersection
//        final PickResultChooser result = new PickResultChooser();
//        NodeHelper.computeIntersects(this, pickRay, result);
//        if (result.getIntersectedNode() == n) {
//            return result.getIntersectedPoint();
//        }
//
//        // there is none, use point on projection plane instead
//        final Point3D ppIntersect = CameraAccess.getCameraAccess().pickProjectPlane(cam, pt.getX(), pt.getY());
//        return n.sceneToLocal(ppIntersect);
//    }
}
