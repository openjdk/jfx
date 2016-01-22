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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.PickRay;

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
public class NGPerspectiveCamera extends NGCamera {
    private final boolean fixedEyeAtCameraZero;
    private double fovrad;
    private boolean verticalFieldOfView;

    public NGPerspectiveCamera(boolean fixedEyeAtCameraZero) {
        this.fixedEyeAtCameraZero = fixedEyeAtCameraZero;
    }

    public void setFieldOfView(float fieldOfViewDegrees) {
        this.fovrad = Math.toRadians(fieldOfViewDegrees);
    }

    public void setVerticalFieldOfView(boolean verticalFieldOfView) {
        this.verticalFieldOfView = verticalFieldOfView;
    }

    @Override
    public PickRay computePickRay(float x, float y, PickRay pickRay) {
        return PickRay.computePerspectivePickRay(x, y, fixedEyeAtCameraZero,
                viewWidth, viewHeight, fovrad, verticalFieldOfView, worldTransform,
                zNear, zFar,
                pickRay);
    }
}
