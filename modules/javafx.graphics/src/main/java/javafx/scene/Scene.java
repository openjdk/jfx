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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Accessible;
import com.sun.javafx.util.Logging;
import com.sun.javafx.util.Utils;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.event.EventQueue;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.scene.CssFlags;
import com.sun.javafx.scene.LayoutFlags;
import com.sun.javafx.scene.SceneEventDispatcher;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.scene.input.DragboardHelper;
import com.sun.javafx.scene.input.ExtendedInputMethodRequests;
import com.sun.javafx.scene.input.InputEventUtils;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.SceneTraversalEngine;
import com.sun.javafx.scene.traversal.TopMostTraversalEngine;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.tk.*;
import com.sun.prism.impl.PrismSettings;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.NamedArg;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.CssMetaData;
import javafx.css.StyleableObjectProperty;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.javafx.logging.PulseLogger;

import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.stage.WindowHelper;
import com.sun.javafx.scene.input.ClipboardHelper;
import com.sun.javafx.scene.input.TouchPointHelper;
import java.lang.ref.WeakReference;

/**
 * The JavaFX {@code Scene} class is the container for all content in a scene graph.
 * The background of the scene is filled as specified by the {@code fill} property.
 * <p>
 * The application must specify the root {@code Node} for the scene graph by setting
 * the {@code root} property.   If a {@code Group} is used as the root, the
 * contents of the scene graph will be clipped by the scene's width and height and
 * changes to the scene's size (if user resizes the stage) will not alter the
 * layout of the scene graph. If a resizable node (layout {@code Region} or
 * {@code Control}) is set as the root, then the root's size will track the
 * scene's size, causing the contents to be relayed out as necessary.
 * <p>
 * The scene's size may be initialized by the application during construction.
 * If no size is specified, the scene will automatically compute its initial
 * size based on the preferred size of its content. If only one dimension is specified,
 * the other dimension is computed using the specified dimension, respecting content bias
 * of a root.
 * <p>
 * An application may request depth buffer support or scene anti-aliasing
 * support at the creation of a {@code Scene}. A scene with only 2D shapes and
 * without any 3D transforms does not need a depth buffer nor scene
 * anti-aliasing support. A scene containing 3D shapes or 2D shapes with 3D
 * transforms may use depth buffer support for proper depth sorted rendering; to
 * avoid depth fighting (also known as Z fighting), disable depth testing on 2D
 * shapes that have no 3D transforms. See
 * {@link Node#depthTestProperty() depthTest} for more information. A scene with
 * 3D shapes may enable scene anti-aliasing to improve its rendering quality.
 * <p>
 * The depthBuffer and antiAliasing flags are conditional features. With the
 * respective default values of: false and {@code SceneAntialiasing.DISABLED}.
 * See {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
 * for more information.
 * <p>
 * A default headlight will be added to a scene that contains one or more
 * {@code Shape3D} nodes, but no light nodes. This light source is a
 * {@code Color.WHITE} {@code PointLight} placed at the camera position.
 *
 * <p>
 * A {@code Scene} may be created and modified on any thread until it is attached
 * to a {@link Window} that is {@link Window#isShowing() showing}.
 * After that, it must be modified only on the JavaFX Application Thread.
 * Note that {@code Scene} is not thread-safe; modifying a {@code Scene} on
 * multiple threads at the same time will lead to unpredictable results and
 * must be avoided.
 * </p>
 *
 * <p>
 * The JavaFX Application Thread is created as part of the startup process for
 * the JavaFX runtime. See the {@link javafx.application.Application} class and
 * the {@link Platform#startup(Runnable)} method for more information.
 * </p>
 *
 * <p>Example:</p>
 *
 * <pre>
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;

Group root = new Group();
Scene s = new Scene(root, 300, 300, Color.BLACK);

Rectangle r = new Rectangle(25,25,250,250);
r.setFill(Color.BLUE);

root.getChildren().add(r);
 * </pre>
 *
 * @since JavaFX 2.0
 */
@DefaultProperty("root")
public class Scene implements EventTarget {

    private double widthSetByUser = -1.0;
    private double heightSetByUser = -1.0;
    private boolean sizeInitialized = false;
    private final boolean depthBuffer;
    private final SceneAntialiasing antiAliasing;

    private EnumSet<DirtyBits> dirtyBits = EnumSet.noneOf(DirtyBits.class);

    final AccessControlContext acc = AccessController.getContext();

    private Camera defaultCamera;

    /**
     * A node that is temporarily responsible for the FOCUS_NODE
     * accessibility attribute. E.g. a currently active MenuBar.
     */
    private Node transientFocusContainer;

    //Neither width nor height are initialized and will be calculated according to content when this Scene
    //is shown for the first time.
//    public Scene() {
//        //this(-1, -1, (Parent) new Group());
//        this(-1, -1, (Parent)null);
//    }

    /**
     * Creates a Scene for a specific root Node.
     *
     * @param root The root node of the scene graph
     *
     * @throws NullPointerException if root is null
     */
    public Scene(@NamedArg("root") Parent root) {
        this(root, -1, -1, Color.WHITE, false, SceneAntialiasing.DISABLED);
    }

//Public constructor initializing public-init properties
//When width < 0, and or height < 0 is passed, then width and/or height are understood as unitialized
//Unitialized dimension is calculated when Scene is shown for the first time.
//    public Scene(
//            @Default("-1") double width,
//            @Default("-1") double height) {
//        //this(width, height, (Parent)new Group());
//        this(width, height, (Parent)null);
//    }
//
//    public Scene(double width, double height, Paint fill) {
//        //this(width, height, (Parent) new Group());
//        this(width, height, (Parent)null);
//        setFill(fill);
//    }

    /**
     * Creates a Scene for a specific root Node with a specific size.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     *
     * @throws NullPointerException if root is null
     */
    public Scene(@NamedArg("root") Parent root, @NamedArg("width") double width, @NamedArg("height") double height) {
        this(root, width, height, Color.WHITE, false, SceneAntialiasing.DISABLED);
    }

    /**
     * Creates a Scene for a specific root Node with a fill.
     *
     * @param root The parent
     * @param fill The fill
     *
     * @throws NullPointerException if root is null
     */
    public Scene(@NamedArg("root") Parent root, @NamedArg(value="fill", defaultValue="WHITE") Paint fill) {
        this(root, -1, -1, fill, false, SceneAntialiasing.DISABLED);
    }

    /**
     * Creates a Scene for a specific root Node with a specific size and fill.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     * @param fill The fill
     *
     * @throws NullPointerException if root is null
     */
    public Scene(@NamedArg("root") Parent root, @NamedArg("width") double width, @NamedArg("height") double height,
            @NamedArg(value="fill", defaultValue="WHITE") Paint fill) {
        this(root, width, height, fill, false, SceneAntialiasing.DISABLED);
    }

    /**
     * Constructs a scene consisting of a root, with a dimension of width and
     * height, and specifies whether a depth buffer is created for this scene.
     * <p>
     * A scene with only 2D shapes and without any 3D transforms does not need a
     * depth buffer. A scene containing 3D shapes or 2D shapes with 3D
     * transforms may use depth buffer support for proper depth sorted
     * rendering; to avoid depth fighting (also known as Z fighting), disable
     * depth testing on 2D shapes that have no 3D transforms. See
     * {@link Node#depthTestProperty() depthTest} for more information.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     * @param depthBuffer The depth buffer flag
     * <p>
     * The depthBuffer flag is a conditional feature and its default value is
     * false. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @throws NullPointerException if root is null
     *
     * @see javafx.scene.Node#setDepthTest(DepthTest)
     */
    public Scene(@NamedArg("root") Parent root, @NamedArg(value="width", defaultValue="-1") double width, @NamedArg(value="height", defaultValue="-1") double height, @NamedArg("depthBuffer") boolean depthBuffer) {
        this(root, width, height, Color.WHITE, depthBuffer, SceneAntialiasing.DISABLED);
    }

