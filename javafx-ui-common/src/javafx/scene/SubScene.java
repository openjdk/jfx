/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.CssFlags;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.SubSceneAccess;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.sg.PGLightBase;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGSubScene;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point3D;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Paint;

/**
 * The {@code SubScene} class is the container for content in a scene graph.
 *
 * @since JavaFX 8
 */
public class SubScene extends Node {

    /**
     * Creates a SubScene for a specific root Node with a specific size.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     *
     * @throws IllegalStateException if this constructor is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if root is null
     */
    public SubScene(Parent root, double width, double height) {
        setRoot(root);
        setWidth(width);
        setHeight(height);
    }

    /**
     * Constructs a SubScene consisting of a root, with a dimension of width and
     * height, specifies whether a depth buffer is created for this scene and
     * specifies whether scene anti-aliasing is requested.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     * @param depthBuffer The depth buffer flag
     * @param antiAliasing The sub-scene anti-aliasing flag
     * <p>
     * The depthBuffer and antiAliasing flags are conditional feature and the default
     * value for both are false. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @throws IllegalStateException if this constructor is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if root is null
     *
     * @see javafx.scene.Node#setDepthTest(DepthTest)
     */
    public SubScene(Parent root, double width, double height,
            boolean depthBuffer, boolean antiAliasing) {
        this(root, width, height);
        this.depthBuffer = depthBuffer;
        //TODO: 3D - verify that depthBuffer is working correctly
        //TODO: 3D - complete antiAliasing
    }

    /**
     * Return true if this {@code SubScene} is anti-aliased otherwise false.
     */
    public boolean isAntiAliasing() {
        throw new UnsupportedOperationException("Unsupported --- *** isAntiAliasing method ***");
    }

    private boolean depthBuffer = false;

    /**
     * Defines the root {@code Node} of the SubScene scene graph.
     * If a {@code Group} is used as the root, the
     * contents of the scene graph will be clipped by the SubScene's width and height.
     *
     * SubScene doesn't accept null root.
     *
     */
    private ObjectProperty<Parent> root;

    public final void setRoot(Parent value) {
        rootProperty().set(value);
    }

    public final Parent getRoot() {
        return root == null ? null : root.get();
    }

    public final ObjectProperty<Parent> rootProperty() {
        if (root == null) {
            root = new ObjectPropertyBase<Parent>() {
                private Parent oldRoot;

                private void forceUnbind() {
                    System.err.println("Unbinding illegal root.");
                    unbind();
                }

                @Override
                protected void invalidated() {
                    Parent _value = get();

                    if (_value == null) {
                        if (isBound()) { forceUnbind(); }
                        throw new NullPointerException("Scene's root cannot be null");
                    }
                    if (_value.getParent() != null) {
                        if (isBound()) { forceUnbind(); }
                        throw new IllegalArgumentException(_value +
                                "is already inside a scene-graph and cannot be set as root");
                    }
                    if (_value.getClipParent() != null) {
                        if (isBound()) forceUnbind();
                        throw new IllegalArgumentException(_value +
                                "is set as a clip on another node, so cannot be set as root");
                    }
                    if ((_value.getScene() != null &&
                            _value.getScene().getRoot() == _value) ||
                            (_value.getSubScene() != null &&
                            _value.getSubScene().getRoot() == _value &&
                            _value.getSubScene() != SubScene.this))
                    {
                        if (isBound()) { forceUnbind(); }
                        throw new IllegalArgumentException(_value +
                                "is already set as root of another scene or subScene");
                    }

                    // disabled and isTreeVisible properties are inherrited
                    _value.setTreeVisible(impl_isTreeVisible());
                    _value.setDisabled(isDisabled());

                    if (oldRoot != null) {
                        oldRoot.setScenes(null, null);
                        oldRoot.setImpl_traversalEngine(null);
                    }
                    oldRoot = _value;
                    if (_value.getImpl_traversalEngine() == null) {
                        _value.setImpl_traversalEngine(new TraversalEngine(_value, true));
                    }
                    _value.getStyleClass().add(0, "root");
                    _value.setScenes(getScene(), SubScene.this);
                    markDirty(SubScene.SubSceneDirtyBits.ROOT_SG_DIRTY);
                    _value.resize(getWidth(), getHeight()); // maybe no-op if root is not resizable
                    _value.requestLayout();
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "root";
                }
            };
        }
        return root;
    }

