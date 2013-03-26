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

package javafx.stage;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;

import com.sun.javafx.WeakReferenceQueue;
import com.sun.javafx.beans.annotations.NoInit;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.stage.WindowBoundsAccessor;
import com.sun.javafx.stage.WindowEventDispatcher;
import com.sun.javafx.stage.WindowPeerListener;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;


/**
 * <p>
 *     A top level window within which a scene is hosted, and with which the user
 *     interacts. A Window might be a {@link Stage}, {@link PopupWindow}, or other
 *     such top level. A Window is used also for browser plug-in based deployments.
 * </p>
 *
 */
public class Window implements EventTarget {

    /**
     * A list of all the currently existing windows. This is only used by SQE for testing.
     */
    private static WeakReferenceQueue<Window>windowQueue = new WeakReferenceQueue<Window>();

    /**
     * Allows window peer listeners to directly change window location and size
     * without changing the xExplicit, yExplicit, widthExplicit and
     * heightExplicit values.
     */
    private static final WindowBoundsAccessor BOUNDS_ACCESSOR =
            new WindowBoundsAccessor() {
                @Override
                public void setLocation(Window window, double x, double y) {
                    window.x.set(x - window.winTranslateX);
                    window.y.set(y - window.winTranslateY);
                }

                @Override
                public void setSize(Window window,
                                    double width, double height) {
                    window.width.set(width);
                    window.height.set(height);
                }
            };

    /**
     * Return all Windows
     *
     * @return Iterator of all Windows
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @NoInit
    public static Iterator<Window> impl_getWindows() {
        final Iterator iterator = AccessController.doPrivileged(
            new PrivilegedAction<Iterator>() {
                @Override public Iterator run() {
                    return windowQueue.iterator();
                }
            }
        );
        return iterator;
    }

    private final AccessControlContext acc = AccessController.getContext();

    protected Window() {
        // necessary for WindowCloseRequestHandler
        initializeInternalEventDispatcher();
    }

    /**
     * The listener that gets called by peer. It's also responsible for
     * window size/location synchronization with the window peer, which
     * occurs on every pulse.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected WindowPeerListener peerListener;

    /**
     * The peer of this Stage. All external access should be
     * made though getPeer(). Implementors note: Please ensure that this
     * variable is defined *after* style and *before* the other variables so
     * that style has been initialized prior to this call, and so that
     * impl_peer is initialized prior to subsequent initialization.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected TKStage impl_peer;

    private TKBoundsConfigurator peerBoundsConfigurator =
            new TKBoundsConfigurator();

    /**
     * Get Stage's peer
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public TKStage impl_getPeer() {
        return impl_peer;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public String impl_getMXWindowType() {
        return getClass().getSimpleName();
    }

    /**
     * Set the width and height of this Window to match the size of the content
     * of this Window's Scene.
     */
    public void sizeToScene() {
        if (getScene() != null && impl_peer != null) {
            getScene().impl_preferredSize();
            adjustSize(false);
        }
    }

    private void adjustSize(boolean selfSizePriority) {
        if (getScene() == null) {
            return;
        }
        if (impl_peer != null) {
            double sceneWidth = getScene().getWidth();
            double cw = (sceneWidth > 0) ? sceneWidth : -1;
            double w = -1;
            if (selfSizePriority && widthExplicit) {
                w = getWidth();
            } else if (cw <= 0) {
                w = widthExplicit ? getWidth() : -1;
            } else {
                widthExplicit = false;
            }
            double sceneHeight = getScene().getHeight();
            double ch = (sceneHeight > 0) ? sceneHeight : -1;
            double h = -1;
            if (selfSizePriority && heightExplicit) {
                h = getHeight();
            } else if (ch <= 0) {
                h = heightExplicit ? getHeight() : -1;
            } else {
                heightExplicit = false;
            }

            peerBoundsConfigurator.setSize(w, h, cw, ch);
            applyBounds();
        }
    }

    private static final float CENTER_ON_SCREEN_X_FRACTION = 1.0f / 2;
    private static final float CENTER_ON_SCREEN_Y_FRACTION = 1.0f / 3;
    
