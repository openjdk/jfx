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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGCamera;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.transform.Transform;
import sun.util.logging.PlatformLogger;


/**
 * Base class for a camera used to render a scene.
 *
 * @since JavaFX 1.3
 */
public abstract class Camera extends Node {
   
    private Affine3D localToSceneTx = new Affine3D();

    protected Camera() {
        this.localToSceneTransformProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                isL2STxChanged = true;
                impl_markDirty(DirtyBits.NODE_CAMERA_TRANSFORM);
            }
        });
    }

    private boolean isL2STxChanged = true;
    private boolean isClipPlaneChanged = true;

    // NOTE: farClipInScene and nearClipInScene are valid only if there is no rotation
    private double farClipInScene;
    private double nearClipInScene;

    double getFarClipInScene() {
        updateMiscProperties();
        return farClipInScene;
    }

    double getNearClipInScene() {
        updateMiscProperties();
        return nearClipInScene;
    }

    /**
     * An affine transform that holds the computed scene-to-local transform.
     * It is used to convert node to camera coordinate when rotation is involved.
     */
    private Affine3D sceneToLocalTx = new Affine3D();

    Affine3D getSceneToLocalTransform() {
        updateMiscProperties();
        return sceneToLocalTx;
    }

    private void updateMiscProperties() {
        if (isL2STxChanged) {
            Transform localToSceneTransform = getLocalToSceneTransform();
            nearClipInScene = localToSceneTransform.transform(0, 0, getNearClip()).getZ();
            farClipInScene = localToSceneTransform.transform(0, 0, getFarClip()).getZ();
            
            sceneToLocalTx.setToIdentity();
            localToSceneTransform.impl_apply(sceneToLocalTx);
            try {
                sceneToLocalTx.invert();
            } catch (NoninvertibleTransformException ex) {
                String logname = Camera.class.getName();
                PlatformLogger.getLogger(logname).severe("updateMiscProperties", ex);
            }
            
            isL2STxChanged = isClipPlaneChanged = false;
        } else if (isClipPlaneChanged) {
            Transform localToSceneTransform = getLocalToSceneTransform();
            nearClipInScene = localToSceneTransform.transform(0, 0, getNearClip()).getZ();
            farClipInScene = localToSceneTransform.transform(0, 0, getFarClip()).getZ();
            
            isClipPlaneChanged = false;
        }
    }

    /**
     * Specifies the near clipping plane of this {@code Camera} in the local
     * coordinate system of this node.
     *
     * @defaultValue 0.1
     * @since JavaFX 8
     */
    private DoubleProperty nearClip;

    public final void setNearClip(double value){
        nearClipProperty().set(value);
    }

    public final double getNearClip() {
        return nearClip == null ? 0.1 : nearClip.get();
    }

    public final DoubleProperty nearClipProperty() {
        if (nearClip == null) {
            nearClip = new SimpleDoubleProperty(Camera.this, "nearClip", 0.1) {
                @Override
                protected void invalidated() {
                    isClipPlaneChanged = true;
                    impl_markDirty(DirtyBits.NODE_CAMERA);
                }
            };
        }
        return nearClip;
    }

    /**
     * Specifies the far clipping plane of this {@code Camera} in the local
     * coordinate system of this node.
     * <p>
     *
     * @defaultValue 100.0
     * @since JavaFX 8
     */
    private DoubleProperty farClip;

    public final void setFarClip(double value){
        farClipProperty().set(value);
    }

    public final double getFarClip() {
        return farClip == null ? 100.0 : farClip.get();
    }

    public final DoubleProperty farClipProperty() {
        if (farClip == null) {
            farClip = new SimpleDoubleProperty(Camera.this, "farClip", 100.0) {
                @Override
                protected void invalidated() {
                    isClipPlaneChanged = true;
                    impl_markDirty(DirtyBits.NODE_CAMERA);
                }
            };
        }
        return farClip;
    }
    
    PGCamera getPlatformCamera() {
        return (PGCamera) impl_getPGNode();
    }

    Camera copy() {
        return this;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();
        PGCamera pgCamera = (PGCamera)impl_getPGNode();
        if (impl_isDirty(DirtyBits.NODE_CAMERA)) {
            pgCamera.setNearClip((float) getNearClip());
            pgCamera.setFarClip((float) getFarClip());
        }
        if (impl_isDirty(DirtyBits.NODE_CAMERA_TRANSFORM)) {
            localToSceneTx.setToIdentity();
            getLocalToSceneTransform().impl_apply(localToSceneTx);
            // TODO: 3D - For now, we are treating the scene as world.
            // This may need to change for the fixed eye position case.
            pgCamera.setWorldTransform(localToSceneTx);
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        return new BoxBounds(0, 0, 0, 0, 0, 0);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        return false;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // NOTE: This can only be called during scenegraph sync time by Scene only.
    GeneralTransform3D computeProjViewTx(GeneralTransform3D tx, double sceneWidth, double sceneHeight) {
        // TODO: Need to cache value at PG update time
        PGCamera pgCamera = (PGCamera) impl_getPGNode();
        return pgCamera.getScreenProjViewTx(tx, sceneWidth, sceneHeight);
    }

    // NOTE: This can only be called during scenegraph sync time by Scene only.
    Rectangle getViewport(Rectangle vp) {
        // TODO: Need to cache value at PG update time
        PGCamera pgCamera = (PGCamera) impl_getPGNode();
        return pgCamera.getViewport(vp);
    }
}