    /**
     * Constructs a scene consisting of a root, with a dimension of width and
     * height, specifies whether a depth buffer is created for this scene and
     * specifies whether scene anti-aliasing is requested.
     * <p>
     * A scene with only 2D shapes and without any 3D transforms does not need a
     * depth buffer nor scene anti-aliasing support. A scene containing 3D
     * shapes or 2D shapes with 3D transforms may use depth buffer support for
     * proper depth sorted rendering; to avoid depth fighting (also known as Z
     * fighting), disable depth testing on 2D shapes that have no 3D transforms.
     * See {@link Node#depthTestProperty() depthTest} for more information. A
     * scene with 3D shapes may enable scene anti-aliasing to improve its
     * rendering quality.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     * @param depthBuffer The depth buffer flag
     * @param antiAliasing The scene anti-aliasing attribute. A value of
     * {@code null} is treated as DISABLED.
     * <p>
     * The depthBuffer and antiAliasing are conditional features. With the
     * respective default values of: false and {@code SceneAntialiasing.DISABLED}. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @throws NullPointerException if root is null
     *
     * @see javafx.scene.Node#setDepthTest(DepthTest)
     * @since JavaFX 8.0
     */
    public Scene(@NamedArg("root") Parent root, @NamedArg(value="width", defaultValue="-1") double width, @NamedArg(value="height", defaultValue="-1") double height,
            @NamedArg("depthBuffer") boolean depthBuffer,
            @NamedArg(value="antiAliasing", defaultValue="DISABLED") SceneAntialiasing antiAliasing) {
        this(root, width, height, Color.WHITE, depthBuffer, antiAliasing);

        if (antiAliasing != null && antiAliasing != SceneAntialiasing.DISABLED &&
                !Toolkit.getToolkit().isMSAASupported())
        {
            String logname = Scene.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                + "antiAliasing");
        }
    }

    private Scene(Parent root, double width, double height, Paint fill,
            boolean depthBuffer, SceneAntialiasing antiAliasing) {
        this.depthBuffer = depthBuffer;
        this.antiAliasing = antiAliasing;
        if (root == null) {
            throw new NullPointerException("Root cannot be null");
        }

        if ((depthBuffer || (antiAliasing != null && antiAliasing != SceneAntialiasing.DISABLED))
                && !Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = Scene.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                    + "ConditionalFeature.SCENE3D");
        }

        init();
        setRoot(root);
        init(width, height);
        setFill(fill);
    }

    static {
            PerformanceTracker.setSceneAccessor(new PerformanceTracker.SceneAccessor() {
                public void setPerfTracker(Scene scene, PerformanceTracker tracker) {
                    synchronized (trackerMonitor) {
                        scene.tracker = tracker;
                    }
                }
                public PerformanceTracker getPerfTracker(Scene scene) {
                    synchronized (trackerMonitor) {
                        return scene.tracker;
                    }
                }
            });
            SceneHelper.setSceneAccessor(
                    new SceneHelper.SceneAccessor() {
                        @Override
                        public void enableInputMethodEvents(Scene scene, boolean enable) {
                            scene.enableInputMethodEvents(enable);
                        }

                        @Override
                        public void processKeyEvent(Scene scene, KeyEvent e) {
                            scene.processKeyEvent(e);
                        }

                        @Override
                        public void processMouseEvent(Scene scene, MouseEvent e) {
                            scene.processMouseEvent(e);
                        }

                        @Override
                        public void preferredSize(Scene scene) {
                            scene.preferredSize();
                        }

                        @Override
                        public void disposePeer(Scene scene) {
                            scene.disposePeer();
                        }

                        @Override
                        public void initPeer(Scene scene) {
                            scene.initPeer();
                        }

                        @Override
                        public void setWindow(Scene scene, Window window) {
                            scene.setWindow(window);
                        }

                        @Override
                        public TKScene getPeer(Scene scene) {
                            return scene.getPeer();
                        }

                        @Override
                        public void setAllowPGAccess(boolean flag) {
                            Scene.setAllowPGAccess(flag);
                        }

                        @Override
                        public void parentEffectiveOrientationInvalidated(
                                final Scene scene) {
                            scene.parentEffectiveOrientationInvalidated();
                        }

                        @Override
                        public Camera getEffectiveCamera(Scene scene) {
                            return scene.getEffectiveCamera();
                        }

                        @Override
                        public Scene createPopupScene(Parent root) {
                            return new Scene(root) {
                                       @Override
                                       void doLayoutPass() {
                                           resizeRootToPreferredSize(getRoot());
                                           super.doLayoutPass();
                                       }

                                       @Override
                                       void resizeRootOnSceneSizeChange(
                                               double newWidth,
                                               double newHeight) {
                                           // don't resize
                                       }
                                   };
                        }

                        @Override
                        public void setTransientFocusContainer(Scene scene, Node node) {
                            if (scene != null) {
                                scene.transientFocusContainer = node;
                            }
                        }

                        @Override
                        public Accessible getAccessible(Scene scene) {
                            return scene.getAccessible();
                        }
                    });
        }

        // Reserve space for 30 nodes in the dirtyNodes set.
        private static final int MIN_DIRTY_CAPACITY = 30;

        // For debugging
        private static boolean inSynchronizer = false;
        private static boolean inMousePick = false;
        private static boolean allowPGAccess = false;
        private static int pgAccessCount = 0;

        /**
         * Used for debugging purposes. Returns true if we are in either the
         * mouse event code (picking) or the synchronizer, or if the scene is
         * not yet initialized,
         *
         */
        static boolean isPGAccessAllowed() {
            return inSynchronizer || inMousePick || allowPGAccess;
        }

        static void setAllowPGAccess(boolean flag) {
            if (Utils.assertionEnabled()) {
                if (flag) {
                    pgAccessCount++;
                    allowPGAccess = true;
                }
                else {
                    if (pgAccessCount <= 0) {
                        throw new java.lang.AssertionError("*** pgAccessCount underflow");
                    }
                    if (--pgAccessCount == 0) {
                        allowPGAccess = false;
                    }
                }
            }
        }

        /**
         * If true, use the platform's drag gesture detection
         * else use Scene-level detection as per DnDGesture.process(MouseEvent, List)
         */
        private static final boolean PLATFORM_DRAG_GESTURE_INITIATION = false;

    /**
     * Set of dirty nodes; processed once per frame by the synchronizer.
     * When a node's state changes such that it becomes "dirty" with respect
     * to the graphics stack and requires synchronization, then that node
     * is added to this list. Note that if state on the Node changes, but it
     * was already dirty, then the Node doesn't add itself again.
     * <p>
     * Because at initialization time every node in the scene graph is dirty,
     * we have a special state and special code path during initialization
     * that does not involve adding each node to the dirtyNodes list. When
     * dirtyNodes is null, that means this Scene has not yet been synchronized.
     * A good default size is then created for the dirtyNodes list.
     * <p>
     * We double-buffer the set so that we can add new nodes to the
     * set while processing the existing set. This avoids our having to
     * take a snapshot of the set (e.g., with toArray()) and reduces garbage.
     */
    private Node[] dirtyNodes;
    private int dirtyNodesSize;

    /**
     * Add the specified node to this scene's dirty list. Called by the
     * markDirty method in Node or when the Node's scene changes.
     */
    void addToDirtyList(Node n) {
        if (dirtyNodes == null || dirtyNodesSize == 0) {
            if (peer != null) {
                Toolkit.getToolkit().requestNextPulse();
            }
        }

        if (dirtyNodes != null) {
            if (dirtyNodesSize == dirtyNodes.length) {
                Node[] tmp = new Node[dirtyNodesSize + (dirtyNodesSize >> 1)];
                System.arraycopy(dirtyNodes, 0, tmp, 0, dirtyNodesSize);
                dirtyNodes = tmp;
            }
            dirtyNodes[dirtyNodesSize++] = n;
        }
    }

    private void doCSSPass() {
        final Parent sceneRoot = getRoot();
        //
        // RT-17547: when the tree is synchronized, the dirty bits are
        // are cleared but the cssFlag might still be something other than
        // clean.
        //
        // Before RT-17547, the code checked the dirty bit. But this is
        // superfluous since the dirty bit will be set if the flag is not clean,
        // but the flag will never be anything other than clean if the dirty
        // bit is not set. The dirty bit is still needed, however, since setting
        // it ensures a pulse if no other dirty bits have been set.
        //
        // For the purpose of showing the change, the dirty bit
        // check code was commented out and not removed.
        //
//        if (sceneRoot.isDirty(com.sun.javafx.scene.DirtyBits.NODE_CSS)) {
        if (sceneRoot.cssFlag != CssFlags.CLEAN) {
            // The dirty bit isn't checked but we must ensure it is cleared.
            // The cssFlag is set to clean in either Node.processCSS or
            // NodeHelper.processCSS
            sceneRoot.clearDirty(com.sun.javafx.scene.DirtyBits.NODE_CSS);
            sceneRoot.processCSS();
        }
    }

    void doLayoutPass() {
        final Parent r = getRoot();
        if (r != null) {
            r.layout();
        }
    }

    /**
     * The peer of this scene
     */
    private TKScene peer;

    /*
     * Get Scene's peer
     */
    TKScene getPeer() {
        return peer;
    }

    /**
     * The scene pulse listener that gets called on toolkit pulses
     */
    ScenePulseListener scenePulseListener = new ScenePulseListener();

    private List<Runnable> preLayoutPulseListeners;
    private List<Runnable> postLayoutPulseListeners;

    /**
     * Adds a new scene pre layout pulse listener to this scene. Every time a pulse occurs,
     * this listener will be called on the JavaFX Application Thread directly
     * <strong>before</strong> the CSS and layout passes, and also before
     * any rendering is done for
     * this frame. This scene pulse listener is suitable for knowing when a
     * scenegraph pulse is happening and also for modifying the scenegraph
     * (as it is called before CSS and layout, so any changes made will be properly
     * styled and positioned).
     *
     * This method must be called on the JavaFX Application thread.
     *
     * @param r The Runnable to be called when the pulse occurs.
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     *
     * @throws NullPointerException if the provided Runnable is null.
     *
     * @since 9
     */
    public final void addPreLayoutPulseListener(Runnable r) {
        Toolkit.getToolkit().checkFxUserThread();

        if (r == null) {
            throw new NullPointerException("Scene pulse listener should not be null");
        }
        if (preLayoutPulseListeners == null) {
            preLayoutPulseListeners = new CopyOnWriteArrayList<>();
        }
        preLayoutPulseListeners.add(r);
    }

    /**
     * Removes a previously registered scene pre layout pulse listener from listening to
     * pulses in this scene. This method does nothing if the specified Runnable is
     * not already in the list.
     *
     * This method must be called on the JavaFX Application thread.
     *
     * @param r The Runnable that should no longer be called when the pulse
     * occurs for this scene.
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     *
     * @since 9
     */
    public final void removePreLayoutPulseListener(Runnable r) {
        Toolkit.getToolkit().checkFxUserThread();

        if (preLayoutPulseListeners == null) {
            return;
        }
        preLayoutPulseListeners.remove(r);
    }

    /**
     * Adds a new scene post layout pulse listener to this scene. Every time a pulse occurs,
     * this listener will be called on the JavaFX Application Thread directly
     * <strong>after</strong> the CSS and layout passes, but before any rendering is done for
     * this frame. This scene pulse listener is suitable for knowing when a
     * scenegraph pulse is happening, but it is not suited to use cases related
     * to modifying the scenegraph (as it is called after CSS and layout, so
     * any changes will possibly be incorrect until the next pulse is run).
     * An alternative (and better) solution for situations where a scenegraph
     * modification is required to happen is to use either the
     * {@link #addPreLayoutPulseListener(Runnable)} API or the the
     * {@link javafx.animation.AnimationTimer} API.
     *
     * This method must be called on the JavaFX Application thread.
     *
     * @param r The Runnable to be called when the pulse occurs.
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     *
     * @throws NullPointerException if the provided Runnable is null.
     *
     * @since 9
     */
    public final void addPostLayoutPulseListener(Runnable r) {
        Toolkit.getToolkit().checkFxUserThread();

        if (r == null) {
            throw new NullPointerException("Scene pulse listener should not be null");
        }
        if (postLayoutPulseListeners == null) {
            postLayoutPulseListeners = new CopyOnWriteArrayList<>();
        }
        postLayoutPulseListeners.add(r);
    }

    /**
     * Removes a previously registered scene post layout pulse listener from listening to
     * pulses in this scene. This method does nothing if the specified Runnable is
     * not already in the list.
     *
     * This method must be called on the JavaFX Application thread.
     *
     * @param r The Runnable that should no longer be called when the pulse
     * occurs for this scene.
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     *
     * @since 9
     */
    public final void removePostLayoutPulseListener(Runnable r) {
        Toolkit.getToolkit().checkFxUserThread();

        if (postLayoutPulseListeners == null) {
            return;
        }
        postLayoutPulseListeners.remove(r);
    }

    /**
     * Return the defined {@code SceneAntialiasing} for this {@code Scene}.
     * <p>
     * Note: this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * and {@link javafx.scene.SceneAntialiasing SceneAntialiasing}
     * for more information.
     * @return the SceneAntialiasing for this scene
     * @since JavaFX 8.0
     */
    public final SceneAntialiasing getAntiAliasing() {
        return antiAliasing;
    }

    private boolean getAntiAliasingInternal() {
        return (antiAliasing != null &&
                Toolkit.getToolkit().isMSAASupported() &&
                Platform.isSupported(ConditionalFeature.SCENE3D)) ?
                antiAliasing != SceneAntialiasing.DISABLED : false;
    }

    /**
     * The {@code Window} for this {@code Scene}
     */
    private ReadOnlyObjectWrapper<Window> window;

    void setWindow(Window value) {
        windowPropertyImpl().set(value);
    }

    public final Window getWindow() {
        return window == null ? null : window.get();
    }

    public final ReadOnlyObjectProperty<Window> windowProperty() {
        return windowPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Window> windowPropertyImpl() {
        if (window == null) {
            window = new ReadOnlyObjectWrapper<Window>() {
                private Window oldWindow;

                @Override protected void invalidated() {
                    final Window newWindow = get();
                    getKeyHandler().windowForSceneChanged(oldWindow, newWindow);
                    if (oldWindow != null) {
                        disposePeer();
                    }
                    if (newWindow != null) {
                        initPeer();
                    }
                    parentEffectiveOrientationInvalidated();

                    oldWindow = newWindow;
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "window";
                }
            };
        }
        return window;
    }

    void initPeer() {
        assert peer == null;

        Window window = getWindow();
        // initPeer() is only called from Window, either when the window
        // is being shown, or the window scene is being changed. In any case
        // this scene's window cannot be null.
        assert window != null;

        TKStage windowPeer = WindowHelper.getPeer(window);
        if (windowPeer == null) {
            // This is fine, the window is not visible. initPeer() will
            // be called again later, when the window is being shown.
            return;
        }

        final boolean isTransparentWindowsSupported = Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW);
        if (!isTransparentWindowsSupported) {
            PlatformImpl.addNoTransparencyStylesheetToScene(this);
        }

        PerformanceTracker.logEvent("Scene.initPeer started");

        setAllowPGAccess(true);

        Toolkit tk = Toolkit.getToolkit();
        peer = windowPeer.createTKScene(isDepthBufferInternal(), getAntiAliasingInternal(), acc);
        PerformanceTracker.logEvent("Scene.initPeer TKScene created");
        peer.setTKSceneListener(new ScenePeerListener());
        peer.setTKScenePaintListener(new ScenePeerPaintListener());
        PerformanceTracker.logEvent("Scene.initPeer TKScene set");
        peer.setRoot(getRoot().getPeer());
        peer.setFillPaint(getFill() == null ? null : tk.getPaint(getFill()));
        NodeHelper.updatePeer(getEffectiveCamera());
        peer.setCamera((NGCamera) getEffectiveCamera().getPeer());
        peer.markDirty();
        PerformanceTracker.logEvent("Scene.initPeer TKScene initialized");

        setAllowPGAccess(false);

        tk.addSceneTkPulseListener(scenePulseListener);
        // listen to dnd gestures coming from the platform
        if (PLATFORM_DRAG_GESTURE_INITIATION) {
            if (dragGestureListener == null) {
                dragGestureListener = new DragGestureListener();
            }
            tk.registerDragGestureListener(peer, EnumSet.allOf(TransferMode.class), dragGestureListener);
        }
        tk.enableDrop(peer, new DropTargetListener());
        tk.installInputMethodRequests(peer, new InputMethodRequestsDelegate());

        PerformanceTracker.logEvent("Scene.initPeer finished");
    }

    public void disposePeer() {
        if (peer == null) {
            // This is fine, the window is either not shown yet and there is no
            // need in disposing scene peer, or is hidden and disposePeer()
            // has already been called.
            return;
        }

        PerformanceTracker.logEvent("Scene.disposePeer started");

        Toolkit tk = Toolkit.getToolkit();
        tk.removeSceneTkPulseListener(scenePulseListener);
        if (accessible != null) {
            disposeAccessibles();
            Node root = getRoot();
            if (root != null) root.releaseAccessible();
            accessible.dispose();
            accessible = null;
        }
        peer.dispose();
        peer = null;

        PerformanceTracker.logEvent("Scene.disposePeer finished");
    }

    DnDGesture dndGesture = null;
    DragGestureListener dragGestureListener;
    /**
     * The horizontal location of this {@code Scene} on the {@code Window}.
     */
    private ReadOnlyDoubleWrapper x;

    private final void setX(double value) {
        xPropertyImpl().set(value);
    }

    public final double getX() {
        return x == null ? 0.0 : x.get();
    }

    public final ReadOnlyDoubleProperty xProperty() {
        return xPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper xPropertyImpl() {
        if (x == null) {
            x = new ReadOnlyDoubleWrapper(this, "x");
        }
        return x;
    }

    /**
     * The vertical location of this {@code Scene} on the {@code Window}.
     */
    private ReadOnlyDoubleWrapper y;

    private final void setY(double value) {
        yPropertyImpl().set(value);
    }

    public final double getY() {
        return y == null ? 0.0 : y.get();
    }

    public final ReadOnlyDoubleProperty yProperty() {
        return yPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper yPropertyImpl() {
        if (y == null) {
            y = new ReadOnlyDoubleWrapper(this, "y");
        }
        return y;
    }

    /**
     * The width of this {@code Scene}
     */
    private ReadOnlyDoubleWrapper width;

    private final void setWidth(double value) {
        widthPropertyImpl().set(value);
    }

    public final double getWidth() {
        return width == null ? 0.0 : width.get();
    }

    public final ReadOnlyDoubleProperty widthProperty() {
        return widthPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper widthPropertyImpl() {
        if (width == null) {
            width = new ReadOnlyDoubleWrapper() {

                @Override
                protected void invalidated() {
                    final Parent _root = getRoot();
                    //TODO - use a better method to update mirroring
                    if (_root.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                        NodeHelper.transformsChanged(_root);
                    }
                    if (_root.isResizable()) {
                        resizeRootOnSceneSizeChange(get() - _root.getLayoutX() - _root.getTranslateX(), _root.getLayoutBounds().getHeight());
                    }

                    getEffectiveCamera().setViewWidth(get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
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
     * The height of this {@code Scene}
     */
    private ReadOnlyDoubleWrapper height;

    private final void setHeight(double value) {
        heightPropertyImpl().set(value);
    }

    public final double getHeight() {
        return height == null ? 0.0 : height.get();
    }

    public final ReadOnlyDoubleProperty heightProperty() {
        return heightPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper heightPropertyImpl() {
        if (height == null) {
            height = new ReadOnlyDoubleWrapper() {

                @Override
                protected void invalidated() {
                    final Parent _root = getRoot();
                    if (_root.isResizable()) {
                        resizeRootOnSceneSizeChange(_root.getLayoutBounds().getWidth(), get() - _root.getLayoutY() - _root.getTranslateY());
                    }

                    getEffectiveCamera().setViewHeight(get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }

    void resizeRootOnSceneSizeChange(double newWidth, double newHeight) {
        getRoot().resize(newWidth, newHeight);
    }

    // Reusable target wrapper (to avoid creating new one for each picking)
    private TargetWrapper tmpTargetWrapper = new TargetWrapper();

    /**
     * Specifies the type of camera use for rendering this {@code Scene}.
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
                Camera oldCamera = null;

                @Override
                protected void invalidated() {
                    Camera _value = get();
                    if (_value != null) {
                        if (_value instanceof PerspectiveCamera
                                && !Platform.isSupported(ConditionalFeature.SCENE3D)) {
                            String logname = Scene.class.getName();
                            PlatformLogger.getLogger(logname).warning("System can't support "
                                    + "ConditionalFeature.SCENE3D");
                        }
                        // Illegal value if it belongs to other scene or any subscene
                        if ((_value.getScene() != null && _value.getScene() != Scene.this)
                                || _value.getSubScene() != null) {
                            throw new IllegalArgumentException(_value
                                    + "is already part of other scene or subscene");
                        }
                        // throws exception if the camera already has a different owner
                        _value.setOwnerScene(Scene.this);
                        _value.setViewWidth(getWidth());
                        _value.setViewHeight(getHeight());
                    }
                    if (oldCamera != null && oldCamera != _value) {
                        oldCamera.setOwnerScene(null);
                    }
                    oldCamera = _value;
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "camera";
                }
            };
        }
        return camera;
    }

    Camera getEffectiveCamera() {
        final Camera cam = getCamera();
        if (cam == null
                || (cam instanceof PerspectiveCamera
                && !Platform.isSupported(ConditionalFeature.SCENE3D))) {
            if (defaultCamera == null) {
                defaultCamera = new ParallelCamera();
                defaultCamera.setOwnerScene(this);
                defaultCamera.setViewWidth(getWidth());
                defaultCamera.setViewHeight(getHeight());
            }
            return defaultCamera;
        }

        return cam;
    }

    // Used by the camera
    void markCameraDirty() {
        markDirty(DirtyBits.CAMERA_DIRTY);
        setNeedsRepaint();
    }

    void markCursorDirty() {
        markDirty(DirtyBits.CURSOR_DIRTY);
    }

    /**
     * Defines the background fill of this {@code Scene}. Both a {@code null}
     * value meaning 'paint no background' and a {@link javafx.scene.paint.Paint}
     * with transparency are supported. The default fill of the Scene is
     * {@link Color#WHITE}, but it is more commonly the case that the initial
     * color shown to users is the background fill of the
     * {@link #rootProperty() root node} of the {@code Scene}, as it typically is
     * stretched to take up all available space in the {@code Scene}. The
     * root node of the {@code Scene} is given the CSS style class 'root', and
     * the default user agent stylesheets that ship with JavaFX (presently
     * Caspian and Modena) apply styling on to this root style class. In the
     * case of Caspian this does not impact the background fill color of the
     * root node, but in the case of Modena the default fill is set to be a
     * light gray color.
     *
     * @defaultValue WHITE
     */
    private ObjectProperty<Paint> fill;

    public final void setFill(Paint value) {
        fillProperty().set(value);
    }

    public final Paint getFill() {
        return fill == null ? Color.WHITE : fill.get();
    }

    public final ObjectProperty<Paint> fillProperty() {
        if (fill == null) {
            fill = new ObjectPropertyBase<Paint>(Color.WHITE) {

                @Override
                protected void invalidated() {
                    markDirty(DirtyBits.FILL_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Scene.this;
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
     * Defines the root {@code Node} of the scene graph.
     * If a {@code Group} is used as the root, the
     * contents of the scene graph will be clipped by the scene's width and height and
     * changes to the scene's size (if user resizes the stage) will not alter the
     * layout of the scene graph.    If a resizable node (layout {@code Region} or
     * {@code Control}) is set as the root, then the root's size will track the
     * scene's size, causing the contents to be relayed out as necessary.
     *
     * Scene doesn't accept null root.
     *
     */
    private ObjectProperty<Parent> root;

    public final void setRoot(Parent value) {
        rootProperty().set(value);
    }

    public final Parent getRoot() {
        return root == null ? null : root.get();
    }

    Parent oldRoot;
    public final ObjectProperty<Parent> rootProperty() {
        if (root == null) {
            root = new ObjectPropertyBase<Parent>() {

                private void forceUnbind() {
                    System.err.println("Unbinding illegal root.");
                    unbind();
                }

                @Override
                protected void invalidated() {
                    Parent _value = get();

                    if (_value == null) {
                        if (isBound()) forceUnbind();
                        throw new NullPointerException("Scene's root cannot be null");
                    }

                    if (_value.getParent() != null) {
                        if (isBound()) forceUnbind();
                        throw new IllegalArgumentException(_value +
                                "is already inside a scene-graph and cannot be set as root");
                    }
                    if (_value.getClipParent() != null) {
                        if (isBound()) forceUnbind();
                        throw new IllegalArgumentException(_value +
                                "is set as a clip on another node, so cannot be set as root");
                    }
                    if (_value.getScene() != null && _value.getScene().getRoot() == _value && _value.getScene() != Scene.this) {
                        if (isBound()) forceUnbind();
                        throw new IllegalArgumentException(_value +
                                "is already set as root of another scene");
                    }

                    if (oldRoot != null) {
                        oldRoot.setScenes(null, null);
                    }
                    oldRoot = _value;
                    _value.getStyleClass().add(0, "root");
                    _value.setScenes(Scene.this, null);
                    markDirty(DirtyBits.ROOT_DIRTY);
                    _value.resize(getWidth(), getHeight()); // maybe no-op if root is not resizable
                    _value.requestLayout();
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "root";
                }
            };
        }
        return root;
    }

    void setNeedsRepaint() {
        if (this.peer != null) {
            peer.entireSceneNeedsRepaint();
        }
    }

    // Process CSS and layout and sync the scene prior to the snapshot
    // operation of the given node for this scene (currently the node
    // is unused but could possibly be used in the future to optimize this)
    void doCSSLayoutSyncForSnapshot(Node node) {
        if (!sizeInitialized) {
            preferredSize();
        } else {
            doCSSPass();
        }

        // we do not need pulse in the snapshot code
        // because this scene can be stage-less
        doLayoutPass();

        getRoot().updateBounds();
        if (peer != null) {
            peer.waitForRenderingToComplete();
            peer.waitForSynchronization();
            try {
                // Run the synchronizer while holding the render lock
                scenePulseListener.synchronizeSceneNodes();
            } finally {
                peer.releaseSynchronization(false);
            }
        } else {
            scenePulseListener.synchronizeSceneNodes();
        }

    }

    // Shared method for Scene.snapshot and Node.snapshot. It is static because
    // we might be doing a Node snapshot with a null scene
    static WritableImage doSnapshot(Scene scene,
            double x, double y, double w, double h,
            Node root, BaseTransform transform, boolean depthBuffer,
            Paint fill, Camera camera, WritableImage wimg) {

        Toolkit tk = Toolkit.getToolkit();
        Toolkit.ImageRenderingContext context = new Toolkit.ImageRenderingContext();

        int xMin = (int)Math.floor(x);
        int yMin = (int)Math.floor(y);
        int width;
        int height;
        if (wimg == null) {
            int xMax = (int)Math.ceil(x + w);
            int yMax = (int)Math.ceil(y + h);
            width = Math.max(xMax - xMin, 1);
            height = Math.max(yMax - yMin, 1);
            wimg = new WritableImage(width, height);
        } else {
            width = (int)wimg.getWidth();
            height = (int)wimg.getHeight();
        }

        setAllowPGAccess(true);
        context.x = xMin;
        context.y = yMin;
        context.width = width;
        context.height = height;
        context.transform = transform;
        context.depthBuffer = depthBuffer;
        context.root = root.getPeer();
        context.platformPaint = fill == null ? null : tk.getPaint(fill);
        double cameraViewWidth = 1.0;
        double cameraViewHeight = 1.0;
        if (camera != null) {
            // temporarily adjust camera viewport to the snapshot size
            cameraViewWidth = camera.getViewWidth();
            cameraViewHeight = camera.getViewHeight();
            camera.setViewWidth(width);
            camera.setViewHeight(height);
            NodeHelper.updatePeer(camera);
            context.camera = camera.getPeer();
        } else {
            context.camera = null;
        }

        // Grab the lights from the scene
        context.lights = null;
        if (scene != null && !scene.lights.isEmpty()) {
            context.lights = new NGLightBase[scene.lights.size()];
            for (int i = 0; i < scene.lights.size(); i++) {
                context.lights[i] = scene.lights.get(i).getPeer();
            }
        }

        Toolkit.WritableImageAccessor accessor = Toolkit.getWritableImageAccessor();
        context.platformImage = accessor.getTkImageLoader(wimg);
        setAllowPGAccess(false);
        Object tkImage = tk.renderToImage(context);

        if (tkImage != null) {
            accessor.loadTkImage(wimg, tkImage);
        }

        if (camera != null) {
            setAllowPGAccess(true);
            camera.setViewWidth(cameraViewWidth);
            camera.setViewHeight(cameraViewHeight);
            NodeHelper.updatePeer(camera);
            setAllowPGAccess(false);
        }

        // if this scene belongs to some stage
        // we need to mark the entire scene as dirty
        // because dirty logic is buggy
        if (scene != null && scene.peer != null) {
            scene.setNeedsRepaint();
        }

        return wimg;
    }

    /**
     * Implementation method for snapshot
     */
    private WritableImage doSnapshot(WritableImage img) {
        // TODO: no need to do CSS, layout or sync in the deferred case,
        // if this scene is attached to a visible stage
        doCSSLayoutSyncForSnapshot(getRoot());

        double w = getWidth();
        double h = getHeight();
        BaseTransform transform = BaseTransform.IDENTITY_TRANSFORM;

        return doSnapshot(this, 0, 0, w, h,
                getRoot(), transform, isDepthBufferInternal(),
                getFill(), getEffectiveCamera(), img);
    }

    // Pulse listener used to run all deferred (async) snapshot requests
    private static TKPulseListener snapshotPulseListener = null;

    private static List<Runnable> snapshotRunnableListA;
    private static List<Runnable> snapshotRunnableListB;
    private static List<Runnable> snapshotRunnableList;

    static void addSnapshotRunnable(final Runnable runnable) {
        Toolkit.getToolkit().checkFxUserThread();

        if (snapshotPulseListener == null) {
            snapshotRunnableListA = new ArrayList<Runnable>();
            snapshotRunnableListB = new ArrayList<Runnable>();
            snapshotRunnableList = snapshotRunnableListA;

            snapshotPulseListener = () -> {
                if (snapshotRunnableList.size() > 0) {
                    List<Runnable> runnables = snapshotRunnableList;
                    if (snapshotRunnableList == snapshotRunnableListA) {
                        snapshotRunnableList = snapshotRunnableListB;
                    } else {
                        snapshotRunnableList = snapshotRunnableListA;
                    }
                    for (Runnable r : runnables) {
                        try {
                            r.run();
                        } catch (Throwable th) {
                            System.err.println("Exception in snapshot runnable");
                            th.printStackTrace(System.err);
                        }
                    }
                    runnables.clear();
                }
            };

            // Add listener that will be called after all of the scenes have
            // had layout and CSS processing, and have been synced
            Toolkit.getToolkit().addPostSceneTkPulseListener(snapshotPulseListener);
        }

        final AccessControlContext acc = AccessController.getContext();
        snapshotRunnableList.add(() -> {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                runnable.run();
                return null;
            }, acc);
        });
        Toolkit.getToolkit().requestNextPulse();
    }

    /**
     * Takes a snapshot of this scene and returns the rendered image when
     * it is ready.
     * CSS and layout processing will be done for the scene prior to
     * rendering it.
     * The entire destination image is cleared using the fill {@code Paint}
     * of this scene. The nodes in the scene are then rendered to the image.
     * The point (0,0) in scene coordinates is mapped to (0,0) in the image.
     * If the image is smaller than the size of the scene, then the rendering
     * will be clipped by the image.
     *
     * <p>
     * When taking a snapshot of a scene that is being animated, either
     * explicitly by the application or implicitly (such as chart animation),
     * the snapshot will be rendered based on the state of the scene graph at
     * the moment the snapshot is taken and will not reflect any subsequent
     * animation changes.
     * </p>
     *
     * @param image the writable image that will be used to hold the rendered scene.
     * It may be null in which case a new WritableImage will be constructed.
     * If the image is non-null, the scene will be rendered into the
     * existing image.
     * In this case, the width and height of the image determine the area
     * that is rendered instead of the width and height of the scene.
     *
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     *
     * @return the rendered image
     * @since JavaFX 2.2
     */
    public WritableImage snapshot(WritableImage image) {
        Toolkit.getToolkit().checkFxUserThread();

        return doSnapshot(image);
    }

    /**
     * Takes a snapshot of this scene at the next frame and calls the
     * specified callback method when the image is ready.
     * CSS and layout processing will be done for the scene prior to
     * rendering it.
     * The entire destination image is cleared using the fill {@code Paint}
     * of this scene. The nodes in the scene are then rendered to the image.
     * The point (0,0) in scene coordinates is mapped to (0,0) in the image.
     * If the image is smaller than the size of the scene, then the rendering
     * will be clipped by the image.
     *
     * <p>
     * This is an asynchronous call, which means that other
     * events or animation might be processed before the scene is rendered.
     * If any such events modify a node in the scene that modification will
     * be reflected in the rendered image (as it will also be reflected in
     * the frame rendered to the Stage).
     * </p>
     *
     * <p>
     * When taking a snapshot of a scene that is being animated, either
     * explicitly by the application or implicitly (such as chart animation),
     * the snapshot will be rendered based on the state of the scene graph at
     * the moment the snapshot is taken and will not reflect any subsequent
     * animation changes.
     * </p>
     *
     * @param callback a class whose call method will be called when the image
     * is ready. The SnapshotResult that is passed into the call method of
     * the callback will contain the rendered image and the source scene
     * that was rendered. The callback parameter must not be null.
     *
     * @param image the writable image that will be used to hold the rendered scene.
     * It may be null in which case a new WritableImage will be constructed.
     * If the image is non-null, the scene will be rendered into the
     * existing image.
     * In this case, the width and height of the image determine the area
     * that is rendered instead of the width and height of the scene.
     *
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     *
     * @throws NullPointerException if the callback parameter is null.
     * @since JavaFX 2.2
     */
    public void snapshot(Callback<SnapshotResult, Void> callback, WritableImage image) {
        Toolkit.getToolkit().checkFxUserThread();
        if (callback == null) {
            throw new NullPointerException("The callback must not be null");
        }

        final Callback<SnapshotResult, Void> theCallback = callback;
        final WritableImage theImage = image;

        // Create a deferred runnable that will be run from a pulse listener
        // that is called after all of the scenes have been synced but before
        // any of them have been rendered.
        final Runnable snapshotRunnable = () -> {
            WritableImage img = doSnapshot(theImage);
//                System.err.println("Calling snapshot callback");
            SnapshotResult result = new SnapshotResult(img, Scene.this, null);
            try {
                Void v = theCallback.call(result);
            } catch (Throwable th) {
                System.err.println("Exception in snapshot callback");
                th.printStackTrace(System.err);
            }
        };
//        System.err.println("Schedule a snapshot in the future");
        addSnapshotRunnable(snapshotRunnable);
    }

    /**
     * Defines the mouse cursor for this {@code Scene}.
     */
    private ObjectProperty<Cursor> cursor;

    public final void setCursor(Cursor value) {
        cursorProperty().set(value);
    }

    public final Cursor getCursor() {
        return cursor == null ? null : cursor.get();
    }

    public final ObjectProperty<Cursor> cursorProperty() {
        if (cursor == null) {
            cursor = new ObjectPropertyBase<Cursor>() {
                         @Override
                         protected void invalidated() {
                             markCursorDirty();
                         }

                         @Override
                         public Object getBean() {
                             return Scene.this;
                         }

                         @Override
                         public String getName() {
                             return "cursor";
                         }
                     };
        }
        return cursor;
    }

    /**
     * Looks for any node within the scene graph based on the specified CSS selector.
     * If more than one node matches the specified selector, this function
     * returns the first of them.
     * If no nodes are found with this id, then null is returned.
     *
     * @param selector The css selector to look up
     * @return the {@code Node} in the scene which matches the CSS {@code selector},
     * or {@code null} if none is found.
     */
     public Node lookup(String selector) {
         return getRoot().lookup(selector);
     }
    /**
     * A ObservableList of string URLs linking to the stylesheets to use with this scene's
     * contents. For additional information about using CSS with the
     * scene graph, see the <a href="doc-files/cssref.html">CSS Reference
     * Guide</a>.
     */
    private final ObservableList<String> stylesheets  = new TrackableObservableList<String>() {
        @Override
        protected void onChanged(Change<String> c) {
            StyleManager.getInstance().stylesheetsChanged(Scene.this, c);
            // RT-9784 - if stylesheet is removed, reset styled properties to
            // their initial value.
            c.reset();
            while(c.next()) {
                if (c.wasRemoved() == false) {
                    continue;
                }
                break; // no point in resetting more than once...
            }
            getRoot().reapplyCSS();
        }
    };

    /**
     * Gets an observable list of string URLs linking to the stylesheets to use
     * with this scene's contents.
     * <p>
     * The URL is a hierarchical URI of the form [scheme:][//authority][path]. If the URL
     * does not have a [scheme:] component, the URL is considered to be the [path] component only.
     * Any leading '/' character of the [path] is ignored and the [path] is treated as a path relative to
     * the root of the application's classpath.
     * </p>
     * <pre><code>
     *
     * package com.example.javafx.app;
     *
     * import javafx.application.Application;
     * import javafx.scene.Group;
     * import javafx.scene.Scene;
     * import javafx.stage.Stage;
     *
     * public class MyApp extends Application {
     *
     *     {@literal @}Override public void start(Stage stage) {
     *         Scene scene = new Scene(new Group());
     *         scene.getStylesheets().add("/com/example/javafx/app/mystyles.css");
     *         stage.setScene(scene);
     *         stage.show();
     *     }
     *
     *     public static void main(String[] args) {
     *         launch(args);
     *     }
     * }
     * </code></pre>
     * For additional information about using CSS with the scene graph,
     * see the <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     *
     * @return the list of stylesheets to use with this scene
     */
    public final ObservableList<String> getStylesheets() { return stylesheets; }

    private ObjectProperty<String> userAgentStylesheet = null;

    /**
     * @return the userAgentStylesheet property.
     * @see #getUserAgentStylesheet()
     * @see #setUserAgentStylesheet(String)
     * @since  JavaFX 8u20
     */
    public final ObjectProperty<String> userAgentStylesheetProperty() {
        if (userAgentStylesheet == null) {
            userAgentStylesheet = new SimpleObjectProperty<String>(Scene.this, "userAgentStylesheet", null) {
                @Override protected void invalidated() {
                    StyleManager.getInstance().forget(Scene.this);
                    getRoot().reapplyCSS();
                }
            };
        }
        return userAgentStylesheet;
    }

    /**
     * Get the URL of the user-agent stylesheet that will be used by this Scene. If the URL has not been set,
     * the platform-default user-agent stylesheet will be used.
     * <p>
     * For additional information about using CSS with the scene graph,
     * see the <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     * </p>
     * @return The URL of the user-agent stylesheet that will be used by this Scene,
     * or null if has not been set.
     * @since  JavaFX 8u20
     */
    public final String getUserAgentStylesheet() {
        return userAgentStylesheet == null ? null : userAgentStylesheet.get();
    }

    /**
     * Set the URL of the user-agent stylesheet that will be used by this Scene in place of the
     * the platform-default user-agent stylesheet. If the URL does not resolve to a valid location,
     * the platform-default user-agent stylesheet will be used.
     * <p>
     * For additional information about using CSS with the scene graph,
     * see the <a href="doc-files/cssref.html">CSS Reference Guide</a>.
     * </p>
     * @param url The URL is a hierarchical URI of the form [scheme:][//authority][path]. If the URL
     * does not have a [scheme:] component, the URL is considered to be the [path] component only.
     * Any leading '/' character of the [path] is ignored and the [path] is treated as a path relative to
     * the root of the application's classpath.
     * @since  JavaFX 8u20
     */
    public final void setUserAgentStylesheet(String url) {
        userAgentStylesheetProperty().set(url);
    }

    /**
     * Retrieves the depth buffer attribute for this scene.
     * @return the depth buffer attribute.
     */
    public final boolean isDepthBuffer() {
        return depthBuffer;
    }

    boolean isDepthBufferInternal() {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            return false;
        }
        return depthBuffer;
    }

    private void init(double width, double height) {
        if (width >= 0) {
            widthSetByUser = width;
            setWidth((float)width);
        }
        if (height >= 0) {
            heightSetByUser = height;
            setHeight((float)height);
        }
        sizeInitialized = (widthSetByUser >= 0 && heightSetByUser >= 0);
    }

    private void init() {
        if (PerformanceTracker.isLoggingEnabled()) {
            PerformanceTracker.logEvent("Scene.init for [" + this + "]");
        }
        mouseHandler = new MouseHandler();
        clickGenerator = new ClickGenerator();

        if (PerformanceTracker.isLoggingEnabled()) {
            PerformanceTracker.logEvent("Scene.init for [" + this + "] - finished");
        }
    }

    void preferredSize() {
        final Parent root = getRoot();

        // one or the other isn't initialized, need to perform layout in
        // order to ensure we can properly measure the preferred size of the
        // scene
        doCSSPass();

        resizeRootToPreferredSize(root);
        doLayoutPass();

        if (widthSetByUser < 0) {
            setWidth(root.isResizable()? root.getLayoutX() + root.getTranslateX() + root.getLayoutBounds().getWidth() :
                            root.getBoundsInParent().getMaxX());
        } else {
            setWidth(widthSetByUser);
        }

        if (heightSetByUser < 0) {
            setHeight(root.isResizable()? root.getLayoutY() + root.getTranslateY() + root.getLayoutBounds().getHeight() :
                            root.getBoundsInParent().getMaxY());
        } else {
            setHeight(heightSetByUser);
        }

        sizeInitialized = (getWidth() > 0) && (getHeight() > 0);

        PerformanceTracker.logEvent("Scene preferred bounds computation complete");
    }

    final void resizeRootToPreferredSize(Parent root) {
        final double preferredWidth;
        final double preferredHeight;

        final Orientation contentBias = root.getContentBias();
        if (contentBias == null) {
            preferredWidth = getPreferredWidth(root, widthSetByUser, -1);
            preferredHeight = getPreferredHeight(root, heightSetByUser, -1);
        } else if (contentBias == Orientation.HORIZONTAL) {
            // height depends on width
            preferredWidth = getPreferredWidth(root, widthSetByUser, -1);
            preferredHeight = getPreferredHeight(root, heightSetByUser,
                                                       preferredWidth);
        } else /* if (contentBias == Orientation.VERTICAL) */ {
            // width depends on height
            preferredHeight = getPreferredHeight(root, heightSetByUser, -1);
            preferredWidth = getPreferredWidth(root, widthSetByUser,
                                                     preferredHeight);
        }

        root.resize(preferredWidth, preferredHeight);
    }

    private static double getPreferredWidth(Parent root,
                                            double forcedWidth,
                                            double height) {
        if (forcedWidth >= 0) {
            return forcedWidth;
        }
        final double normalizedHeight = (height >= 0) ? height : -1;
        return root.boundedSize(root.prefWidth(normalizedHeight),
                                root.minWidth(normalizedHeight),
                                root.maxWidth(normalizedHeight));
    }

    private static double getPreferredHeight(Parent root,
                                             double forcedHeight,
                                             double width) {
        if (forcedHeight >= 0) {
            return forcedHeight;
        }
        final double normalizedWidth = (width >= 0) ? width : -1;
        return root.boundedSize(root.prefHeight(normalizedWidth),
                                root.minHeight(normalizedWidth),
                                root.maxHeight(normalizedWidth));
    }

    private PerformanceTracker tracker;
    private static final Object trackerMonitor = new Object();

    // mouse events handling
    private MouseHandler mouseHandler;
    private ClickGenerator clickGenerator;

    // gesture events handling
    private Point2D cursorScreenPos;
    private Point2D cursorScenePos;

    private static class TouchGesture {
        WeakReference<EventTarget> target;
        Point2D sceneCoords;
        Point2D screenCoords;
        boolean finished;
    }

    private final TouchGesture scrollGesture = new TouchGesture();
    private final TouchGesture zoomGesture = new TouchGesture();
    private final TouchGesture rotateGesture = new TouchGesture();
    private final TouchGesture swipeGesture = new TouchGesture();

    // touch events handling
    private TouchMap touchMap = new TouchMap();
    private TouchEvent nextTouchEvent = null;
    private TouchPoint[] touchPoints = null;
    private int touchEventSetId = 0;
    private int touchPointIndex = 0;
    private Map<Integer, EventTarget> touchTargets =
            new HashMap<Integer, EventTarget>();

    void processMouseEvent(MouseEvent e) {
        mouseHandler.process(e, false);
    }

    private void processMenuEvent(double x2, double y2, double xAbs, double yAbs, boolean isKeyboardTrigger) {
        EventTarget eventTarget = null;
        Scene.inMousePick = true;
        if (isKeyboardTrigger) {
            Node sceneFocusOwner = getFocusOwner();

            // for keyboard triggers set coordinates inside focus owner
            final double xOffset = xAbs - x2;
            final double yOffset = yAbs - y2;
            if (sceneFocusOwner != null) {
                final Bounds bounds = sceneFocusOwner.localToScene(
                        sceneFocusOwner.getBoundsInLocal());
                x2 = bounds.getMinX() + bounds.getWidth() / 4;
                y2 = bounds.getMinY() + bounds.getHeight() / 2;
                eventTarget = sceneFocusOwner;
            } else {
                x2 = Scene.this.getWidth() / 4;
                y2 = Scene.this.getWidth() / 2;
                eventTarget = Scene.this;
            }

            xAbs = x2 + xOffset;
            yAbs = y2 + yOffset;
        }

        final PickResult res = pick(x2, y2);

        if (!isKeyboardTrigger) {
            eventTarget = res.getIntersectedNode();
            if (eventTarget == null) {
                eventTarget = this;
            }
        }

        if (eventTarget != null) {
            ContextMenuEvent context = new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                    x2, y2, xAbs, yAbs, isKeyboardTrigger, res);
            Event.fireEvent(eventTarget, context);
        }
        Scene.inMousePick = false;
    }

    private void processGestureEvent(GestureEvent e, TouchGesture gesture) {
        EventTarget pickedTarget = null;

        if (e.getEventType() == ZoomEvent.ZOOM_STARTED ||
                e.getEventType() == RotateEvent.ROTATION_STARTED ||
                e.getEventType() == ScrollEvent.SCROLL_STARTED) {
            gesture.target = null;
            gesture.finished = false;
        }

        if (gesture.target != null && (!gesture.finished || e.isInertia())) {
            pickedTarget = gesture.target.get();
        } else {
            pickedTarget = e.getPickResult().getIntersectedNode();
            if (pickedTarget == null) {
                pickedTarget = this;
            }
        }

        if (e.getEventType() == ZoomEvent.ZOOM_STARTED ||
                e.getEventType() == RotateEvent.ROTATION_STARTED ||
                e.getEventType() == ScrollEvent.SCROLL_STARTED) {
            gesture.target = new WeakReference<>(pickedTarget);
        }
        if (e.getEventType() != ZoomEvent.ZOOM_FINISHED &&
                e.getEventType() != RotateEvent.ROTATION_FINISHED &&
                e.getEventType() != ScrollEvent.SCROLL_FINISHED &&
                !e.isInertia()) {
            gesture.sceneCoords = new Point2D(e.getSceneX(), e.getSceneY());
            gesture.screenCoords = new Point2D(e.getScreenX(), e.getScreenY());
        }

        if (pickedTarget != null) {
            Event.fireEvent(pickedTarget, e);
        }

        if (e.getEventType() == ZoomEvent.ZOOM_FINISHED ||
                e.getEventType() == RotateEvent.ROTATION_FINISHED ||
                e.getEventType() == ScrollEvent.SCROLL_FINISHED) {
            gesture.finished = true;
        }
    }

    private void processTouchEvent(TouchEvent e, TouchPoint[] touchPoints) {
        inMousePick = true;
        touchEventSetId++;

        List<TouchPoint> touchList = Arrays.asList(touchPoints);

        // fire all the events
        for (TouchPoint tp : touchPoints) {
            if (tp.getTarget() != null) {
                EventType<TouchEvent> type = null;
                switch (tp.getState()) {
                    case MOVED:
                        type = TouchEvent.TOUCH_MOVED;
                        break;
                    case PRESSED:
                        type = TouchEvent.TOUCH_PRESSED;
                        break;
                    case RELEASED:
                        type = TouchEvent.TOUCH_RELEASED;
                        break;
                    case STATIONARY:
                        type = TouchEvent.TOUCH_STATIONARY;
                        break;
                }

                for (TouchPoint t : touchPoints) {
                    TouchPointHelper.reset(t);
                }

                TouchEvent te = new TouchEvent(type, tp, touchList,
                        touchEventSetId, e.isShiftDown(), e.isControlDown(),
                        e.isAltDown(), e.isMetaDown());

                Event.fireEvent(tp.getTarget(), te);
            }
        }

        // process grabbing
        for (TouchPoint tp : touchPoints) {
            EventTarget grabbed = tp.getGrabbed();
            if (grabbed != null) {
                touchTargets.put(tp.getId(), grabbed);
            };

            if (grabbed == null || tp.getState() == TouchPoint.State.RELEASED) {
                touchTargets.remove(tp.getId());
            }
        }

        inMousePick = false;
    }

    /**
     * Note: The only user of this method is in unit test: PickAndContainTest.
     */
    Node test_pick(double x, double y) {
        inMousePick = true;
        PickResult result = mouseHandler.pickNode(new PickRay(x, y, 1,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        inMousePick = false;
        if (result != null) {
            return result.getIntersectedNode();
        }
        return null;
    }

    private PickResult pick(final double x, final double y) {
        pick(tmpTargetWrapper, x, y);
        return tmpTargetWrapper.getResult();
    }

    private boolean isInScene(double x, double y) {
        if (x < 0 || y < 0 || x > getWidth() || y > getHeight())  {
            return false;
        }

        Window w = getWindow();
        if (w instanceof Stage
                && ((Stage) w).getStyle() == StageStyle.TRANSPARENT
                && getFill() == null) {
            return false;
        }

        return true;
    }

    private void pick(TargetWrapper target, final double x, final double y) {
        final PickRay pickRay = getEffectiveCamera().computePickRay(
                x, y, null);

        final double mag = pickRay.getDirectionNoClone().length();
        pickRay.getDirectionNoClone().normalize();
        final PickResult res = mouseHandler.pickNode(pickRay);
        if (res != null) {
            target.setNodeResult(res);
        } else {
            //TODO: is this the intersection with projection plane?
            Vec3d o = pickRay.getOriginNoClone();
            Vec3d d = pickRay.getDirectionNoClone();
            target.setSceneResult(new PickResult(
                    null, new Point3D(
                    o.x + mag * d.x,
                    o.y + mag * d.y,
                    o.z + mag * d.z),
                    mag),
                    isInScene(x, y) ? this : null);
        }
    }

    /***************************************************************************
     *                                                                         *
     * Key Events and Focus Traversal                                          *
     *                                                                         *
     **************************************************************************/

    /*
     * We cannot initialize keyHandler in init because some of the triggers
     * access it before the init block.
     * No clue why def keyHandler = bind lazy {KeyHandler{scene:this};}
     * does not compile.
     */
    private KeyHandler keyHandler = null;
    private KeyHandler getKeyHandler() {
        if (keyHandler == null) {
            keyHandler = new KeyHandler();
        }
        return keyHandler;
    }
    /**
     * Set to true if something has happened to the focused node that makes
     * it no longer eligible to have the focus.
     *
     */
    private boolean focusDirty = true;

    final void setFocusDirty(boolean value) {
        if (!focusDirty) {
            Toolkit.getToolkit().requestNextPulse();
        }
        focusDirty = value;
    }

    final boolean isFocusDirty() {
        return focusDirty;
    }

    private TopMostTraversalEngine traversalEngine = new SceneTraversalEngine(this);

    /**
     * Traverses focus from the given node in the given direction.
     */
    boolean traverse(Node node, Direction dir) {
        if (node.getSubScene() != null) {
            return node.getSubScene().traverse(node, dir);
        }
        return traversalEngine.trav(node, dir) != null;
    }

    /**
     * Moves the focus to a reasonable initial location. Called when a scene's
     * focus is dirty and there's no current owner, or if the owner has been
     * removed from the scene.
     */
    private void focusInitial() {
        traversalEngine.traverseToFirst();
    }

    /**
     * Moves the focus to a reasonble location "near" the given node.
     * Called when the focused node is no longer eligible to have
     * the focus because it has become invisible or disabled. This
     * function assumes that it is still a member of the same scene.
     */
    private void focusIneligible(Node node) {
        traverse(node, Direction.NEXT);
    }

    public void processKeyEvent(KeyEvent e) {
        if (dndGesture != null) {
            if (!dndGesture.processKey(e)) {
                dndGesture = null;
            }
        }

        getKeyHandler().process(e);
    }

    void requestFocus(Node node) {
        getKeyHandler().requestFocus(node);
    }

    private Node oldFocusOwner;

    /**
      * The scene's current focus owner node. This node's "focused"
      * variable might be false if this scene has no window, or if the
      * window is inactive (window.focused == false).
      * @since JavaFX 2.2
      */
    private ReadOnlyObjectWrapper<Node> focusOwner = new ReadOnlyObjectWrapper<Node>(this, "focusOwner") {

        @Override
        protected void invalidated() {
            if (oldFocusOwner != null) {
                ((Node.FocusedProperty) oldFocusOwner.focusedProperty()).store(false);
            }
            Node value = get();
            if (value != null) {
                ((Node.FocusedProperty) value.focusedProperty()).store(keyHandler.windowFocused);
                if (value != oldFocusOwner) {
                    value.getScene().enableInputMethodEvents(
                            value.getInputMethodRequests() != null
                            && value.getOnInputMethodTextChanged() != null);
                }
            }
            // for the rest of the method we need to update the oldFocusOwner
            // and use a local copy of it because the user handlers can cause
            // recurrent calls of requestFocus
            Node localOldOwner = oldFocusOwner;
            oldFocusOwner = value;
            if (localOldOwner != null) {
                ((Node.FocusedProperty) localOldOwner.focusedProperty()).notifyListeners();
            }
            if (value != null) {
                ((Node.FocusedProperty) value.focusedProperty()).notifyListeners();
            }
            PlatformLogger logger = Logging.getFocusLogger();
            if (logger.isLoggable(Level.FINE)) {
                if (value == get()) {
                    logger.fine("Changed focus from "
                            + localOldOwner + " to " + value);
                } else {
                    logger.fine("Changing focus from "
                            + localOldOwner + " to " + value
                            + " canceled by nested requestFocus");
                }
            }
            if (accessible != null) {
                accessible.sendNotification(AccessibleAttribute.FOCUS_NODE);
            }
        }
    };

    public final Node getFocusOwner() {
        return focusOwner.get();
    }

    public final ReadOnlyObjectProperty<Node> focusOwnerProperty() {
        return focusOwner.getReadOnlyProperty();
    }

    // For testing.
    void focusCleanup() {
        scenePulseListener.focusCleanup();
    }

    private void processInputMethodEvent(InputMethodEvent e) {
        Node node = getFocusOwner();
        if (node != null) {
            node.fireEvent(e);
        }
    }

    public void enableInputMethodEvents(boolean enable) {
       if (peer != null) {
           peer.enableInputMethodEvents(enable);
       }
    }

    /**
     * Returns true if this scene is quiescent, i.e. it has no activity
     * pending on it such as CSS processing or layout requests.
     *
     * Intended to be used for tests only
     *
     * @return boolean indicating whether the scene is quiescent
     */
    boolean isQuiescent() {
        final Parent r = getRoot();
        return !isFocusDirty()
               && (r == null || (r.cssFlag == CssFlags.CLEAN &&
                r.layoutFlag == LayoutFlags.CLEAN));
    }

    /**
     * A listener for pulses, used for testing. If non-null, this is called at
     * the very end of ScenePulseListener.pulse().
     *
     * Intended to be used for tests only
     */
    Runnable testPulseListener = null;

    /**
     * Set the specified dirty bit and mark the peer as dirty
     */
    private void markDirty(DirtyBits dirtyBit) {
        setDirty(dirtyBit);
        if (peer != null) {
            Toolkit.getToolkit().requestNextPulse();
        }
    }

    /**
     * Set the specified dirty bit
     */
    private void setDirty(DirtyBits dirtyBit) {
        dirtyBits.add(dirtyBit);
    }

    /**
     * Test the specified dirty bit
     */
    private boolean isDirty(DirtyBits dirtyBit) {
        return dirtyBits.contains(dirtyBit);
    }

    /**
     * Test whether the dirty bits are empty
     */
    private boolean isDirtyEmpty() {
        return dirtyBits.isEmpty();
    }

    /**
     * Clear all dirty bits
     */
    private void clearDirty() {
        dirtyBits.clear();
    }

    private enum DirtyBits {
        FILL_DIRTY,
        ROOT_DIRTY,
        CAMERA_DIRTY,
        LIGHTS_DIRTY,
        CURSOR_DIRTY;
    }

    private List<LightBase> lights = new ArrayList<>();

    // @param light must not be null
    final void addLight(LightBase light) {
        if (!lights.contains(light)) {
            lights.add(light);
            markDirty(DirtyBits.LIGHTS_DIRTY);
        }
    }

    final void removeLight(LightBase light) {
        if (lights.remove(light)) {
            markDirty(DirtyBits.LIGHTS_DIRTY);
        }
    }

    /**
     * PG Light synchronizer.
     */
    private void syncLights() {
        if (!isDirty(DirtyBits.LIGHTS_DIRTY)) {
            return;
        }
        inSynchronizer = true;
        NGLightBase peerLights[] = peer.getLights();
        if (!lights.isEmpty() || (peerLights != null)) {
            if (lights.isEmpty()) {
                peer.setLights(null);
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
                peer.setLights(peerLights);
            }
        }
        inSynchronizer = false;
    }

    //INNER CLASSES

    /*******************************************************************************
     *                                                                             *
     * Scene Pulse Listener                                                        *
     *                                                                             *
     ******************************************************************************/

    class ScenePulseListener implements TKPulseListener {

        private boolean firstPulse = true;

        /**
         * PG synchronizer. Called once per frame from the pulse listener.
         * This function calls the synchronizePGNode method on each node in
         * the dirty list.
         */
        private void synchronizeSceneNodes() {
            Toolkit.getToolkit().checkFxUserThread();

            Scene.inSynchronizer = true;

            // if dirtyNodes is null then that means this Scene has not yet been
            // synchronized, and so we will simply synchronize every node in the
            // scene and then create the dirty nodes array list
            if (Scene.this.dirtyNodes == null) {
                // must do this recursively
                syncAll(getRoot());
                dirtyNodes = new Node[MIN_DIRTY_CAPACITY];

            } else {
                // This is not the first time this scene has been synchronized,
                // so we will only synchronize those nodes that need it
                for (int i = 0 ; i < dirtyNodesSize; ++i) {
                    Node node = dirtyNodes[i];
                    dirtyNodes[i] = null;
                    if (node.getScene() == Scene.this) {
                            node.syncPeer();
                        }
                    }
                dirtyNodesSize = 0;
            }

            Scene.inSynchronizer = false;
        }

        /**
         * Recursive function for synchronizing every node in the scenegraph.
         * The return value is the number of nodes in the graph.
         */
        private int syncAll(Node node) {
            node.syncPeer();
            int size = 1;
            if (node instanceof Parent) {
                Parent p = (Parent) node;
                final int childrenCount = p.getChildren().size();

                for (int i = 0; i < childrenCount; i++) {
                    Node n = p.getChildren().get(i);
                    if (n != null) {
                        size += syncAll(n);
                    }
                }
            } else if (node instanceof SubScene) {
                SubScene subScene = (SubScene)node;
                size += syncAll(subScene.getRoot());
            }
            if (node.getClip() != null) {
                size += syncAll(node.getClip());
            }

            return size;
        }

        private void synchronizeSceneProperties() {
            inSynchronizer = true;
            if (isDirty(DirtyBits.ROOT_DIRTY)) {
                peer.setRoot(getRoot().getPeer());
            }

            if (isDirty(DirtyBits.FILL_DIRTY)) {
                Toolkit tk = Toolkit.getToolkit();
                peer.setFillPaint(getFill() == null ? null : tk.getPaint(getFill()));
            }

            // new camera was set on the scene or old camera changed
            final Camera cam = getEffectiveCamera();
            if (isDirty(DirtyBits.CAMERA_DIRTY)) {
                NodeHelper.updatePeer(cam);
                peer.setCamera((NGCamera) cam.getPeer());
            }

            if (isDirty(DirtyBits.CURSOR_DIRTY)) {
                mouseHandler.updateCursor(getCursor());
                mouseHandler.updateCursorFrame();
            }

            clearDirty();
            inSynchronizer = false;
        }

        /**
         * The focus is considered dirty if something happened to
         * the scene graph that may require the focus to be moved.
         * This must handle cases where (a) the focus owner may have
         * become ineligible to have the focus, and (b) where the focus
         * owner is null and a node may have become traversable and eligible.
         */
        private void focusCleanup() {
            if (Scene.this.isFocusDirty()) {
                final Node oldOwner = Scene.this.getFocusOwner();
                if (oldOwner == null) {
                    Scene.this.focusInitial();
                } else if (oldOwner.getScene() != Scene.this) {
                    Scene.this.requestFocus(null);
                    Scene.this.focusInitial();
                } else if (!oldOwner.isCanReceiveFocus()) {
                    Scene.this.requestFocus(null);
                    Scene.this.focusIneligible(oldOwner);
                }
                Scene.this.setFocusDirty(false);
            }
        }

        @Override
        public void pulse() {
            if (Scene.this.tracker != null) {
                Scene.this.tracker.pulse();
            }
            if (firstPulse) {
                PerformanceTracker.logEvent("Scene - first repaint");
            }

            focusCleanup();

            disposeAccessibles();

            // run any scene pre pulse listeners immediately _before_ css / layout,
            // and before scene synchronization
            if (preLayoutPulseListeners != null) {
                for (Runnable r : preLayoutPulseListeners) {
                    r.run();
                }
            }

            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newPhase("CSS Pass");
            }
            Scene.this.doCSSPass();

            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newPhase("Layout Pass");
            }
            Scene.this.doLayoutPass();

            // run any scene post pulse listeners immediately _after_ css / layout,
            // and before scene synchronization
            if (postLayoutPulseListeners != null) {
                for (Runnable r : postLayoutPulseListeners) {
                    r.run();
                }
            }

            boolean dirty = dirtyNodes == null || dirtyNodesSize != 0 || !isDirtyEmpty();
            if (dirty) {
                if (PULSE_LOGGING_ENABLED) {
                    PulseLogger.newPhase("Update bounds");
                }
                getRoot().updateBounds();
                if (peer != null) {
                    try {
                        if (PULSE_LOGGING_ENABLED) {
                            PulseLogger.newPhase("Waiting for previous rendering");
                        }
                        peer.waitForRenderingToComplete();
                        peer.waitForSynchronization();
                        // synchronize scene properties
                        if (PULSE_LOGGING_ENABLED) {
                            PulseLogger.newPhase("Copy state to render graph");
                        }
                        syncLights();
                        synchronizeSceneProperties();
                        // Run the synchronizer
                        synchronizeSceneNodes();
                        Scene.this.mouseHandler.pulse();
                        // Tell the scene peer that it needs to repaint
                        peer.markDirty();
                    } finally {
                        peer.releaseSynchronization(true);
                    }
                } else {
                    if (PULSE_LOGGING_ENABLED) {
                        PulseLogger.newPhase("Synchronize with null peer");
                    }
                    synchronizeSceneNodes();
                    Scene.this.mouseHandler.pulse();
                }

                if (Scene.this.getRoot().cssFlag != CssFlags.CLEAN) {
                    NodeHelper.markDirty(Scene.this.getRoot(),
                            com.sun.javafx.scene.DirtyBits.NODE_CSS);
                }
            }

            // required for image cursor created from animated image
            Scene.this.mouseHandler.updateCursorFrame();

            if (firstPulse) {
                if (PerformanceTracker.isLoggingEnabled()) {
                    PerformanceTracker.logEvent("Scene - first repaint - layout complete");
                    if (PrismSettings.perfLogFirstPaintFlush) {
                        PerformanceTracker.outputLog();
                    }
                    if (PrismSettings.perfLogFirstPaintExit) {
                        System.exit(0);
                    }
                }
                firstPulse = false;
            }

            if (testPulseListener != null) {
                testPulseListener.run();
            }
        }
    }

    /*******************************************************************************
     *                                                                             *
     * Scene Peer Listener                                                         *
     *                                                                             *
     ******************************************************************************/

    class ScenePeerListener implements TKSceneListener {
        @Override
        public void changedLocation(float x, float y) {
            if (x != Scene.this.getX()) {
                Scene.this.setX(x);
            }
            if (y != Scene.this.getY()) {
                Scene.this.setY(y);
            }
        }

        @Override
        public void changedSize(float w, float h) {
            if (w != Scene.this.getWidth()) Scene.this.setWidth(w);
            if (h != Scene.this.getHeight()) Scene.this.setHeight(h);
        }

        @Override
        public void mouseEvent(EventType<MouseEvent> type, double x, double y, double screenX, double screenY,
                               MouseButton button, boolean popupTrigger, boolean synthesized,
                               boolean shiftDown, boolean controlDown, boolean altDown, boolean metaDown,
                               boolean primaryDown, boolean middleDown, boolean secondaryDown,
                               boolean backDown, boolean forwardDown)
        {
            MouseEvent mouseEvent = new MouseEvent(type, x, y, screenX, screenY, button,
                    0, // click count will be adjusted by clickGenerator later anyway
                    shiftDown, controlDown, altDown, metaDown,
                    primaryDown, middleDown, secondaryDown, backDown, forwardDown,
                    synthesized, popupTrigger, false, null);
            processMouseEvent(mouseEvent);
        }


        @Override
        public void keyEvent(KeyEvent keyEvent)
        {
            processKeyEvent(keyEvent);
        }

        @Override
        public void inputMethodEvent(EventType<InputMethodEvent> type,
                                     ObservableList<InputMethodTextRun> composed, String committed,
                                     int caretPosition)
        {
            InputMethodEvent inputMethodEvent = new InputMethodEvent(
               type, composed, committed, caretPosition);
            processInputMethodEvent(inputMethodEvent);
        }

        public void menuEvent(double x, double y, double xAbs, double yAbs,
                boolean isKeyboardTrigger) {
            Scene.this.processMenuEvent(x, y, xAbs,yAbs, isKeyboardTrigger);
        }

        @Override
        public void scrollEvent(
                EventType<ScrollEvent> eventType,
                double scrollX, double scrollY,
                double totalScrollX, double totalScrollY,
                double xMultiplier, double yMultiplier,
                int touchCount,
                int scrollTextX, int scrollTextY,
                int defaultTextX, int defaultTextY,
                double x, double y, double screenX, double screenY,
                boolean _shiftDown, boolean _controlDown,
                boolean _altDown, boolean _metaDown,
                boolean _direct, boolean _inertia) {

            ScrollEvent.HorizontalTextScrollUnits xUnits = scrollTextX > 0 ?
                    ScrollEvent.HorizontalTextScrollUnits.CHARACTERS :
                    ScrollEvent.HorizontalTextScrollUnits.NONE;

            double xText = scrollTextX < 0 ? 0 : scrollTextX * scrollX;

            ScrollEvent.VerticalTextScrollUnits yUnits = scrollTextY > 0 ?
                    ScrollEvent.VerticalTextScrollUnits.LINES :
                    (scrollTextY < 0 ?
                        ScrollEvent.VerticalTextScrollUnits.PAGES :
                        ScrollEvent.VerticalTextScrollUnits.NONE);

            double yText = scrollTextY < 0 ? scrollY : scrollTextY * scrollY;

            xMultiplier = defaultTextX > 0 && scrollTextX >= 0
                    ? Math.round(xMultiplier * scrollTextX / defaultTextX)
                    : xMultiplier;

            yMultiplier = defaultTextY > 0 && scrollTextY >= 0
                    ? Math.round(yMultiplier * scrollTextY / defaultTextY)
                    : yMultiplier;

            if (eventType == ScrollEvent.SCROLL_FINISHED) {
                x = scrollGesture.sceneCoords.getX();
                y = scrollGesture.sceneCoords.getY();
                screenX = scrollGesture.screenCoords.getX();
                screenY = scrollGesture.screenCoords.getY();
            } else if (Double.isNaN(x) || Double.isNaN(y) ||
                    Double.isNaN(screenX) || Double.isNaN(screenY)) {
                if (cursorScenePos == null || cursorScreenPos == null) {
                    return;
                }
                x = cursorScenePos.getX();
                y = cursorScenePos.getY();
                screenX = cursorScreenPos.getX();
                screenY = cursorScreenPos.getY();
            }

            inMousePick = true;
            Scene.this.processGestureEvent(new ScrollEvent(
                    eventType,
                    x, y, screenX, screenY,
                    _shiftDown, _controlDown, _altDown, _metaDown,
                    _direct, _inertia,
                    scrollX * xMultiplier, scrollY * yMultiplier,
                    totalScrollX * xMultiplier, totalScrollY * yMultiplier,
                    xMultiplier, yMultiplier,
                    xUnits, xText, yUnits, yText, touchCount, pick(x, y)),
                    scrollGesture);
            inMousePick = false;
        }

        @Override
        public void zoomEvent(
                EventType<ZoomEvent> eventType,
                double zoomFactor, double totalZoomFactor,
                double x, double y, double screenX, double screenY,
                boolean _shiftDown, boolean _controlDown,
                boolean _altDown, boolean _metaDown,
                boolean _direct, boolean _inertia) {

            if (eventType == ZoomEvent.ZOOM_FINISHED) {
                x = zoomGesture.sceneCoords.getX();
                y = zoomGesture.sceneCoords.getY();
                screenX = zoomGesture.screenCoords.getX();
                screenY = zoomGesture.screenCoords.getY();
            } else if (Double.isNaN(x) || Double.isNaN(y) ||
                    Double.isNaN(screenX) || Double.isNaN(screenY)) {
                if (cursorScenePos == null || cursorScreenPos == null) {
                    return;
                }
                x = cursorScenePos.getX();
                y = cursorScenePos.getY();
                screenX = cursorScreenPos.getX();
                screenY = cursorScreenPos.getY();
            }

            inMousePick = true;
            Scene.this.processGestureEvent(new ZoomEvent(eventType,
                    x, y, screenX, screenY,
                    _shiftDown, _controlDown, _altDown, _metaDown,
                    _direct, _inertia,
                    zoomFactor, totalZoomFactor, pick(x, y)),
                    zoomGesture);
            inMousePick = false;
        }

        @Override
        public void rotateEvent(
                EventType<RotateEvent> eventType, double angle, double totalAngle,
                double x, double y, double screenX, double screenY,
                boolean _shiftDown, boolean _controlDown,
                boolean _altDown, boolean _metaDown,
                boolean _direct, boolean _inertia) {

            if (eventType == RotateEvent.ROTATION_FINISHED) {
                x = rotateGesture.sceneCoords.getX();
                y = rotateGesture.sceneCoords.getY();
                screenX = rotateGesture.screenCoords.getX();
                screenY = rotateGesture.screenCoords.getY();
            } else if (Double.isNaN(x) || Double.isNaN(y) ||
                    Double.isNaN(screenX) || Double.isNaN(screenY)) {
                if (cursorScenePos == null || cursorScreenPos == null) {
                    return;
                }
                x = cursorScenePos.getX();
                y = cursorScenePos.getY();
                screenX = cursorScreenPos.getX();
                screenY = cursorScreenPos.getY();
            }

            inMousePick = true;
            Scene.this.processGestureEvent(new RotateEvent(
                    eventType, x, y, screenX, screenY,
                    _shiftDown, _controlDown, _altDown, _metaDown,
                    _direct, _inertia, angle, totalAngle, pick(x, y)),
                    rotateGesture);
            inMousePick = false;

        }

        @Override
        public void swipeEvent(
                EventType<SwipeEvent> eventType, int touchCount,
                double x, double y, double screenX, double screenY,
                boolean _shiftDown, boolean _controlDown,
                boolean _altDown, boolean _metaDown, boolean _direct) {

            if (Double.isNaN(x) || Double.isNaN(y) ||
                    Double.isNaN(screenX) || Double.isNaN(screenY)) {
                if (cursorScenePos == null || cursorScreenPos == null) {
                    return;
                }
                x = cursorScenePos.getX();
                y = cursorScenePos.getY();
                screenX = cursorScreenPos.getX();
                screenY = cursorScreenPos.getY();
            }

            inMousePick = true;
            Scene.this.processGestureEvent(new SwipeEvent(
                    eventType, x, y, screenX, screenY,
                    _shiftDown, _controlDown, _altDown, _metaDown, _direct,
                    touchCount, pick(x, y)),
                    swipeGesture);
            inMousePick = false;
        }

        @Override
        public void touchEventBegin(
                long time, int touchCount, boolean isDirect,
                boolean _shiftDown, boolean _controlDown,
                boolean _altDown, boolean _metaDown) {

            if (!isDirect) {
                nextTouchEvent = null;
                return;
            }
            nextTouchEvent = new TouchEvent(
                    TouchEvent.ANY, null, null, 0,
                    _shiftDown, _controlDown, _altDown, _metaDown);
            if (touchPoints == null || touchPoints.length != touchCount) {
                touchPoints = new TouchPoint[touchCount];
            }
            touchPointIndex = 0;
        }

        @Override
        public void touchEventNext(
                TouchPoint.State state, long touchId,
                double x, double y, double screenX, double screenY) {

            inMousePick = true;
            if (nextTouchEvent == null) {
                // ignore indirect touch events
                return;
            }
            touchPointIndex++;
            int id = (state == TouchPoint.State.PRESSED
                    ? touchMap.add(touchId) :  touchMap.get(touchId));
            if (state == TouchPoint.State.RELEASED) {
                touchMap.remove(touchId);
            }
            int order = touchMap.getOrder(id);

            if (order >= touchPoints.length) {
                throw new RuntimeException("Too many touch points reported");
            }

            // pick target
            boolean isGrabbed = false;
            PickResult pickRes = pick(x, y);
            EventTarget pickedTarget = touchTargets.get(id);
            if (pickedTarget == null) {
                pickedTarget = pickRes.getIntersectedNode();
                if (pickedTarget == null) {
                    pickedTarget = Scene.this;
                }
            } else {
                isGrabbed = true;
            }

            TouchPoint tp = new TouchPoint(id, state,
                    x, y, screenX, screenY, pickedTarget, pickRes);

            touchPoints[order] = tp;

            if (isGrabbed) {
                tp.grab(pickedTarget);
            }
            if (tp.getState() == TouchPoint.State.PRESSED) {
                tp.grab(pickedTarget);
                touchTargets.put(tp.getId(), pickedTarget);
            } else if (tp.getState() == TouchPoint.State.RELEASED) {
                touchTargets.remove(tp.getId());
            }
            inMousePick = false;
        }

        @Override
        public void touchEventEnd() {
            if (nextTouchEvent == null) {
                // ignore indirect touch events
                return;
            }

            if (touchPointIndex != touchPoints.length) {
                throw new RuntimeException("Wrong number of touch points reported");
            }

            Scene.this.processTouchEvent(nextTouchEvent, touchPoints);

            if (touchMap.cleanup()) {
                // gesture finished
                touchEventSetId = 0;
            }
        }

        @Override
        public Accessible getSceneAccessible() {
            return getAccessible();
        }
    }

    private class ScenePeerPaintListener implements TKScenePaintListener {
        @Override
        public void frameRendered() {
            // must use tracker with synchronization since this method is called on render thread
            synchronized (trackerMonitor) {
                if (Scene.this.tracker != null) {
                    Scene.this.tracker.frameRendered();
                }
            }
        }
    }

    /*******************************************************************************
     *                                                                             *
     * Drag and Drop                                                               *
     *                                                                             *
     ******************************************************************************/

    class DropTargetListener implements TKDropTargetListener {

        /*
         * This function is called when an drag operation enters a valid drop target.
         * This may be from either an internal or external dnd operation.
         */
        @Override
        public TransferMode dragEnter(double x, double y, double screenX, double screenY,
                                      TransferMode transferMode, TKClipboard dragboard)
        {
            if (dndGesture == null) {
                dndGesture = new DnDGesture();
            }
            Dragboard db = DragboardHelper.createDragboard(dragboard);
            dndGesture.dragboard = db;
            DragEvent dragEvent =
                    new DragEvent(DragEvent.ANY, dndGesture.dragboard, x, y, screenX, screenY,
                            transferMode, null, null, pick(x, y));
            return dndGesture.processTargetEnterOver(dragEvent);
        }

        @Override
        public TransferMode dragOver(double x, double y, double screenX, double screenY,
                                     TransferMode transferMode)
        {
            if (Scene.this.dndGesture == null) {
                System.err.println("GOT A dragOver when dndGesture is null!");
                return null;
            } else {
                if (dndGesture.dragboard == null) {
                    throw new RuntimeException("dndGesture.dragboard is null in dragOver");
                }
                DragEvent dragEvent =
                        new DragEvent(DragEvent.ANY, dndGesture.dragboard, x, y, screenX, screenY,
                                transferMode, null, null, pick(x, y));
                return dndGesture.processTargetEnterOver(dragEvent);
            }
        }

        @Override
        public void dragExit(double x, double y, double screenX, double screenY) {
            if (dndGesture == null) {
                System.err.println("GOT A dragExit when dndGesture is null!");
            } else {
                if (dndGesture.dragboard == null) {
                    throw new RuntimeException("dndGesture.dragboard is null in dragExit");
                }
                DragEvent dragEvent =
                        new DragEvent(DragEvent.ANY, dndGesture.dragboard, x, y, screenX, screenY,
                                null, null, null, pick(x, y));
                dndGesture.processTargetExit(dragEvent);
                if (dndGesture.source == null) {
                    dndGesture.dragboard = null;
                    dndGesture = null;
                }
            }
        }


        @Override
        public TransferMode drop(double x, double y, double screenX, double screenY,
                                  TransferMode transferMode)
        {
            if (dndGesture == null) {
                System.err.println("GOT A drop when dndGesture is null!");
                return null;
            } else {
                if (dndGesture.dragboard == null) {
                    throw new RuntimeException("dndGesture.dragboard is null in dragDrop");
                }
                DragEvent dragEvent =
                        new DragEvent(DragEvent.ANY, dndGesture.dragboard, x, y, screenX, screenY,
                                transferMode, null, null, pick(x, y));
                // Data dropped to the app can be accessed without restriction
                DragboardHelper.setDataAccessRestriction(dndGesture.dragboard, false);

                TransferMode tm;
                try {
                    tm = dndGesture.processTargetDrop(dragEvent);
                } finally {
                    DragboardHelper.setDataAccessRestriction(
                            dndGesture.dragboard, true);
                }

                if (dndGesture.source == null) {
                    dndGesture.dragboard = null;
                    dndGesture = null;
                }
                return tm;
            }
        }
    }

    class DragGestureListener implements TKDragGestureListener {

       @Override
       public void dragGestureRecognized(double x, double y, double screenX, double screenY,
                                         int button, TKClipboard dragboard)
       {
           Dragboard db = DragboardHelper.createDragboard(dragboard);
           dndGesture = new DnDGesture();
           dndGesture.dragboard = db;
           // TODO: support mouse buttons in DragEvent
           DragEvent dragEvent = new DragEvent(DragEvent.ANY, db, x, y, screenX, screenY,
                   null, null, null, pick(x, y));
           dndGesture.processRecognized(dragEvent);
           dndGesture = null;
        }
    }

    /**
     * A Drag and Drop gesture has a lifespan that lasts from mouse
     * PRESSED event to mouse RELEASED event.
     */
    class DnDGesture {
        private final double hysteresisSizeX =
                Toolkit.getToolkit().getMultiClickMaxX();
        private final double hysteresisSizeY =
                Toolkit.getToolkit().getMultiClickMaxY();

        private EventTarget source = null;
        private Set<TransferMode> sourceTransferModes = null;
        private TransferMode acceptedTransferMode = null;
        private Dragboard dragboard = null;
        private EventTarget potentialTarget = null;
        private EventTarget target = null;
        private DragDetectedState dragDetected = DragDetectedState.NOT_YET;
        private double pressedX;
        private double pressedY;
        private List<EventTarget> currentTargets = new ArrayList<EventTarget>();
        private List<EventTarget> newTargets = new ArrayList<EventTarget>();
        private EventTarget fullPDRSource = null;

        /**
         * Fires event on a given target or on scene if the node is null
         */
        private void fireEvent(EventTarget target, Event e) {
            if (target != null) {
                Event.fireEvent(target, e);
            }
        }

        /**
         * Called when DRAG_DETECTED event is going to be processed by
         * application
         */
        private void processingDragDetected() {
            dragDetected = DragDetectedState.PROCESSING;
        }

        /**
         * Called after DRAG_DETECTED event has been processed by application
         */
        private void dragDetectedProcessed() {
            dragDetected = DragDetectedState.DONE;
            final boolean hasContent = (dragboard != null) && (ClipboardHelper.contentPut(dragboard));
            if (hasContent) {
                /* start DnD */
                Toolkit.getToolkit().startDrag(Scene.this.peer,
                                                sourceTransferModes,
                                                new DragSourceListener(),
                                                dragboard);
            } else if (fullPDRSource != null) {
                /* start PDR */
                Scene.this.mouseHandler.enterFullPDR(fullPDRSource);
            }

            fullPDRSource = null;
        }

        /**
         * Sets the default dragDetect value
         */
        private void processDragDetection(MouseEvent mouseEvent) {

            if (dragDetected != DragDetectedState.NOT_YET) {
                mouseEvent.setDragDetect(false);
                return;
            }

            if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED) {
                pressedX = mouseEvent.getSceneX();
                pressedY = mouseEvent.getSceneY();

                mouseEvent.setDragDetect(false);

            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {

                double deltaX = Math.abs(mouseEvent.getSceneX() - pressedX);
                double deltaY = Math.abs(mouseEvent.getSceneY() - pressedY);
                mouseEvent.setDragDetect(deltaX > hysteresisSizeX ||
                                         deltaY > hysteresisSizeY);

            }
        }

        /**
         * This function is useful for drag gesture recognition from
         * within this Scene (as opposed to in the TK implementation... by the platform)
         */
        private boolean process(MouseEvent mouseEvent, EventTarget target) {
            boolean continueProcessing = true;
            if (!PLATFORM_DRAG_GESTURE_INITIATION) {

                if (dragDetected != DragDetectedState.DONE &&
                        (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED ||
                        mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) &&
                        mouseEvent.isDragDetect()) {

                    processingDragDetected();

                    if (target != null) {
                        final MouseEvent detectedEvent = mouseEvent.copyFor(
                                mouseEvent.getSource(), target,
                                MouseEvent.DRAG_DETECTED);

                        try {
                            fireEvent(target, detectedEvent);
                        } finally {
                            // Putting data to dragboard finished, restrict access to them
                            if (dragboard != null) {
                                DragboardHelper.setDataAccessRestriction(
                                        dragboard, true);
                            }
                        }
                    }

                    dragDetectedProcessed();
                }

                if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED) {
                    continueProcessing = false;
                }
            }
            return continueProcessing;
        }

        /*
         * Called when a drag source is recognized. This occurs at the very start of
         * the publicly visible drag and drop API, as it is responsible for calling
         * the Node.onDragSourceRecognized function.
         */
        private boolean processRecognized(DragEvent de) {
            MouseEvent me = new MouseEvent(
                    MouseEvent.DRAG_DETECTED, de.getX(), de.getY(),
                    de.getSceneX(), de.getScreenY(), MouseButton.PRIMARY, 1,
                    false, false, false, false, false, true, false, false, false,
                    false, de.getPickResult());

            processingDragDetected();

            final EventTarget target = de.getPickResult().getIntersectedNode();
            try {
                fireEvent(target != null ? target : Scene.this, me);
            } finally {
                // Putting data to dragboard finished, restrict access to them
                if (dragboard != null) {
                    DragboardHelper.setDataAccessRestriction(
                            dragboard, true);
                }
            }

            dragDetectedProcessed();

            final boolean hasContent = dragboard != null
                    && !dragboard.getContentTypes().isEmpty();
            return hasContent;
        }

        private void processDropEnd(DragEvent de) {
            if (source == null) {
                System.out.println("Scene.DnDGesture.processDropEnd() - UNEXPECTD - source is NULL");
                return;
            }

            de = new DragEvent(de.getSource(), source, DragEvent.DRAG_DONE,
                    de.getDragboard(), de.getSceneX(), de.getSceneY(),
                    de.getScreenX(), de.getScreenY(),
                    de.getTransferMode(), source, target, de.getPickResult());

            Event.fireEvent(source, de);

            tmpTargetWrapper.clear();
            handleExitEnter(de, tmpTargetWrapper);

            // at this point the drag and drop operation is completely over, so we
            // can tell the toolkit that it can clean up if needs be.
            Toolkit.getToolkit().stopDrag(dragboard);
        }

        private TransferMode processTargetEnterOver(DragEvent de) {
            pick(tmpTargetWrapper, de.getSceneX(), de.getSceneY());
            final EventTarget pickedTarget = tmpTargetWrapper.getEventTarget();

            if (dragboard == null) {
                dragboard = createDragboard(de, false);
            }

            de = new DragEvent(de.getSource(), pickedTarget, de.getEventType(),
                    dragboard, de.getSceneX(), de.getSceneY(),
                    de.getScreenX(), de.getScreenY(),
                    de.getTransferMode(), source, potentialTarget, de.getPickResult());

            handleExitEnter(de, tmpTargetWrapper);

            de = new DragEvent(de.getSource(), pickedTarget, DragEvent.DRAG_OVER,
                    de.getDragboard(), de.getSceneX(), de.getSceneY(),
                    de.getScreenX(), de.getScreenY(),
                    de.getTransferMode(), source, potentialTarget, de.getPickResult());

            fireEvent(pickedTarget, de);

            Object acceptingObject = de.getAcceptingObject();
            potentialTarget = acceptingObject instanceof EventTarget
                    ? (EventTarget) acceptingObject : null;
            acceptedTransferMode = de.getAcceptedTransferMode();
            return acceptedTransferMode;
        }

        private void processTargetActionChanged(DragEvent de) {
            // Do we want DRAG_TRANSFER_MODE_CHANGED event?
//            final Node pickedNode = Scene.this.mouseHandler.pickNode(de.getX(), de.getY());
//            if (pickedNode != null && pickedNode.isTreeVisible()) {
//                de = DragEvent.copy(de.getSource(), pickedNode, source,
//                        pickedNode, de, DragEvent.DRAG_TRANSFER_MODE_CHANGED);
//
//                if (dragboard == null) {
//                    dragboard = createDragboard(de);
//                }
//                dragboard = de.getPlatformDragboard();
//
//                fireEvent(pickedNode, de);
//            }
        }

        private void processTargetExit(DragEvent de) {
            if (dragboard == null) {
                // dragboard should have been created in processTargetEnterOver()
                throw new NullPointerException("dragboard is null in processTargetExit()");
            }

            if (currentTargets.size() > 0) {
                potentialTarget = null;
                tmpTargetWrapper.clear();
                handleExitEnter(de, tmpTargetWrapper);
            }
        }

        private TransferMode processTargetDrop(DragEvent de) {
            pick(tmpTargetWrapper, de.getSceneX(), de.getSceneY());
            final EventTarget pickedTarget = tmpTargetWrapper.getEventTarget();

            de = new DragEvent(de.getSource(), pickedTarget, DragEvent.DRAG_DROPPED,
                    de.getDragboard(), de.getSceneX(), de.getSceneY(),
                    de.getScreenX(), de.getScreenY(),
                    acceptedTransferMode, source, potentialTarget, de.getPickResult());

            if (dragboard == null) {
                // dragboard should have been created in processTargetEnterOver()
                throw new NullPointerException("dragboard is null in processTargetDrop()");
            }

            handleExitEnter(de, tmpTargetWrapper);

            fireEvent(pickedTarget, de);

            Object acceptingObject = de.getAcceptingObject();
            potentialTarget = acceptingObject instanceof EventTarget
                    ? (EventTarget) acceptingObject : null;
            target = potentialTarget;

            TransferMode result = de.isDropCompleted() ?
                de.getAcceptedTransferMode() : null;

            tmpTargetWrapper.clear();
            handleExitEnter(de, tmpTargetWrapper);

            return result;
        }

        private void handleExitEnter(DragEvent e, TargetWrapper target) {
            EventTarget currentTarget =
                    currentTargets.size() > 0 ? currentTargets.get(0) : null;

            if (target.getEventTarget() != currentTarget) {

                target.fillHierarchy(newTargets);

                int i = currentTargets.size() - 1;
                int j = newTargets.size() - 1;

                while (i >= 0 && j >= 0 && currentTargets.get(i) == newTargets.get(j)) {
                    i--;
                    j--;
                }

                for (; i >= 0; i--) {
                    EventTarget t = currentTargets.get(i);
                    if (potentialTarget == t) {
                        potentialTarget = null;
                    }
                    e = e.copyFor(e.getSource(), t, source,
                            potentialTarget, DragEvent.DRAG_EXITED_TARGET);
                    Event.fireEvent(t, e);
                }

                potentialTarget = null;
                for (; j >= 0; j--) {
                    EventTarget t = newTargets.get(j);
                    e = e.copyFor(e.getSource(), t, source,
                            potentialTarget, DragEvent.DRAG_ENTERED_TARGET);
                    Object acceptingObject = e.getAcceptingObject();
                    if (acceptingObject instanceof EventTarget) {
                        potentialTarget = (EventTarget) acceptingObject;
                    }
                    Event.fireEvent(t, e);
                }

                currentTargets.clear();
                currentTargets.addAll(newTargets);
                newTargets.clear();
            }
        }

//        function getIntendedTransferMode(e:MouseEvent):TransferMode {
//            return if (e.altDown) TransferMode.COPY else TransferMode.MOVE;
//        }

        /*
         * Function that hooks into the key processing code in Scene to handle the
         * situation where a drag and drop event is taking place and the user presses
         * the escape key to cancel the drag and drop operation.
         */
        private boolean processKey(KeyEvent e) {
            //note: this seems not to be called, the DnD cancelation is provided by platform
            if ((e.getEventType() == KeyEvent.KEY_PRESSED) && (e.getCode() == KeyCode.ESCAPE)) {

                // cancel drag and drop
                DragEvent de = new DragEvent(
                        source, source, DragEvent.DRAG_DONE, dragboard, 0, 0, 0, 0,
                        null, source, null, null);
                if (source != null) {
                    Event.fireEvent(source, de);
                }

                tmpTargetWrapper.clear();
                handleExitEnter(de, tmpTargetWrapper);

                return false;
            }
            return true;
        }

        /*
         * This starts the drag gesture running, creating the dragboard used for
         * the remainder of this drag and drop operation.
         */
        private Dragboard startDrag(EventTarget source, Set<TransferMode> t) {
            if (dragDetected != DragDetectedState.PROCESSING) {
                throw new IllegalStateException("Cannot start drag and drop "
                        + "outside of DRAG_DETECTED event handler");
            }

            if (t.isEmpty()) {
                dragboard = null;
            } else if (dragboard == null) {
                dragboard = createDragboard(null, true);
            }

            // The app can see what it puts to dragboard without restriction
            DragboardHelper.setDataAccessRestriction(dragboard, false);

            this.source = source;
            potentialTarget = source;
            sourceTransferModes = t;
            return dragboard;
        }

        /*
         * This starts the full PDR gesture.
         */
        private void startFullPDR(EventTarget source) {
            fullPDRSource = source;
        }

        private Dragboard createDragboard(final DragEvent de, boolean isDragSource) {
            Dragboard dragboard = null;
            if (de != null) {
                dragboard = de.getDragboard();
                if (dragboard != null) {
                    return dragboard;
                }
            }
            TKClipboard dragboardPeer = peer.createDragboard(isDragSource);
            return DragboardHelper.createDragboard(dragboardPeer);
        }
    }

    /**
     * State of a drag gesture with regards to DRAG_DETECTED event.
     */
    private enum DragDetectedState {
        NOT_YET,
        PROCESSING,
        DONE
    }

    class DragSourceListener implements TKDragSourceListener {

        @Override
        public void dragDropEnd(double x, double y, double screenX, double screenY,
                                TransferMode transferMode)
        {
            if (dndGesture != null) {
                if (dndGesture.dragboard == null) {
                    throw new RuntimeException("dndGesture.dragboard is null in dragDropEnd");
                }
                DragEvent dragEvent =
                        new DragEvent(DragEvent.ANY, dndGesture.dragboard, x, y, screenX, screenY,
                        transferMode, null, null, null);

                // DRAG_DONE event is delivered to gesture source, it can access
                // its own data without restriction
                DragboardHelper.setDataAccessRestriction(dndGesture.dragboard, false);
                try {
                    dndGesture.processDropEnd(dragEvent);
                } finally {
                    DragboardHelper.setDataAccessRestriction(dndGesture.dragboard, true);
                }
                dndGesture = null;
            }
        }
    }

    /*******************************************************************************
     *                                                                             *
     * Mouse Event Handling                                                        *
     *                                                                             *
     ******************************************************************************/

    static class ClickCounter {
        Toolkit toolkit = Toolkit.getToolkit();
        private int count;
        private boolean out;
        private boolean still;
        private Timeline timeout;
        private double pressedX, pressedY;

        private void inc() { count++; }
        private int get() { return count; }
        private boolean isStill() { return still; }

        private void clear() {
            count = 0;
            stopTimeout();
        }

        private void out() {
            out = true;
            stopTimeout();
        }

        private void applyOut() {
            if (out) clear();
            out = false;
        }

        private void moved(double x, double y) {
            if (Math.abs(x - pressedX) > toolkit.getMultiClickMaxX() ||
                    Math.abs(y - pressedY) > toolkit.getMultiClickMaxY()) {
                out();
                still = false;
            }
        }

        private void start(double x, double y) {
            pressedX = x;
            pressedY = y;
            out = false;

            if (timeout != null) {
                timeout.stop();
            }
            timeout = new Timeline();
            timeout.getKeyFrames().add(
                    new KeyFrame(new Duration(toolkit.getMultiClickTime()),
                            event -> {
                                out = true;
                                timeout = null;
                            }
                    ));
            timeout.play();
            still = true;
        }

        private void stopTimeout() {
            if (timeout != null) {
                timeout.stop();
                timeout = null;
            }
        }
    }

    static class ClickGenerator {
        private ClickCounter lastPress = null;

        private Map<MouseButton, ClickCounter> counters =
                new EnumMap<MouseButton, ClickCounter>(MouseButton.class);
        private List<EventTarget> pressedTargets = new ArrayList<EventTarget>();
        private List<EventTarget> releasedTargets = new ArrayList<EventTarget>();

        public ClickGenerator() {
            for (MouseButton mb : MouseButton.values()) {
                if (mb != MouseButton.NONE) {
                    counters.put(mb, new ClickCounter());
                }
            }
        }

        private MouseEvent preProcess(MouseEvent e) {
            for (ClickCounter cc : counters.values()) {
                cc.moved(e.getSceneX(), e.getSceneY());
            }

            ClickCounter cc = counters.get(e.getButton());
            boolean still = lastPress != null ? lastPress.isStill() : false;

            if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {

                if (! e.isPrimaryButtonDown()) { counters.get(MouseButton.PRIMARY).clear(); }
                if (! e.isSecondaryButtonDown()) { counters.get(MouseButton.SECONDARY).clear(); }
                if (! e.isMiddleButtonDown()) { counters.get(MouseButton.MIDDLE).clear(); }
                if (! e.isBackButtonDown()) { counters.get(MouseButton.BACK).clear(); }
                if (! e.isForwardButtonDown()) { counters.get(MouseButton.FORWARD).clear(); }
                cc.applyOut();
                cc.inc();
                cc.start(e.getSceneX(), e.getSceneY());
                lastPress = cc;
            }

            return new MouseEvent(e.getEventType(), e.getSceneX(), e.getSceneY(),
                    e.getScreenX(), e.getScreenY(), e.getButton(),
                    cc != null && e.getEventType() != MouseEvent.MOUSE_MOVED ? cc.get() : 0,
                    e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(),
                    e.isPrimaryButtonDown(), e.isMiddleButtonDown(), e.isSecondaryButtonDown(),
                    e.isBackButtonDown(), e.isForwardButtonDown(),
                    e.isSynthesized(), e.isPopupTrigger(), still, e.getPickResult());
        }

        private void postProcess(MouseEvent e, TargetWrapper target, TargetWrapper pickedTarget) {

            if (e.getEventType() == MouseEvent.MOUSE_RELEASED) {
                ClickCounter cc = counters.get(e.getButton());

                target.fillHierarchy(pressedTargets);
                pickedTarget.fillHierarchy(releasedTargets);
                int i = pressedTargets.size() - 1;
                int j = releasedTargets.size() - 1;

                EventTarget clickedTarget = null;
                while (i >= 0 && j >= 0 && pressedTargets.get(i) == releasedTargets.get(j)) {
                    clickedTarget = pressedTargets.get(i);
                    i--;
                    j--;
                }

                pressedTargets.clear();
                releasedTargets.clear();

                if (clickedTarget != null && lastPress != null) {
                    MouseEvent click = new MouseEvent(null, clickedTarget,
                            MouseEvent.MOUSE_CLICKED, e.getSceneX(), e.getSceneY(),
                            e.getScreenX(), e.getScreenY(), e.getButton(),
                            cc.get(),
                            e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(),
                            e.isPrimaryButtonDown(), e.isMiddleButtonDown(), e.isSecondaryButtonDown(),
                            e.isBackButtonDown(), e.isForwardButtonDown(),
                            e.isSynthesized(), e.isPopupTrigger(), lastPress.isStill(), e.getPickResult());
                    Event.fireEvent(clickedTarget, click);
                }
            }
        }
    }

    /**
     * Generates mouse exited event for a node which is going to be removed
     * and its children, where appropriate.
     * @param removing Node which is going to be removed
     */
    void generateMouseExited(Node removing) {
        mouseHandler.handleNodeRemoval(removing);
    }

    class MouseHandler {
        private TargetWrapper pdrEventTarget = new TargetWrapper(); // pdr - press-drag-release
        private boolean pdrInProgress = false;
        private boolean fullPDREntered = false;

        private EventTarget currentEventTarget = null;
        private MouseEvent lastEvent;
        private boolean hover = false;

        private boolean primaryButtonDown = false;
        private boolean secondaryButtonDown = false;
        private boolean middleButtonDown = false;
        private boolean backButtonDown = false;
        private boolean forwardButtonDown = false;

        private EventTarget fullPDRSource = null;
        private TargetWrapper fullPDRTmpTargetWrapper = new TargetWrapper();

        /* lists needed for enter/exit events generation */
        private final List<EventTarget> pdrEventTargets = new ArrayList<EventTarget>();
        private final List<EventTarget> currentEventTargets = new ArrayList<EventTarget>();
        private final List<EventTarget> newEventTargets = new ArrayList<EventTarget>();

        private final List<EventTarget> fullPDRCurrentEventTargets = new ArrayList<EventTarget>();
        private final List<EventTarget> fullPDRNewEventTargets = new ArrayList<EventTarget>();
        private EventTarget fullPDRCurrentTarget = null;

        private Cursor currCursor;
        private CursorFrame currCursorFrame;
        private EventQueue queue = new EventQueue();

        private Runnable pickProcess = new Runnable() {

            @Override
            public void run() {
                // Make sure this is run only if the peer is still alive
                // and there is an event to deliver
                if (Scene.this.peer != null && lastEvent != null) {
                    process(lastEvent, true);
                }
            }
        };

        private void pulse() {
            if (hover && lastEvent != null) {
                //Shouldn't run user code directly. User can call stage.showAndWait() and block the pulse.
                Platform.runLater(pickProcess);
            }
        }

        private void clearPDREventTargets() {
            pdrInProgress = false;
            currentEventTarget = currentEventTargets.size() > 0
                    ? currentEventTargets.get(0) : null;
            pdrEventTarget.clear();
            pdrEventTargets.clear();
        }

        public void enterFullPDR(EventTarget gestureSource) {
            fullPDREntered = true;
            fullPDRSource = gestureSource;
            fullPDRCurrentTarget = null;
            fullPDRCurrentEventTargets.clear();
        }

        public void exitFullPDR(MouseEvent e) {
            if (!fullPDREntered) {
                return;
            }
            fullPDREntered = false;
            for (int i = fullPDRCurrentEventTargets.size() - 1; i >= 0; i--) {
                EventTarget entered = fullPDRCurrentEventTargets.get(i);
                Event.fireEvent(entered, MouseEvent.copyForMouseDragEvent(e,
                        entered, entered,
                        MouseDragEvent.MOUSE_DRAG_EXITED_TARGET,
                        fullPDRSource, e.getPickResult()));
            }
            fullPDRSource = null;
            fullPDRCurrentEventTargets.clear();
            fullPDRCurrentTarget = null;
        }

        private void handleNodeRemoval(Node removing) {
            if (lastEvent == null) {
                // this can happen only if everything has been exited anyway
                return;
            }


            if (currentEventTargets.contains(removing)) {
                int i = 0;
                EventTarget trg = null;
                while(trg != removing) {
                    trg = currentEventTargets.get(i++);

                    queue.postEvent(lastEvent.copyFor(trg, trg,
                            MouseEvent.MOUSE_EXITED_TARGET));
                }
                currentEventTargets.subList(0, i).clear();
            }

            if (fullPDREntered && fullPDRCurrentEventTargets.contains(removing)) {
                int i = 0;
                EventTarget trg = null;
                while (trg != removing) {
                    trg = fullPDRCurrentEventTargets.get(i++);

                    queue.postEvent(
                            MouseEvent.copyForMouseDragEvent(lastEvent, trg, trg,
                            MouseDragEvent.MOUSE_DRAG_EXITED_TARGET,
                            fullPDRSource, lastEvent.getPickResult()));
                }

                fullPDRCurrentEventTargets.subList(0, i).clear();
            }

            queue.fire();

            if (pdrInProgress && pdrEventTargets.contains(removing)) {
                int i = 0;
                EventTarget trg = null;
                while (trg != removing) {
                    trg = pdrEventTargets.get(i++);

                    // trg.setHover(false) - already taken care of
                    // by the code above which sent a mouse exited event
                    ((Node) trg).setPressed(false);
                }
                pdrEventTargets.subList(0, i).clear();

                trg = pdrEventTargets.get(0);
                final PickResult res = pdrEventTarget.getResult();
                if (trg instanceof Node) {
                    pdrEventTarget.setNodeResult(new PickResult((Node) trg,
                            res.getIntersectedPoint(), res.getIntersectedDistance()));
                } else {
                    pdrEventTarget.setSceneResult(new PickResult(null,
                            res.getIntersectedPoint(), res.getIntersectedDistance()),
                            (Scene) trg);
                }
            }
        }

        private void handleEnterExit(MouseEvent e, TargetWrapper pickedTarget) {
            if (pickedTarget.getEventTarget() != currentEventTarget ||
                    e.getEventType() == MouseEvent.MOUSE_EXITED) {

                if (e.getEventType() == MouseEvent.MOUSE_EXITED) {
                    newEventTargets.clear();
                } else {
                    pickedTarget.fillHierarchy(newEventTargets);
                }

                int newTargetsSize = newEventTargets.size();
                int i = currentEventTargets.size() - 1;
                int j = newTargetsSize - 1;
                int k = pdrEventTargets.size() - 1;

                while (i >= 0 && j >= 0 && currentEventTargets.get(i) == newEventTargets.get(j)) {
                    i--;
                    j--;
                    k--;
                }

                final int memk = k;
                for (; i >= 0; i--, k--) {
                    final EventTarget exitedEventTarget = currentEventTargets.get(i);
                    if (pdrInProgress &&
                            (k < 0 || exitedEventTarget != pdrEventTargets.get(k))) {
                         break;
                    }
                    queue.postEvent(e.copyFor(
                            exitedEventTarget, exitedEventTarget,
                            MouseEvent.MOUSE_EXITED_TARGET));
                }

                k = memk;
                for (; j >= 0; j--, k--) {
                    final EventTarget enteredEventTarget = newEventTargets.get(j);
                    if (pdrInProgress &&
                            (k < 0 || enteredEventTarget != pdrEventTargets.get(k))) {
                        break;
                    }
                    queue.postEvent(e.copyFor(
                            enteredEventTarget, enteredEventTarget,
                            MouseEvent.MOUSE_ENTERED_TARGET));
                }

                currentEventTarget = pickedTarget.getEventTarget();
                currentEventTargets.clear();
                for (j++; j < newTargetsSize; j++) {
                    currentEventTargets.add(newEventTargets.get(j));
                }
            }
            queue.fire();
        }

        private void process(MouseEvent e, boolean onPulse) {
            Toolkit.getToolkit().checkFxUserThread();
            Scene.inMousePick = true;

            cursorScreenPos = new Point2D(e.getScreenX(), e.getScreenY());
            cursorScenePos = new Point2D(e.getSceneX(), e.getSceneY());

            boolean gestureStarted = false;
            if (!onPulse) {
                if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                    if (!(primaryButtonDown || secondaryButtonDown || middleButtonDown ||
                            backButtonDown || forwardButtonDown) &&
                            Scene.this.dndGesture == null) {
                        //old gesture ended and new one started
                        gestureStarted = true;
                        if (!PLATFORM_DRAG_GESTURE_INITIATION) {
                            Scene.this.dndGesture = new DnDGesture();
                        }
                        clearPDREventTargets();
                    }
                } else if (e.getEventType() == MouseEvent.MOUSE_MOVED) {
                    // gesture ended
                    clearPDREventTargets();
                } else if (e.getEventType() == MouseEvent.MOUSE_ENTERED) {
                    hover = true;
                } else if (e.getEventType() == MouseEvent.MOUSE_EXITED) {
                    hover = false;
                }

                primaryButtonDown = e.isPrimaryButtonDown();
                secondaryButtonDown = e.isSecondaryButtonDown();
                middleButtonDown = e.isMiddleButtonDown();
                backButtonDown = e.isBackButtonDown();
                forwardButtonDown = e.isForwardButtonDown();
            }

            pick(tmpTargetWrapper, e.getSceneX(), e.getSceneY());
            PickResult res = tmpTargetWrapper.getResult();
            if (res != null) {
                e = new MouseEvent(e.getEventType(), e.getSceneX(), e.getSceneY(),
                    e.getScreenX(), e.getScreenY(), e.getButton(), e.getClickCount(),
                    e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(),
                    e.isPrimaryButtonDown(), e.isMiddleButtonDown(), e.isSecondaryButtonDown(),
                    e.isBackButtonDown(), e.isForwardButtonDown(),
                    e.isSynthesized(), e.isPopupTrigger(), e.isStillSincePress(), res);
            }

            if (e.getEventType() == MouseEvent.MOUSE_EXITED) {
                tmpTargetWrapper.clear();
            }

            TargetWrapper target;
            if (pdrInProgress) {
                target = pdrEventTarget;
            } else {
                target = tmpTargetWrapper;
            }

            if (gestureStarted) {
                pdrEventTarget.copy(target);
                pdrEventTarget.fillHierarchy(pdrEventTargets);
            }

            if (!onPulse) {
                e = clickGenerator.preProcess(e);
            }

            // enter/exit handling
            handleEnterExit(e, tmpTargetWrapper);

            //deliver event to the target node
            if (Scene.this.dndGesture != null) {
                Scene.this.dndGesture.processDragDetection(e);
            }

            if (fullPDREntered && e.getEventType() == MouseEvent.MOUSE_RELEASED) {
                processFullPDR(e, onPulse);
            }

            if (target.getEventTarget() != null) {
                if (e.getEventType() != MouseEvent.MOUSE_ENTERED
                        && e.getEventType() != MouseEvent.MOUSE_EXITED
                        && !onPulse) {
                    Event.fireEvent(target.getEventTarget(), e);
                }
            }

            if (fullPDREntered && e.getEventType() != MouseEvent.MOUSE_RELEASED) {
                processFullPDR(e, onPulse);
            }

            if (!onPulse) {
                clickGenerator.postProcess(e, target, tmpTargetWrapper);
            }

            // handle drag and drop

            if (!PLATFORM_DRAG_GESTURE_INITIATION && !onPulse) {
                if (Scene.this.dndGesture != null) {
                    if (!Scene.this.dndGesture.process(e, target.getEventTarget())) {
                        dndGesture = null;
                    }
                }
            }

            Cursor cursor = target.getCursor();
            if (e.getEventType() != MouseEvent.MOUSE_EXITED) {
                if (cursor == null && hover) {
                    cursor = Scene.this.getCursor();
                }

                updateCursor(cursor);
                updateCursorFrame();
            }

            if (gestureStarted) {
                pdrInProgress = true;
            }

            if (pdrInProgress &&
                    !(primaryButtonDown || secondaryButtonDown || middleButtonDown ||
                            backButtonDown || forwardButtonDown)) {
                clearPDREventTargets();
                exitFullPDR(e);
                // we need to do new picking in case the originally picked node
                // was moved or removed by the event handlers
                pick(tmpTargetWrapper, e.getSceneX(), e.getSceneY());
                handleEnterExit(e, tmpTargetWrapper);
            }

            lastEvent = e.getEventType() == MouseEvent.MOUSE_EXITED ? null : e;
            Scene.inMousePick = false;
        }

        private void processFullPDR(MouseEvent e, boolean onPulse) {

            pick(fullPDRTmpTargetWrapper, e.getSceneX(), e.getSceneY());
            final PickResult result = fullPDRTmpTargetWrapper.getResult();

            final EventTarget eventTarget = fullPDRTmpTargetWrapper.getEventTarget();

            // enter/exit handling
            if (eventTarget != fullPDRCurrentTarget) {

                fullPDRTmpTargetWrapper.fillHierarchy(fullPDRNewEventTargets);

                int newTargetsSize = fullPDRNewEventTargets.size();
                int i = fullPDRCurrentEventTargets.size() - 1;
                int j = newTargetsSize - 1;

                while (i >= 0 && j >= 0 &&
                        fullPDRCurrentEventTargets.get(i) == fullPDRNewEventTargets.get(j)) {
                    i--;
                    j--;
                }

                for (; i >= 0; i--) {
                    final EventTarget exitedEventTarget = fullPDRCurrentEventTargets.get(i);
                    Event.fireEvent(exitedEventTarget, MouseEvent.copyForMouseDragEvent(e,
                            exitedEventTarget, exitedEventTarget,
                            MouseDragEvent.MOUSE_DRAG_EXITED_TARGET,
                            fullPDRSource, result));
                }

                for (; j >= 0; j--) {
                    final EventTarget enteredEventTarget = fullPDRNewEventTargets.get(j);
                    Event.fireEvent(enteredEventTarget, MouseEvent.copyForMouseDragEvent(e,
                            enteredEventTarget, enteredEventTarget,
                            MouseDragEvent.MOUSE_DRAG_ENTERED_TARGET,
                            fullPDRSource, result));
                }

                fullPDRCurrentTarget = eventTarget;
                fullPDRCurrentEventTargets.clear();
                fullPDRCurrentEventTargets.addAll(fullPDRNewEventTargets);
                fullPDRNewEventTargets.clear();
            }
            // done enter/exit handling

            // event delivery
            if (eventTarget != null && !onPulse) {
                if (e.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                    Event.fireEvent(eventTarget, MouseEvent.copyForMouseDragEvent(e,
                            eventTarget, eventTarget,
                            MouseDragEvent.MOUSE_DRAG_OVER,
                            fullPDRSource, result));
                }
                if (e.getEventType() == MouseEvent.MOUSE_RELEASED) {
                    Event.fireEvent(eventTarget, MouseEvent.copyForMouseDragEvent(e,
                            eventTarget, eventTarget,
                            MouseDragEvent.MOUSE_DRAG_RELEASED,
                            fullPDRSource, result));
                }
            }
        }

        private void updateCursor(Cursor newCursor) {
            if (currCursor != newCursor) {
                if (currCursor != null) {
                    currCursor.deactivate();
                }

                if (newCursor != null) {
                    newCursor.activate();
                }

                currCursor = newCursor;
            }
        }

        public void updateCursorFrame() {
            final CursorFrame newCursorFrame =
                    (currCursor != null)
                           ? currCursor.getCurrentFrame()
                           : Cursor.DEFAULT.getCurrentFrame();
            if (currCursorFrame != newCursorFrame) {
                if (Scene.this.peer != null) {
                    Scene.this.peer.setCursor(newCursorFrame);
                }

                currCursorFrame = newCursorFrame;
            }
        }

        private PickResult pickNode(PickRay pickRay) {
            PickResultChooser r = new PickResultChooser();
            Scene.this.getRoot().pickNode(pickRay, r);
            return r.toPickResult();
        }
    }

    /*******************************************************************************
     *                                                                             *
     * Key Event Handling                                                          *
     *                                                                             *
     ******************************************************************************/

    class KeyHandler {
        private void setFocusOwner(final Node value) {
            // Cancel IM composition if there is one in progress.
            // This needs to be done before the focus owner is switched as it
            // generates event that needs to be delivered to the old focus owner.
            if (oldFocusOwner != null) {
                final Scene s = oldFocusOwner.getScene();
                if (s != null) {
                    final TKScene peer = s.getPeer();
                    if (peer != null) {
                        peer.finishInputMethodComposition();
                    }
                }
            }
            focusOwner.set(value);
        }

        private boolean windowFocused;
        protected boolean isWindowFocused() { return windowFocused; }
        protected void setWindowFocused(boolean value) {
            windowFocused = value;
            if (getFocusOwner() != null) {
                getFocusOwner().setFocused(windowFocused);
            }
            if (windowFocused) {
                if (accessible != null) {
                    accessible.sendNotification(AccessibleAttribute.FOCUS_NODE);
                }
            }
        }

        private void windowForSceneChanged(Window oldWindow, Window window) {
            if (oldWindow != null) {
                oldWindow.focusedProperty().removeListener(sceneWindowFocusedListener);
            }

            if (window != null) {
                window.focusedProperty().addListener(sceneWindowFocusedListener);
                setWindowFocused(window.isFocused());
            } else {
                setWindowFocused(false);
            }
        }

        private final InvalidationListener sceneWindowFocusedListener = valueModel -> setWindowFocused(((ReadOnlyBooleanProperty)valueModel).get());

        private void process(KeyEvent e) {
            final Node sceneFocusOwner = getFocusOwner();
            final EventTarget eventTarget =
                    (sceneFocusOwner != null && sceneFocusOwner.getScene() == Scene.this) ? sceneFocusOwner
                                              : Scene.this;

            // send the key event to the current focus owner or to scene if
            // the focus owner is not set
            Event.fireEvent(eventTarget, e);
        }

        private void requestFocus(Node node) {
            if (getFocusOwner() == node || (node != null && !node.isCanReceiveFocus())) {
                return;
            }
            setFocusOwner(node);
        }
    }
    /***************************************************************************
     *                                                                         *
     *                         Event Dispatch                                  *
     *                                                                         *
     **************************************************************************/
    // PENDING_DOC_REVIEW
    /**
     * Specifies the event dispatcher for this scene. When replacing the value
     * with a new {@code EventDispatcher}, the new dispatcher should forward
     * events to the replaced dispatcher to keep the scene's default event
     * handling behavior.
     */
    private ObjectProperty<EventDispatcher> eventDispatcher;

    public final void setEventDispatcher(EventDispatcher value) {
        eventDispatcherProperty().set(value);
    }

    public final EventDispatcher getEventDispatcher() {
        return eventDispatcherProperty().get();
    }

    public final ObjectProperty<EventDispatcher>
            eventDispatcherProperty() {
        initializeInternalEventDispatcher();
        return eventDispatcher;
    }

    private SceneEventDispatcher internalEventDispatcher;

    // Delegates requests from platform input method to the focused
    // node's one, if any.
    class InputMethodRequestsDelegate implements ExtendedInputMethodRequests {
        @Override
        public Point2D getTextLocation(int offset) {
            InputMethodRequests requests = getClientRequests();
            if (requests != null) {
                return requests.getTextLocation(offset);
            } else {
                return new Point2D(0, 0);
            }
        }

        @Override
        public int getLocationOffset(int x, int y) {
            InputMethodRequests requests = getClientRequests();
            if (requests != null) {
                return requests.getLocationOffset(x, y);
            } else {
                return 0;
            }
        }

        @Override
        public void cancelLatestCommittedText() {
            InputMethodRequests requests = getClientRequests();
            if (requests != null) {
                requests.cancelLatestCommittedText();
            }
        }

        @Override
        public String getSelectedText() {
            InputMethodRequests requests = getClientRequests();
            if (requests != null) {
                return requests.getSelectedText();
            }
            return null;
        }

        @Override
        public int getInsertPositionOffset() {
            InputMethodRequests requests = getClientRequests();
            if (requests != null && requests instanceof ExtendedInputMethodRequests) {
                return ((ExtendedInputMethodRequests)requests).getInsertPositionOffset();
            }
            return 0;
        }

        @Override
        public String getCommittedText(int begin, int end) {
            InputMethodRequests requests = getClientRequests();
            if (requests != null && requests instanceof ExtendedInputMethodRequests) {
                return ((ExtendedInputMethodRequests)requests).getCommittedText(begin, end);
            }
            return null;
        }

        @Override
        public int getCommittedTextLength() {
            InputMethodRequests requests = getClientRequests();
            if (requests != null && requests instanceof ExtendedInputMethodRequests) {
                return ((ExtendedInputMethodRequests)requests).getCommittedTextLength();
            }
            return 0;
        }

        private InputMethodRequests getClientRequests() {
            Node focusOwner = getFocusOwner();
            if (focusOwner != null) {
                return focusOwner.getInputMethodRequests();
            }
            return null;
        }
    }

    // PENDING_DOC_REVIEW
    /**
     * Registers an event handler to this scene. The handler is called when the
     * scene receives an {@code Event} of the specified type during the bubbling
     * phase of event delivery.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the type of the events to receive by the handler
     * @param eventHandler the handler to register
     * @throws NullPointerException if the event type or handler is null
     */
    public final <T extends Event> void addEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .addEventHandler(eventType, eventHandler);
    }

    // PENDING_DOC_REVIEW
    /**
     * Unregisters a previously registered event handler from this scene. One
     * handler might have been registered for different event types, so the
     * caller needs to specify the particular event type from which to
     * unregister the handler.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type from which to unregister
     * @param eventHandler the handler to unregister
     * @throws NullPointerException if the event type or handler is null
     */
    public final <T extends Event> void removeEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .removeEventHandler(eventType,
                                                        eventHandler);
    }

    // PENDING_DOC_REVIEW
    /**
     * Registers an event filter to this scene. The filter is called when the
     * scene receives an {@code Event} of the specified type during the
     * capturing phase of event delivery.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the type of the events to receive by the filter
     * @param eventFilter the filter to register
     * @throws NullPointerException if the event type or filter is null
     */
    public final <T extends Event> void addEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .addEventFilter(eventType, eventFilter);
    }

    // PENDING_DOC_REVIEW
    /**
     * Unregisters a previously registered event filter from this scene. One
     * filter might have been registered for different event types, so the
     * caller needs to specify the particular event type from which to
     * unregister the filter.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the event type from which to unregister
     * @param eventFilter the filter to unregister
     * @throws NullPointerException if the event type or filter is null
     */
    public final <T extends Event> void removeEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .removeEventFilter(eventType, eventFilter);
    }

    /**
     * Sets the handler to use for this event type. There can only be one such
     * handler specified at a time. This handler is guaranteed to be called
     * first. This is used for registering the user-defined onFoo event
     * handlers.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type to associate with the given eventHandler
     * @param eventHandler the handler to register, or null to unregister
     * @throws NullPointerException if the event type is null
     */
    protected final <T extends Event> void setEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .setEventHandler(eventType, eventHandler);
    }

    private SceneEventDispatcher getInternalEventDispatcher() {
        initializeInternalEventDispatcher();
        return internalEventDispatcher;
    }

    final void initializeInternalEventDispatcher() {
        if (internalEventDispatcher == null) {
            internalEventDispatcher = createInternalEventDispatcher();
            eventDispatcher = new SimpleObjectProperty<EventDispatcher>(
                                          this,
                                          "eventDispatcher",
                                          internalEventDispatcher);
        }
    }

    private SceneEventDispatcher createInternalEventDispatcher() {
        return new SceneEventDispatcher(this);
    }

    /**
     * Registers the specified mnemonic.
     *
     * @param m The mnemonic
     */
    public void addMnemonic(Mnemonic m) {
        getInternalEventDispatcher().getKeyboardShortcutsHandler()
                                    .addMnemonic(m);
    }


    /**
     * Unregisters the specified mnemonic.
     *
     * @param m The mnemonic
     */
    public void removeMnemonic(Mnemonic m) {
        getInternalEventDispatcher().getKeyboardShortcutsHandler()
                                    .removeMnemonic(m);
    }

    final void clearNodeMnemonics(Node node) {
        getInternalEventDispatcher().getKeyboardShortcutsHandler()
                .clearNodeMnemonics(node);
    }


    /**
     * Gets the list of mnemonics for this {@code Scene}.
     *
     * @return the list of mnemonics
     */
    public ObservableMap<KeyCombination, ObservableList<Mnemonic>> getMnemonics() {
        return getInternalEventDispatcher().getKeyboardShortcutsHandler()
                                           .getMnemonics();
    }

    /**
     * Gets the list of accelerators for this {@code Scene}.
     *
     * @return the list of accelerators
     */
    public ObservableMap<KeyCombination, Runnable> getAccelerators() {
        return getInternalEventDispatcher().getKeyboardShortcutsHandler()
                                           .getAccelerators();
    }

    // PENDING_DOC_REVIEW
    /**
     * Construct an event dispatch chain for this scene. The event dispatch
     * chain contains all event dispatchers from the stage to this scene.
     *
     * @param tail the initial chain to build from
     * @return the resulting event dispatch chain for this scene
     */
    @Override
    public EventDispatchChain buildEventDispatchChain(
            EventDispatchChain tail) {
        if (eventDispatcher != null) {
            final EventDispatcher eventDispatcherValue = eventDispatcher.get();
            if (eventDispatcherValue != null) {
                tail = tail.prepend(eventDispatcherValue);
            }
        }

        if (getWindow() != null) {
            tail = getWindow().buildEventDispatchChain(tail);
        }

        return tail;
    }

    /***************************************************************************
     *                                                                         *
     *                             Context Menus                               *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines a function to be called when a mouse button has been clicked
     * (pressed and released) on this {@code Scene}.
     * @since JavaFX 2.1
     */

    private ObjectProperty<EventHandler<? super ContextMenuEvent>> onContextMenuRequested;

    public final void setOnContextMenuRequested(EventHandler<? super ContextMenuEvent> value) {
        onContextMenuRequestedProperty().set(value);
    }

    public final EventHandler<? super ContextMenuEvent> getOnContextMenuRequested() {
        return onContextMenuRequested == null ? null : onContextMenuRequested.get();
    }

    public final ObjectProperty<EventHandler<? super ContextMenuEvent>> onContextMenuRequestedProperty() {
        if (onContextMenuRequested == null) {
            onContextMenuRequested = new ObjectPropertyBase<EventHandler<? super ContextMenuEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onContextMenuRequested";
                }
            };
        }
        return onContextMenuRequested;
    }

    /***************************************************************************
     *                                                                         *
     *                             Mouse Handling                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines a function to be called when a mouse button has been clicked
     * (pressed and released) on this {@code Scene}.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onMouseClicked;

    public final void setOnMouseClicked(EventHandler<? super MouseEvent> value) {
        onMouseClickedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseClicked() {
        return onMouseClicked == null ? null : onMouseClicked.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseClickedProperty() {
        if (onMouseClicked == null) {
            onMouseClicked = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.MOUSE_CLICKED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseClicked";
                }
            };
        }
        return onMouseClicked;
    }

    /**
     * Defines a function to be called when a mouse button is pressed
     * on this {@code Scene} and then dragged.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onMouseDragged;

    public final void setOnMouseDragged(EventHandler<? super MouseEvent> value) {
        onMouseDraggedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseDragged() {
        return onMouseDragged == null ? null : onMouseDragged.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseDraggedProperty() {
        if (onMouseDragged == null) {
            onMouseDragged = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.MOUSE_DRAGGED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseDragged";
                }
            };
        }
        return onMouseDragged;
    }

    /**
     * Defines a function to be called when the mouse enters this {@code Scene}.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onMouseEntered;

    public final void setOnMouseEntered(EventHandler<? super MouseEvent> value) {
        onMouseEnteredProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseEntered() {
        return onMouseEntered == null ? null : onMouseEntered.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseEnteredProperty() {
        if (onMouseEntered == null) {
            onMouseEntered = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.MOUSE_ENTERED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseEntered";
                }
            };
        }
        return onMouseEntered;
    }

    /**
     * Defines a function to be called when the mouse exits this {@code Scene}.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onMouseExited;

    public final void setOnMouseExited(EventHandler<? super MouseEvent> value) {
        onMouseExitedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseExited() {
        return onMouseExited == null ? null : onMouseExited.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseExitedProperty() {
        if (onMouseExited == null) {
            onMouseExited = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.MOUSE_EXITED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseExited";
                }
            };
        }
        return onMouseExited;
    }

    /**
     * Defines a function to be called when mouse cursor moves within
     * this {@code Scene} but no buttons have been pushed.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onMouseMoved;

    public final void setOnMouseMoved(EventHandler<? super MouseEvent> value) {
        onMouseMovedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseMoved() {
        return onMouseMoved == null ? null : onMouseMoved.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseMovedProperty() {
        if (onMouseMoved == null) {
            onMouseMoved = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.MOUSE_MOVED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseMoved";
                }
            };
        }
        return onMouseMoved;
    }

    /**
     * Defines a function to be called when a mouse button
     * has been pressed on this {@code Scene}.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onMousePressed;

    public final void setOnMousePressed(EventHandler<? super MouseEvent> value) {
        onMousePressedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMousePressed() {
        return onMousePressed == null ? null : onMousePressed.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onMousePressedProperty() {
        if (onMousePressed == null) {
            onMousePressed = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.MOUSE_PRESSED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMousePressed";
                }
            };
        }
        return onMousePressed;
    }

    /**
     * Defines a function to be called when a mouse button
     * has been released on this {@code Scene}.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onMouseReleased;

    public final void setOnMouseReleased(EventHandler<? super MouseEvent> value) {
        onMouseReleasedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnMouseReleased() {
        return onMouseReleased == null ? null : onMouseReleased.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseReleasedProperty() {
        if (onMouseReleased == null) {
            onMouseReleased = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.MOUSE_RELEASED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseReleased";
                }
            };
        }
        return onMouseReleased;
    }

    /**
     * Defines a function to be called when drag gesture has been
     * detected. This is the right place to start drag and drop operation.
     */
    private ObjectProperty<EventHandler<? super MouseEvent>> onDragDetected;

    public final void setOnDragDetected(EventHandler<? super MouseEvent> value) {
        onDragDetectedProperty().set(value);
    }

    public final EventHandler<? super MouseEvent> getOnDragDetected() {
        return onDragDetected == null ? null : onDragDetected.get();
    }

    public final ObjectProperty<EventHandler<? super MouseEvent>> onDragDetectedProperty() {
        if (onDragDetected == null) {
            onDragDetected = new ObjectPropertyBase<EventHandler<? super MouseEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseEvent.DRAG_DETECTED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onDragDetected";
                }
            };
        }
        return onDragDetected;
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * progresses within this {@code Scene}.
     * @since JavaFX 2.1
     */
    private ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragOver;

    public final void setOnMouseDragOver(EventHandler<? super MouseDragEvent> value) {
        onMouseDragOverProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragOver() {
        return onMouseDragOver == null ? null : onMouseDragOver.get();
    }

    public final ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragOverProperty() {
        if (onMouseDragOver == null) {
            onMouseDragOver = new ObjectPropertyBase<EventHandler<? super MouseDragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseDragOver";
                }
            };
        }
        return onMouseDragOver;
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * ends within this {@code Scene}.
     * @since JavaFX 2.1
     */
    private ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragReleased;

    public final void setOnMouseDragReleased(EventHandler<? super MouseDragEvent> value) {
        onMouseDragReleasedProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragReleased() {
        return onMouseDragReleased == null ? null : onMouseDragReleased.get();
    }

    public final ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragReleasedProperty() {
        if (onMouseDragReleased == null) {
            onMouseDragReleased = new ObjectPropertyBase<EventHandler<? super MouseDragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseDragReleased";
                }
            };
        }
        return onMouseDragReleased;
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * enters this {@code Scene}.
     * @since JavaFX 2.1
     */
    private ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragEntered;

    public final void setOnMouseDragEntered(EventHandler<? super MouseDragEvent> value) {
        onMouseDragEnteredProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragEntered() {
        return onMouseDragEntered == null ? null : onMouseDragEntered.get();
    }

    public final ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragEnteredProperty() {
        if (onMouseDragEntered == null) {
            onMouseDragEntered = new ObjectPropertyBase<EventHandler<? super MouseDragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseDragEntered";
                }
            };
        }
        return onMouseDragEntered;
    }

    /**
     * Defines a function to be called when a full press-drag-release gesture
     * exits this {@code Scene}.
     * @since JavaFX 2.1
     */
    private ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragExited;

    public final void setOnMouseDragExited(EventHandler<? super MouseDragEvent> value) {
        onMouseDragExitedProperty().set(value);
    }

    public final EventHandler<? super MouseDragEvent> getOnMouseDragExited() {
        return onMouseDragExited == null ? null : onMouseDragExited.get();
    }

    public final ObjectProperty<EventHandler<? super MouseDragEvent>> onMouseDragExitedProperty() {
        if (onMouseDragExited == null) {
            onMouseDragExited = new ObjectPropertyBase<EventHandler<? super MouseDragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onMouseDragExited";
                }
            };
        }
        return onMouseDragExited;
    }


    /***************************************************************************
     *                                                                         *
     *                           Gestures Handling                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines a function to be called when a scrolling gesture is detected.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super ScrollEvent>> onScrollStarted;

    public final void setOnScrollStarted(EventHandler<? super ScrollEvent> value) {
        onScrollStartedProperty().set(value);
    }

    public final EventHandler<? super ScrollEvent> getOnScrollStarted() {
        return onScrollStarted == null ? null : onScrollStarted.get();
    }

    public final ObjectProperty<EventHandler<? super ScrollEvent>> onScrollStartedProperty() {
        if (onScrollStarted == null) {
            onScrollStarted = new ObjectPropertyBase<EventHandler<? super ScrollEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(ScrollEvent.SCROLL_STARTED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onScrollStarted";
                }
            };
        }
        return onScrollStarted;
    }

    /**
     * Defines a function to be called when user performs a scrolling action.
     */
    private ObjectProperty<EventHandler<? super ScrollEvent>> onScroll;

    public final void setOnScroll(EventHandler<? super ScrollEvent> value) {
        onScrollProperty().set(value);
    }

    public final EventHandler<? super ScrollEvent> getOnScroll() {
        return onScroll == null ? null : onScroll.get();
    }

    public final ObjectProperty<EventHandler<? super ScrollEvent>> onScrollProperty() {
        if (onScroll == null) {
            onScroll = new ObjectPropertyBase<EventHandler<? super ScrollEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(ScrollEvent.SCROLL, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onScroll";
                }
            };
        }
        return onScroll;
    }

    /**
     * Defines a function to be called when a scrolling gesture ends.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super ScrollEvent>> onScrollFinished;

    public final void setOnScrollFinished(EventHandler<? super ScrollEvent> value) {
        onScrollFinishedProperty().set(value);
    }

    public final EventHandler<? super ScrollEvent> getOnScrollFinished() {
        return onScrollFinished == null ? null : onScrollFinished.get();
    }

    public final ObjectProperty<EventHandler<? super ScrollEvent>> onScrollFinishedProperty() {
        if (onScrollFinished == null) {
            onScrollFinished = new ObjectPropertyBase<EventHandler<? super ScrollEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(ScrollEvent.SCROLL_FINISHED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onScrollFinished";
                }
            };
        }
        return onScrollFinished;
    }

    /**
     * Defines a function to be called when a rotating gesture is detected.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super RotateEvent>> onRotationStarted;

    public final void setOnRotationStarted(EventHandler<? super RotateEvent> value) {
        onRotationStartedProperty().set(value);
    }

    public final EventHandler<? super RotateEvent> getOnRotationStarted() {
        return onRotationStarted == null ? null : onRotationStarted.get();
    }

    public final ObjectProperty<EventHandler<? super RotateEvent>> onRotationStartedProperty() {
        if (onRotationStarted == null) {
            onRotationStarted = new ObjectPropertyBase<EventHandler<? super RotateEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(RotateEvent.ROTATION_STARTED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onRotationStarted";
                }
            };
        }
        return onRotationStarted;
    }

    /**
     * Defines a function to be called when user performs a rotating action.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super RotateEvent>> onRotate;

    public final void setOnRotate(EventHandler<? super RotateEvent> value) {
        onRotateProperty().set(value);
    }

    public final EventHandler<? super RotateEvent> getOnRotate() {
        return onRotate == null ? null : onRotate.get();
    }

    public final ObjectProperty<EventHandler<? super RotateEvent>> onRotateProperty() {
        if (onRotate == null) {
            onRotate = new ObjectPropertyBase<EventHandler<? super RotateEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(RotateEvent.ROTATE, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onRotate";
                }
            };
        }
        return onRotate;
    }

    /**
     * Defines a function to be called when a rotating gesture ends.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super RotateEvent>> onRotationFinished;

    public final void setOnRotationFinished(EventHandler<? super RotateEvent> value) {
        onRotationFinishedProperty().set(value);
    }

    public final EventHandler<? super RotateEvent> getOnRotationFinished() {
        return onRotationFinished == null ? null : onRotationFinished.get();
    }

    public final ObjectProperty<EventHandler<? super RotateEvent>> onRotationFinishedProperty() {
        if (onRotationFinished == null) {
            onRotationFinished = new ObjectPropertyBase<EventHandler<? super RotateEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(RotateEvent.ROTATION_FINISHED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onRotationFinished";
                }
            };
        }
        return onRotationFinished;
    }

    /**
     * Defines a function to be called when a zooming gesture is detected.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super ZoomEvent>> onZoomStarted;

    public final void setOnZoomStarted(EventHandler<? super ZoomEvent> value) {
        onZoomStartedProperty().set(value);
    }

    public final EventHandler<? super ZoomEvent> getOnZoomStarted() {
        return onZoomStarted == null ? null : onZoomStarted.get();
    }

    public final ObjectProperty<EventHandler<? super ZoomEvent>> onZoomStartedProperty() {
        if (onZoomStarted == null) {
            onZoomStarted = new ObjectPropertyBase<EventHandler<? super ZoomEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(ZoomEvent.ZOOM_STARTED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onZoomStarted";
                }
            };
        }
        return onZoomStarted;
    }

    /**
     * Defines a function to be called when user performs a zooming action.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super ZoomEvent>> onZoom;

    public final void setOnZoom(EventHandler<? super ZoomEvent> value) {
        onZoomProperty().set(value);
    }

    public final EventHandler<? super ZoomEvent> getOnZoom() {
        return onZoom == null ? null : onZoom.get();
    }

    public final ObjectProperty<EventHandler<? super ZoomEvent>> onZoomProperty() {
        if (onZoom == null) {
            onZoom = new ObjectPropertyBase<EventHandler<? super ZoomEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(ZoomEvent.ZOOM, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onZoom";
                }
            };
        }
        return onZoom;
    }

    /**
     * Defines a function to be called when a zooming gesture ends.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super ZoomEvent>> onZoomFinished;

    public final void setOnZoomFinished(EventHandler<? super ZoomEvent> value) {
        onZoomFinishedProperty().set(value);
    }

    public final EventHandler<? super ZoomEvent> getOnZoomFinished() {
        return onZoomFinished == null ? null : onZoomFinished.get();
    }

    public final ObjectProperty<EventHandler<? super ZoomEvent>> onZoomFinishedProperty() {
        if (onZoomFinished == null) {
            onZoomFinished = new ObjectPropertyBase<EventHandler<? super ZoomEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(ZoomEvent.ZOOM_FINISHED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onZoomFinished";
                }
            };
        }
        return onZoomFinished;
    }

    /**
     * Defines a function to be called when an upward swipe gesture
     * happens in this scene.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeUp;

    public final void setOnSwipeUp(EventHandler<? super SwipeEvent> value) {
        onSwipeUpProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeUp() {
        return onSwipeUp == null ? null : onSwipeUp.get();
    }

    public final ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeUpProperty() {
        if (onSwipeUp == null) {
            onSwipeUp = new ObjectPropertyBase<EventHandler<? super SwipeEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(SwipeEvent.SWIPE_UP, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onSwipeUp";
                }
            };
        }
        return onSwipeUp;
    }

    /**
     * Defines a function to be called when an downward swipe gesture
     * happens in this scene.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeDown;

    public final void setOnSwipeDown(EventHandler<? super SwipeEvent> value) {
        onSwipeDownProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeDown() {
        return onSwipeDown == null ? null : onSwipeDown.get();
    }

    public final ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeDownProperty() {
        if (onSwipeDown == null) {
            onSwipeDown = new ObjectPropertyBase<EventHandler<? super SwipeEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(SwipeEvent.SWIPE_DOWN, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onSwipeDown";
                }
            };
        }
        return onSwipeDown;
    }

    /**
     * Defines a function to be called when an leftward swipe gesture
     * happens in this scene.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeLeft;

    public final void setOnSwipeLeft(EventHandler<? super SwipeEvent> value) {
        onSwipeLeftProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeLeft() {
        return onSwipeLeft == null ? null : onSwipeLeft.get();
    }

    public final ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeLeftProperty() {
        if (onSwipeLeft == null) {
            onSwipeLeft = new ObjectPropertyBase<EventHandler<? super SwipeEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(SwipeEvent.SWIPE_LEFT, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onSwipeLeft";
                }
            };
        }
        return onSwipeLeft;
    }

    /**
     * Defines a function to be called when an rightward swipe gesture
     * happens in this scene.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeRight;

    public final void setOnSwipeRight(EventHandler<? super SwipeEvent> value) {
        onSwipeRightProperty().set(value);
    }

    public final EventHandler<? super SwipeEvent> getOnSwipeRight() {
        return onSwipeRight == null ? null : onSwipeRight.get();
    }

    public final ObjectProperty<EventHandler<? super SwipeEvent>> onSwipeRightProperty() {
        if (onSwipeRight == null) {
            onSwipeRight = new ObjectPropertyBase<EventHandler<? super SwipeEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(SwipeEvent.SWIPE_RIGHT, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onSwipeRight";
                }
            };
        }
        return onSwipeRight;
    }

    /***************************************************************************
     *                                                                         *
     *                            Touch Handling                               *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines a function to be called when a new touch point is pressed.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super TouchEvent>> onTouchPressed;

    public final void setOnTouchPressed(EventHandler<? super TouchEvent> value) {
        onTouchPressedProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchPressed() {
        return onTouchPressed == null ? null : onTouchPressed.get();
    }

    public final ObjectProperty<EventHandler<? super TouchEvent>> onTouchPressedProperty() {
        if (onTouchPressed == null) {
            onTouchPressed = new ObjectPropertyBase<EventHandler<? super TouchEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(TouchEvent.TOUCH_PRESSED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onTouchPressed";
                }
            };
        }
        return onTouchPressed;
    }

    /**
     * Defines a function to be called when a touch point is moved.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super TouchEvent>> onTouchMoved;

    public final void setOnTouchMoved(EventHandler<? super TouchEvent> value) {
        onTouchMovedProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchMoved() {
        return onTouchMoved == null ? null : onTouchMoved.get();
    }

    public final ObjectProperty<EventHandler<? super TouchEvent>> onTouchMovedProperty() {
        if (onTouchMoved == null) {
            onTouchMoved = new ObjectPropertyBase<EventHandler<? super TouchEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(TouchEvent.TOUCH_MOVED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onTouchMoved";
                }
            };
        }
        return onTouchMoved;
    }

    /**
     * Defines a function to be called when a new touch point is pressed.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super TouchEvent>> onTouchReleased;

    public final void setOnTouchReleased(EventHandler<? super TouchEvent> value) {
        onTouchReleasedProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchReleased() {
        return onTouchReleased == null ? null : onTouchReleased.get();
    }

    public final ObjectProperty<EventHandler<? super TouchEvent>> onTouchReleasedProperty() {
        if (onTouchReleased == null) {
            onTouchReleased = new ObjectPropertyBase<EventHandler<? super TouchEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(TouchEvent.TOUCH_RELEASED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onTouchReleased";
                }
            };
        }
        return onTouchReleased;
    }

    /**
     * Defines a function to be called when a touch point stays pressed and
     * still.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<? super TouchEvent>> onTouchStationary;

    public final void setOnTouchStationary(EventHandler<? super TouchEvent> value) {
        onTouchStationaryProperty().set(value);
    }

    public final EventHandler<? super TouchEvent> getOnTouchStationary() {
        return onTouchStationary == null ? null : onTouchStationary.get();
    }

    public final ObjectProperty<EventHandler<? super TouchEvent>> onTouchStationaryProperty() {
        if (onTouchStationary == null) {
            onTouchStationary = new ObjectPropertyBase<EventHandler<? super TouchEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(TouchEvent.TOUCH_STATIONARY, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onTouchStationary";
                }
            };
        }
        return onTouchStationary;
    }

    /*
     * This class provides reordering and ID mapping of particular touch points.
     * Platform may report arbitrary touch point IDs and they may be reused
     * during one gesture. This class keeps track of it and provides
     * sequentially sorted IDs, unique in scope of a gesture.
     *
     * Some platforms report always small numbers, these take fast paths through
     * the algorithm, directly indexing an array. Bigger numbers take a slow
     * path using a hash map.
     *
     * The algorithm performance was measured and it doesn't impose
     * any significant slowdown on the event delivery.
     */
    private static class TouchMap {
        private static final int FAST_THRESHOLD = 10;
        int[] fastMap = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Map<Long, Integer> slowMap = new HashMap<Long, Integer>();
        List<Integer> order = new LinkedList<Integer>();
        List<Long> removed = new ArrayList<Long>(10);
        int counter = 0;
        int active = 0;

        public int add(long id) {
            counter++;
            active++;
            if (id < FAST_THRESHOLD) {
                fastMap[(int) id] = counter;
            } else {
                slowMap.put(id, counter);
            }
            order.add(counter);
            return counter;
        }

        public void remove(long id) {
            // book the removal - it needs to be done after all touch points
            // of an event are processed - see cleanup()
            removed.add(id);
        }

        public int get(long id) {
            if (id < FAST_THRESHOLD) {
                int result = fastMap[(int) id];
                if (result == 0) {
                    throw new RuntimeException("Platform reported wrong "
                            + "touch point ID");
                }
                return result;
            } else {
                try {
                    return slowMap.get(id);
                } catch (NullPointerException e) {
                    throw new RuntimeException("Platform reported wrong "
                            + "touch point ID");
                }
            }
        }

        public int getOrder(int id) {
            return order.indexOf(id);
        }

        // returns true if gesture finished (no finger is touched)
        public boolean cleanup() {
            for (long id : removed) {
                active--;
                order.remove(Integer.valueOf(get(id)));
                if (id < FAST_THRESHOLD) {
                    fastMap[(int) id] = 0;
                } else {
                    slowMap.remove(id);
                }
                if (active == 0) {
                    // gesture finished
                    counter = 0;
                }
            }
            removed.clear();
            return active == 0;
        }
    }


    /***************************************************************************
     *                                                                         *
     *                         Drag and Drop Handling                          *
     *                                                                         *
     **************************************************************************/

    private ObjectProperty<EventHandler<? super DragEvent>> onDragEntered;

    public final void setOnDragEntered(EventHandler<? super DragEvent> value) {
        onDragEnteredProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragEntered() {
        return onDragEntered == null ? null : onDragEntered.get();
    }

    /**
     * Defines a function to be called when drag gesture
     * enters this {@code Scene}.
     * @return function to be called when drag gesture enters this scene
     */
    public final ObjectProperty<EventHandler<? super DragEvent>> onDragEnteredProperty() {
        if (onDragEntered == null) {
            onDragEntered = new ObjectPropertyBase<EventHandler<? super DragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(DragEvent.DRAG_ENTERED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onDragEntered";
                }
            };
        }
        return onDragEntered;
    }

    private ObjectProperty<EventHandler<? super DragEvent>> onDragExited;

    public final void setOnDragExited(EventHandler<? super DragEvent> value) {
        onDragExitedProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragExited() {
        return onDragExited == null ? null : onDragExited.get();
    }

    /**
     * Defines a function to be called when drag gesture
     * exits this {@code Scene}.
     * @return the function to be called when drag gesture exits this scene
     */
    public final ObjectProperty<EventHandler<? super DragEvent>> onDragExitedProperty() {
        if (onDragExited == null) {
            onDragExited = new ObjectPropertyBase<EventHandler<? super DragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(DragEvent.DRAG_EXITED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onDragExited";
                }
            };
        }
        return onDragExited;
    }

    private ObjectProperty<EventHandler<? super DragEvent>> onDragOver;

    public final void setOnDragOver(EventHandler<? super DragEvent> value) {
        onDragOverProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragOver() {
        return onDragOver == null ? null : onDragOver.get();
    }

    /**
     * Defines a function to be called when drag gesture progresses
     * within this {@code Scene}.
     * @return the function to be called when drag gesture progresses within
     * this scene
     */
    public final ObjectProperty<EventHandler<? super DragEvent>> onDragOverProperty() {
        if (onDragOver == null) {
            onDragOver = new ObjectPropertyBase<EventHandler<? super DragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(DragEvent.DRAG_OVER, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onDragOver";
                }
            };
        }
        return onDragOver;
    }

    // Do we want DRAG_TRANSFER_MODE_CHANGED event?
