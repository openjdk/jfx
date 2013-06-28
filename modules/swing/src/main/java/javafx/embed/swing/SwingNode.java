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

package javafx.embed.swing;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGExternalNode;
import com.sun.javafx.stage.FocusUngrabEvent;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javafx.scene.Scene;
import javafx.stage.Window;
import sun.awt.UngrabEvent;

import sun.swing.LightweightContent;
import sun.swing.JLightweightFrame;

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
 *         private SwingNode swingNode;
 *
 *         &#064;Override
 *         public void start(Stage stage) {
 *             swingNode = new SwingNode();
 *
 *             createAndSetSwingContent();
 *
 *             StackPane pane = new StackPane();
 *             pane.getChildren().add(swingNode);
 *
 *             stage.setScene(new Scene(pane, 100, 50));
 *             stage.show();
 *         }
 *
 *         private void createAndSetSwingContent() {
 *             SwingUtilities.invokeLater(new Runnable() {
 *                 &#064;Override
 *                 public void run() {
 *                     swingNode.setContent(new JButton("Click me!"));
 *                 }
 *             });
 *         }
 *     }
 * </pre>
 * @since JavaFX 8.0
 */
public class SwingNode extends Node {

    private double width;
    private double height;

    private volatile JComponent content;
    private volatile JLightweightFrame lwFrame;

    private volatile PGExternalNode peer;

    private final ReentrantLock paintLock = new ReentrantLock();

    private boolean skipBackwardUnrgabNotification;
    private boolean grabbed; // lwframe initiated grab
    
