/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.SubSceneTraversalEngine;
import com.sun.javafx.scene.traversal.TopMostTraversalEngine;
import com.sun.javafx.scene.traversal.TraversalMethod;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.*;
import javafx.css.Stylesheet;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point3D;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Paint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.CssFlags;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SubSceneHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGSubScene;
import com.sun.javafx.tk.Toolkit;

import com.sun.javafx.logging.PlatformLogger;

/**
 * The {@code SubScene} class is the container for content in a scene graph.
 * {@code SubScene} provides separation of different parts of a scene, each
 * of which can be rendered with a different camera, depth buffer, or scene
 * anti-aliasing. A {@code SubScene} is embedded into the main scene or another
 * sub-scene.
 * <p>
 * An application may request depth buffer support or scene anti-aliasing
 * support at the creation of a {@code SubScene}. A sub-scene with only 2D
 * shapes and without any 3D transforms does not need a depth buffer nor scene
 * anti-aliasing support. A sub-scene containing 3D shapes or 2D shapes with 3D
 * transforms may use depth buffer support for proper depth sorted rendering; to
 * avoid depth fighting (also known as Z fighting), disable depth testing on 2D
 * shapes that have no 3D transforms. See
 * {@link Node#depthTestProperty depthTest} for more information. A sub-scene
 * with 3D shapes may enable scene anti-aliasing to improve its rendering
 * quality.
 * <p>
 * The depthBuffer and antiAliasing flags are conditional features. With the
 * respective default values of: false and {@code SceneAntialiasing.DISABLED}.
 * See {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
 * for more information.
 *
 * <p>
 * Possible use cases are:
 * <ul>
 * <li> Mixing 2D and 3D content </li>
 * <li> Overlay for UI controls </li>
 * <li> Underlay for background </li>
 * <li> Heads-up display </li>
 * </ul>
 *
 * <p>
 * A default headlight will be added to a {@code SubScene} that contains one or more
 * {@code Shape3D} nodes, but no light nodes. This light source is a
 * {@code Color.WHITE} {@code PointLight} placed at the camera position.
 * </p>
 *
 * @since JavaFX 8.0
 */
public class SubScene extends Node {
    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        SubSceneHelper.setSubSceneAccessor(new SubSceneHelper.SubSceneAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((SubScene) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((SubScene) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((SubScene) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((SubScene) node).doComputeContains(localX, localY);
            }

            @Override
            public void doProcessCSS(Node node) {
                ((SubScene) node).doProcessCSS();
            }

            @Override
            public void doPickNodeLocal(Node node, PickRay localPickRay,
                    PickResultChooser result) {
                ((SubScene) node).doPickNodeLocal(localPickRay, result);
            }

            @Override
            public boolean isDepthBuffer(SubScene subScene) {
                return subScene.isDepthBufferInternal();
            }

            @Override
            public Camera getEffectiveCamera(SubScene subScene) {
                return subScene.getEffectiveCamera();
            }

        });
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        SubSceneHelper.initHelper(this);
    }
    /**
     * Creates a {@code SubScene} for a specific root Node with a specific size.
     *
     * @param root The root node of the scene graph
     * @param width The width of the sub-scene
     * @param height The height of the sub-scene
     *
     * @throws NullPointerException if root is null
     */
    public SubScene(@NamedArg("root") Parent root, @NamedArg("width") double width, @NamedArg("height") double height) {
        this(root, width, height, false, SceneAntialiasing.DISABLED);
    }

