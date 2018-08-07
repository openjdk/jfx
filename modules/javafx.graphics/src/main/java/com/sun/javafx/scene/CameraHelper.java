/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Node;

/**
 * Used to access internal methods of Camera.
 */
public class CameraHelper extends NodeHelper {

    private static final CameraHelper theInstance;
    private static CameraAccessor cameraAccessor;

    static {
        theInstance = new CameraHelper();
        Utils.forceInit(Camera.class);
    }

    private static CameraHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(Camera camera) {
        setHelper(camera, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        throw new UnsupportedOperationException("Applications should not extend the Camera class directly.");
    }

    @Override
    protected void updatePeerImpl(Node node) {
        super.updatePeerImpl(node);
        cameraAccessor.doUpdatePeer(node);
    }

    @Override
    protected void markDirtyImpl(Node node, DirtyBits dirtyBit) {
        super.markDirtyImpl(node, dirtyBit);
        cameraAccessor.doMarkDirty(node, dirtyBit);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return cameraAccessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected boolean computeContainsImpl(Node node, double localX, double localY) {
        return cameraAccessor.doComputeContains(node, localX, localY);
    }

    public static Point2D project(Camera camera, Point3D p) {
        return cameraAccessor.project(camera, p);
    }

    public static Point2D pickNodeXYPlane(Camera camera, Node node, double x, double y) {
        return cameraAccessor.pickNodeXYPlane(camera, node, x, y);
    }

    public static Point3D pickProjectPlane(Camera camera, double x, double y) {
        return cameraAccessor.pickProjectPlane(camera, x, y);
    }

    public static void setCameraAccessor(final CameraAccessor newAccessor) {
        if (cameraAccessor != null) {
            throw new IllegalStateException();
        }

        cameraAccessor = newAccessor;
    }

    public interface CameraAccessor {
        void doMarkDirty(Node node, DirtyBits dirtyBit);
        void doUpdatePeer(Node node);
        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);
        boolean doComputeContains(Node node, double localX, double localY);
        Point2D project(Camera camera, Point3D p);
        Point2D pickNodeXYPlane(Camera camera, Node node, double x, double y);
        Point3D pickProjectPlane(Camera camera, double x, double y);
    }

}
