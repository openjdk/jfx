/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;

import com.sun.javafx.geom.CameraImpl;
import com.sun.javafx.geom.PerspectiveCameraImpl;
import com.sun.javafx.tk.Toolkit;



/**
 * Specifies a perspective camera for rendering a scene.
 *
 * <p> This camera defines a viewing volume for a perspective projection;
 * a truncated right pyramid.
 * The {@code fieldOfView} value can be used to change viewing volume.
 * This camera is always located at center of the window and looks along the
 * positive z-axis. The coordinate system defined by this camera has its
 * origin in the upper left corner of the panel with the Y-axis pointing
 * down and the Z axis pointing away from the viewer (into the screen). The
 * units are in pixel coordinates at the projection plane (Z=0).
 *
 * @since JavaFX 1.3
 */
public  class PerspectiveCamera extends Camera {   
    /**
     * Specifies the vertical angle of the camera's projection.
     *
     * @defaultvalue 30.0
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
            fieldOfView = new DoublePropertyBase(30) {

                @Override
                protected void invalidated() {
                    impl_markDirty();
                }

                @Override
                public Object getBean() {
                    return PerspectiveCamera.this;
                }

                @Override
                public String getName() {
                    return "fieldOfView";
                }
            };
        }
        return fieldOfView;
    }

    public PerspectiveCamera() {
        impl_markDirty();
    }

    @Override
    CameraImpl createPlatformCamera() {
        return Toolkit.getToolkit().createPerspectiveCamera();
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_update() {
        if (impl_isDirty()) {
            PerspectiveCameraImpl perspectiveCameraImpl = (PerspectiveCameraImpl) getPlatformCamera();
            perspectiveCameraImpl.setFieldOfView((float)getFieldOfView());
            impl_clearDirty();
        }
    }
}