    /**
     * Constructs a {@code SubScene} consisting of a root, with a dimension of width and
     * height, specifies whether a depth buffer is created for this scene and
     * specifies whether scene anti-aliasing is requested.
     * <p>
     * A sub-scene with only 2D shapes and without any 3D transforms does not
     * need a depth buffer nor scene anti-aliasing support. A sub-scene
     * containing 3D shapes or 2D shapes with 3D transforms may use depth buffer
     * support for proper depth sorted rendering; to avoid depth fighting (also
     * known as Z fighting), disable depth testing on 2D shapes that have no 3D
     * transforms. See {@link Node#depthTestProperty depthTest} for more
     * information. A sub-scene with 3D shapes may enable scene anti-aliasing to
     * improve its rendering quality.
     *
     * @param root The root node of the scene graph
     * @param width The width of the sub-scene
     * @param height The height of the sub-scene
     * @param depthBuffer The depth buffer flag
     * @param antiAliasing The sub-scene anti-aliasing attribute. A value of
     * {@code null} is treated as DISABLED.
     * <p>
     * The depthBuffer and antiAliasing flags are conditional features. With the
     * respective default values of: false and {@code SceneAntialiasing.DISABLED}.
     * See {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @throws NullPointerException if root is null
     *
     * @see javafx.scene.Node#setDepthTest(DepthTest)
     */
    public SubScene(@NamedArg("root") Parent root, @NamedArg("width") double width, @NamedArg("height") double height,
            @NamedArg("depthBuffer") boolean depthBuffer, @NamedArg("antiAliasing") SceneAntialiasing antiAliasing)
    {
        this.depthBuffer = depthBuffer;
        this.antiAliasing = antiAliasing;
        boolean isAntiAliasing = !(antiAliasing == null || antiAliasing == SceneAntialiasing.DISABLED);
        setRoot(root);
        setWidth(width);
        setHeight(height);

        if ((depthBuffer || isAntiAliasing) && !is3DSupported) {
            String logname = SubScene.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                    + "ConditionalFeature.SCENE3D");
        }
        if (isAntiAliasing && !Toolkit.getToolkit().isMSAASupported()) {
            String logname = SubScene.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                    + "antiAliasing");
        }
    }

    private static boolean is3DSupported =
            Platform.isSupported(ConditionalFeature.SCENE3D);

    private final SceneAntialiasing antiAliasing;

    /**
     * Return the defined {@code SceneAntialiasing} for this {@code SubScene}.
     * <p>
     * Note: this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * and {@link javafx.scene.SceneAntialiasing SceneAntialiasing}
     * for more information.
     * @return the SceneAntialiasing for this sub-scene
     * @since JavaFX 8.0
     */
    public final SceneAntialiasing getAntiAliasing() {
        return antiAliasing;
    }

    private final boolean depthBuffer;

    /**
     * Retrieves the depth buffer attribute for this {@code SubScene}.
     * @return the depth buffer attribute.
     */
    public final boolean isDepthBuffer() {
        return depthBuffer;
    }

    private boolean isDepthBufferInternal() {
        return is3DSupported ? depthBuffer : false;
    }

    /**
     * Defines the root {@code Node} of the {@code SubScene} scene graph.
     * If a {@code Group} is used as the root, the
     * contents of the scene graph will be clipped by the {@code SubScene}'s width and height.
     *
     * {@code SubScene} doesn't accept null root.
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
            root = new ObjectPropertyBase<>() {
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

                    // disabled and isTreeVisible properties are inherited
                    _value.setTreeVisible(isTreeVisible());
                    _value.setDisabled(isDisabled());

                    if (oldRoot != null) {
                        StyleManager.getInstance().forget(SubScene.this);
                        oldRoot.setScenes(null, null);
                        oldRoot.getStyleClass().remove("root");
                    }
                    oldRoot = _value;
                    _value.getStyleClass().add(0, "root");
                    _value.setScenes(getScene(), SubScene.this);
                    markDirty(SubSceneDirtyBits.ROOT_SG_DIRTY);
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
            camera = new ObjectPropertyBase<>() {
                Camera oldCamera = null;

                @Override
                protected void invalidated() {
                    Camera _value = get();
                    if (_value != null) {
                        if (_value instanceof PerspectiveCamera
                                && !SubScene.is3DSupported) {
                            String logname = SubScene.class.getName();
                            PlatformLogger.getLogger(logname).warning("System can't support "
                                    + "ConditionalFeature.SCENE3D");
                        }
                        // Illegal value if it belongs to any scene or other subscene
                        if ((_value.getScene() != null || _value.getSubScene() != null)
                                && (_value.getScene() != getScene() || _value.getSubScene() != SubScene.this)) {
                            throw new IllegalArgumentException(_value
                                    + "is already part of other scene or subscene");
                        }
                        // throws exception if the camera already has a different owner
                        _value.setOwnerSubScene(SubScene.this);
                        _value.setViewWidth(getWidth());
                        _value.setViewHeight(getHeight());
                    }
                    markDirty(SubSceneDirtyBits.CAMERA_DIRTY);
                    if (oldCamera != null && oldCamera != _value) {
                        oldCamera.setOwnerSubScene(null);
                    }
                    oldCamera = _value;
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
        if (cam == null
                || (cam instanceof PerspectiveCamera && !is3DSupported)) {
            if (defaultCamera == null) {
                defaultCamera = new ParallelCamera();
                defaultCamera.setOwnerSubScene(this);
                defaultCamera.setViewWidth(getWidth());
                defaultCamera.setViewHeight(getHeight());
            }
            return defaultCamera;
        }

        return cam;
    }

    // Used by the camera
    final void markContentDirty() {
        markDirty(SubSceneDirtyBits.CONTENT_DIRTY);
    }

    /**
     * Defines the width of this {@code SubScene}
     *
     * @defaultValue 0.0
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
                        NodeHelper.transformsChanged(_root);
                    }
                    if (_root.isResizable()) {
                        _root.resize(get() - _root.getLayoutX() - _root.getTranslateX(), _root.getLayoutBounds().getHeight());
                    }
                    markDirty(SubSceneDirtyBits.SIZE_DIRTY);
                    NodeHelper.geomChanged(SubScene.this);

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
     * @defaultValue 0.0
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
                    markDirty(SubSceneDirtyBits.SIZE_DIRTY);
                    NodeHelper.geomChanged(SubScene.this);

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
                    markDirty(SubSceneDirtyBits.FILL_DIRTY);
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

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        // TODO deal with clip node

        dirtyNodes = false;
        if (isDirty()) {
            NGSubScene peer = getPeer();
            final Camera cam = getEffectiveCamera();
            boolean contentChanged = false;
            if (cam.getSubScene() == null &&
                    isDirty(SubSceneDirtyBits.CONTENT_DIRTY)) {
                // When camera is not a part of the graph, then its
                // owner(subscene) must take care of syncing it. And when a
                // property on the camera changes it will mark subscenes
                // CONTENT_DIRTY.
                cam.syncPeer();
            }
            if (isDirty(SubSceneDirtyBits.FILL_DIRTY)) {
                Object platformPaint = getFill() == null ? null :
                        Toolkit.getPaintAccessor().getPlatformPaint(getFill());
                peer.setFillPaint(platformPaint);
                contentChanged = true;
            }
            if (isDirty(SubSceneDirtyBits.SIZE_DIRTY)) {
                // Note change in size is a geom change and is handled by peer
                peer.setWidth((float)getWidth());
                peer.setHeight((float)getHeight());
            }
            if (isDirty(SubSceneDirtyBits.CAMERA_DIRTY)) {
                peer.setCamera((NGCamera) cam.getPeer());
                contentChanged = true;
            }
            if (isDirty(SubSceneDirtyBits.ROOT_SG_DIRTY)) {
                peer.setRoot(getRoot().getPeer());
                contentChanged = true;
            }
            contentChanged |= syncLights();
            if (contentChanged || isDirty(SubSceneDirtyBits.CONTENT_DIRTY)) {
                peer.markContentDirty();
            }

            clearDirtyBits();
        }

    }

    @Override
    void nodeResolvedOrientationChanged() {
        getRoot().parentResolvedOrientationInvalidated();
    }

    /* *********************************************************************
     *                         CSS                                         *
     **********************************************************************/
    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doProcessCSS() {
        // Nothing to do...
        if (cssFlag == CssFlags.CLEAN) { return; }

        if (getRoot().cssFlag == CssFlags.CLEAN) {
            getRoot().cssFlag = cssFlag;
        }
        SubSceneHelper.superProcessCSS(this);
        getRoot().processCSS();
    }

    @Override
    void processCSS() {
        Parent root = getRoot();
        if (root.isDirty(DirtyBits.NODE_CSS)) {
            root.clearDirty(DirtyBits.NODE_CSS);
            if (cssFlag == CssFlags.CLEAN) { cssFlag = CssFlags.UPDATE; }
        }
        super.processCSS();
    }

    private ObjectProperty<String> userAgentStylesheet = null;
    /**
     * Gets the userAgentStylesheet property.
     * @return the userAgentStylesheet property
     * @see #getUserAgentStylesheet()
     * @see #setUserAgentStylesheet(String)
     * @since  JavaFX 8u20
     */
    public final ObjectProperty<String> userAgentStylesheetProperty() {
        if (userAgentStylesheet == null) {
            userAgentStylesheet = new SimpleObjectProperty<>(SubScene.this, "userAgentStylesheet", null) {
                @Override protected void invalidated() {
                    StyleManager.getInstance().forget(SubScene.this);
                    reapplyCSS();
                }
            };
        }
        return userAgentStylesheet;
    }

    /**
     * Get the URL of the user-agent stylesheet that will be used by this SubScene. If the URL has not been set,
     * the platform-default user-agent stylesheet will be used.
     * <p>
     * For additional information about using CSS with the scene graph,
     * see the <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     * </p>
     * @return The URL of the user-agent stylesheet that will be used by this SubScene,
     * or null if has not been set.
     * @since  JavaFX 8u20
     */
    public final String getUserAgentStylesheet() {
        return userAgentStylesheet == null ? null : userAgentStylesheet.get();
    }

    /**
     * Set the URL of the user-agent stylesheet that will be used by this SubScene in place of the
     * the platform-default user-agent stylesheet. If the URL does not resolve to a valid location,
     * the platform-default user-agent stylesheet will be used.
     * <p>
     * The URL is a hierarchical URI of the form [scheme:][//authority][path]. If the URL
     * does not have a [scheme:] component, the URL is considered to be the [path] component only.
     * Any leading '/' character of the [path] is ignored and the [path] is treated as a path relative to
     * the root of the application's classpath.
     * <p>
     * The RFC 2397 "data" scheme for URLs is supported in addition to the protocol handlers that
     * are registered for the application.
     * If a URL uses the "data" scheme and the MIME type is either empty, "text/plain", or "text/css",
     * the payload will be interpreted as a CSS file.
     * If the MIME type is "application/octet-stream", the payload will be interpreted as a binary
     * CSS file (see {@link Stylesheet#convertToBinary(File, File)}).
     * <p>
     * For additional information about using CSS with the scene graph,
     * see the <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     *
     * @param url the URL of the user-agent stylesheet
     * @since  JavaFX 8u20
     */
    public final void setUserAgentStylesheet(String url) {
        userAgentStylesheetProperty().set(url);
    }

    @Override void updateBounds() {
        super.updateBounds();
        getRoot().updateBounds();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        if (!is3DSupported) {
            return new NGSubScene(false, false);
        }
        boolean aa = !(antiAliasing == null || antiAliasing == SceneAntialiasing.DISABLED);
        return new NGSubScene(depthBuffer, aa && Toolkit.getToolkit().isMSAASupported());
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        int w = (int)Math.ceil(width.get());
        int h = (int)Math.ceil(height.get());
        bounds = bounds.deriveWithNewBounds(0.0f, 0.0f, 0.0f,
                                            w, h, 0.0f);
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    /* *********************************************************************
     *                         Dirty Bits                                  *
     **********************************************************************/
    boolean dirtyLayout = false;
    void setDirtyLayout(Parent p) {
        if (!dirtyLayout && p != null && p.getSubScene() == this &&
                this.getScene() != null) {
            dirtyLayout = true;
            markDirtyLayoutBranch();
            markDirty(SubSceneDirtyBits.CONTENT_DIRTY);
        }
    }

    private boolean dirtyNodes = false;
    void setDirty(Node n) {
        if (!dirtyNodes && n != null && n.getSubScene() == this &&
                this.getScene() != null) {
            dirtyNodes = true;
            markDirty(SubSceneDirtyBits.CONTENT_DIRTY);
        }
    }

    void layoutPass() {
        if (dirtyLayout) {
            Parent r = getRoot();
            if (r != null) {
                r.layout();
            }
            dirtyLayout = false;
        }
    }

    private TopMostTraversalEngine traversalEngine = new SubSceneTraversalEngine(this);

    boolean traverse(Node node, Direction dir, TraversalMethod method) {
        return traversalEngine.trav(node, dir, method) != null;
    }

    private enum SubSceneDirtyBits {
        SIZE_DIRTY,
        FILL_DIRTY,
        ROOT_SG_DIRTY,
        CAMERA_DIRTY,
        LIGHTS_DIRTY,
        CONTENT_DIRTY;

        private int mask;

        private SubSceneDirtyBits() { mask = 1 << ordinal(); }

        public final int getMask() { return mask; }
    }

    private int dirtyBits = ~0;

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
            NodeHelper.markDirty(this, DirtyBits.NODE_CONTENTS);
        }
        setDirty(dirtyBit);
    }

    /* *********************************************************************
     *                           Picking                                   *
     **********************************************************************/

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        if (subSceneComputeContains(localX, localY)) {
            return true;
        } else {
            return NodeHelper.computeContains(getRoot(), localX, localY);
        }
    }

    /**
     * Determines whether {@code SubScene} contains the given point.
     * It does not consider the contained nodes, only {@code SubScene}'s
     * size and fills.
     * @param localX horizontal coordinate in the local space of the {@code SubScene} node
     * @param localY vertical coordinate in the local space of the {@code SubScene} node
     * @return true if the point is inside {@code SubScene}'s area covered by its fill
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
        pickRay.getDirectionNoClone().normalize();
        getRoot().pickNode(pickRay, result);
        return result.toPickResult();
    }

    /**
     * Finds a top-most child node that contains the given local coordinates.
     *
     * Returns the picked node, null if no such node was found.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doPickNodeLocal(PickRay localPickRay, PickResultChooser result) {
        final double boundsDistance = intersectsBounds(localPickRay);
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

    private List<LightBase> lights = new ArrayList<>();

    // @param light must not be null
    final void addLight(LightBase light) {
        if (!lights.contains(light)) {
            markDirty(SubSceneDirtyBits.LIGHTS_DIRTY);
            lights.add(light);
        }
    }

    final void removeLight(LightBase light) {
        if (lights.remove(light)) {
            markDirty(SubSceneDirtyBits.LIGHTS_DIRTY);
        }
    }

    /**
     * PG Light synchronizer.
     */
    private boolean syncLights() {
        boolean lightOwnerChanged = false;
        if (!isDirty(SubSceneDirtyBits.LIGHTS_DIRTY)) {
            return lightOwnerChanged;
        }
        NGSubScene pgSubScene = getPeer();
        NGLightBase peerLights[] = pgSubScene.getLights();
        if (!lights.isEmpty() || (peerLights != null)) {
            if (lights.isEmpty()) {
                pgSubScene.setLights(null);
            } else {
                if (peerLights == null || peerLights.length < lights.size()) {
                    peerLights = new NGLightBase[lights.size()];
                }
                int i = 0;
                for (; i < lights.size(); i++) {
                    peerLights[i] = lights.get(i).getPeer();
                }
                // Clear the rest of the list
                while (i < peerLights.length && peerLights[i] != null) {
                    peerLights[i++] = null;
                }
                pgSubScene.setLights(peerLights);
            }
            lightOwnerChanged = true;
        }
        return lightOwnerChanged;
    }

}