    /**
     * Specifies the type of camera use for rendering this {@code SubScene}.
     * If {@code camera} is null, a parallel camera is used for rendering.
     * It is illegal to set a camera that belongs to other {@code Scene}
     * or {@code SubScene}.
     * <p>
     * Note: this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @defaultValue null
     */
    private ObjectProperty<Camera> camera;

    public final void setCamera(Camera value) {
        
        cameraProperty().set(value);
    }

    public final Camera getCamera() {
        return camera == null ? null : camera.get();
    }

    public final ObjectProperty<Camera> cameraProperty() {
        if (camera == null) {
            camera = new ObjectPropertyBase<Camera>() {

                @Override
                protected void invalidated() {
                    Camera _value = get();                    
                    if (_value != null) {
                        // Illegal value if it belongs to any scene or other subscene
                        if ((_value.getScene() != null || _value.getSubScene() != null)
                                && (_value.getScene() != getScene() || _value.getSubScene() != SubScene.this)) {
                            throw new IllegalArgumentException(_value
                                    + "is already set as camera in other scene/subscene");
                        }
                        _value.setViewWidth(getWidth());
                        _value.setViewHeight(getHeight());
                    }
                    markDirty(SubScene.SubSceneDirtyBits.CAMERA_DIRTY);
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "camera";
                }
            };
        }
        return camera;
    }

    private Camera defaultCamera;

    Camera getEffectiveCamera() {
        final Camera cam = getCamera();
        if (cam == null) {
            if (defaultCamera == null) {
                defaultCamera = new ParallelCamera();
                defaultCamera.setViewWidth(getWidth());
                defaultCamera.setViewHeight(getHeight());
            }
            return defaultCamera;
        }

        return cam;
    }

    /**
     * Defines the width of this {@code SubScene}
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty width;

    public final void setWidth(double value) {
        widthProperty().set(value);
    }

    public final double getWidth() {
        return width == null ? 0.0 : width.get();
    }

    public final DoubleProperty widthProperty() {
        if (width == null) {
            width = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    final Parent _root = getRoot();
                    //TODO - use a better method to update mirroring
                    if (_root.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                        _root.impl_transformsChanged();
                    }
                    if (_root.isResizable()) {
                        _root.resize(get() - _root.getLayoutX() - _root.getTranslateX(), _root.getLayoutBounds().getHeight());
                    }
                    markDirty(SubScene.SubSceneDirtyBits.CAMERA_DIRTY);
                    SubScene.this.impl_geomChanged();

                    getEffectiveCamera().setViewWidth(get());
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "width";
                }
            };
        }
        return width;
    }

    /**
     * Defines the height of this {@code SubScene}
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty height;

    public final void setHeight(double value) {
        heightProperty().set(value);
    }

    public final double getHeight() {
        return height == null ? 0.0 : height.get();
    }

    public final DoubleProperty heightProperty() {
        if (height == null) {
            height = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    final Parent _root = getRoot();
                    if (_root.isResizable()) {
                        _root.resize(_root.getLayoutBounds().getWidth(), get() - _root.getLayoutY() - _root.getTranslateY());
                    }
                    markDirty(SubScene.SubSceneDirtyBits.CAMERA_DIRTY);
                    SubScene.this.impl_geomChanged();

                    getEffectiveCamera().setViewHeight(get());
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }

    /**
     * Defines the background fill of this {@code SubScene}. Both a {@code null}
     * value meaning paint no background and a {@link javafx.scene.paint.Paint}
     * with transparency are supported. The default value is null.
     *
     * @defaultValue null
     */
    private ObjectProperty<Paint> fill;

    public final void setFill(Paint value) {
        fillProperty().set(value);
    }

    public final Paint getFill() {
        return fill == null ? null : fill.get();
    }