    /**
     * Sets x and y properties on this Window so that it is centered on the screen.
     */
    public void centerOnScreen() {
        xExplicit = false;
        yExplicit = false;
        if (impl_peer != null) {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            double centerX = 
                    bounds.getMinX() + (bounds.getWidth() - getWidth())
                                           * CENTER_ON_SCREEN_X_FRACTION;
            double centerY =
                    bounds.getMinY() + (bounds.getHeight() - getHeight())
                                           * CENTER_ON_SCREEN_Y_FRACTION;

            x.set(centerX - winTranslateX);
            y.set(centerY - winTranslateY);
            peerBoundsConfigurator.setLocation(centerX, centerY,
                                               CENTER_ON_SCREEN_X_FRACTION,
                                               CENTER_ON_SCREEN_Y_FRACTION);
            applyBounds();
        }
    }

    private double winTranslateX;
    private double winTranslateY;

    final void setWindowTranslate(final double translateX,
                                  final double translateY) {
        if (translateX != winTranslateX) {
            winTranslateX = translateX;
            peerBoundsConfigurator.setX(getX() + translateX, 0);
        }
        if (translateY != winTranslateY) {
            winTranslateY = translateY;
            peerBoundsConfigurator.setY(getY() + translateY, 0);
        }
    }

    final double getWindowTranslateX() {
        return winTranslateX;
    }

    final double getWindowTranslateY() {
        return winTranslateY;
    }

    private boolean xExplicit = false;
    /**
     * The horizontal location of this {@code Stage} on the screen. Changing
     * this attribute will move the {@code Stage} horizontally. Changing this
     * attribute will not visually affect a {@code Stage} while
     * {@code fullScreen} is true, but will be honored by the {@code Stage} once
     * {@code fullScreen} becomes false.
     */
    private ReadOnlyDoubleWrapper x =
            new ReadOnlyDoubleWrapper(this, "x", Double.NaN);

    public final void setX(double value) {
        x.set(value);
        peerBoundsConfigurator.setX(value + winTranslateX, 0);
        xExplicit = true;
    }
    public final double getX() { return x.get(); }
    public final ReadOnlyDoubleProperty xProperty() { return x.getReadOnlyProperty(); }

    private boolean yExplicit = false;
    /**
     * The vertical location of this {@code Stage} on the screen. Changing this
     * attribute will move the {@code Stage} vertically. Changing this
     * attribute will not visually affect a {@code Stage} while
     * {@code fullScreen} is true, but will be honored by the {@code Stage} once
     * {@code fullScreen} becomes false.
     */
    private ReadOnlyDoubleWrapper y =
            new ReadOnlyDoubleWrapper(this, "y", Double.NaN);

    public final void setY(double value) {
        y.set(value);
        peerBoundsConfigurator.setY(value + winTranslateY, 0);
        yExplicit = true;
    }
    public final double getY() { return y.get(); }
    public final ReadOnlyDoubleProperty yProperty() { return y.getReadOnlyProperty(); }

    private boolean widthExplicit = false;
    /**
     * The width of this {@code Stage}. Changing this attribute will narrow or
     * widen the width of the {@code Stage}. Changing this
     * attribute will not visually affect a {@code Stage} while
     * {@code fullScreen} is true, but will be honored by the {@code Stage} once
     * {@code fullScreen} becomes false. This value includes any and all
     * decorations which may be added by the Operating System such as resizable
     * frame handles. Typical applications will set the {@link javafx.scene.Scene} width
     * instead.
     * <p>
     * The property is read only because it can be changed externally
     * by the underlying platform and therefore must not be bindable.
     * </p>
     */
    private ReadOnlyDoubleWrapper width =
            new ReadOnlyDoubleWrapper(this, "width", Double.NaN);

    public final void setWidth(double value) {
        width.set(value);
        peerBoundsConfigurator.setWindowWidth(value);
        widthExplicit = true;
    }
    public final double getWidth() { return width.get(); }
    public final ReadOnlyDoubleProperty widthProperty() { return width.getReadOnlyProperty(); }

