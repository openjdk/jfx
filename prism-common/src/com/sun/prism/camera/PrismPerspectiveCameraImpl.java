/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.camera;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import sun.util.logging.PlatformLogger;

/**
 * Specifies a Swing-coordinates camera, suitable for mixing with Swing and
 * the 2D scene graph. The coordinate system defined by this camera has its
 * origin in the upper left corner of the panel with the Y-axis pointing
 * down and the Z axis pointing away from the viewer (into the screen). The
 * units are in pixel coordinates at the projection plane (Z=0),
 * regardless of the size of the panel.
 * The viewing transform is defined by specifying the zero point, the viewing
 * direction, and the up vector.
 * This resulting transform is used as the view portion of the ModelView matrix.
 * The projection transform a fixed perspective transform. The 3D viewport is
 * set to the bounds of the panel.
 */
public class PrismPerspectiveCameraImpl extends PrismCameraImpl {

    private final boolean fixedEyePosition;

    private double fov;
    private boolean verticalFieldOfView;

    private GeneralTransform3D invProjTx = new GeneralTransform3D();
    private Affine3D invViewTx = new Affine3D();

    private static final Affine3D lookAtTx = new Affine3D();
    static {
        // Compute the lookAt matrix such that the zero point ends up at
        // the z=-1 plane.
        lookAtTx.setToTranslation(0, 0, -1);
        // Y-axis pointing down
        lookAtTx.rotate(Math.PI, 1, 0, 0);
//        System.err.println("lookAtTx = " + lookAtTx);        
    }
    
    /**
     * Constructs a camera object.
     */
    public PrismPerspectiveCameraImpl(boolean fixedEyePosition) {
        this.fixedEyePosition = fixedEyePosition;
    }

    @Override
    protected void computeProjection(GeneralTransform3D proj) {
        proj.perspective(verticalFieldOfView, fov, aspect, getNearClip(), getFarClip());
        invProjTx.set(proj);
        invProjTx.invert();
    }

    @Override
    protected void computeViewTransform(Affine3D view) {
        double halfW = viewport.width / 2.0;
        double halfH = viewport.height / 2.0;
        double tanOfHalfFOV = Math.tan(fov / 2.0);
        double zDistance;
        double widthOrHeight;

        // In the case of fixedEyePosition the camera position is (0,0,0) in
        // local coord. of the camera node. In non-fixed eye case, the camera
        // position is (w/2, h/2, h/2/tan) in local coord. of the camera.
        if (fixedEyePosition) {
            localPosition.set(0, 0, 0);
            view.setTransform(lookAtTx);
        } else {
            // Compute local position
            if (verticalFieldOfView) {
                zDistance = halfH / tanOfHalfFOV;
                widthOrHeight = viewport.height;
            } else {
                zDistance = halfW / tanOfHalfFOV;
                widthOrHeight = viewport.width;
            }
            localPosition.set(halfW, halfH, -zDistance);
//            System.err.println("localPosition = " + localPosition);

            // Translate the zero point to the upper-left corner
            double xOffset = -tanOfHalfFOV * (verticalFieldOfView ? aspect : 1);
            double yOffset = tanOfHalfFOV * (verticalFieldOfView ? 1 : 1.0/aspect);

            view.setToTranslation(xOffset, yOffset, 0.0);
//            System.err.println("(1) View Matrix = " + view);

            view.concatenate(lookAtTx);
//            System.err.println("(2) View Matrix = " + view);

            // Compute scale factor as 2/viewport.width or height, after adjusting for fov
            double scale = 2.0 * tanOfHalfFOV / widthOrHeight;
            Affine3D scaleTx = new Affine3D();
            scaleTx.setToScale(scale, scale, scale);
            view.concatenate(scaleTx);
//            System.err.println("(3) View Matrix = " + view);
        }
        invViewTx.setTransform(view);
        try {
            // Invert the view transform.
            // NOTE: if we decide to define picking in Ec rather
            // than Wc then this step becomes unnecessary; instead it will
            // be handled as part of the ModelView transform
            invViewTx.invert();
        } catch (NoninvertibleTransformException ex) {
            String logname = PrismPerspectiveCameraImpl.class.getName();
            PlatformLogger.getLogger(logname).severe("computeViewTransform", ex);
        }
    }

    @Override
    public PickRay computePickRay(float x, float y, PickRay pickRay) {
        if (pickRay == null) {
            pickRay = new PickRay();
        }
        Vec3d eye = pickRay.getOriginNoClone();
        Vec3d dir = pickRay.getDirectionNoClone();

        double xWin = x;
        double yWin = y;
        double xCc = (xWin / viewport.width) * 2.0 - 1.0;
        double yCc = (yWin / viewport.height) * -2.0 + 1.0;

        // Perspective projection
        double zCc = projTx.computeClipZCoord();
        dir.set(xCc, yCc, zCc);

        // Transform the Cc point into Ec via the inverse projection transform
        invProjTx.transform(dir, dir);
        invViewTx.transform(dir, dir);

        eye.set(0.0, 0.0, 0.0);
        invViewTx.transform(eye, eye);

        dir.sub(eye);

        return pickRay;
    }

    public void setFieldOfView(float fieldOfView) {
        this.fov = Math.toRadians(fieldOfView);
        projViewTxDirty = true;
    }

    public void setVerticalFieldOfView(boolean verticalFieldOfView) {
        this.verticalFieldOfView = verticalFieldOfView;
        projViewTxDirty = true;
    }

    public boolean isVerticalFieldOfView() {
        return verticalFieldOfView;
    }

    public boolean isFixedEyePosition() {
        return fixedEyePosition;
    }
}
