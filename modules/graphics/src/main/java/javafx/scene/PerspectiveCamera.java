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
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGPerspectiveCamera;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import sun.util.logging.PlatformLogger;



/**
 * Specifies a perspective camera for rendering a scene.
 *
 * <p> This camera defines a viewing volume for a perspective projection;
 * a truncated right pyramid.
 * The {@code fieldOfView} value can be used to change viewing volume.
 * This camera is always located at center of the scene and looks along the
 * positive z-axis. The coordinate system defined by this camera has its
 * origin in the upper left corner of the panel with the Y-axis pointing
 * down and the Z axis pointing away from the viewer (into the screen). 
 * 
 * <p> In the default camera, where fixedEyeAtCameraZero is false, the Z value
 * of the eye position is adjusted in Z such that the projection matrix generated
 * using the specified {@code fieldOfView} will produce units at 
 * Z = 0 (the projection plane), in device-independent pixels, matches that of
 * the ParallelCamera.
 * When the Scene is resized,
 * the objects in the scene at the projection plane (Z = 0) will stay the same size,
 * but more or less content of the scene is viewable.
 * 
 * <p> If fixedEyeAtCameraZero is true, the eye position is fixed at (0, 0, 0)
 * in the local coordinates of the camera. The projection matrix is generated
 * using the specified {@code fieldOfView} and the projection volume is mapped
 * onto the viewport (window) such that it is stretched over more or fewer
 * device-independent pixels at the projection plane.
 * When the Scene is resized,
 * the objects in the scene will shrink or grow proportionally,
 * but the visible portion of the content is unchanged.
 * 
 * <p> We recommend setting fixedEyeAtCameraZero to true if you are going to
 * transform (move) the camera. Transforming the camera when fixedEyeAtCameraZero
 * is set to false may lead to results that are not intuitive.
 * 
 * <p> Note that this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
 * for more information.
 * 
 * @since JavaFX 2.0
 */
public class PerspectiveCamera extends Camera {

    private boolean fixedEyeAtCameraZero = false;

    // Lookat transform for legacy case
    private static final Affine3D LOOK_AT_TX = new Affine3D();

    // Lookat transform for fixedEyeAtCameraZero case
    private static final Affine3D LOOK_AT_TX_FIXED_EYE = new Affine3D();