    private boolean heightExplicit = false;
    /**
     * The height of this {@code Stage}. Changing this attribute will shrink
     * or heighten the height of the {@code Stage}. Changing this
     * attribute will not visually affect a {@code Stage} while
     * {@code fullScreen} is true, but will be honored by the {@code Stage} once
     * {@code fullScreen} becomes false. This value includes any and all
     * decorations which may be added by the Operating System such as the title
     * bar. Typical applications will set the {@link javafx.scene.Scene} height instead.
     * <p>
     * The property is read only because it can be changed externally
     * by the underlying platform and therefore must not be bindable.
     * </p>
     */
    private ReadOnlyDoubleWrapper height =
            new ReadOnlyDoubleWrapper(this, "height", Double.NaN);

    public final void setHeight(double value) {
        height.set(value);
        peerBoundsConfigurator.setWindowHeight(value);
        heightExplicit = true;
    }
    public final double getHeight() { return height.get(); }
    public final ReadOnlyDoubleProperty heightProperty() { return height.getReadOnlyProperty(); }

    /**
     * Whether or not this {@code Window} has the keyboard or input focus.
     * <p>
     * The property is read only because it can be changed externally
     * by the underlying platform and therefore must not be bindable.
     * </p>
     *
     * @profile common
     */
    private ReadOnlyBooleanWrapper focused = new ReadOnlyBooleanWrapper() {
        @Override protected void invalidated() {
            focusChanged(get());
        }

        @Override
        public Object getBean() {
            return Window.this;
        }

        @Override
        public String getName() {
            return "focused";
        }
    };

    /**
     * @treatAsPrivate
     * @deprecated
     */
    @Deprecated
    public final void setFocused(boolean value) { focused.set(value); }
    
    /**
     * Requests that this {@code Window} get the input focus.
     */
    public final void requestFocus() {
        if (impl_peer != null) {
            impl_peer.requestFocus();
        }
    }
    public final boolean isFocused() { return focused.get(); }
    public final ReadOnlyBooleanProperty focusedProperty() { return focused.getReadOnlyProperty(); }

    /**
     * The {@code Scene} to be rendered on this {@code Stage}. There can only
     * be one {@code Scene} on the {@code Stage} at a time, and a {@code Scene}
     * can only be on one {@code Stage} at a time. Setting a {@code Scene} on
     * a different {@code Stage} will cause the old {@code Stage} to lose the
     * reference before the new one gains it. You may swap {@code Scene}s on
     * a {@code Stage} at any time, even while in full-screen exclusive mode.
     *
     * An {@link IllegalStateException} is thrown if this property is set
     * on a thread other than the JavaFX Application Thread.
     *
     * @defaultValue null
     */
    private SceneModel scene = new SceneModel();
    protected void setScene(Scene value) { scene.set(value); }
    public final Scene getScene() { return scene.get(); }
    public final ReadOnlyObjectProperty<Scene> sceneProperty() { return scene.getReadOnlyProperty(); }

    private final class SceneModel extends ReadOnlyObjectWrapper<Scene> {
        private Scene oldScene;

        @Override protected void invalidated() {
            final Scene newScene = get();
            if (oldScene != newScene) {
                Toolkit.getToolkit().checkFxUserThread();
                // Clear the "window" on the old scene, if there was one. This
                // will also trigger scene's peer disposal.
                if (oldScene != null) {
                    oldScene.impl_setWindow(null);
                }
                if (newScene != null) {
                    final Window oldWindow = newScene.getWindow();
                    if (oldWindow != null) {
                        // if the new scene was previously set to a window
                        // we need to remove it from that window without
                        // generating unnecessary changes in the new scene's
                        // window property
                        oldWindow.scene.notifySceneLost();
                    }

                    // Set the "window" on the new scene. This will also trigger
                    // scene's peer creation.
                    newScene.impl_setWindow(Window.this);
                    // Set scene impl on stage impl
                    updatePeerStage(newScene.impl_getPeer());

                    // Fix for RT-15432: we should update new Scene's stylesheets, if the
                    // window is already showing. For not yet shown windows, the update is
                    // performed in Window.visibleChanging()
                    if (isShowing()) {
                        newScene.getRoot().impl_reapplyCSS();
                        getScene().impl_preferredSize();

                        if (!widthExplicit || !heightExplicit) {
                            adjustSize(true);
                        }
                    }
                } else {
                    updatePeerStage(null);
                }

                oldScene = newScene;
            }
        }

