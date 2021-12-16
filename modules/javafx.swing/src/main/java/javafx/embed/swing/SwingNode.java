/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.embed.swing;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Window;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.javafx.embed.swing.Disposer;
import com.sun.javafx.embed.swing.DisposerRecord;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGExternalNode;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.stage.FocusUngrabEvent;
import com.sun.javafx.stage.WindowHelper;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.NodeHelper;
import static javafx.stage.WindowEvent.WINDOW_HIDDEN;

import com.sun.javafx.embed.swing.SwingNodeHelper;
import com.sun.javafx.embed.swing.SwingEvents;
import com.sun.javafx.embed.swing.newimpl.SwingNodeInteropN;

/**
 * This class is used to embed a Swing content into a JavaFX application.
 * The content to be displayed is specified with the {@link #setContent} method
 * that accepts an instance of Swing {@code JComponent}. The hierarchy of components
 * contained in the {@code JComponent} instance should not contain any heavyweight
 * components, otherwise {@code SwingNode} may fail to paint it. The content gets
 * repainted automatically. All the input and focus events are forwarded to the
 * {@code JComponent} instance transparently to the developer.
 * <p>
 * Here is a typical pattern which demonstrates how {@code SwingNode} can be used:
 * <pre>
 *     public class SwingFx extends Application {
 *
 *         &#064;Override
 *         public void start(Stage stage) {
 *             final SwingNode swingNode = new SwingNode();
 *             createAndSetSwingContent(swingNode);
 *
 *             StackPane pane = new StackPane();
 *             pane.getChildren().add(swingNode);
 *
 *             stage.setScene(new Scene(pane, 100, 50));
 *             stage.show();
 *         }
 *
 *         private void createAndSetSwingContent(final SwingNode swingNode) {
 *             SwingUtilities.invokeLater(new Runnable() {
 *                 &#064;Override
 *                 public void run() {
 *                     swingNode.setContent(new JButton("Click me!"));
 *                 }
 *             });
 *         }
 *
 *         public static void main(String[] args) {
 *             launch(args);
 *         }
 *     }
 * </pre>
 * @since JavaFX 8.0
 */