    /**
     * Constructs a new instance of {@code SwingNode}.
     */
    public SwingNode() {
        setFocusTraversable(true);
        setEventHandler(MouseEvent.ANY, new SwingMouseEventHandler());
        setEventHandler(KeyEvent.ANY, new SwingKeyEventHandler());

        focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, final Boolean newValue) {
                 activateLwFrame(newValue);
            }
        });
    }

    /**
     * Attaches a {@code JComponent} instance to display in this {@code SwingNode}.
     * <p>
     * The method can be called either on the JavaFX Application thread or the Swing thread.
     * Note however, that access to a Swing component must occur from the Swing thread according
     * to the Swing threading restrictions.
     *
     * @param content a Swing component to display in this {@code SwingNode}
     *
     * @see java.awt.EventQueue#isDispatchThread()
     * @see javafx.application.Platform#isFxApplicationThread()
     */
    public void setContent(final JComponent content) {
        this.content = content;

        invokeOnEDT(new Runnable() {
            @Override
            public void run() {
                setContentImpl(content);
            }
        });
    }

   /**
     * Returns the {@code JComponent} instance attached to this {@code SwingNode}.
     * <p>
     * The method can be called either on the JavaFX Application thread or the Swing thread.
     * Note however, that access to a Swing component must occur from the Swing thread according
     * to the Swing threading restrictions.
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
     * Called on Swing thread
     */
    private void setContentImpl(JComponent content) {
        if (lwFrame != null) {
            lwFrame.dispose();
            lwFrame = null;
        }
        if (content != null) {
            lwFrame = new JLightweightFrame();

            lwFrame.addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                }
                @Override
                public void windowLostFocus(WindowEvent e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            ungrabFocus(true);
                        }
                    });
                }
            });

            lwFrame.setContent(new SwingNodeContent(content));
            lwFrame.setVisible(true);

            locateLwFrame(); // initialize location

            if (focusedProperty().get()) {
                activateLwFrame(true);
            }
        }
    }

    private List<Runnable> peerRequests = new ArrayList<>();

    /*
     * Called on Swing thread
     */
    void setImageBuffer(final int[] data,
                        final int x, final int y,
                        final int w, final int h,
                        final int linestride)
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                peer.setImageBuffer(IntBuffer.wrap(data), x, y, w, h, linestride);
            }
        };
        if (peer != null) {
            Platform.runLater(r);
        } else {
            peerRequests.clear();
            peerRequests.add(r);
        }
    }

    /*
     * Called on Swing thread
     */
    void setImageBounds(final int x, final int y, final int w, final int h) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                peer.setImageBounds(x, y, w, h);
            }
        };
        if (peer != null) {
            Platform.runLater(r);
        } else {
            peerRequests.add(r);
        }
    }

    /*
     * Called on Swing thread
     */
    void repaintDirtyRegion(final int dirtyX, final int dirtyY, final int dirtyWidth, final int dirtyHeight) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                peer.repaintDirtyRegion(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
                impl_markDirty(DirtyBits.NODE_CONTENTS);
            }
        };
        if (peer != null) {
            Platform.runLater(r);
        } else {
            peerRequests.add(r);
        }
    }

    @Override public boolean isResizable() {
        return true;
    }

    @Override public void resize(final double width, final double height) {
        this.width = width;
        this.height = height;
        super.resize(width, height);
        impl_geomChanged();
        impl_markDirty(DirtyBits.NODE_GEOMETRY);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (lwFrame != null) {
                    lwFrame.setSize((int)width, (int)height);
                }
            }
        });
    }

    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double prefWidth(double height) {
        return -1;
    }

    @Override
    public double prefHeight(double width) {
        return -1;
    }

    @Override
    public double minWidth(double height) {
        return 0;
    }

    @Override
    public double minHeight(double width) {
        return 0;
    }

    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        return true;
    }

    private InvalidationListener locationListener = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            locateLwFrame();
        }
    };

    private EventHandler<FocusUngrabEvent> ungrabHandler = new EventHandler<FocusUngrabEvent>() {
        @Override
        public void handle(FocusUngrabEvent event) {
            if (!skipBackwardUnrgabNotification) {
                AccessController.doPrivileged(new PostEventAction(new UngrabEvent(lwFrame)));
            }
        }
    };

    private ChangeListener<Boolean> windowVisibleListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (!newValue) {
                disposeLwFrame();

            } else {
                setContent(content);
            }
        }
    };

    private void removeListeners(Scene scene) {
        Window window = scene.getWindow();
        if (window != null) {
            window.xProperty().removeListener(locationListener);
            window.yProperty().removeListener(locationListener);
            window.removeEventHandler(FocusUngrabEvent.FOCUS_UNGRAB, ungrabHandler);
            window.showingProperty().removeListener(windowVisibleListener);
        }
    }

    private void addListeners(Scene scene) {
        Window window = scene.getWindow();
        if (window != null) {
            window.xProperty().addListener(locationListener);
            window.yProperty().addListener(locationListener);
            window.addEventHandler(FocusUngrabEvent.FOCUS_UNGRAB, ungrabHandler);
            window.showingProperty().addListener(windowVisibleListener);
        }
    }

    @Override
    protected PGNode impl_createPGNode() {
        peer = com.sun.javafx.tk.Toolkit.getToolkit().createPGExternalNode();
        peer.setLock(paintLock);
        for (Runnable request : peerRequests) {
            request.run();
        }
        peerRequests = null;

        if (content != null) {
            setContent(content); // in case the Node is re-added to Scene
        }
        addListeners(getScene());

        sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override
            public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                // Removed from scene, or added to another scene.
                // The lwFrame will be recreated from impl_createPGNode().
                removeListeners(oldValue);
                disposeLwFrame();
            }
        });

        impl_treeVisibleProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                setLwFrameVisible(newValue);
            }
        });

        return peer;
    }

    @Override
    public void impl_updatePG() {
        super.impl_updatePG();

        if (impl_isDirty(DirtyBits.NODE_VISIBLE)) {
            locateLwFrame(); // initialize location
        }
        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            peer.markContentDirty();
        }
    }

    private void locateLwFrame() {
        if (getScene() == null || lwFrame == null) {
            return;
        }
        final Point2D loc = localToScene(0, 0);
        final int windowX = (int)getScene().getWindow().getX();
        final int windowY = (int)getScene().getWindow().getY();
        final int sceneX = (int)getScene().getX();
        final int sceneY = (int)getScene().getY();

        invokeOnEDT(new Runnable() {
            @Override
            public void run() {
                if (lwFrame != null) {
                    lwFrame.setLocation(windowX + sceneX + (int)loc.getX(),
                                        windowY + sceneY + (int)loc.getY());
                }
            }
        });
    }

    private void activateLwFrame(final boolean activate) {
        if (lwFrame == null) {
            return;
        }
        invokeOnEDT(new Runnable() {
            @Override
            public void run() {
                if (lwFrame != null) {
                    lwFrame.emulateActivation(activate);
                }
            }
        });
    }

    private void disposeLwFrame() {
        if (lwFrame == null) {
            return;
        }
        invokeOnEDT(new Runnable() {
            @Override
            public void run() {
                if (lwFrame != null) {
                    lwFrame.dispose();
                    lwFrame = null;
                }
            }
        });
    }

    private void setLwFrameVisible(final boolean visible) {
        if (lwFrame == null) {
            return;
        }
        invokeOnEDT(new Runnable() {
            @Override
            public void run() {
                if (lwFrame != null) {
                    lwFrame.setVisible(visible);
                }
            }
        });
    }

    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        bounds.deriveWithNewBounds(0, 0, 0, (float)width, (float)height, 0);
        tx.transform(bounds, bounds);
        return bounds;
    }

    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return alg.processLeafNode(this, ctx);
    }

    private class SwingNodeContent implements LightweightContent {
        private JComponent comp;
        public SwingNodeContent(JComponent comp) {
            this.comp = comp;
        }
        @Override
        public JComponent getComponent() {
            return comp;
        }
        @Override
        public void paintLock() {
            paintLock.lock();
        }
        @Override
        public void paintUnlock() {
            paintLock.unlock();
        }
        @Override
        public void imageBufferReset(int[] data, int x, int y, int width, int height, int linestride) {
            SwingNode.this.setImageBuffer(data, x, y, width, height, linestride);
        }
        @Override
        public void imageReshaped(int x, int y, int width, int height) {
            SwingNode.this.setImageBounds(x, y, width, height);
        }
        @Override
        public void imageUpdated(int dirtyX, int dirtyY, int dirtyWidth, int dirtyHeight) {
            SwingNode.this.repaintDirtyRegion(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
        }
        @Override
        public void focusGrabbed() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (getScene() != null && getScene().getWindow() != null) {
                        getScene().getWindow().impl_getPeer().grabFocus();
                        grabbed = true;
                    }
                }
            });
        }
        @Override
        public void focusUngrabbed() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ungrabFocus(false);
                }
            });
        }
    }

    private void ungrabFocus(boolean postUngrabEvent) {
        if (grabbed &&
            getScene() != null && getScene().getWindow() != null)
        {
            skipBackwardUnrgabNotification = !postUngrabEvent;
            getScene().getWindow().impl_getPeer().ungrabFocus();
            skipBackwardUnrgabNotification = false;
            grabbed = false;
        }
    }

    private class PostEventAction implements PrivilegedAction<Void> {
        private AWTEvent event;
        public PostEventAction(AWTEvent event) {
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
        @Override
        public void handle(MouseEvent event) {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED &&
                !SwingNode.this.isFocused() && SwingNode.this.isFocusTraversable())
            {
                SwingNode.this.requestFocus();
            }
            int swingID = SwingEvents.fxMouseEventTypeToMouseID(event);
            if (swingID < 0) {
                return;
            }
            int swingModifiers = SwingEvents.fxMouseModsToMouseMods(event);
            // TODO: popupTrigger
            boolean swingPopupTrigger = event.getButton() == MouseButton.SECONDARY;
            int swingButton = SwingEvents.fxMouseButtonToMouseButton(event);
            long swingWhen = System.currentTimeMillis();
            java.awt.event.MouseEvent mouseEvent =
                    new java.awt.event.MouseEvent(
                        lwFrame, swingID, swingWhen, swingModifiers,
                        (int)event.getX(), (int)event.getY(), (int)event.getScreenX(), (int)event.getSceneY(),
                        event.getClickCount(), swingPopupTrigger, swingButton);
            AccessController.doPrivileged(new PostEventAction(mouseEvent));
        }
    }

    private class SwingKeyEventHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            if (event.getCharacter().isEmpty()) {
                // TODO: should we post an "empty" character?
                return;
            }
            // Let Ctrl+Tab, Shift+Strl+Tab traverse focus out.
            if (event.getCode() == KeyCode.TAB && event.isControlDown()) {
                Direction d = event.isShiftDown() ? Direction.PREVIOUS : Direction.NEXT;
                getParent().getImpl_traversalEngine().trav(SwingNode.this, d);
                return;
            }
            // Don't let Arrows, Tab, Shift+Tab traverse focus out.
            if (event.getCode() == KeyCode.LEFT  ||
                event.getCode() == KeyCode.RIGHT ||
                event.getCode() == KeyCode.TAB)
            {
                event.consume();
            }

            int swingID = SwingEvents.fxKeyEventTypeToKeyID(event);
            if (swingID < 0) {
                return;
            }
            int swingModifiers = SwingEvents.fxKeyModsToKeyMods(event);
            int swingKeyCode = event.getCode().impl_getCode();
            char swingChar = event.getCharacter().charAt(0);
            long swingWhen = System.currentTimeMillis();
            java.awt.event.KeyEvent keyEvent = new java.awt.event.KeyEvent(
                    lwFrame, swingID, swingWhen, swingModifiers,
                    swingKeyCode, swingChar);
            AccessController.doPrivileged(new PostEventAction(keyEvent));
        }
    }

    private static void invokeOnEDT(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    r.run();
                }
            });
        }
    }
}