        @Override
        public Object getBean() {
            return Window.this;
        }

        @Override
        public String getName() {
            return "scene";
        }

        public void notifySceneLost() {
            // we are going to change the scene to null, if the sceen is
            // bound we have to unbind first
            if (isBound()) {
                unbind();
            }

            // don't call oldScene.impl_setWindow(null)
            oldScene = null;
            set(null);
            updatePeerStage(null);
        }

        private void updatePeerStage(final TKScene tkScene) {
            if (impl_peer != null) {
                // Set scene impl on stage impl
                impl_peer.setScene(tkScene);
            }
        }
    }

    /**
     * Defines the opacity of the {@code Stage} as a value between 0.0 and 1.0.
     * The opacity is reflected across the {@code Stage}, its {@code Decoration}
     * and its {@code Scene} content. On a JavaFX runtime platform that does not
     * support opacity, assigning a value to this variable will have no
     * visible difference. A {@code Stage} with 0% opacity is fully translucent.
     * Typically, a {@code Stage} with 0% opacity will not receive any mouse
     * events.
     *
     * @defaultValue 1.0
     */
    private DoubleProperty opacity;

    public final void setOpacity(double value) {
        opacityProperty().set(value);
    }

    public final double getOpacity() {
        return opacity == null ? 1.0 : opacity.get();
    }

    public final DoubleProperty opacityProperty() {
        if (opacity == null) {
            opacity = new DoublePropertyBase(1.0) {

                @Override
                protected void invalidated() {
                    if (impl_peer != null) {
                        impl_peer.setOpacity((float) get());
                    }
                }

                @Override
                public Object getBean() {
                    return Window.this;
                }

                @Override
                public String getName() {
                    return "opacity";
                }
            };
        }
        return opacity;
    }