public class SwingNode extends Node {
    private static boolean isThreadMerged;

    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                isThreadMerged = Boolean.valueOf(
                        System.getProperty("javafx.embed.singleThread"));
                return null;
            }
        });


         // This is used by classes in different packages to get access to
         // private and package private methods.
        SwingNodeHelper.setSwingNodeAccessor(new SwingNodeHelper.SwingNodeAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((SwingNode) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((SwingNode) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((SwingNode) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((SwingNode) node).doComputeContains(localX, localY);
            }

            @Override
            public Object getLightweightFrame(SwingNode node) {
                return node.getLightweightFrame();
            }

            @Override
            public ReentrantLock getPaintLock(SwingNode node) {
                return node.getPaintLock();
            }

            @Override
            public void setImageBuffer(SwingNode node, final int[] data,
                               final int x, final int y,
                               final int w, final int h, final int linestride,
                               final double scaleX, final double scaleY) {
                node.setImageBuffer(data, x, y, w, h, linestride, scaleX, scaleY);
            }

            @Override
            public void setImageBounds(SwingNode node, final int x, final int y,
                               final int w, final int h) {
                node.setImageBounds(x, y, w, h);
            }

            @Override
            public void repaintDirtyRegion(SwingNode node, final int dirtyX, final int dirtyY,
                               final int dirtyWidth, final int dirtyHeight) {
                node.repaintDirtyRegion(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            }

            @Override
            public void ungrabFocus(SwingNode node, boolean postUngrabEvent) {
                node.ungrabFocus(postUngrabEvent);
            }

            @Override
            public void setSwingPrefWidth(SwingNode node, int swingPrefWidth) {
                node.swingPrefWidth = swingPrefWidth;
            }

            @Override
            public void setSwingPrefHeight(SwingNode node, int swingPrefHeight) {
                node.swingPrefHeight = swingPrefHeight;
            }

            @Override
            public void setSwingMaxWidth(SwingNode node, int swingMaxWidth) {
                node.swingMaxWidth = swingMaxWidth;
            }

            @Override
            public void setSwingMaxHeight(SwingNode node, int swingMaxHeight) {
                node.swingMaxHeight = swingMaxHeight;
            }

            @Override
            public void setSwingMinWidth(SwingNode node, int swingMinWidth) {
                node.swingMinWidth = swingMinWidth;
            }

            @Override
            public void setSwingMinHeight(SwingNode node, int swingMinHeight) {
                node.swingMinHeight = swingMinHeight;
            }

            @Override
            public void setGrabbed(SwingNode node, boolean grab) {
                node.grabbed = grab;
            }
        });
    }

    private double fxWidth;
    private double fxHeight;
    private int swingPrefWidth;
    private int swingPrefHeight;
    private int swingMaxWidth;
    private int swingMaxHeight;
    private int swingMinWidth;
    private int swingMinHeight;

    private volatile JComponent content;
    private volatile Object lwFrame;
    private final Object getLightweightFrame() { return lwFrame; }

    private NGExternalNode peer;

    private final ReentrantLock paintLock = new ReentrantLock();

    private ReentrantLock getPaintLock() {
        return paintLock;
    }

    private boolean skipBackwardUnrgabNotification;
    private boolean grabbed; // lwframe initiated grab
    private Timer deactivate; // lwFrame deactivate delay for Linux
    private SwingNodeInteropN swNodeIOP;

    {
        // To initialize the class helper at the begining each constructor of this class
        SwingNodeHelper.initHelper(this);
    }

    /**
     * Constructs a new instance of {@code SwingNode}.
     */
    public SwingNode() {
        swNodeIOP = new SwingNodeInteropN();
        setFocusTraversable(true);
        setEventHandler(MouseEvent.ANY, new SwingMouseEventHandler());
        setEventHandler(KeyEvent.ANY, new SwingKeyEventHandler());
        setEventHandler(ScrollEvent.SCROLL, new SwingScrollEventHandler());

        focusedProperty().addListener((observable, oldValue, newValue) -> {
             activateLwFrame(newValue);
        });

        //Workaround for RT-34170
        javafx.scene.text.Font.getFamilies();
    }


    private EventHandler windowHiddenHandler = (Event event) -> {
        if (lwFrame != null &&  event.getTarget() instanceof Window) {
            final Window w = (Window) event.getTarget();
            TKStage tk = WindowHelper.getPeer(w);
            if (tk != null) {
                if (isThreadMerged) {
                    swNodeIOP.overrideNativeWindowHandle(lwFrame, 0L, null);
                } else {
                    // Postpone actual window closing to ensure that
                    // a native window handler is valid on a Swing side
                    tk.postponeClose();
                    SwingNodeHelper.runOnEDT(() -> {
                        swNodeIOP.overrideNativeWindowHandle(lwFrame, 0L,
                            (Runnable) () -> SwingNodeHelper.runOnFxThread(
                                        () -> tk.closePostponed()));
                    });
                }
            }
        }

    };

    private Window hWindow = null;
    private void notifyNativeHandle(Window window) {
        if (hWindow != window) {
            if (hWindow != null) {
                hWindow.removeEventHandler(WINDOW_HIDDEN, windowHiddenHandler);
            }
            if (window != null) {
                window.addEventHandler(WINDOW_HIDDEN, windowHiddenHandler);
            }
            hWindow = window;
        }

        if (lwFrame != null) {
            long rawHandle = 0L;
            if (window != null) {
                TKStage tkStage = WindowHelper.getPeer(window);
                if (tkStage != null) {
                    rawHandle = tkStage.getRawHandle();
                }
            }
            swNodeIOP.overrideNativeWindowHandle(lwFrame, rawHandle, null);
        }
    }

    /**
     * Attaches a {@code JComponent} instance to display in this {@code SwingNode}.
     * <p>
     * The method can be called either on the JavaFX Application thread or the Event Dispatch thread.
     * Note however, that access to a Swing component must occur from the Event Dispatch thread
     * according to the Swing threading restrictions.
     *
     * @param content a Swing component to display in this {@code SwingNode}
     *
     * @see java.awt.EventQueue#isDispatchThread()
     * @see javafx.application.Platform#isFxApplicationThread()
     */
    public void setContent(final JComponent content) {
        this.content = content;

        SwingNodeHelper.runOnEDT(() -> setContentImpl(content));
    }

    /**
     * Returns the {@code JComponent} instance attached to this {@code SwingNode}.
     * <p>
     * The method can be called either on the JavaFX Application thread or the Event Dispatch thread.
     * Note however, that access to a Swing component must occur from the Event Dispatch thread
     * according to the Swing threading restrictions.
     *
     * @see java.awt.EventQueue#isDispatchThread()
     * @see javafx.application.Platform#isFxApplicationThread()
     *
     * @return the Swing component attached to this {@code SwingNode}
     */
    public JComponent getContent() {
        return content;
    }

    /*
     * Called on EDT
     */
    private void setContentImpl(JComponent content) {
        if (lwFrame != null) {
            swNodeIOP.disposeFrame(lwFrame);
            lwFrame = null;
        }
        if (content != null) {
            lwFrame = swNodeIOP.createLightweightFrame();

            SwingNodeWindowFocusListener snfListener =
                                 new SwingNodeWindowFocusListener(this);
            swNodeIOP.addWindowFocusListener(lwFrame, snfListener);

            if (getScene() != null) {
                Window window = getScene().getWindow();
                if (window != null) {
                    swNodeIOP.notifyDisplayChanged(lwFrame, window.getRenderScaleX(),
                                               window.getRenderScaleY());
                }
            }
            swNodeIOP.setContent(lwFrame, swNodeIOP.createSwingNodeContent(content, this));
            swNodeIOP.setVisible(lwFrame, true);

            Disposer.addRecord(this, swNodeIOP.createSwingNodeDisposer(lwFrame));

            if (getScene() != null) {
                notifyNativeHandle(getScene().getWindow());
            }

            SwingNodeHelper.runOnFxThread(() -> {
                locateLwFrame();// initialize location

                if (focusedProperty().get()) {
                    activateLwFrame(true);
                }
            });
        }
    }

    private List<Runnable> peerRequests = new ArrayList<>();

    /*
     * Called on EDT
     */
    void setImageBuffer(final int[] data,
                        final int x, final int y,
                        final int w, final int h,
                        final int linestride,
                        final double scaleX,
                        final double scaleY)
    {
        Runnable r = () -> peer.setImageBuffer(IntBuffer.wrap(data), x, y, w, h,
                                w, h, linestride, scaleX, scaleY);
        SwingNodeHelper.runOnFxThread(() -> {
            if (peer != null) {
                r.run();
            } else {
                peerRequests.clear();
                peerRequests.add(r);
            }
        });
    }

    /*
     * Called on EDT
     */
    void setImageBounds(final int x, final int y, final int w, final int h) {
        Runnable r = () -> peer.setImageBounds(x, y, w, h, w, h);
        SwingNodeHelper.runOnFxThread(() -> {
            if (peer != null) {
                r.run();
            } else {
                peerRequests.add(r);
            }
        });
    }

    /*
     * Called on EDT
     */
    void repaintDirtyRegion(final int dirtyX, final int dirtyY, final int dirtyWidth, final int dirtyHeight) {
        Runnable r = () -> {
            peer.repaintDirtyRegion(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            NodeHelper.markDirty(this, DirtyBits.NODE_CONTENTS);
        };
        SwingNodeHelper.runOnFxThread(() -> {
            if (peer != null) {
                r.run();
            } else {
                peerRequests.add(r);
            }
        });
    }

    @Override public boolean isResizable() {
        return true;
    }

    /**
     * Invoked by the {@code SwingNode}'s parent during layout to set the {@code SwingNode}'s
     * width and height. <b>Applications should not invoke this method directly</b>.
     * If an application needs to directly set the size of the {@code SwingNode}, it should
     * set the Swing component's minimum/preferred/maximum size constraints which will
     * be propagated correspondingly to the {@code SwingNode} and it's parent will honor those
     * settings during layout.
     *
     * @param width the target layout bounds width
     * @param height the target layout bounds height
     */
    @Override public void resize(final double width, final double height) {
        super.resize(width, height);
        if (width != this.fxWidth || height != this.fxHeight) {
            this.fxWidth = width;
            this.fxHeight = height;
            NodeHelper.geomChanged(this);
            NodeHelper.markDirty(this, DirtyBits.NODE_GEOMETRY);
            SwingNodeHelper.runOnEDT(() -> {
                if (lwFrame != null) {
                    locateLwFrame();
                }
            });
        }
    }

    /**
     * Returns the {@code SwingNode}'s preferred width for use in layout calculations.
     * This value corresponds to the preferred width of the Swing component.
     *
     * @return the preferred width that the node should be resized to during layout
     */
    @Override
    public double prefWidth(double height) {
        return swingPrefWidth;
    }

    /**
     * Returns the {@code SwingNode}'s preferred height for use in layout calculations.
     * This value corresponds to the preferred height of the Swing component.
     *
     * @return the preferred height that the node should be resized to during layout
     */
    @Override
    public double prefHeight(double width) {
        return swingPrefHeight;
    }

    /**
     * Returns the {@code SwingNode}'s maximum width for use in layout calculations.
     * This value corresponds to the maximum width of the Swing component.
     *
     * @return the maximum width that the node should be resized to during layout
     */
    @Override public double maxWidth(double height) {
        return swingMaxWidth;
    }

    /**
     * Returns the {@code SwingNode}'s maximum height for use in layout calculations.
     * This value corresponds to the maximum height of the Swing component.
     *
     * @return the maximum height that the node should be resized to during layout
     */
    @Override public double maxHeight(double width) {
        return swingMaxHeight;
    }

    /**
     * Returns the {@code SwingNode}'s minimum width for use in layout calculations.
     * This value corresponds to the minimum width of the Swing component.
     *
     * @return the minimum width that the node should be resized to during layout
     */
    @Override public double minWidth(double height) {
        return swingMinWidth;
    }

    /**
     * Returns the {@code SwingNode}'s minimum height for use in layout calculations.
     * This value corresponds to the minimum height of the Swing component.
     *
     * @return the minimum height that the node should be resized to during layout
     */
    @Override public double minHeight(double width) {
        return swingMinHeight;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        return true;
    }

    private final InvalidationListener locationListener = observable -> {
        locateLwFrame();
    };

    @SuppressWarnings("removal")
    private final EventHandler<FocusUngrabEvent> ungrabHandler = event -> {
        if (!skipBackwardUnrgabNotification) {
            if (lwFrame != null) {
                AccessController.doPrivileged(new PostEventAction(
                    swNodeIOP.createUngrabEvent(lwFrame)));
            }
        }
    };

    private final ChangeListener<Boolean> windowVisibleListener = (observable, oldValue, newValue) -> {
        if (!newValue) {
            disposeLwFrame();
        } else {
            setContent(content);
        }
    };

    private final ChangeListener<Window> sceneWindowListener = (observable, oldValue, newValue) -> {
        if (oldValue != null) {
            removeWindowListeners(oldValue);
        }

        notifyNativeHandle(newValue);

        if (newValue != null) {
            addWindowListeners(newValue);
        }
    };

    private void removeSceneListeners(Scene scene) {
        Window window = scene.getWindow();
        if (window != null) {
            removeWindowListeners(window);
        }
        scene.windowProperty().removeListener(sceneWindowListener);
    }

    private void addSceneListeners(final Scene scene) {
        Window window = scene.getWindow();
        if (window != null) {
            addWindowListeners(window);
            notifyNativeHandle(window);
        }
        scene.windowProperty().addListener(sceneWindowListener);
    }

    private void addWindowListeners(final Window window) {
        window.xProperty().addListener(locationListener);
        window.yProperty().addListener(locationListener);
        window.widthProperty().addListener(locationListener);
        window.heightProperty().addListener(locationListener);
        window.renderScaleXProperty().addListener(locationListener);
        window.addEventHandler(FocusUngrabEvent.FOCUS_UNGRAB, ungrabHandler);
        window.showingProperty().addListener(windowVisibleListener);
        setLwFrameScale(window.getRenderScaleX(), window.getRenderScaleY());
    }

    private void removeWindowListeners(final Window window) {
        window.xProperty().removeListener(locationListener);
        window.yProperty().removeListener(locationListener);
        window.widthProperty().removeListener(locationListener);
        window.heightProperty().removeListener(locationListener);
        window.renderScaleXProperty().removeListener(locationListener);
        window.removeEventHandler(FocusUngrabEvent.FOCUS_UNGRAB, ungrabHandler);
        window.showingProperty().removeListener(windowVisibleListener);
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        peer = new NGExternalNode();
        peer.setLock(paintLock);
        for (Runnable request : peerRequests) {
            request.run();
        }
        peerRequests = null;

        if (getScene() != null) {
            addSceneListeners(getScene());
        }

        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                // Removed from scene
                removeSceneListeners(oldValue);
                disposeLwFrame();
            }
            if (newValue != null) {
                // Added to another scene
                if (content != null && lwFrame == null) {
                    setContent(content);
                }
                addSceneListeners(newValue);
            }
        });

        NodeHelper.treeVisibleProperty(this).addListener((observable, oldValue, newValue) -> {
            setLwFrameVisible(newValue);
        });

        return peer;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        if (NodeHelper.isDirty(this, DirtyBits.NODE_VISIBLE)
                || NodeHelper.isDirty(this, DirtyBits.NODE_BOUNDS)) {
            locateLwFrame(); // initialize location
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            peer.markContentDirty();
        }
    }

    private void locateLwFrame() {
        if (getScene() == null
                || lwFrame == null
                || getScene().getWindow() == null
                || !getScene().getWindow().isShowing()) {
            // Not initialized yet. Skip the update to set the real values later
            return;
        }
        Window w = getScene().getWindow();
        double renderScaleX = w.getRenderScaleX();
        double renderScaleY = w.getRenderScaleY();
        final Point2D loc = localToScene(0, 0);
        final int windowX = (int) (w.getX());
        final int windowY = (int) (w.getY());
        final int windowW = (int) (w.getWidth());
        final int windowH = (int) (w.getHeight());
        final int frameX = (int) Math.round(w.getX() + getScene().getX() + loc.getX());
        final int frameY = (int) Math.round(w.getY() + getScene().getY() + loc.getY());
        final int frameW = (int) (fxWidth);
        final int frameH = (int) (fxHeight);

        SwingNodeHelper.runOnEDT(() -> {
            if (lwFrame != null) {
                swNodeIOP.notifyDisplayChanged(lwFrame, renderScaleX, renderScaleY);
                swNodeIOP.setBounds(lwFrame, frameX, frameY, frameW, frameH);
                swNodeIOP.setHostBounds(lwFrame, windowX, windowY, windowW, windowH);
            }
        });
    }

    private void activateLwFrame(final boolean activate) {
        if (lwFrame == null) {
            return;
        }
        if (PlatformUtil.isLinux()) {
            // Workaround to block FocusOut/FocusIn notifications from Unity
            // focus grabbing upon Alt press
            if (deactivate == null || !deactivate.isRunning()) {
                if (!activate) {
                    deactivate = new Timer(50, (e) -> {
                        {
                            if (lwFrame != null) {
                                swNodeIOP.emulateActivation(lwFrame, false);
                            }
                        }
                    });
                    deactivate.start();
                    return;
                }
            } else {
                deactivate.stop();
            }
        }

        SwingNodeHelper.runOnEDT(() -> {
            if (lwFrame != null) {
                swNodeIOP.emulateActivation(lwFrame, activate);
            }
        });
    }

    private void disposeLwFrame() {
        if (lwFrame == null) {
            return;
        }
        SwingNodeHelper.runOnEDT(() -> {
            if (lwFrame != null) {
                swNodeIOP.disposeFrame(lwFrame);
                lwFrame = null;
            }
        });
    }

    private void setLwFrameVisible(final boolean visible) {
        if (lwFrame == null) {
            return;
        }
        SwingNodeHelper.runOnEDT(() -> {
            if (lwFrame != null) {
                swNodeIOP.setVisible(lwFrame, visible);
            }
        });
    }

    private void setLwFrameScale(final double scaleX, final double scaleY) {
        if (lwFrame == null) {
            return;
        }
        SwingNodeHelper.runOnEDT(() -> {
            if (lwFrame != null) {
                swNodeIOP.notifyDisplayChanged(lwFrame, scaleX, scaleY);
        }
    });
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        bounds.deriveWithNewBounds(0, 0, 0, (float)fxWidth, (float)fxHeight, 0);
        tx.transform(bounds, bounds);
        return bounds;
    }


    private static class SwingNodeWindowFocusListener implements WindowFocusListener {
        private WeakReference<SwingNode> swingNodeRef;

        SwingNodeWindowFocusListener(SwingNode swingNode) {
            this.swingNodeRef = new WeakReference<SwingNode>(swingNode);
        }

        @Override
        public void windowGainedFocus(WindowEvent e) {
            SwingNodeHelper.runOnFxThread(() -> {
                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    swingNode.requestFocus();
                }
            });
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
            SwingNodeHelper.runOnFxThread(() -> {
                SwingNode swingNode = swingNodeRef.get();
                if (swingNode != null) {
                    swingNode.ungrabFocus(true);
                }
            });
        }
    }

    private void ungrabFocus(boolean postUngrabEvent) {
        // On X11 grab is limited to a single XDisplay connection,
        // so we can't delegate it to another GUI toolkit.
        if (PlatformUtil.isLinux()) return;

        if (grabbed &&
            getScene() != null &&
            getScene().getWindow() != null &&
            WindowHelper.getPeer(getScene().getWindow()) != null)
        {
            skipBackwardUnrgabNotification = !postUngrabEvent;
            WindowHelper.getPeer(getScene().getWindow()).ungrabFocus();
            skipBackwardUnrgabNotification = false;
            grabbed = false;
        }
    }

    private class PostEventAction implements PrivilegedAction<Void> {
        private AWTEvent event;
        PostEventAction(AWTEvent event) {
            this.event = event;
        }
        @Override
        public Void run() {
            EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
            eq.postEvent(event);
            return null;
        }
    }

    private class SwingMouseEventHandler implements EventHandler<MouseEvent> {
        private final Set<MouseButton> mouseClickedAllowed = new HashSet<>();

        @Override
        public void handle(MouseEvent event) {
            Object frame = swNodeIOP.getLightweightFrame();
            if (frame == null) {
                return;
            }
            int swingID = SwingEvents.fxMouseEventTypeToMouseID(event);
            if (swingID < 0) {
                return;
            }

            // Prevent ancestors of the SwingNode from stealing the focus
            event.consume();

            final EventType<?> type = event.getEventType();
            if (type == MouseEvent.MOUSE_PRESSED) {
                mouseClickedAllowed.add(event.getButton());
            } else if (type == MouseEvent.MOUSE_RELEASED) {
                // RELEASED comes before CLICKED, so we don't remove the button from the set
                //mouseClickedAllowed.remove(event.getButton());
            } else if (type == MouseEvent.MOUSE_DRAGGED) {
                // This is what AWT/Swing do
                mouseClickedAllowed.clear();
            } else if (type == MouseEvent.MOUSE_CLICKED) {
                if (event.getClickCount() == 1 && !mouseClickedAllowed.contains(event.getButton())) {
                    // RT-34610: In FX, CLICKED events are generated even after dragging the mouse pointer
                    // Note that this is only relevant for single clicks. Double clicks use a smudge factor.
                    return;
                }
                mouseClickedAllowed.remove(event.getButton());
            }
            int swingModifiers = SwingEvents.fxMouseModsToMouseMods(event);
            boolean swingPopupTrigger = event.isPopupTrigger();
            int swingButton = SwingEvents.fxMouseButtonToMouseButton(event);
            long swingWhen = System.currentTimeMillis();
            int relX = (int) Math.round(event.getX());
            int relY = (int) Math.round(event.getY());
            int absX = (int) Math.round(event.getScreenX());
            int absY = (int) Math.round(event.getScreenY());
            java.awt.event.MouseEvent mouseEvent =
                    swNodeIOP.createMouseEvent(
                        frame, swingID, swingWhen, swingModifiers,
                        relX, relY, absX, absY,
                        event.getClickCount(), swingPopupTrigger, swingButton);
            @SuppressWarnings("removal")
            var dummy = AccessController.doPrivileged(new PostEventAction(mouseEvent));
        }
    }

    private class SwingScrollEventHandler implements EventHandler<ScrollEvent> {
        @Override
        public void handle(ScrollEvent event) {
            Object frame = swNodeIOP.getLightweightFrame();
            if (frame == null) {
                return;
            }

            int swingModifiers = SwingEvents.fxScrollModsToMouseWheelMods(event);
            final boolean isShift = (swingModifiers & InputEvent.SHIFT_DOWN_MASK) != 0;

            // Vertical scroll.
            if (!isShift && event.getDeltaY() != 0.0) {
                sendMouseWheelEvent(frame, event.getX(), event.getY(),
                        swingModifiers, event.getDeltaY() / event.getMultiplierY());
            }
            // Horizontal scroll or shirt+vertical scroll.
            final double delta = isShift && event.getDeltaY() != 0.0
                                  ? event.getDeltaY() / event.getMultiplierY()
                                  : event.getDeltaX() / event.getMultiplierX();
            if (delta != 0.0) {
                swingModifiers |= InputEvent.SHIFT_DOWN_MASK;
                sendMouseWheelEvent(frame, event.getX(), event.getY(),
                        swingModifiers, delta);
            }
        }

        private void sendMouseWheelEvent(Object source, double fxX, double fxY, int swingModifiers, double delta) {
            int wheelRotation = (int) delta;
            int signum = (int) Math.signum(delta);
            if (signum * delta < 1) {
                wheelRotation = signum;
            }
            int x = (int) Math.round(fxX);
            int y = (int) Math.round(fxY);
            MouseWheelEvent mouseWheelEvent =
                swNodeIOP.createMouseWheelEvent(source, swingModifiers, x, y, -wheelRotation);
            @SuppressWarnings("removal")
            var dummy = AccessController.doPrivileged(new PostEventAction(mouseWheelEvent));
        }
    }

    private class SwingKeyEventHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            Object frame = swNodeIOP.getLightweightFrame();
            if (frame == null) {
                return;
            }
            if (event.getCharacter().isEmpty()) {
                // TODO: should we post an "empty" character?
                return;
            }
            // Don't let Arrows, Tab, Shift+Tab traverse focus out.
            if (event.getCode() == KeyCode.LEFT  ||
                event.getCode() == KeyCode.RIGHT ||
                event.getCode() == KeyCode.UP ||
                event.getCode() == KeyCode.DOWN ||
                event.getCode() == KeyCode.TAB)
            {
                event.consume();
            }

            int swingID = SwingEvents.fxKeyEventTypeToKeyID(event);
            if (swingID < 0) {
                return;
            }
            int swingModifiers = SwingEvents.fxKeyModsToKeyMods(event);
            int swingKeyCode = event.getCode().getCode();
            char swingChar = event.getCharacter().charAt(0);

            // A workaround. Some swing L&F's process mnemonics on KEY_PRESSED,
            // for which swing provides a keychar. Extracting it from the text.
            if (event.getEventType() == javafx.scene.input.KeyEvent.KEY_PRESSED) {
                String text = event.getText();
                if (text.length() == 1) {
                    swingChar = text.charAt(0);
                }
            }
            long swingWhen = System.currentTimeMillis();
            java.awt.event.KeyEvent keyEvent = swNodeIOP.createKeyEvent(frame,
                swingID, swingWhen, swingModifiers, swingKeyCode,
                swingChar);
            @SuppressWarnings("removal")
            var dummy = AccessController.doPrivileged(new PostEventAction(keyEvent));
        }
    }
}