//    private ObjectProperty<EventHandler<? super DragEvent>> onDragTransferModeChanged;
//
//    public final void setOnDragTransferModeChanged(EventHandler<? super DragEvent> value) {
//        onDragTransferModeChangedProperty().set(value);
//    }
//
//    public final EventHandler<? super DragEvent> getOnDragTransferModeChanged() {
//        return onDragTransferModeChanged == null ? null : onDragTransferModeChanged.get();
//    }
//
//    /**
//     * Defines a function to be called this {@code Scene} if it is a potential
//     * drag-and-drop target when the user takes action to change the intended
//     * {@code TransferMode}.
//     * The user can change the intended {@link TransferMode} by holding down
//     * or releasing key modifiers.
//     */
//    public ObjectProperty<EventHandler<? super DragEvent>> onDragTransferModeChangedProperty() {
//        if (onDragTransferModeChanged == null) {
//            onDragTransferModeChanged = new SimpleObjectProperty<EventHandler<? super DragEvent>>() {
//
//                @Override
//                protected void invalidated() {
//                    setEventHandler(DragEvent.DRAG_TRANSFER_MODE_CHANGED, get());
//                }
//            };
//        }
//        return onDragTransferModeChanged;
//    }

    private ObjectProperty<EventHandler<? super DragEvent>> onDragDropped;

    public final void setOnDragDropped(EventHandler<? super DragEvent> value) {
        onDragDroppedProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragDropped() {
        return onDragDropped == null ? null : onDragDropped.get();
    }

    /**
     * Defines a function to be called when the mouse button is released
     * on this {@code Scene} during drag and drop gesture. Transfer of data from
     * the {@link DragEvent}'s {@link DragEvent#getDragboard() dragboard} should
     * happen in this function.
     * @return the function to be called when the mouse button is released on
     * this scene during drag and drop gesture
     */
    public final ObjectProperty<EventHandler<? super DragEvent>> onDragDroppedProperty() {
        if (onDragDropped == null) {
            onDragDropped = new ObjectPropertyBase<EventHandler<? super DragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(DragEvent.DRAG_DROPPED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onDragDropped";
                }
            };
        }
        return onDragDropped;
    }

    private ObjectProperty<EventHandler<? super DragEvent>> onDragDone;

    public final void setOnDragDone(EventHandler<? super DragEvent> value) {
        onDragDoneProperty().set(value);
    }

    public final EventHandler<? super DragEvent> getOnDragDone() {
        return onDragDone == null ? null : onDragDone.get();
    }

    /**
     * Defines a function to be called when this {@code Scene} is a
     * drag and drop gesture source after its data has
     * been dropped on a drop target. The {@code transferMode} of the
     * event shows what just happened at the drop target.
     * If {@code transferMode} has the value {@code MOVE}, then the source can
     * clear out its data. Clearing the source's data gives the appropriate
     * appearance to a user that the data has been moved by the drag and drop
     * gesture. A {@code transferMode} that has the value {@code NONE}
     * indicates that no data was transferred during the drag and drop gesture.
     * Positional data for the {@code DragEvent} is invalid.  Valid positional
     * data for the {@code DragEvent} is presented in the
     * {@link #onDragDroppedProperty() onDragDropped} event handler.
     * @return the function to be called when this scene is a drag and drop
     * gesture source after its data has been dropped on a drop target
     */
    public final ObjectProperty<EventHandler<? super DragEvent>> onDragDoneProperty() {
        if (onDragDone == null) {
            onDragDone = new ObjectPropertyBase<EventHandler<? super DragEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(DragEvent.DRAG_DONE, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onDragDone";
                }
            };
        }
        return onDragDone;
    }

    /**
     * Confirms a potential drag and drop gesture that is recognized over this
     * {@code Scene}.
     * Can be called only from a DRAG_DETECTED event handler. The returned
     * {@link Dragboard} is used to transfer data during
     * the drag and drop gesture. Placing this {@code Scene}'s data on the
     * {@link Dragboard} also identifies this {@code Scene} as the source of
     * the drag and drop gesture.
     * More detail about drag and drop gestures is described in the overivew
     * of {@link DragEvent}.
     *
     * @see DragEvent
     * @param transferModes The supported {@code TransferMode}(s) of this {@code Node}
     * @return A {@code Dragboard} to place this {@code Scene}'s data on
     * @throws IllegalStateException if drag and drop cannot be started at this
     * moment (it's called outside of {@code DRAG_DETECTED} event handling).
     */
    public Dragboard startDragAndDrop(TransferMode... transferModes) {
        return startDragAndDrop(this, transferModes);
    }

    /**
     * Starts a full press-drag-release gesture with this scene as gesture
     * source. This method can be called only from a {@code DRAG_DETECTED} mouse
     * event handler. More detail about dragging gestures can be found
     * in the overview of {@link MouseEvent} and {@link MouseDragEvent}.
     *
     * @see MouseEvent
     * @see MouseDragEvent
     * @throws IllegalStateException if the full press-drag-release gesture
     * cannot be started at this moment (it's called outside of
     * {@code DRAG_DETECTED} event handling).
     * @since JavaFX 2.1
     */
    public void startFullDrag() {
        startFullDrag(this);
    }


    Dragboard startDragAndDrop(EventTarget source, TransferMode... transferModes) {
        Toolkit.getToolkit().checkFxUserThread();
        if (dndGesture == null ||
            (dndGesture.dragDetected != DragDetectedState.PROCESSING))
        {
            throw new IllegalStateException("Cannot start drag and drop " +
                    "outside of DRAG_DETECTED event handler");
        }

        Set<TransferMode> set = EnumSet.noneOf(TransferMode.class);
        for (TransferMode tm : InputEventUtils.safeTransferModes(transferModes)) {
            set.add(tm);
        }
        return dndGesture.startDrag(source, set);
    }

    void startFullDrag(EventTarget source) {
        Toolkit.getToolkit().checkFxUserThread();
        if (dndGesture.dragDetected != DragDetectedState.PROCESSING) {
            throw new IllegalStateException("Cannot start full drag " +
                    "outside of DRAG_DETECTED event handler");
        }

        if (dndGesture != null) {
            dndGesture.startFullPDR(source);
            return;
        }

        throw new IllegalStateException("Cannot start full drag when "
                + "mouse button is not pressed");
    }

    /***************************************************************************
     *                                                                         *
     *                           Keyboard Handling                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines a function to be called when some {@code Node} of this
     * {@code Scene} has input focus and a key has been pressed. The function
     * is called only if the event hasn't been already consumed during its
     * capturing or bubbling phase.
     */
    private ObjectProperty<EventHandler<? super KeyEvent>> onKeyPressed;

    public final void setOnKeyPressed(EventHandler<? super KeyEvent> value) {
        onKeyPressedProperty().set(value);
    }

    public final EventHandler<? super KeyEvent> getOnKeyPressed() {
        return onKeyPressed == null ? null : onKeyPressed.get();
    }

    public final ObjectProperty<EventHandler<? super KeyEvent>> onKeyPressedProperty() {
        if (onKeyPressed == null) {
            onKeyPressed = new ObjectPropertyBase<EventHandler<? super KeyEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(KeyEvent.KEY_PRESSED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onKeyPressed";
                }
            };
        }
        return onKeyPressed;
    }

    /**
     * Defines a function to be called when some {@code Node} of this
     * {@code Scene} has input focus and a key has been released. The function
     * is called only if the event hasn't been already consumed during its
     * capturing or bubbling phase.
     */
    private ObjectProperty<EventHandler<? super KeyEvent>> onKeyReleased;

    public final void setOnKeyReleased(EventHandler<? super KeyEvent> value) {
        onKeyReleasedProperty().set(value);
    }

    public final EventHandler<? super KeyEvent> getOnKeyReleased() {
        return onKeyReleased == null ? null : onKeyReleased.get();
    }

    public final ObjectProperty<EventHandler<? super KeyEvent>> onKeyReleasedProperty() {
        if (onKeyReleased == null) {
            onKeyReleased = new ObjectPropertyBase<EventHandler<? super KeyEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(KeyEvent.KEY_RELEASED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onKeyReleased";
                }
            };
        }
        return onKeyReleased;
    }

    /**
     * Defines a function to be called when some {@code Node} of this
     * {@code Scene} has input focus and a key has been typed. The function
     * is called only if the event hasn't been already consumed during its
     * capturing or bubbling phase.
     */
    private ObjectProperty<EventHandler<? super KeyEvent>> onKeyTyped;

    public final void setOnKeyTyped(
            EventHandler<? super KeyEvent> value) {
        onKeyTypedProperty().set( value);

    }

    public final EventHandler<? super KeyEvent> getOnKeyTyped(
            ) {
        return onKeyTyped == null ? null : onKeyTyped.get();
    }

    public final ObjectProperty<EventHandler<? super KeyEvent>> onKeyTypedProperty(
    ) {
        if (onKeyTyped == null) {
            onKeyTyped = new ObjectPropertyBase<EventHandler<? super KeyEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(KeyEvent.KEY_TYPED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onKeyTyped";
                }
            };
        }
        return onKeyTyped;
    }

    /***************************************************************************
     *                                                                         *
     *                           Input Method Handling                         *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines a function to be called when this {@code Node}
     * has input focus and the input method text has changed.  If this
     * function is not defined in this {@code Node}, then it
     * receives the result string of the input method composition as a
     * series of {@code onKeyTyped} function calls.
     * <p>
     * When the {@code Node} loses the input focus, the JavaFX runtime
     * automatically commits the existing composed text if any.
     */
    private ObjectProperty<EventHandler<? super InputMethodEvent>> onInputMethodTextChanged;

    public final void setOnInputMethodTextChanged(
            EventHandler<? super InputMethodEvent> value) {
        onInputMethodTextChangedProperty().set( value);
    }

    public final EventHandler<? super InputMethodEvent> getOnInputMethodTextChanged() {
        return onInputMethodTextChanged == null ? null : onInputMethodTextChanged.get();
    }

    public final ObjectProperty<EventHandler<? super InputMethodEvent>> onInputMethodTextChangedProperty() {
        if (onInputMethodTextChanged == null) {
            onInputMethodTextChanged = new ObjectPropertyBase<EventHandler<? super InputMethodEvent>>() {

                @Override
                protected void invalidated() {
                    setEventHandler(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, get());
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "onInputMethodTextChanged";
                }
            };
        }
        return onInputMethodTextChanged;
    }

    /*
     * This class represents a picked target - either node, or scne, or null.
     * It provides functionality needed for the targets and covers the fact
     * that they are different kinds of animals.
     */
    private static class TargetWrapper {
        private Scene scene;
        private Node node;
        private PickResult result;

        /**
         * Fills the list with the target and all its parents (including scene)
         */
        public void fillHierarchy(final List<EventTarget> list) {
            list.clear();
            Node n = node;
            while(n != null) {
                list.add(n);
                final Parent p = n.getParent();
                n = p != null ? p : n.getSubScene();
            }

            if (scene != null) {
                list.add(scene);
            }
        }

        public EventTarget getEventTarget() {
            return node != null ? node : scene;
        }

        public Cursor getCursor() {
            Cursor cursor = null;
            if (node != null) {
                cursor = node.getCursor();
                Node n = node.getParent();
                while (cursor == null && n != null) {
                    cursor = n.getCursor();

                    final Parent p = n.getParent();
                    n = p != null ? p : n.getSubScene();
                }
            }
            return cursor;
        }

        public void clear() {
            set(null, null);
            result = null;
        }

        public void setNodeResult(PickResult result) {
            if (result != null) {
                this.result = result;
                final Node n = result.getIntersectedNode();
                set(n, n.getScene());
            }
        }

        // Pass null scene if the mouse is outside of the window content
        public void setSceneResult(PickResult result, Scene scene) {
            if (result != null) {
                this.result = result;
                set(null, scene);
            }
        }

        public PickResult getResult() {
            return result;
        }

        public void copy(TargetWrapper tw) {
            node = tw.node;
            scene = tw.scene;
            result = tw.result;
        }

        private void set(Node n, Scene s) {
            node = n;
            scene = s;
        }
    }

    /*************************************************************************
    *                                                                        *
    *                                                                        *
    *                                                                        *
    *************************************************************************/

    private static final Object USER_DATA_KEY = new Object();
    // A map containing a set of properties for this scene
    private ObservableMap<Object, Object> properties;

    /**
      * Returns an observable map of properties on this node for use primarily
      * by application developers.
      *
      * @return an observable map of properties on this node for use primarily
      * by application developers
      *
      * @since JavaFX 8u40
      */
     public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableMap(new HashMap<Object, Object>());
        }
        return properties;
    }

    /**
     * Tests if Scene has properties.
     * @return true if node has properties.
     *
     * @since JavaFX 8u40
     */
     public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    /**
     * Convenience method for setting a single Object property that can be
     * retrieved at a later date. This is functionally equivalent to calling
     * the getProperties().put(Object key, Object value) method. This can later
     * be retrieved by calling {@link Scene#getUserData()}.
     *
     * @param value The value to be stored - this can later be retrieved by calling
     *          {@link Scene#getUserData()}.
     *
     * @since JavaFX 8u40
     */
    public void setUserData(Object value) {
        getProperties().put(USER_DATA_KEY, value);
    }

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the {@link Scene#setUserData(java.lang.Object)} method.
     *
     * @return The Object that was previously set, or null if no property
     *          has been set or if null was set.
     *
     * @since JavaFX 8u40
     */
    public Object getUserData() {
        return getProperties().get(USER_DATA_KEY);
    }

    /***************************************************************************
     *                                                                         *
     *                       Component Orientation Properties                  *
     *                                                                         *
     **************************************************************************/

    private static final NodeOrientation defaultNodeOrientation =
        AccessController.doPrivileged(
                (PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.scene.nodeOrientation.RTL")) ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.INHERIT;



    private ObjectProperty<NodeOrientation> nodeOrientation;
    private EffectiveOrientationProperty effectiveNodeOrientationProperty;

    private NodeOrientation effectiveNodeOrientation;

    public final void setNodeOrientation(NodeOrientation orientation) {
        nodeOrientationProperty().set(orientation);
    }

    public final NodeOrientation getNodeOrientation() {
        return nodeOrientation == null ? defaultNodeOrientation : nodeOrientation.get();
    }

    /**
     * Property holding NodeOrientation.
     * <p>
     * Node orientation describes the flow of visual data within a node.
     * In the English speaking world, visual data normally flows from
     * left-to-right. In an Arabic or Hebrew world, visual data flows
     * from right-to-left.  This is consistent with the reading order
     * of text in both worlds.  The default value is left-to-right.
     * </p>
     *
     * @return NodeOrientation
     * @since JavaFX 8.0
     */
    public final ObjectProperty<NodeOrientation> nodeOrientationProperty() {
        if (nodeOrientation == null) {
            nodeOrientation = new StyleableObjectProperty<NodeOrientation>(defaultNodeOrientation) {
                @Override
                protected void invalidated() {
                    sceneEffectiveOrientationInvalidated();
                    getRoot().applyCss();
                }

                @Override
                public Object getBean() {
                    return Scene.this;
                }

                @Override
                public String getName() {
                    return "nodeOrientation";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    //TODO - not yet supported
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
        }
        return nodeOrientation;
    }

    public final NodeOrientation getEffectiveNodeOrientation() {
        if (effectiveNodeOrientation == null) {
            effectiveNodeOrientation = calcEffectiveNodeOrientation();
        }

        return effectiveNodeOrientation;
    }

    /**
     * The effective node orientation of a scene resolves the inheritance of
     * node orientation, returning either left-to-right or right-to-left.
     * @return the effective node orientation of this scene
     * @since JavaFX 8.0
     */
    public final ReadOnlyObjectProperty<NodeOrientation>
            effectiveNodeOrientationProperty() {
        if (effectiveNodeOrientationProperty == null) {
            effectiveNodeOrientationProperty =
                    new EffectiveOrientationProperty();
        }

        return effectiveNodeOrientationProperty;
    }

    private void parentEffectiveOrientationInvalidated() {
        if (getNodeOrientation() == NodeOrientation.INHERIT) {
            sceneEffectiveOrientationInvalidated();
        }
    }

    private void sceneEffectiveOrientationInvalidated() {
        effectiveNodeOrientation = null;

        if (effectiveNodeOrientationProperty != null) {
            effectiveNodeOrientationProperty.invalidate();
        }

        getRoot().parentResolvedOrientationInvalidated();
    }

    private NodeOrientation calcEffectiveNodeOrientation() {
        NodeOrientation orientation = getNodeOrientation();
        if (orientation == NodeOrientation.INHERIT) {
            Window window = getWindow();
            if (window != null) {
                Window parent = null;
                if (window instanceof Stage) {
                    parent = ((Stage)window).getOwner();
                } else {
                    if (window instanceof PopupWindow) {
                        parent = ((PopupWindow)window).getOwnerWindow();
                    }
                }
                if (parent != null) {
                    Scene scene = parent.getScene();
                    if (scene != null) return scene.getEffectiveNodeOrientation();
                }
            }
            return NodeOrientation.LEFT_TO_RIGHT;
        }
        return orientation;
    }

    private final class EffectiveOrientationProperty
            extends ReadOnlyObjectPropertyBase<NodeOrientation> {
        @Override
        public NodeOrientation get() {
            return getEffectiveNodeOrientation();
        }

        @Override
        public Object getBean() {
            return Scene.this;
        }

        @Override
        public String getName() {
            return "effectiveNodeOrientation";
        }

        public void invalidate() {
            fireValueChangedEvent();
        }
    }

    private Map<Node, Accessible> accMap;
    Accessible removeAccessible(Node node) {
        if (accMap == null) return null;
        return accMap.remove(node);
    }

    void addAccessible(Node node, Accessible acc) {
        if (accMap == null) {
            accMap = new HashMap<Node, Accessible>();
        }
        accMap.put(node, acc);
    }

    private void disposeAccessibles() {
        if (accMap != null) {
            for (Map.Entry<Node, Accessible> entry : accMap.entrySet()) {
                Node node = entry.getKey();
                Accessible acc = entry.getValue();
                if (node.accessible != null) {
                    /* This node has already been initialized to another scene.
                     * Note an accessible can be returned to the node before the
                     * pulse if getAccessible() is called. In which case it must
                     * already being removed from accMap.
                     */
                    if (node.accessible == acc) {
                        System.err.println("[A11y] 'node.accessible == acc' should never happen.");
                    }
                    if (node.getScene() == this) {
                        System.err.println("[A11y] 'node.getScene() == this' should never happen.");
                    }
                    acc.dispose();
                } else {
                    if (node.getScene() == this) {
                        node.accessible = acc;
                    } else {
                        acc.dispose();
                    }
                }
            }
            accMap.clear();
        }
    }

    private Accessible accessible;
    Accessible getAccessible() {
        /*
         * The accessible for the Scene should never be
         * requested when the peer is not set.
         * This can only happen in a error case where a
         * descender of this Scene was not disposed and
         * it still being used by the AT client and trying
         * to reach to the top level window.
         */
        if (peer == null) return null;
        if (accessible == null) {
            accessible = Application.GetApplication().createAccessible();
            accessible.setEventHandler(new Accessible.EventHandler() {
                @Override public AccessControlContext getAccessControlContext() {
                    return getPeer().getAccessControlContext();
                }

                @Override public Object getAttribute(AccessibleAttribute attribute,
                                                     Object... parameters) {
                    switch (attribute) {
                        case CHILDREN: {
                            Parent root = getRoot();
                            if (root != null) {
                                return FXCollections.observableArrayList(root);
                            }
                            break;
                        }
                        case TEXT: {
                            Window w = getWindow();
                            if (w instanceof Stage) {
                                return ((Stage)w).getTitle();
                            }
                            break;
                        }
                        case NODE_AT_POINT: {
                            Window window = getWindow();
                            /* is this screen to scene translation correct ? not considering camera ? */
                            Point2D pt = (Point2D)parameters[0];
                            PickResult res = pick(pt.getX() - getX() - window.getX(), pt.getY() - getY() - window.getY());
                            if (res != null) {
                                Node node = res.getIntersectedNode();
                                if (node != null) return node;
                            }
                            return getRoot();//not sure
                        }
                        case ROLE: return AccessibleRole.PARENT;
                        case SCENE: return Scene.this;
                        case FOCUS_NODE: {
                            if (transientFocusContainer != null) {
                                return transientFocusContainer.queryAccessibleAttribute(AccessibleAttribute.FOCUS_NODE);
                            }
                            return getFocusOwner();
                        }
                        default:
                    }
                    return super.getAttribute(attribute, parameters);
                }
            });
            PlatformImpl.accessibilityActiveProperty().set(true);
        }
        return accessible;
    }
}