    /**
     * Called when there is an external request to close this {@code Window}.
     * The installed event handler can prevent window closing by consuming the
     * received event.
     */
    private ObjectProperty<EventHandler<WindowEvent>> onCloseRequest;
    public final void setOnCloseRequest(EventHandler<WindowEvent> value) {
        onCloseRequestProperty().set(value);
    }
    public final EventHandler<WindowEvent> getOnCloseRequest() {
        return (onCloseRequest != null) ? onCloseRequest.get() : null;
    }
    public final ObjectProperty<EventHandler<WindowEvent>>
            onCloseRequestProperty() {
        if (onCloseRequest == null) {
            onCloseRequest = new ObjectPropertyBase<EventHandler<WindowEvent>>() {
                @Override protected void invalidated() {
                    setEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, get());
                }

                @Override
                public Object getBean() {
                    return Window.this;
                }

                @Override
                public String getName() {
                    return "onCloseRequest";
                }
            };
        }
        return onCloseRequest;
    }

    /**
     * Called just prior to the Window being shown.
     */
    private ObjectProperty<EventHandler<WindowEvent>> onShowing;
    public final void setOnShowing(EventHandler<WindowEvent> value) { onShowingProperty().set(value); }
    public final EventHandler<WindowEvent> getOnShowing() {
        return onShowing == null ? null : onShowing.get();
    }
    public final ObjectProperty<EventHandler<WindowEvent>> onShowingProperty() {
        if (onShowing == null) {
            onShowing = new ObjectPropertyBase<EventHandler<WindowEvent>>() {
                @Override protected void invalidated() {
                    setEventHandler(WindowEvent.WINDOW_SHOWING, get());
                }

                @Override
                public Object getBean() {
                    return Window.this;
                }

                @Override
                public String getName() {
                    return "onShowing";
                }
            };
        }
        return onShowing;
    }

    /**
     * Called just after the Window is shown.
     */
    private ObjectProperty<EventHandler<WindowEvent>> onShown;
    public final void setOnShown(EventHandler<WindowEvent> value) { onShownProperty().set(value); }
    public final EventHandler<WindowEvent> getOnShown() {
        return onShown == null ? null : onShown.get();
    }
    public final ObjectProperty<EventHandler<WindowEvent>> onShownProperty() {
        if (onShown == null) {
            onShown = new ObjectPropertyBase<EventHandler<WindowEvent>>() {
                @Override protected void invalidated() {
                    setEventHandler(WindowEvent.WINDOW_SHOWN, get());
                }

                @Override
                public Object getBean() {
                    return Window.this;
                }

                @Override
                public String getName() {
                    return "onShown";
                }
            };
        }
        return onShown;
    }

    /**
     * Called just prior to the Window being hidden.
     */
    private ObjectProperty<EventHandler<WindowEvent>> onHiding;
    public final void setOnHiding(EventHandler<WindowEvent> value) { onHidingProperty().set(value); }
    public final EventHandler<WindowEvent> getOnHiding() {
        return onHiding == null ? null : onHiding.get();
    }
    public final ObjectProperty<EventHandler<WindowEvent>> onHidingProperty() {
        if (onHiding == null) {
            onHiding = new ObjectPropertyBase<EventHandler<WindowEvent>>() {
                @Override protected void invalidated() {
                    setEventHandler(WindowEvent.WINDOW_HIDING, get());
                }

                @Override
                public Object getBean() {
                    return Window.this;
                }

                @Override
                public String getName() {
                    return "onHiding";
                }
            };
        }
        return onHiding;
    }

    /**
     * Called just after the Window has been hidden.
     * When the {@code Window} is hidden, this event handler is invoked allowing
     * the developer to clean up resources or perform other tasks when the
     * {@link Window} is closed.
     */
    private ObjectProperty<EventHandler<WindowEvent>> onHidden;
    public final void setOnHidden(EventHandler<WindowEvent> value) { onHiddenProperty().set(value); }
    public final EventHandler<WindowEvent> getOnHidden() {
        return onHidden == null ? null : onHidden.get();
    }
    public final ObjectProperty<EventHandler<WindowEvent>> onHiddenProperty() {
        if (onHidden == null) {
            onHidden = new ObjectPropertyBase<EventHandler<WindowEvent>>() {
                @Override protected void invalidated() {
                    setEventHandler(WindowEvent.WINDOW_HIDDEN, get());
                }

                @Override
                public Object getBean() {
                    return Window.this;
                }

                @Override
                public String getName() {
                    return "onHidden";
                }
            };
        }
        return onHidden;
    }

    /**
     * Whether or not this {@code Stage} is showing (that is, open on the
     * user's system). The Stage might be "showing", yet the user might not
     * be able to see it due to the Stage being rendered behind another window
     * or due to the Stage being positioned off the monitor.
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper showing = new ReadOnlyBooleanWrapper() {
        private boolean oldVisible;

        @Override protected void invalidated() {
            final boolean newVisible = get();
            if (oldVisible == newVisible) {
                return;
            }

            if (!oldVisible && newVisible) {
                fireEvent(new WindowEvent(Window.this, WindowEvent.WINDOW_SHOWING));
            } else {
                fireEvent(new WindowEvent(Window.this, WindowEvent.WINDOW_HIDING));
            }

            oldVisible = newVisible;
            impl_visibleChanging(newVisible);
            if (newVisible) {
                hasBeenVisible = true;
                windowQueue.add(Window.this);
            } else {
                windowQueue.remove(Window.this);
            }
            Toolkit tk = Toolkit.getToolkit();
            if (impl_peer != null) {
                if (newVisible) {
                    impl_peer.setSecurityContext(acc);

                    if (peerListener == null) {
                        peerListener = new WindowPeerListener(Window.this);
                    }

                    peerListener.setBoundsAccessor(BOUNDS_ACCESSOR);

                    // Setup listener for changes coming back from peer
                    impl_peer.setTKStageListener(peerListener);
                    // Register pulse listener
                    tk.addStageTkPulseListener(peerBoundsConfigurator);

                    if (getScene() != null) {
                        getScene().impl_initPeer();
                        impl_peer.setScene(getScene().impl_getPeer());
                        getScene().impl_preferredSize();
                    }

                    // Set peer bounds
                    if ((getScene() != null) && (!widthExplicit || !heightExplicit)) {
                        adjustSize(true);
                    } else {
                        peerBoundsConfigurator.setSize(
                                getWidth(), getHeight(), -1, -1);
                    }
                    
                    if (!xExplicit && !yExplicit) {
                        centerOnScreen();
                    } else {
                        peerBoundsConfigurator.setLocation(
                                getX() + winTranslateX,
                                getY() + winTranslateY,
                                0, 0);
                    }

                    // set peer bounds before the window is shown
                    applyBounds();

                    impl_peer.setOpacity((float)getOpacity());

                    impl_peer.setVisible(true);
                    fireEvent(new WindowEvent(Window.this, WindowEvent.WINDOW_SHOWN));
                } else {
                    impl_peer.setVisible(false);

                    // Call listener
                    fireEvent(new WindowEvent(Window.this, WindowEvent.WINDOW_HIDDEN));

                    if (getScene() != null) {
                        impl_peer.setScene(null);
                        getScene().impl_disposePeer();
                    }

                    // Remove toolkit pulse listener
                    tk.removeStageTkPulseListener(peerBoundsConfigurator);
                    // Remove listener for changes coming back from peer
                    impl_peer.setTKStageListener(null);

                    // Notify peer
                    impl_peer.close();
                }
            }
            if (newVisible) {
                tk.requestNextPulse();
            }
            impl_visibleChanged(newVisible);
        }

        @Override
        public Object getBean() {
            return Window.this;
        }

        @Override
        public String getName() {
            return "showing";
        }
    };
    private void setShowing(boolean value) {
        Toolkit.getToolkit().checkFxUserThread();
        showing.set(value);
    }
    public final boolean isShowing() { return showing.get(); }
    public final ReadOnlyBooleanProperty showingProperty() { return showing.getReadOnlyProperty(); }

    // flag indicating whether this window has ever been made visible.
    boolean hasBeenVisible = false;

    /**
     * Attempts to show this Window by setting visibility to true
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    protected void show() {
        setShowing(true);
    }

    /**
     * Attempts to hide this Window by setting the visibility to false.
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public void hide() {
        setShowing(false);
    }

    /**
     * This can be replaced by listening for the onShowing/onHiding events
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected void impl_visibleChanging(boolean visible) {
        if (visible && (getScene() != null)) {
            getScene().getRoot().impl_reapplyCSS();
        }
    }

    /**
     * This can be replaced by listening for the onShown/onHidden events
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected void impl_visibleChanged(boolean visible) {
    }

    // PENDING_DOC_REVIEW
    /**
     * Specifies the event dispatcher for this node. The default event
     * dispatcher sends the received events to the registered event handlers and
     * filters. When replacing the value with a new {@code EventDispatcher},
     * the new dispatcher should forward events to the replaced dispatcher
     * to maintain the node's default event handling behavior.
     */
    private ObjectProperty<EventDispatcher> eventDispatcher;

    public final void setEventDispatcher(EventDispatcher value) {
        eventDispatcherProperty().set(value);
    }

    public final EventDispatcher getEventDispatcher() {
        return eventDispatcherProperty().get();
    }

    public final ObjectProperty<EventDispatcher> eventDispatcherProperty() {
        initializeInternalEventDispatcher();
        return eventDispatcher;
    }

    private WindowEventDispatcher internalEventDispatcher;

    // PENDING_DOC_REVIEW
    /**
     * Registers an event handler to this node. The handler is called when the
     * node receives an {@code Event} of the specified type during the bubbling
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
     * Unregisters a previously registered event handler from this node. One
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
     * Registers an event filter to this node. The filter is called when the
     * node receives an {@code Event} of the specified type during the capturing
     * phase of event delivery.
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
     * Unregisters a previously registered event filter from this node. One
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
     * Sets the handler to use for this event type. There can only be one such handler
     * specified at a time. This handler is guaranteed to be called first. This is
     * used for registering the user-defined onFoo event handlers.
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

    WindowEventDispatcher getInternalEventDispatcher() {
        initializeInternalEventDispatcher();
        return internalEventDispatcher;
    }

    private void initializeInternalEventDispatcher() {
        if (internalEventDispatcher == null) {
            internalEventDispatcher = createInternalEventDispatcher();
            eventDispatcher = new SimpleObjectProperty<EventDispatcher>(
                                          this,
                                          "eventDispatcher",
                                          internalEventDispatcher);
        }
    }

    WindowEventDispatcher createInternalEventDispatcher() {
        return new WindowEventDispatcher(this);
    }

    /**
     * Fires the specified event.
     * <p>
     * This method must be called on the FX user thread.
     *
     * @param event the event to fire
     */
    public final void fireEvent(Event event) {
        Event.fireEvent(this, event);
    }

    // PENDING_DOC_REVIEW
    /**
     * Construct an event dispatch chain for this window.
     *
     * @param tail the initial chain to build from
     * @return the resulting event dispatch chain for this window
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

        return tail;
    }

    private int focusGrabCounter;

    void increaseFocusGrabCounter() {
        if ((++focusGrabCounter == 1) && (impl_peer != null) && isFocused()) {
            impl_peer.grabFocus();
        }
    }

    void decreaseFocusGrabCounter() {
        if ((--focusGrabCounter == 0) && (impl_peer != null)) {
            impl_peer.ungrabFocus();
        }
    }

    private void focusChanged(final boolean newIsFocused) {
        if ((focusGrabCounter > 0) && (impl_peer != null) && newIsFocused) {
            impl_peer.grabFocus();
        }
    }

    final void applyBounds() {
        peerBoundsConfigurator.apply();
    }
    
    /**
     * Caches all requested bounds settings and applies them at once during
     * the next pulse.
     */
    private final class TKBoundsConfigurator implements TKPulseListener {
        private double x;
        private double y;
        private float xGravity;
        private float yGravity;
        private double windowWidth;
        private double windowHeight;
        private double clientWidth;
        private double clientHeight;

        private boolean dirty;

        public TKBoundsConfigurator() {
            reset();
        }

        public void setX(final double x, final float xGravity) {
            this.x = x;
            this.xGravity = xGravity;
            setDirty();
        }

        public void setY(final double y, final float yGravity) {
            this.y = y;
            this.yGravity = yGravity;
            setDirty();
        }

        public void setWindowWidth(final double windowWidth) {
            this.windowWidth = windowWidth;
            setDirty();
        }

        public void setWindowHeight(final double windowHeight) {
            this.windowHeight = windowHeight;
            setDirty();
        }

        public void setClientWidth(final double clientWidth) {
            this.clientWidth = clientWidth;
            setDirty();
        }

        public void setClientHeight(final double clientHeight) {
            this.clientHeight = clientHeight;
            setDirty();
        }

        public void setLocation(final double x,
                                final double y,
                                final float xGravity,
                                final float yGravity) {
            this.x = x;
            this.y = y;
            this.xGravity = xGravity;
            this.yGravity = yGravity;
            setDirty();
        }

        public void setSize(final double windowWidth,
                            final double windowHeight,
                            final double clientWidth,
                            final double clientHeight) {
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
            this.clientWidth = clientWidth;
            this.clientHeight = clientHeight;
            setDirty();
        }

        public void apply() {
            if (dirty) {
                impl_peer.setBounds((float) (Double.isNaN(x) ? 0 : x),
                                    (float) (Double.isNaN(y) ? 0 : y),
                                    !Double.isNaN(x),
                                    !Double.isNaN(y),
                                    (float) windowWidth,
                                    (float) windowHeight,
                                    (float) clientWidth,
                                    (float) clientHeight,
                                    xGravity, yGravity);

                reset();
            }
        }

        @Override
        public void pulse() {
            apply();
        }

        private void reset() {
            x = Double.NaN;
            y = Double.NaN;
            xGravity = 0;
            yGravity = 0;
            windowWidth = -1;
            windowHeight = -1;
            clientWidth = -1;
            clientHeight = -1;
            dirty = false;
        }

        private void setDirty() {
            if (!dirty) {
                Toolkit.getToolkit().requestNextPulse();
                dirty = true;
            }
        }
    }
}
