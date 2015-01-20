/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.input;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SubSceneHelper;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.input.PickResult;

/**
 * Used during 3D picking process to determine the best pick result.
 */
public class PickResultChooser {

    private double distance = Double.POSITIVE_INFINITY;
    private Node node;
    private int face = -1;
    private Point3D point;
    private Point3D normal;
    private Point2D texCoord;
    private boolean empty = true;
    private boolean closed = false;

    /**
     * Helper method for computing intersected point.
     * This method would fit better to PickRay but it cannot work with
     * Point3D (dependency issues).
     *
     * @param ray Pick ray used for picking
     * @param distance Distance measured in ray direction magnitudes
     * @return the intersection point
     */
    public static Point3D computePoint(PickRay ray, double distance) {
        Vec3d origin = ray.getOriginNoClone();
        Vec3d dir = ray.getDirectionNoClone();

        return new Point3D(
                origin.x + dir.x * distance,
                origin.y + dir.y * distance,
                origin.z + dir.z * distance);
    }

    /**
     * Converts the current content of this instance to the unmodifiable
     * PickResult.
     * @return PickResult containing the current values of this chooser
     */
    public PickResult toPickResult() {
        if (empty) {
            return null;
        }
        return new PickResult(node, point, distance, face, normal, texCoord);
    }

    /**
     * Returns true if the given distance is smaller than the distance stored
     * in this instance.
     * @param distance The distance to compare
     * @return true if the given distance is smaller
     */
    public boolean isCloser(double distance) {
        return distance < this.distance || empty;
    }

    /**
     * Returns true if there is no intersection stored in this instance.
     * @return true if there is no intersection stored in this instance.
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Returns true if this chooser has been closed. The chooser is closed when
     * it is clear that no further result can be accepted (due to disabled
     * depth testing).
     * @return true if this chooser has been closed.
     * @see close()
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Offers an intersection. If the given intersection is closer to the camera
     * than the current one (the distance is smaller), this instance is updated
     * to hold the given values.
     * @param node The intersected node
     * @param distance The intersected distance measured in pickRay direction magnitudes
     * @param face The intersected face
     * @param point The intersection point
     * @param texCoord The intersected texture coordinates
     * @return true if the offered intersection has been used
     */
    public boolean offer(Node node, double distance, int face, Point3D point, Point2D texCoord) {
        return processOffer(node, node, distance, point, face, normal, texCoord);
    }

    /**
     * Offers an intersection with a non-Shape3D object. This method is used
     * for 2D objects and for 3D objects with pickOnBounds==true; in both cases
     * face and texCoord make no sense.
     *
     * If the given intersection is closer to the camera
     * than the current one (the distance is smaller), this instance is updated
     * to hold the given values.
     * @param node The intersected node
     * @param distance The intersected distance measured in pickRay direction magnitudes
     * @param point The intersection point
     * @return true if the offered intersection has been used
     */
    public boolean offer(Node node, double distance, Point3D point) {
        return processOffer(node, node, distance, point, PickResult.FACE_UNDEFINED, null, null);
    }

    /**
     * Offers an intersection found inside a SubScene.
     * @param subScene SubScene where the result was picked
     * @param pickResult Picking result from the subScene
     * @param distance distance from the camera to the intersection point
     *                 with the subScene plane
     * @return true if the offered intersection has been used
     */
    public boolean offerSubScenePickResult(SubScene subScene, PickResult pickResult, double distance) {
        if (pickResult == null) {
            return false;
        }
        return processOffer(pickResult.getIntersectedNode(), subScene, distance,
                pickResult.getIntersectedPoint(), pickResult.getIntersectedFace(),
                pickResult.getIntersectedNormal(), pickResult.getIntersectedTexCoord());
    }

    /**
     * Process an offered intersection.
     * @see PickResultChooser#offer(javafx.scene.Node, double, int, javafx.geometry.Point3D, javafx.geometry.Point2D)
     * @see PickResultChooser#offer(javafx.scene.Node, double, javafx.geometry.Point3D)
     * @param node The intersected node
     * @param depthTestNode The node whose depthTest is considered. When
     *        processing subScene pick result we need to consider the inner
     *        picked node but subScene's depth test
     * @param distance The intersected distance measured in pickRay direction magnitudes
     * @param point The intersection point
     * @param face The intersected face
     * @param normal The intersected normal
     * @param texCoord The intersected texture coordinates
     * @return true if the offered intersection has been used
     */
    private boolean processOffer(Node node, Node depthTestNode, double distance,
            Point3D point, int face, Point3D normal, Point2D texCoord) {

        final SubScene subScene = NodeHelper.getSubScene(depthTestNode);
        final boolean hasDepthBuffer = Platform.isSupported(ConditionalFeature.SCENE3D)
                ? (subScene != null
                    ? SubSceneHelper.isDepthBuffer(subScene)
                    : depthTestNode.getScene().isDepthBuffer())
                : false;
        final boolean hasDepthTest =
                hasDepthBuffer && NodeHelper.isDerivedDepthTest(depthTestNode);

        boolean accepted = false;
        if ((empty || (hasDepthTest && distance < this.distance)) && !closed) {
            this.node = node;
            this.distance = distance;
            this.face = face;
            this.point = point;
            this.normal = normal;
            this.texCoord = texCoord;
            this.empty = false;
            accepted = true;
        }

        if (!hasDepthTest) {
            this.closed = true;
        }

        return accepted;
    }

    /**
     * Returns the intersected Node
     *
     * @return the picked Node
     */
    public final Node getIntersectedNode() {
        return node;
    }

    /**
     * Returns the intersected distance between camera position and the picked Node
     *
     * @return the distance from camera to the intersection
     */
    public final double getIntersectedDistance() {
        return distance;
    }

    /**
     * Returns the intersected face of the picked Node
     *
     * @return the picked face
     */
    public final int getIntersectedFace() {
        return face;
     }

     /**
     * Return the intersected point in local coordinate of the picked Node
     *
     * @return new Point3D presenting the intersected point
     */
    public final Point3D getIntersectedPoint() {
        return point;
    }

     /**
     * Return the intersected normal in local coordinate of the picked Node
     *
     * @return new Point3D presenting the intersected normal
     *
     */
    public final Point3D getIntersectedNormal() {
        return normal;
    }

    /**
     * Return the intersected texture coordinates of the picked Node
     *
     * return new Point2D presenting the intersected TexCoord
     */
    public final javafx.geometry.Point2D getIntersectedTexCoord() {
        return texCoord;
    }
}
