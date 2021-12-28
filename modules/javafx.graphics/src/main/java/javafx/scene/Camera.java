/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.Transform;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.scene.CameraHelper;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.transform.TransformHelper;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.logging.PlatformLogger;


/**
 * Base class for a camera used to render a scene.
 * The camera defines the mapping of the scene coordinate space onto the window.
 * Camera is an abstract class with two concrete subclasses:
 * {@link ParallelCamera} and {@link PerspectiveCamera}.
 *
 * <p>
 * The default camera is positioned in the scene such that its projection plane
 * in the scene coordinate space is at Z = 0, and it is looking into the screen in
 * the positive Z direction. The distance in Z from the camera to the projection
 * plane is determined by the {@code width} and {@code height} of the Scene to
 * which it is attached and its {@code fieldOfView}.
 * </p>
 *
 * <p>
 * The {@code nearClip} and {@code farClip} of this camera are specified in the
 * eye coordinate space. This space is defined such that the eye is at its
 * origin and the projection plane is one unit in front of the eye in the
 * positive Z direction.
 * </p>
 *
 * <p>
 * The following pseudo code is the math used to compute the near and far clip
 * distances in the scene coordinate space:
 * </p>
 *
 * <pre>
 * final double tanOfHalfFOV = Math.tan(Math.toRadians(FOV) / 2.0);
 * final double halfHeight = HEIGHT / 2;
 * final double focalLenght = halfHeight / tanOfHalfFOV;
 * final double eyePositionZ = -1.0 * focalLenght;
 * final double nearClipDistance = focalLenght * NEAR + eyePositionZ;
 * final double farClipDistance = focalLenght * FAR + eyePositionZ;
 * </pre>
 *
 * <p>
 * where {@code FOV} is {@code fieldOfView} in degrees,
 * {@code NEAR} is {@code nearClip} specified in eye space,
 * and {@code FAR} is {@code farClip} specified in eye space.
 * </p>
 *
 * <p>
 * Note: Since the ParallelCamera class has no {@code fieldOfView} property, a
 * 30 degrees vertical field of view is used.
 * </p>
 *
 * <p>
 * Note: For the case of a PerspectiveCamera where the fixedEyeAtCameraZero
 * attribute is true, the scene coordinate space is normalized in order to fit
 * into the view frustum (see {@link PerspectiveCamera} for more details). In
 * this mode, the eye coordinate space is the same as this Camera node's local
 * coordinate space. Hence the conversion formula mentioned above is not used.
 * </p>
 *
 * <p>
 * An application should not extend the Camera class directly. Doing so may lead to
 * an UnsupportedOperationException being thrown.
 * </p>
 *
 * @since JavaFX 2.0
 */