    public final ObjectProperty<Paint> fillProperty() {
        if (fill == null) {
            fill = new ObjectPropertyBase<Paint>(null) {

                @Override
                protected void invalidated() {
                    markDirty(SubScene.SubSceneDirtyBits.FILL_DIRTY);
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "fill";
                }
            };
        }
        return fill;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override
    public void impl_updatePG() {
        super.impl_updatePG();
        final Camera cam = getEffectiveCamera();
        cam.impl_syncPGNode();

        // TODO deal with clip node

        dirtyNodes = dirtyLayout = false;
        if (isDirty()) {
            PGSubScene peer = (PGSubScene) impl_getPGNode();
            if (isDirty(SubSceneDirtyBits.ROOT_SG_DIRTY)) {
                // TODO: Will call peer.setRoot either if root has changed or
                // if root is dirty. This is a workaround for dirty region support.
                peer.setRoot(getRoot().impl_getPGNode());
            }
            if (isDirty(SubSceneDirtyBits.FILL_DIRTY)) {
                Object platformPaint = getFill() == null ? null :
                        Toolkit.getPaintAccessor().getPlatformPaint(getFill());
                peer.setFillPaint(platformPaint);
            }
            peer.setDepthBuffer(depthBuffer);
            if (isDirty(SubSceneDirtyBits.CAMERA_DIRTY)) {
                peer.setWidth((float)getWidth());
                peer.setHeight((float)getHeight());
                peer.setCamera(cam.getPlatformCamera());
            }
            syncLights();
            clearDirtyBits();
        }

    }

    @Override
    void nodeResolvedOrientationChanged() {
        getRoot().parentResolvedOrientationInvalidated();
    }

    /***********************************************************************
     *                         CSS                                         *
     **********************************************************************/
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override
    protected void impl_processCSS() {
        // Nothing to do...
        if (cssFlag == CssFlags.CLEAN) { return; }

        if (getRoot().cssFlag == CssFlags.CLEAN) {
            getRoot().cssFlag = cssFlag;
        }
        super.impl_processCSS();
        getRoot().processCSS();
    }

    @Override
    void processCSS() {
        Parent root = getRoot();
        if (root.impl_isDirty(DirtyBits.NODE_CSS)) {
            root.impl_clearDirty(DirtyBits.NODE_CSS);
            if (cssFlag == CssFlags.CLEAN) { cssFlag = CssFlags.UPDATE; }
        }
        super.processCSS();
    }

    @Override void updateBounds() {
        super.updateBounds();
        getRoot().updateBounds();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGSubScene();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        int w = (int)Math.ceil(width.get());
        int h = (int)Math.ceil(height.get());
        bounds = bounds.deriveWithNewBounds(0.0f, 0.0f, 0.0f,
                                            w, h, 0.0f);
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    /***********************************************************************
     *                         Dirty Bits                                  *
     **********************************************************************/
    boolean dirtyLayout = false;
    void setDirtyLayout(Parent p) {
        if (!dirtyLayout && p != null && p.getSubScene() == this &&
                this.getScene() != null) {
            dirtyLayout = true;
            markDirty(SubSceneDirtyBits.ROOT_SG_DIRTY);
        }
    }

    private boolean dirtyNodes = false;
    void setDirty(Node n) {
        if (!dirtyNodes && n != null && n.getSubScene() == this &&
                this.getScene() != null) {
            dirtyNodes = true;
            markDirty(SubSceneDirtyBits.ROOT_SG_DIRTY);
        }
    }

    private enum SubSceneDirtyBits {
        FILL_DIRTY,
        /* ROOT_SG_DIRTY is set either when the root has changed or hint that
         * root scene graph needs to be re-rendered. For example when layout
         * occurs, or node has been added or removed.
         */
        ROOT_SG_DIRTY,
        CAMERA_DIRTY, // get set if the camera, width or height change
        LIGHTS_DIRTY;

        private int mask;

        private SubSceneDirtyBits() { mask = 1 << ordinal(); }

        public final int getMask() { return mask; }
    }

    private int dirtyBits;

    private void clearDirtyBits() { dirtyBits = 0; }

    private boolean isDirty() { return dirtyBits != 0; }

    // Should not be called directly, instead use markDirty
    private void setDirty(SubSceneDirtyBits dirtyBit) {
        this.dirtyBits |= dirtyBit.getMask();
    }

    private boolean isDirty(SubSceneDirtyBits dirtyBit) {
        return ((this.dirtyBits & dirtyBit.getMask()) != 0);
    }

    private void markDirty(SubSceneDirtyBits dirtyBit) {
        if (!isDirty()) {
            // Force SubScene to redraw
            impl_markDirty(DirtyBits.NODE_CONTENTS);
        }
        setDirty(dirtyBit);
    }

    /***********************************************************************
     *                           Picking                                   *
     **********************************************************************/

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        if (subSceneComputeContains(localX, localY)) {
            return true;
        } else {
            return getRoot().impl_computeContains(localX, localY);
        }
    }

    /**
     * Determines whether subScene contains the given point.
     * It does not consider the contained nodes, only subScene's
     * size and fills.
     * @param localX horizontal coordinate in the local space of the subScene node
     * @param localY vertical coordinate in the local space of the subScene node
     * @return true if the point is inside subScene's area covered by its fill
     */
    private boolean subSceneComputeContains(double localX, double localY) {
        if (localX < 0 || localY < 0 || localX > getWidth() || localY > getHeight()) {
            return false;
        }
        return getFill() != null;
    }

    /*
     * Generates a pick ray based on local coordinates and camera. Then finds a
     * top-most child node that intersects the pick ray.
     */
    private PickResult pickRootSG(double localX, double localY) {
        final double viewWidth = getWidth();
        final double viewHeight = getHeight();
        if (localX < 0 || localY < 0 || localX > viewWidth || localY > viewHeight) {
            return null;
        }
        final PickResultChooser result = new PickResultChooser();
        final PickRay pickRay = getEffectiveCamera().computePickRay(localX, localY, new PickRay());
        getRoot().impl_pickNode(pickRay, result);
        return result.toPickResult(!pickRay.isParallel());
    }

    /**
     * Finds a top-most child node that contains the given local coordinates.
     *
     * Returns the picked node, null if no such node was found.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override
    protected void impl_pickNodeLocal(PickRay localPickRay, PickResultChooser result) {
        final double boundsDistance = impl_intersectsBounds(localPickRay);
        if (!Double.isNaN(boundsDistance) && result.isCloser(boundsDistance)) {
            final Point3D intersectPt = PickResultChooser.computePoint(
                    localPickRay, boundsDistance);
            final PickResult subSceneResult =
                    pickRootSG(intersectPt.getX(), intersectPt.getY());
            if (subSceneResult != null) {
                result.offerSubScenePickResult(this, subSceneResult, boundsDistance);
            } else if (isPickOnBounds() ||
                    subSceneComputeContains(intersectPt.getX(), intersectPt.getY())) {
                result.offer(this, boundsDistance, intersectPt);
            }
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    // TODO: 3D - Should avoid the need to do costly linear search and update of 
    //            lights at every light add and graph sync.
    private List<LightBase> lights = new ArrayList<LightBase>();

    final void addLight(LightBase light) {
        // There is only an add light method and no removed method. However, if
        // a light is no longer attached it will be removed via syncLights.
        if (!lights.contains(light)) {
            markDirty(SubScene.SubSceneDirtyBits.LIGHTS_DIRTY);
            lights.add(light);
        }
    }

    /**
     * PG Light synchronizer. It will verify if light is attached, if not the
     * light is removed.
     */
    private void syncLights() {
        if (!isDirty(SubSceneDirtyBits.LIGHTS_DIRTY)) {
            return;
        }
        PGSubScene pgSubScene = (PGSubScene) impl_getPGNode();
        Object peerLights[] = pgSubScene.getLights();
        if (!lights.isEmpty() || (peerLights != null)) {
            if (lights.isEmpty()) {
                pgSubScene.setLights(null);
            } else {
                if (peerLights == null || peerLights.length != lights.size()) {
                    peerLights = new PGLightBase[lights.size()];
                }
                int i = 0;
                for (; i < lights.size(); i++) {
                    LightBase light = lights.get(i);
                    if (light.getSubScene() == this) {
                        peerLights[i] = (PGLightBase) light.impl_getPGNode();
                    } else {
                        // The light does not belong here
                        lights.remove(i--);
                    }
                }
                // Clear the rest of the list
                while (i < peerLights.length && peerLights[i] != null) {
                    peerLights[i++] = null;
                }
                pgSubScene.setLights(peerLights);
            }
        }
    }

    /**
     * This class is used by classes in different packages to get access to
     * private and package private methods.
     */
    private static class SubSceneAccessImpl extends SubSceneAccess {

        @Override
        public boolean isDepthBuffer(SubScene subScene) {
            return subScene.depthBuffer;
        };

    }

    static {
        SubSceneAccess.setSubSceneAccess(new SubSceneAccessImpl());
    }
}
