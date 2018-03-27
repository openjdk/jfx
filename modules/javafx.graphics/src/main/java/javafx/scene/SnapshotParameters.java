/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Transform;
import com.sun.javafx.logging.PlatformLogger;

/**
 * Parameters used to specify the rendering attributes for Node snapshot.
 * @since JavaFX 2.2
 */
public class SnapshotParameters {

    private boolean depthBuffer;
    private Camera camera;
    private Transform transform;
    private Paint fill;
    private Rectangle2D viewport;

    /**
     * Constructs a new SnapshotParameters object with default values for
     * all rendering attributes.
     */
    public SnapshotParameters() {
    }

    /**
     * Gets the current depthBuffer flag.
     *
     * @return the depthBuffer flag
     */
    public boolean isDepthBuffer() {
        return depthBuffer;
    }

    boolean isDepthBufferInternal() {
        if(!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            return false;
        }
        return depthBuffer;
    }

    /**
     * Sets the depthBuffer flag to the specified value.
     * The default value is false.
     *
     * Note that this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     *
     * @param depthBuffer the depthBuffer to set
     */
    public void setDepthBuffer(boolean depthBuffer) {
        if (depthBuffer && !Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = SnapshotParameters.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                    + "ConditionalFeature.SCENE3D");
        }
        this.depthBuffer = depthBuffer;
    }

    /**
     * Gets the current camera.
     *
     * @return the camera
     */
    public Camera getCamera() {
        return camera;
    }

    Camera defaultCamera;

    Camera getEffectiveCamera() {
        if (camera instanceof PerspectiveCamera
                && !Platform.isSupported(ConditionalFeature.SCENE3D)) {
            if (defaultCamera == null) {
                // According to Scene.doSnapshot, temporarily, it adjusts camera
                // viewport to the snapshot size. So, its viewport doesn't matter.
                defaultCamera = new ParallelCamera();
            }
            return defaultCamera;
        }
        return camera;
    }

    /**
     * Sets the camera to the specified value.
     * The default value is null, which means a ParallelCamera will be used.
     *
     * @param camera the camera to set
     */
    public void setCamera(Camera camera) {
        if (camera instanceof PerspectiveCamera
                && !Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = SnapshotParameters.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                    + "ConditionalFeature.SCENE3D");
        }
        this.camera = camera;
    }

    /**
     * Gets the current transform.
     *
     * @return the transform
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Sets the transform to the specified value. This transform is applied to
     * the node being rendered before any local transforms are applied.
     * A value of null indicates that the identity transform should be used.
     * The default value is null.
     *
     * @param transform the transform to set
     */
    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    /**
     * Gets the current fill.
     *
     * @return the fill
     */
    public Paint getFill() {
        return fill;
    }

    /**
     * Sets the fill to the specified value. This is used to fill the entire
     * image being rendered prior to rendering the node. A value of null
     * indicates that the color white should be used for the fill.
     * The default value is null.
     *
     * @param fill the fill to set
     */
    public void setFill(Paint fill) {
        this.fill = fill;
    }

    /**
     * Gets the current viewport
     *
     * @return the viewport
     */
    public Rectangle2D getViewport() {
        return viewport;
    }

    /**
     * Sets the viewport used for rendering.
     * The viewport is specified in the parent coordinate system of the
     * node being rendered. It is not transformed by the transform
     * of this SnapshotParameters.
     * If this viewport is non-null it is used instead of the bounds of the
     * node being rendered and specifies the source rectangle that will be
     * rendered into the image.
     * In this case, the upper-left pixel of the viewport will map to
     * the upper-left pixel (0,0)
     * in the rendered image.
     * If the viewport is null, then the entire area of the node defined
     * by its boundsInParent, after first applying the
     * transform of this SnapshotParameters, will be rendered.
     * The default value is null.
     *
     * @param viewport the viewport to set
     */
    public void setViewport(Rectangle2D viewport) {
        this.viewport = viewport;
    }

    /**
     * Returns a deep clone of this SnapshotParameters
     *
     * @return a clone
     */
    SnapshotParameters copy() {
        SnapshotParameters params = new SnapshotParameters();
        params.camera = camera == null ? null : camera.copy();
        params.depthBuffer = depthBuffer;
        params.fill = fill;
        params.viewport = viewport;
        params.transform = transform == null ? null : transform.clone();
        return params;
    }
}