    static {
        // Compute the legacy look at matrix such that the zero point ends up at
        // the z=-1 plane.
        LOOK_AT_TX.setToTranslation(0, 0, -1);
        // Y-axis pointing down
        LOOK_AT_TX.rotate(Math.PI, 1, 0, 0);

        // Compute the fixed eye at (0, 0, 0) look at matrix such that the zero point
        // ends up at the z=0 plane and Y-axis pointing down
        LOOK_AT_TX_FIXED_EYE.rotate(Math.PI, 1, 0, 0);
    }

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
     * @since JavaFX 8.0
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
        this(false);
    }

    /**
     * Constructs a PerspectiveCamera with the specified fixedEyeAtCameraZero flag.
     *
     * <p> In the default camera, where fixedEyeAtCameraZero is false, the Z value of 
     * the eye position is adjusted in Z such that the projection matrix generated
     * using the specified {@code fieldOfView} will produce units at
     * Z = 0 (the projection plane), in device-independent pixels, matches that of
     * the ParallelCamera.
     * When the Scene is resized,
     * the objects in the scene at the projection plane (Z = 0) will stay the same size,
     * but more or less content of the scene is viewable.
     *
     * <p> If fixedEyeAtCameraZero is true, the eye position is fixed at (0, 0, 0)
     * in the local coordinates of the camera. The projection matrix is generated
     * using the specified {@code fieldOfView} and the projection volume is mapped
     * onto the viewport (window) such that it is stretched over more or fewer
     * device-independent pixels at the projection plane.
     * When the Scene is resized,
     * the objects in the scene will shrink or grow proportionally,
     * but the visible portion of the content is unchanged.
     *
     * <p> We recommend setting fixedEyeAtCameraZero to true if you are going to
     * transform (move) the camera. Transforming the camera when fixedEyeAtCameraZero
     * is set to false may lead to results that are not intuitive.
     * 
     * @since JavaFX 8.0
     */ 
    public PerspectiveCamera(boolean fixedEyeAtCameraZero) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = PerspectiveCamera.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                    + "ConditionalFeature.SCENE3D");
        }
        this.fixedEyeAtCameraZero = fixedEyeAtCameraZero;
    }

    public final boolean isFixedEyeAtCameraZero() {
        return fixedEyeAtCameraZero;
    }

    @Override
    final PickRay computePickRay(double x, double y, PickRay pickRay) {

        return PickRay.computePerspectivePickRay(x, y, fixedEyeAtCameraZero,
                getViewWidth(), getViewHeight(),
                Math.toRadians(getFieldOfView()), isVerticalFieldOfView(),
                getCameraTransform(),
                getNearClip(), getFarClip(),
                pickRay);
    }

    @Override Camera copy() {
        PerspectiveCamera c = new PerspectiveCamera(fixedEyeAtCameraZero);
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
    protected NGNode impl_createPeer() {
        NGPerspectiveCamera peer = new NGPerspectiveCamera(fixedEyeAtCameraZero);
        peer.setNearClip((float) getNearClip());
        peer.setFarClip((float) getFarClip());
        peer.setFieldOfView((float) getFieldOfView());
        return peer;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePeer() {
        super.impl_updatePeer();
        NGPerspectiveCamera pgPerspectiveCamera = impl_getPeer();
        if (impl_isDirty(DirtyBits.NODE_CAMERA)) {
            pgPerspectiveCamera.setVerticalFieldOfView(isVerticalFieldOfView());
            pgPerspectiveCamera.setFieldOfView((float) getFieldOfView());
        }
    }

    @Override
    void computeProjectionTransform(GeneralTransform3D proj) {
        proj.perspective(isVerticalFieldOfView(), Math.toRadians(getFieldOfView()),
                getViewWidth() / getViewHeight(), getNearClip(), getFarClip());
    }

    @Override
    void computeViewTransform(Affine3D view) {

        // In the case of fixedEyeAtCameraZero the camera position is (0,0,0) in
        // local coord. of the camera node. In non-fixed eye case, the camera
        // position is (w/2, h/2, h/2/tan) in local coord. of the camera.
        if (isFixedEyeAtCameraZero()) {
            view.setTransform(LOOK_AT_TX_FIXED_EYE);
        } else {
            final double viewWidth = getViewWidth();
            final double viewHeight = getViewHeight();
            final boolean verticalFOV = isVerticalFieldOfView();

            final double aspect = viewWidth / viewHeight;
            final double tanOfHalfFOV = Math.tan(Math.toRadians(getFieldOfView()) / 2.0);

            // Translate the zero point to the upper-left corner
            final double xOffset = -tanOfHalfFOV * (verticalFOV ? aspect : 1.0);
            final double yOffset = tanOfHalfFOV * (verticalFOV ? 1.0 : 1.0 / aspect);

            // Compute scale factor as 2/viewport.width or height, after adjusting for fov
            final double scale = 2.0 * tanOfHalfFOV /
                    (verticalFOV ? viewHeight : viewWidth);

            view.setToTranslation(xOffset, yOffset, 0.0);
            view.concatenate(LOOK_AT_TX);
            view.scale(scale, scale, scale);
        }
    }

    @Override
    Vec3d computePosition(Vec3d position) {
        if (position == null) {
            position = new Vec3d();
        }

        if (fixedEyeAtCameraZero) {
            position.set(0.0, 0.0, 0.0);
        } else {
            final double halfViewWidth = getViewWidth() / 2.0;
            final double halfViewHeight = getViewHeight() / 2.0;
            final double halfViewDim = isVerticalFieldOfView()
                    ? halfViewHeight : halfViewWidth;
            final double distanceZ = halfViewDim
                    / Math.tan(Math.toRadians(getFieldOfView() / 2.0));

            position.set(halfViewWidth, halfViewHeight, -distanceZ);
        }
        return position;
    }
}
