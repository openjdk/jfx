/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGPerspectiveCamera;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;



/**
 * Specifies a perspective camera for rendering a scene.
 *
 * <p> This camera defines a viewing volume for a perspective projection;
 * a truncated right pyramid.
 * The {@code fieldOfView} value can be used to change viewing volume.
 * This camera is always located at center of the scene and looks along the
 * positive z-axis. The coordinate system defined by this camera has its
 * origin in the upper left corner of the panel with the Y-axis pointing
 * down and the Z axis pointing away from the viewer (into the screen). The
 * units are in pixel coordinates at the projection plane (Z=0).
 *
 * @since JavaFX 1.3
 */
public  class PerspectiveCamera extends Camera {   

    private boolean fixedEyePosition = false;

    /**
     * Specifies the field of view angle of the camera's projection plane,
     * measured in degrees.
     *
     * @defaultValue 30.0
     */
    private DoubleProperty fieldOfView;
    
    public final void setFieldOfView(double value){
        fieldOfViewProperty().set(value);
    }

    public final double getFieldOfView() {
        return fieldOfView == null ? 30 : fieldOfView.get();
    }

    public final DoubleProperty fieldOfViewProperty() {
        if (fieldOfView == null) {
            fieldOfView = new SimpleDoubleProperty(PerspectiveCamera.this, "fieldOfView", 30) {
                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_CAMERA);
                }
            };
        }
        return fieldOfView;
    }

    /**
     * Defines whether the {@code fieldOfView} property is to apply to the vertical 
     * dimension of the projection plane. If it is false, {@code fieldOfView} is to 
     * apply to the horizontal dimension of the projection plane.
     *
     * @defaultValue true
     * @since JavaFX 8
     */
    private BooleanProperty verticalFieldOfView;

    public final void setVerticalFieldOfView(boolean value) {
        verticalFieldOfViewProperty().set(value);
    }

    public final boolean isVerticalFieldOfView() {
        return verticalFieldOfView == null ? true : verticalFieldOfView.get();
    }

    public final BooleanProperty verticalFieldOfViewProperty() {
        if (verticalFieldOfView == null) {
            verticalFieldOfView = new SimpleBooleanProperty(PerspectiveCamera.this, "verticalFieldOfView", true) {
                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_CAMERA);
                }
            };
        }
        return verticalFieldOfView;
    }

    public PerspectiveCamera() {
    }

   /**
    * Construct a PerspectiveCamera that may fix its eye position at (0, 0, 0),
    * in its coordinate space, regardless in the change in the dimension
    * of the projection area (or Window resize) if {@code fixedEyePosition} is true. 
    *
    * @since JavaFX 8
    */
    public PerspectiveCamera(boolean fixedEyePosition) {
        this.fixedEyePosition = fixedEyePosition;
    }

    public final boolean isFixedEyePosition() {
        return fixedEyePosition;
    }

    @Override
    final PickRay computePickRay(double localX, double localY,
                           double viewWidth, double viewHeight,
                           PickRay pickRay) {
        if (pickRay == null) {
            pickRay = new PickRay();
        }

        Vec3d direction = pickRay.getDirectionNoClone();
        double halfViewWidth = viewWidth / 2.0;
        double halfViewHeight = viewHeight / 2.0;
        double halfViewDim = isVerticalFieldOfView() ? halfViewHeight: halfViewWidth;
        // Distance to projection plane from eye
        double distanceZ = halfViewDim / Math.tan(Math.toRadians(getFieldOfView()/2.0));

        direction.x = localX - halfViewWidth;
        direction.y = localY - halfViewHeight;
        direction.z = distanceZ;

        Vec3d eye = pickRay.getOriginNoClone();
        // Projection plane is at Z = 0, implies that eye must be located at:
        eye.set(halfViewWidth, halfViewHeight, -distanceZ);
        // set eye at center of viewport and move back so that projection plane
        // is at Z = 0
        if (pickRay.isParallel()) { pickRay.set(eye, direction); }

        if (getScene() != null) {
            pickRay.transform(getCameraTransform());
        }

        return pickRay;
    }

    @Override Camera copy() {
        PerspectiveCamera c = new PerspectiveCamera(fixedEyePosition);
        c.setNearClip(getNearClip());
        c.setFarClip(getFarClip());
        c.setFieldOfView(getFieldOfView());
        return c;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        PGPerspectiveCamera pgCamera = Toolkit.getToolkit().createPGPerspectiveCamera(fixedEyePosition);    
        pgCamera.setNearClip((float) getNearClip());
        pgCamera.setFarClip((float) getFarClip());
        pgCamera.setFieldOfView((float) getFieldOfView());
        return pgCamera;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();
//        System.err.println("XXXXXXXX PerspectiveCamera.impl_updatePG() XXXXXXXX");
        PGPerspectiveCamera pgPerspectiveCamera = (PGPerspectiveCamera)impl_getPGNode();
        if (impl_isDirty(DirtyBits.NODE_CAMERA)) {
            pgPerspectiveCamera.setVerticalFieldOfView(isVerticalFieldOfView());
            pgPerspectiveCamera.setFieldOfView((float) getFieldOfView());
        }
    }
}