public abstract class Camera extends Node {
    static {
         // This is used by classes in different packages to get access to
         // private and package private methods.
        CameraHelper.setCameraAccessor(new CameraHelper.CameraAccessor() {
            @Override
            public void doMarkDirty(Node node, DirtyBits dirtyBit) {
                ((Camera) node).doMarkDirty(dirtyBit);
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Camera) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Camera) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((Camera) node).doComputeContains(localX, localY);
            }

            @Override
            public Point2D project(Camera camera, Point3D p) {
                return camera.project(p);
            }

            @Override
            public Point2D pickNodeXYPlane(Camera camera, Node node, double x, double y) {
                return camera.pickNodeXYPlane(node, x, y);
            }

            @Override
            public Point3D pickProjectPlane(Camera camera, double x, double y) {
                return camera.pickProjectPlane(x, y);
            }
        });
    }

    private Affine3D localToSceneTx = new Affine3D();

    {
        // To initialize the class helper at the begining each constructor of this class
        CameraHelper.initHelper(this);
    }

    /**
     * Constructor for subclasses to call.
     */
    protected Camera() {
        InvalidationListener dirtyTransformListener = observable
                -> NodeHelper.markDirty(this, DirtyBits.NODE_CAMERA_TRANSFORM);

        this.localToSceneTransformProperty().addListener(dirtyTransformListener);
        // if camera is removed from scene it needs to stop using its transforms
        this.sceneProperty().addListener(dirtyTransformListener);
    }

    // NOTE: farClipInScene and nearClipInScene are valid only if there is no rotation
    private double farClipInScene;
    private double nearClipInScene;

    // only one of them can be non-null at a time
    private Scene ownerScene = null;
    private SubScene ownerSubScene = null;

    private GeneralTransform3D projViewTx = new GeneralTransform3D();
    private GeneralTransform3D projTx = new GeneralTransform3D();
    private Affine3D viewTx = new Affine3D();
    private double viewWidth = 1.0;
    private double viewHeight = 1.0;
    private Vec3d position = new Vec3d();

    private boolean clipInSceneValid = false;
    private boolean projViewTxValid = false;
    private boolean localToSceneValid = false;
    private boolean sceneToLocalValid = false;

    double getFarClipInScene() {
        updateClipPlane();
        return farClipInScene;
    }

    double getNearClipInScene() {
        updateClipPlane();
        return nearClipInScene;
    }

    private void updateClipPlane() {
        if (!clipInSceneValid) {
            final Transform localToSceneTransform = getLocalToSceneTransform();
            nearClipInScene = localToSceneTransform.transform(0, 0, getNearClip()).getZ();
            farClipInScene = localToSceneTransform.transform(0, 0, getFarClip()).getZ();
            clipInSceneValid = true;
        }
    }

    /**
     * An affine transform that holds the computed scene-to-local transform.
     * It is used to convert node to camera coordinate when rotation is involved.
     */
    private Affine3D sceneToLocalTx = new Affine3D();

    Affine3D getSceneToLocalTransform() {
        if (!sceneToLocalValid) {
            sceneToLocalTx.setTransform(getCameraTransform());
            try {
                sceneToLocalTx.invert();
            } catch (NoninvertibleTransformException ex) {
                String logname = Camera.class.getName();
                PlatformLogger.getLogger(logname).severe("getSceneToLocalTransform", ex);
                sceneToLocalTx.setToIdentity();
            }
            sceneToLocalValid = true;
        }

        return sceneToLocalTx;
    }

    /**
     * Specifies the distance from the eye of the near clipping plane of
     * this {@code Camera} in the eye coordinate space.
     * Objects closer to the eye than {@code nearClip} are not drawn.
     * {@code nearClip} is specified as a value greater than zero. A value less
     * than or equal to zero is treated as a very small positive number.
     *
     * @defaultValue 0.1
     * @since JavaFX 8.0
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
                    clipInSceneValid = false;
                    NodeHelper.markDirty(Camera.this, DirtyBits.NODE_CAMERA);
                }
            };
        }
        return nearClip;
    }

    /**
     * Specifies the distance from the eye of the far clipping plane of
     * this {@code Camera} in the eye coordinate space.
     * Objects farther away from the eye than {@code farClip} are not
     * drawn.
     * {@code farClip} is specified as a value greater than {@code nearClip}.
     * A value less than or equal to {@code nearClip} is treated as
     * {@code nearClip} plus a very small positive number.
     *
     * @defaultValue 100.0
     * @since JavaFX 8.0
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
                    clipInSceneValid = false;
                    NodeHelper.markDirty(Camera.this, DirtyBits.NODE_CAMERA);
                }
            };
        }
        return farClip;
    }

    Camera copy() {
        return this;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        NGCamera peer = getPeer();
        if (!NodeHelper.isDirtyEmpty(this)) {
            if (isDirty(DirtyBits.NODE_CAMERA)) {
                peer.setNearClip((float) getNearClip());
                peer.setFarClip((float) getFarClip());
                peer.setViewWidth(getViewWidth());
                peer.setViewHeight(getViewHeight());
            }
            if (isDirty(DirtyBits.NODE_CAMERA_TRANSFORM)) {
                // TODO: 3D - For now, we are treating the scene as world.
                // This may need to change for the fixed eye position case.
                peer.setWorldTransform(getCameraTransform());
            }

            peer.setProjViewTransform(getProjViewTransform());

            position = computePosition(position);
            getCameraTransform().transform(position, position);
            peer.setPosition(position);
        }
    }

    void setViewWidth(double width) {
        this.viewWidth = width;
        NodeHelper.markDirty(this, DirtyBits.NODE_CAMERA);
    }

    double getViewWidth() {
        return viewWidth;
    }

    void setViewHeight(double height) {
        this.viewHeight = height;
        NodeHelper.markDirty(this, DirtyBits.NODE_CAMERA);
    }

    double getViewHeight() {
        return viewHeight;
    }

    void setOwnerScene(Scene s) {
        if (s == null) {
            ownerScene = null;
        } else if (s != ownerScene) {
            if (ownerScene != null || ownerSubScene != null) {
                throw new IllegalArgumentException(this
                        + "is already set as camera in other scene or subscene");
            }
            ownerScene = s;
            markOwnerDirty();
        }
    }

    void setOwnerSubScene(SubScene s) {
        if (s == null) {
            ownerSubScene = null;
        } else if (s != ownerSubScene) {
            if (ownerScene != null || ownerSubScene != null) {
                throw new IllegalArgumentException(this
                        + "is already set as camera in other scene or subscene");
            }
            ownerSubScene = s;
            markOwnerDirty();
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doMarkDirty(DirtyBits dirtyBit) {
        if (dirtyBit == DirtyBits.NODE_CAMERA_TRANSFORM) {
            localToSceneValid = false;
            sceneToLocalValid = false;
            clipInSceneValid = false;
            projViewTxValid = false;
        } else if (dirtyBit == DirtyBits.NODE_CAMERA) {
            projViewTxValid = false;
        }
        markOwnerDirty();
    }

    private void markOwnerDirty() {
        // if the camera is part of the scene/subScene, we will need to notify
        // the owner to mark the entire scene/subScene dirty.
        if (ownerScene != null) {
            ownerScene.markCameraDirty();
        }
        if (ownerSubScene != null) {
            ownerSubScene.markContentDirty();
        }
    }

    /**
     * Returns the local-to-scene transform of this camera.
     * Package private, for use in our internal subclasses.
     * Returns directly the internal instance, it must not be altered.
     */
    Affine3D getCameraTransform() {
        if (!localToSceneValid) {
            localToSceneTx.setToIdentity();
            TransformHelper.apply(getLocalToSceneTransform(), localToSceneTx);
            localToSceneValid = true;
        }
        return localToSceneTx;
    }

    abstract void computeProjectionTransform(GeneralTransform3D proj);
    abstract void computeViewTransform(Affine3D view);

    /**
     * Returns the projView transform of this camera.
     * Package private, for internal use.
     * Returns directly the internal instance, it must not be altered.
     */
    GeneralTransform3D getProjViewTransform() {
        if (!projViewTxValid) {
            computeProjectionTransform(projTx);
            computeViewTransform(viewTx);

            projViewTx.set(projTx);
            projViewTx.mul(viewTx);
            projViewTx.mul(getSceneToLocalTransform());

            projViewTxValid = true;
        }

        return projViewTx;
    }

    /**
     * Transforms the given 3D point to the flat projected coordinates.
     */
    private Point2D project(Point3D p) {

        final Vec3d vec = getProjViewTransform().transform(new Vec3d(
                p.getX(), p.getY(), p.getZ()));

        final double halfViewWidth = getViewWidth() / 2.0;
        final double halfViewHeight = getViewHeight() / 2.0;

        return new Point2D(
                halfViewWidth * (1 + vec.x),
                halfViewHeight * (1 - vec.y));
    }

    /**
     * Computes intersection point of the pick ray cast by the given coordinates
     * and the node's local XY plane.
     */
    private Point2D pickNodeXYPlane(Node node, double x, double y) {
        final PickRay ray = computePickRay(x, y, null);

        final Affine3D localToScene = new Affine3D();
        TransformHelper.apply(node.getLocalToSceneTransform(), localToScene);

        final Vec3d o = ray.getOriginNoClone();
        final Vec3d d = ray.getDirectionNoClone();

        try {
            localToScene.inverseTransform(o, o);
            localToScene.inverseDeltaTransform(d, d);
        } catch (NoninvertibleTransformException e) {
            return null;
        }

        if (almostZero(d.z)) {
            return null;
        }

        final double t = -o.z / d.z;
        return new Point2D(o.x + (d.x * t), o.y + (d.y * t));
    }

    /**
     * Computes intersection point of the pick ray cast by the given coordinates
     * and the projection plane.
     */
    Point3D pickProjectPlane(double x, double y) {
        final PickRay ray = computePickRay(x, y, null);
        final Vec3d p = new Vec3d();
        p.add(ray.getOriginNoClone(), ray.getDirectionNoClone());

        return new Point3D(p.x, p.y, p.z);
    }


    /**
     * Computes pick ray for the content rendered by this camera.
     * @param x horizontal coordinate of the pick ray in the projected
     *               view, usually mouse cursor position
     * @param y vertical coordinate of the pick ray in the projected
     *               view, usually mouse cursor position
     * @param pickRay pick ray to be reused. New instance is created in case
     *                of null.
     * @return The PickRay instance computed based on this camera and the given
     *         arguments.
     */
    abstract PickRay computePickRay(double x, double y, PickRay pickRay);

    /**
     * Computes local position of the camera in the scene.
     * @param position Position to be reused. New instance is created in case
     *                 of null.
     * @return The position of the camera in the scene in camera local coords
     */
    abstract Vec3d computePosition(Vec3d position);

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        return new BoxBounds(0, 0, 0, 0, 0, 0);
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        return false;
    }

}
