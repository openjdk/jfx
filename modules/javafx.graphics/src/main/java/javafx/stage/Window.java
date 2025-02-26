/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;

import javafx.application.Platform;
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
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;

import com.sun.javafx.util.Utils;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.stage.EmbeddedWindow;
import com.sun.javafx.stage.WindowEventDispatcher;
import com.sun.javafx.stage.WindowHelper;
import com.sun.javafx.stage.WindowPeerListener;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SceneHelper;


/**
 * A top level window within which a scene is hosted, and with which the user
 * interacts. A Window might be a {@link Stage}, {@link PopupWindow}, or other
 * such top level window.
 * <p>
 * Window objects must be constructed and modified on the
 * JavaFX Application Thread.
 * </p>
 * <p>
 * The JavaFX Application Thread is created as part of the startup process for
 * the JavaFX runtime. See the {@link javafx.application.Application} class and
 * the {@link Platform#startup(Runnable)} method for more information.
 * </p>
 * <p>
 * Some {@code Window} properties are read-only, even though they have
 * corresponding set methods, because they can be changed externally by the
 * underlying platform, and therefore must not be bindable.
 * Further, these properties might be ignored on some platforms, depending on
 * whether or not there is a window manager and how it is configured.
 * For example, a tiling window manager might ignore the {@code x} and {@code y}
 * properties, or treat them as hints, placing the window in a location of its
 * choosing.
 * </p>
 *
 * @since JavaFX 2.0
 */
public class Window implements EventTarget {

    /**
     * A list of all the currently _showing_ windows. This is publicly accessible via the unmodifiableWindows wrapper.
     */
    private static ObservableList<Window> windows = FXCollections.observableArrayList();
    private static ObservableList<Window> unmodifiableWindows = FXCollections.unmodifiableObservableList(windows);

    /*
     * Store the singleton instance of the WindowHelper subclass corresponding
     * to the subclass of this instance of Window
     */
    private WindowHelper windowHelper = null;

    static {
        WindowHelper.setWindowAccessor(
                new WindowHelper.WindowAccessor() {
                    @Override
                    public WindowHelper getHelper(Window window) {
                        return window.windowHelper;
                    }

                    @Override
                    public void setHelper(Window window, WindowHelper windowHelper) {
                        window.windowHelper = windowHelper;
                    }

                    @Override
                    public void doVisibleChanging(Window window, boolean visible) {
                        window.doVisibleChanging(visible);
                    }

                    @Override
                    public void doVisibleChanged(Window window, boolean visible) {
                        window.doVisibleChanged(visible);
                    }

                    @Override
                    public TKStage getPeer(Window window) {
                        return window.getPeer();
                    }

                    @Override
                    public void setPeer(Window window, TKStage peer) {
                        window.setPeer(peer);
                    }

                    @Override
                    public WindowPeerListener getPeerListener(Window window) {
                        return window.getPeerListener();
                    }

                    @Override
                    public void  setPeerListener(Window window, WindowPeerListener peerListener) {
                        window.setPeerListener(peerListener);
                    }

                    @Override
                    public void setFocused(Window window, boolean value) {
                        window.setFocused(value);
                    }

                    /*
                     * Allow window peer listeners to directly change reported
                     * window location and size without changing the xExplicit,
                     * yExplicit, widthExplicit and heightExplicit values.
                     */
                    @Override
                    public void notifyLocationChanged(
                            Window window, double x, double y) {
                        window.notifyLocationChanged(x, y);
                    }

                    @Override
                    public void notifySizeChanged(Window window,
                                                  double width,
                                                  double height) {
                        window.notifySizeChanged(width, height);
                    }

                    @Override
                    public void notifyScaleChanged(Window window,
                                                   double newOutputScaleX,
                                                   double newOutputScaleY) {
                        window.updateOutputScales(newOutputScaleX, newOutputScaleY);
                    }

                    @Override
                    public void notifyScreenChanged(Window window,
                                                  Object from,
                                                  Object to) {
                        window.notifyScreenChanged(from, to);
                    }

                    @Override
                    public float getPlatformScaleX(Window window) {
                        TKStage peer = window.getPeer();
                        return peer == null ? 1.0f : peer.getPlatformScaleX();
                    }

                    @Override
                    public float getPlatformScaleY(Window window) {
                        TKStage peer = window.getPeer();
                        return peer == null ? 1.0f : peer.getPlatformScaleY();
                    }

                    @Override
                    public ReadOnlyObjectProperty<Screen> screenProperty(Window window) {
                        return window.screenProperty();
                    }
                });
    }

    /**
     * Returns a list containing a reference to the currently showing JavaFX windows. The list is unmodifiable -
     * attempting to modify this list will result in an {@link UnsupportedOperationException} being thrown at runtime.
     *
     * @return A list containing all windows that are currently showing.
     * @since 9
     */
    public static ObservableList<Window> getWindows() {
        return unmodifiableWindows;
    }

    /**
     * Constructor for subclasses to call.
     */
    protected Window() {
        // necessary for WindowCloseRequestHandler
        initializeInternalEventDispatcher();
        WindowHelper.initHelper(this);
    }

    /*
     * The listener that gets called by peer. It's also responsible for
     * window size/location synchronization with the window peer, which
     * occurs on every pulse.
     */
    private WindowPeerListener peerListener;

    WindowPeerListener getPeerListener() {
        return peerListener;
    }

    void setPeerListener(WindowPeerListener peerListener) {
        this.peerListener = peerListener;
    }

    /*
     * The peer of this Stage. All external access should be
     * made though getPeer(). Implementors note: Please ensure that this
     * variable is defined *after* style and *before* the other variables so
     * that style has been initialized prior to this call, and so that
     * peer is initialized prior to subsequent initialization.
     */
    private TKStage peer;

    private TKBoundsConfigurator peerBoundsConfigurator =
            new TKBoundsConfigurator();

    /*
     * Get Stage's peer
     */
    TKStage getPeer() {
        return peer;
    }

    void setPeer(TKStage peer) {
        this.peer = peer;
    }

    /**
     * Indicates if a user requested the window to be sized to match the scene
     * size.
     */
    private boolean sizeToScene = false;
    /**
     * Set the width and height of this Window to match the size of the content
     * of this Window's Scene.
     * <p>This request might be ignored if the Window is not allowed to do so, for example a {@link Stage}
     * may be {@code maximized} or in {@code fullScreen} and therefore does not allow this request.
     * If that is the case, this request is remembered and reapplied later when allowed.</p>
     */
    public void sizeToScene() {
        if (isSizeToSceneAllowed() && getScene() != null && peer != null) {
            SceneHelper.preferredSize(getScene());
            adjustSize(false);
        } else {
            // Remember the request to reapply it later if needed
            sizeToScene = true;
        }
    }

    /**
     * Determines whether the {@link #sizeToScene()} request is allowed or not.
     *
     * @return true if allowed, false otherwise
     */
    boolean isSizeToSceneAllowed() {
        return true;
    }

    private void adjustSize(boolean selfSizePriority) {
        if (getScene() == null) {
            return;
        }
        if (peer != null) {
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
     * Sets x and y properties on this Window so that it is centered on the
     * current screen.
     * The current screen is determined from the intersection of current window bounds and
     * visual bounds of all screens.
     */
    public void centerOnScreen() {
        xExplicit = false;
        yExplicit = false;
        if (peer != null) {
            Rectangle2D bounds = getWindowScreen().getVisualBounds();
            double centerX =
                    bounds.getMinX() + (bounds.getWidth() - getWidth())
                                           * CENTER_ON_SCREEN_X_FRACTION;
            double centerY =
                    bounds.getMinY() + (bounds.getHeight() - getHeight())
                                           * CENTER_ON_SCREEN_Y_FRACTION;

            x.set(centerX);
            y.set(centerY);
            peerBoundsConfigurator.setLocation(centerX, centerY,
                                               CENTER_ON_SCREEN_X_FRACTION,
                                               CENTER_ON_SCREEN_Y_FRACTION);
            applyBounds();
        }
    }

    private void updateOutputScales(double sx, double sy) {
        // We call updateRenderScales() before updating the property
        // values so that an application can listen to the properties
        // and set their own values overriding the default values we set.
        updateRenderScales(sx, sy);
        // Now set the properties and trigger any potential listeners.
        outputScaleX.set(sx);
        outputScaleY.set(sy);
    }

    void updateRenderScales(double sx, double sy) {
        boolean forceInt = forceIntegerRenderScale.get();
        if (!renderScaleX.isBound()) {
            renderScaleX.set(forceInt ? Math.ceil(sx) : sx);
        }
        if (!renderScaleY.isBound()) {
            renderScaleY.set(forceInt ? Math.ceil(sy) : sy);
        }
    }

    /**
     * The scale that the {@code Window} will apply to horizontal scene
     * coordinates in all stages of rendering and compositing the output
     * to the screen or other destination device.
     * This property is updated asynchronously by the system at various
     * times including:
     * <ul>
     * <li>Window creation
     * <li>At some point during moving a window to a new {@code Screen}
     * which may be before or after the {@link Screen} property is updated.
     * <li>In response to a change in user preferences for output scaling.
     * </ul>
     *
     * @see #renderScaleXProperty()
     * @since 9
     */
    private ReadOnlyDoubleWrapper outputScaleX =
        new ReadOnlyDoubleWrapper(this, "outputScaleX", 1.0);
    public final double getOutputScaleX() {
        return outputScaleX.get();
    }
    public final ReadOnlyDoubleProperty outputScaleXProperty() {
        return outputScaleX.getReadOnlyProperty();
    }

    /**
     * The scale that the {@code Window} will apply to vertical scene
     * coordinates in all stages of rendering and compositing the output
     * to the screen or other destination device.
     * This property is updated asynchronously by the system at various
     * times including:
     * <ul>
     * <li>Window creation
     * <li>At some point during moving a window to a new {@code Screen}
     * which may be before or after the {@link Screen} property is updated.
     * <li>In response to a change in user preferences for output scaling.
     * </ul>
     *
     * @see #renderScaleYProperty()
     * @since 9
     */
    private ReadOnlyDoubleWrapper outputScaleY =
        new ReadOnlyDoubleWrapper(this, "outputScaleY", 1.0);
    public final double getOutputScaleY() {
        return outputScaleY.get();
    }
    public final ReadOnlyDoubleProperty outputScaleYProperty() {
        return outputScaleY.getReadOnlyProperty();
    }

    /**
     * Boolean property that controls whether only integer render scales
     * are set by default by the system when there is a change in the
     * associated output scale.
     * The {@code renderScale} properties will be updated directly and
     * simultaneously with any changes in the associated {@code outputScale}
     * properties, but the values can be overridden by subsequent calls to
     * the {@code setRenderScale} setters or through appropriate use of
     * binding.
     * This property will not prevent setting non-integer scales
     * directly using the {@code renderScale} property object or the
     * convenience setter method.
     *
     * @defaultValue false
     * @see #renderScaleXProperty()
     * @see #renderScaleYProperty()
     * @since 9
     */
    private BooleanProperty forceIntegerRenderScale =
        new SimpleBooleanProperty(this, "forceIntegerRenderScale", false) {
            @Override
            protected void invalidated() {
                updateRenderScales(getOutputScaleX(),
                                   getOutputScaleY());
            }
        };
    public final void setForceIntegerRenderScale(boolean forced) {
        forceIntegerRenderScale.set(forced);
    }
    public final boolean isForceIntegerRenderScale() {
        return forceIntegerRenderScale.get();
    }
    public final BooleanProperty forceIntegerRenderScaleProperty() {
        return forceIntegerRenderScale;
    }

    /**
     * The horizontal scale that the {@code Window} will use when rendering
     * its {@code Scene} to the rendering buffer.
     * This property is automatically updated whenever there is a change in
     * the {@link #outputScaleXProperty() outputScaleX} property and can be overridden either by
     * calling {@code setRenderScaleX()} in response to a listener on the
     * {@code outputScaleX} property or by binding it appropriately.
     *
     * @defaultValue outputScaleX
     * @see #outputScaleXProperty()
     * @see #forceIntegerRenderScaleProperty()
     * @since 9
     */
    private DoubleProperty renderScaleX =
        new SimpleDoubleProperty(this, "renderScaleX", 1.0) {
            @Override
            protected void invalidated() {
                peerBoundsConfigurator.setRenderScaleX(get());
            }
        };
    public final void setRenderScaleX(double scale) {
        renderScaleX.set(scale);
    }
    public final double getRenderScaleX() {
        return renderScaleX.get();
    }
    public final DoubleProperty renderScaleXProperty() {
        return renderScaleX;
    }

    /**
     * The vertical scale that the {@code Window} will use when rendering
     * its {@code Scene} to the rendering buffer.
     * This property is automatically updated whenever there is a change in
     * the {@link #outputScaleYProperty() outputScaleY} property and can be overridden either by
     * calling {@code setRenderScaleY()} in response to a listener on the
     * {@code outputScaleY} property or by binding it appropriately.
     *
     * @defaultValue outputScaleY
     * @see #outputScaleYProperty()
     * @see #forceIntegerRenderScaleProperty()
     * @since 9
     */
    private DoubleProperty renderScaleY =
        new SimpleDoubleProperty(this, "renderScaleY", 1.0) {
            @Override
            protected void invalidated() {
                peerBoundsConfigurator.setRenderScaleY(get());
            }
        };
    public final void setRenderScaleY(double scale) {
        renderScaleY.set(scale);
    }
    public final double getRenderScaleY() {
        return renderScaleY.get();
    }
    public final DoubleProperty renderScaleYProperty() {
        return renderScaleY;
    }

    private boolean xExplicit = false;

    /**
     * The horizontal location of this {@code Window} on the screen. Changing
     * this attribute will move the {@code Window} horizontally. If this
     * {@code Window} is an instance of {@code Stage}, changing this attribute
     * will not visually affect the {@code Window} while
     * {@link Stage#fullScreenProperty() fullScreen} is true, but will be honored
     * by the {@code Window} once {@link Stage#fullScreenProperty() fullScreen}
     * becomes false.
     * <p>
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     */
    private ReadOnlyDoubleWrapper x =
            new ReadOnlyDoubleWrapper(this, "x", Double.NaN);

    public final void setX(double value) {
        setXInternal(value);
    }
    public final double getX() { return x.get(); }
    public final ReadOnlyDoubleProperty xProperty() { return x.getReadOnlyProperty(); }

    void setXInternal(double value) {
        x.set(value);
        peerBoundsConfigurator.setX(value, 0);
        xExplicit = true;
    }

    private boolean yExplicit = false;

    /**
     * The vertical location of this {@code Window} on the screen. Changing this
     * attribute will move the {@code Window} vertically. If this
     * {@code Window} is an instance of {@code Stage}, changing this attribute
     * will not visually affect the {@code Window} while
     * {@link Stage#fullScreenProperty() fullScreen} is true, but will be honored
     * by the {@code Window} once {@link Stage#fullScreenProperty() fullScreen}
     * becomes false.
     * <p>
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     */
    private ReadOnlyDoubleWrapper y =
            new ReadOnlyDoubleWrapper(this, "y", Double.NaN);

    public final void setY(double value) {
        setYInternal(value);
    }
    public final double getY() { return y.get(); }
    public final ReadOnlyDoubleProperty yProperty() { return y.getReadOnlyProperty(); }

    void setYInternal(double value) {
        y.set(value);
        peerBoundsConfigurator.setY(value, 0);
        yExplicit = true;
    }

    /**
     * Notification from the windowing system that the window's position has
     * changed.
     *
     * @param newX the new window x position
     * @param newY the new window y position
     */
    void notifyLocationChanged(double newX, double newY) {
        x.set(newX);
        y.set(newY);
    }

    private boolean widthExplicit = false;

    /**
     * The width of this {@code Window}. Changing this attribute will narrow or
     * widen the width of the {@code Window}. This value includes any and all
     * decorations which may be added by the Operating System such as resizable
     * frame handles. Typical applications will set the {@link javafx.scene.Scene}
     * width instead. This {@code Window} will take its width from the scene if
     * it has never been set by the application. Resizing the window by end user
     * does not count as a setting the width by the application. If this
     * {@code Window} is an instance of {@code Stage}, changing this attribute
     * will not visually affect the {@code Window} while
     * {@link Stage#fullScreenProperty() fullScreen} is true, but will be honored
     * by the {@code Window} once {@link Stage#fullScreenProperty() fullScreen}
     * becomes false.
     * <p>
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     */
    private ReadOnlyDoubleWrapper width =
            new ReadOnlyDoubleWrapper(this, "width", Double.NaN);

    public final void setWidth(double value) {
        width.set(value);
        peerBoundsConfigurator.setWindowWidth(value);
        widthExplicit = true;
        sizeToScene = false;
    }
    public final double getWidth() { return width.get(); }
    public final ReadOnlyDoubleProperty widthProperty() { return width.getReadOnlyProperty(); }

    private boolean heightExplicit = false;

    /**
     * The height of this {@code Window}. Changing this attribute will shrink
     * or heighten the height of the {@code Window}. This value includes any and all
     * decorations which may be added by the Operating System such as the title
     * bar. Typical applications will set the {@link javafx.scene.Scene} height
     * instead. This window will take its height from the scene if it has never
     * been set by the application. Resizing this window by end user does not
     * count as a setting the height by the application.  If this
     * {@code Window} is an instance of {@code Stage}, changing this attribute
     * will not visually affect the {@code Window} while
     * {@link Stage#fullScreenProperty() fullScreen} is true, but will be honored
     * by the {@code Window} once {@link Stage#fullScreenProperty() fullScreen}
     * becomes false.
     * <p>
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     */
    private ReadOnlyDoubleWrapper height =
            new ReadOnlyDoubleWrapper(this, "height", Double.NaN);

    public final void setHeight(double value) {
        height.set(value);
        peerBoundsConfigurator.setWindowHeight(value);
        heightExplicit = true;
        sizeToScene = false;
    }
    public final double getHeight() { return height.get(); }
    public final ReadOnlyDoubleProperty heightProperty() { return height.getReadOnlyProperty(); }

    /**
     * Notification from the windowing system that the window's size has
     * changed.
     *
     * @param newWidth the new window width
     * @param newHeight the new window height
     */
    void notifySizeChanged(double newWidth, double newHeight) {
        width.set(newWidth);
        height.set(newHeight);
    }

    /**
     * Whether or not this {@code Window} has the keyboard or input focus.
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

    final void setFocused(boolean value) { focused.set(value); }

    /**
     * Requests that this {@code Window} get the input focus.
     */
    public final void requestFocus() {
        if (peer != null) {
            peer.requestFocus();
        }
    }
    public final boolean isFocused() { return focused.get(); }
    public final ReadOnlyBooleanProperty focusedProperty() { return focused.getReadOnlyProperty(); }

    /* ***********************************************************************
    *                                                                        *
    *                                                                        *
    *                                                                        *
    *************************************************************************/

    private static final Object USER_DATA_KEY = new Object();
    // A map containing a set of properties for this window
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
            properties = FXCollections.observableMap(new HashMap<>());
        }
        return properties;
    }

    /**
     * Tests if Window has properties.
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
     * be retrieved by calling {@link Window#getUserData()}.
     *
     * @param value The value to be stored - this can later be retrieved by calling
     *          {@link Window#getUserData()}.
     *
     * @since JavaFX 8u40
     */
    public void setUserData(Object value) {
        getProperties().put(USER_DATA_KEY, value);
    }

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the {@link Window#setUserData(java.lang.Object)} method.
     *
     * @return The Object that was previously set, or null if no property
     *          has been set or if null was set.
     *
     * @since JavaFX 8u40
     */
    public Object getUserData() {
        return getProperties().get(USER_DATA_KEY);
    }

    /**
     * The {@code Scene} to be rendered on this {@code Window}. There can only
     * be one {@code Scene} on the {@code Window} at a time, and a {@code Scene}
     * can only be on one {@code Window} at a time. Setting a {@code Scene} on
     * a different {@code Window} will cause the old {@code Window} to lose the
     * reference before the new one gains it. You may swap {@code Scene}s on
     * a {@code Window} at any time, even if it is an instance of {@code Stage}
     * and with {@link Stage#fullScreenProperty() fullScreen} set to {@code true}.
     * <p>If the width or height of this {@code Window} have never been set by the
     * application, setting the scene will cause this {@code Window} to take its
     * width or height from that scene. Resizing this {@code Window} by end user does
     * not count as setting the width or height by the application.</p>
     *
     * <p>An {@link IllegalStateException} is thrown if this property is set
     * on a thread other than the JavaFX Application Thread.</p>
     *
     * @defaultValue {@code null}
     */
    private SceneModel scene = new SceneModel();

    /**
     * Sets the value of the {@code scene} property.
     *
     * <p>The {@code Scene} to be rendered on this {@code Window}. There can only
     * be one {@code Scene} on the {@code Window} at a time, and a {@code Scene}
     * can only be on one {@code Window} at a time. Setting a {@code Scene} on
     * a different {@code Window} will cause the old {@code Window} to lose the
     * reference before the new one gains it. You may swap {@code Scene}s on
     * a {@code Window} at any time, even if it is an instance of {@code Stage}
     * and with {@link Stage#fullScreenProperty() fullScreen} set to {@code true}.</p>
     * <p>If the width or height of this {@code Window} have never been set by the
     * application, setting the scene will cause this {@code Window} to take its
     * width or height from that scene. Resizing this {@code Window} by end user does
     * not count as setting the width or height by the application.</p>
     *
     * @throws IllegalStateException if this property is set on a thread other than
     * the JavaFX Application Thread.
     *
     * @defaultValue {@code null}
     *
     * @param value the value for the {@code scene} property
     * @see #getScene()
     * @see #sceneProperty()
     */
    protected void setScene(Scene value) { scene.set(value); }
    public final Scene getScene() { return scene.get(); }
    public final ReadOnlyObjectProperty<Scene> sceneProperty() { return scene.getReadOnlyProperty(); }

    private final class SceneModel extends ReadOnlyObjectWrapper<Scene> {
        private Scene oldScene;

        @Override protected void invalidated() {
            final Scene newScene = get();
            if (oldScene == newScene) {
                return;
            }
            if (peer != null) {
                Toolkit.getToolkit().checkFxUserThread();
            }
            // First, detach scene peer from this window
            updatePeerScene(null);
            // Second, dispose scene peer
            if (oldScene != null) {
                SceneHelper.setWindow(oldScene, null);
                StyleManager.getInstance().forget(oldScene);
            }
            if (newScene != null) {
                final Window oldWindow = newScene.getWindow();
                if (oldWindow != null) {
                    // if the new scene was previously set to a window
                    // we need to remove it from that window
                    // NOTE: can this "scene" property be bound?
                    oldWindow.setScene(null);
                }

                // Set the "window" on the new scene. This will also trigger
                // scene's peer creation.
                SceneHelper.setWindow(newScene, Window.this);
                // Set scene impl on stage impl
                updatePeerScene(SceneHelper.getPeer(newScene));

                // Fix for JDK-8113774: we should update new Scene's stylesheets, if the
                // window is already showing. For not yet shown windows, the update is
                // performed in doVisibleChanging()
                if (isShowing()) {
                    NodeHelper.reapplyCSS(newScene.getRoot());

                    if (!widthExplicit || !heightExplicit) {
                        SceneHelper.preferredSize(getScene());
                        adjustSize(true);
                    }
                }
            }

            oldScene = newScene;
        }

        @Override
        public Object getBean() {
            return Window.this;
        }

        @Override
        public String getName() {
            return "scene";
        }

        private void updatePeerScene(final TKScene tkScene) {
            if (peer != null) {
                // Set scene impl on stage impl
                peer.setScene(tkScene);
            }
        }
    }

    /**
     * Defines the opacity of the {@code Window} as a value between 0.0 and 1.0.
     * The opacity is reflected across the {@code Window}, its {@code Decoration}
     * and its {@code Scene} content. On a JavaFX runtime platform that does not
     * support opacity, assigning a value to this variable will have no
     * visible difference. A {@code Window} with 0% opacity is fully translucent.
     * Typically, a {@code Window} with 0% opacity will not receive any mouse
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
                    if (peer != null) {
                        peer.setOpacity((float) get());
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
            onCloseRequest = new ObjectPropertyBase<>() {
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
            onShowing = new ObjectPropertyBase<>() {
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
            onShown = new ObjectPropertyBase<>() {
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
            onHiding = new ObjectPropertyBase<>() {
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
            onHidden = new ObjectPropertyBase<>() {
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
     * Whether or not this {@code Window} is showing (that is, open on the
     * user's system). The Window might be "showing", yet the user might not
     * be able to see it due to the Window being rendered behind another Window
     * or due to the Window being positioned off the monitor.
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
            WindowHelper.visibleChanging(Window.this, newVisible);
            if (newVisible) {
                hasBeenVisible = true;
                windows.add(Window.this);
            } else {
                windows.remove(Window.this);
            }
            Toolkit tk = Toolkit.getToolkit();
            if (peer != null) {
                if (newVisible) {
                    if (peerListener == null) {
                        peerListener = new WindowPeerListener(Window.this);
                    }

                    // Setup listener for changes coming back from peer
                    peer.setTKStageListener(peerListener);
                    // Register pulse listener
                    tk.addStageTkPulseListener(peerBoundsConfigurator);

                    boolean isEmbeddedWindow = Window.this instanceof EmbeddedWindow;
                    if (isEmbeddedWindow && getScene() != null) {
                        // JDK-8257719: The scene of embedded windows like JFXPanel
                        // or FXCanvas has to be initialized before setting the
                        // output scales
                        SceneHelper.initPeer(getScene());
                        peer.setScene(SceneHelper.getPeer(getScene()));
                        SceneHelper.preferredSize(getScene());
                    }

                    updateOutputScales(peer.getOutputScaleX(), peer.getOutputScaleY());
                    // updateOutputScales may cause an update to the render
                    // scales in many cases, but if the scale has not changed
                    // then the lazy render scale properties might think
                    // they do not need to send down the new values.  In some
                    // cases we have been show()n with a brand new peer, so
                    // it is better to force the render scales into the PBC.
                    // This may usually be a NOP, but it is similar to the
                    // forced setSize and setLocation down below.
                    peerBoundsConfigurator.setRenderScaleX(getRenderScaleX());
                    peerBoundsConfigurator.setRenderScaleY(getRenderScaleY());

                    if (!isEmbeddedWindow && getScene() != null) {
                        // The scene of regular windows is initialized
                        // after setting the output scale
                        SceneHelper.initPeer(getScene());
                        peer.setScene(SceneHelper.getPeer(getScene()));
                        SceneHelper.preferredSize(getScene());
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
                        peerBoundsConfigurator.setLocation(getX(), getY(),
                                                           0, 0);
                    }

                    // set peer bounds before the window is shown
                    applyBounds();

                    peer.setOpacity((float)getOpacity());

                    peer.setVisible(true);
                    fireEvent(new WindowEvent(Window.this, WindowEvent.WINDOW_SHOWN));
                } else {
                    peer.setVisible(false);

                    // Call listener
                    fireEvent(new WindowEvent(Window.this, WindowEvent.WINDOW_HIDDEN));

                    if (getScene() != null) {
                        peer.setScene(null);
                        SceneHelper.disposePeer(getScene());
                        StyleManager.getInstance().forget(getScene());
                    }

                    // Remove toolkit pulse listener
                    tk.removeStageTkPulseListener(peerBoundsConfigurator);
                    // Remove listener for changes coming back from peer
                    peer.setTKStageListener(null);

                    // Notify peer
                    peer.close();
                }
            }
            if (newVisible) {
                tk.requestNextPulse();
            }
            WindowHelper.visibleChanged(Window.this, newVisible);

            if (sizeToScene) {
                if (newVisible) {
                    // Now that the visibleChanged has completed, the insets of the window
                    // might have changed (e.g. due to setResizable(false)). Reapply the
                    // sizeToScene() request if needed to account for the new insets.
                    sizeToScene();
                }

                // Reset the flag unconditionally upon visibility changes
                sizeToScene = false;
            }
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

    /*
     * This can be replaced by listening for the onShowing/onHiding events
     * Note: This method MUST only be called via its accessor method.
     */
    private void doVisibleChanging(boolean visible) {
        if (visible && (getScene() != null)) {
            NodeHelper.reapplyCSS(getScene().getRoot());
        }
    }

    /*
     * This can be replaced by listening for the onShown/onHidden events
     * Note: This method MUST only be called via its accessor method.
     */
    private void doVisibleChanged(boolean visible) {
        assert peer != null;
        if (!visible) {
            peerListener = null;
            peer = null;
        }
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

    @Override
    public final <T extends Event> void addEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .addEventHandler(eventType, eventHandler);
    }

    @Override
    public final <T extends Event> void removeEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .removeEventHandler(eventType,
                                                        eventHandler);
    }

    @Override
    public final <T extends Event> void addEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getInternalEventDispatcher().getEventHandlerManager()
                                    .addEventFilter(eventType, eventFilter);
    }

    @Override
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
            eventDispatcher = new SimpleObjectProperty<>(
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
        if ((++focusGrabCounter == 1) && (peer != null) && isFocused()) {
            peer.grabFocus();
        }
    }

    void decreaseFocusGrabCounter() {
        if ((--focusGrabCounter == 0) && (peer != null)) {
            peer.ungrabFocus();
        }
    }

    private void focusChanged(final boolean newIsFocused) {
        if ((focusGrabCounter > 0) && (peer != null) && newIsFocused) {
            peer.grabFocus();
        }
    }

    final void applyBounds() {
        peerBoundsConfigurator.apply();
    }

    Window getWindowOwner() {
        return null;
    }

    private Screen getWindowScreen() {
        Window window = this;
        do {
            if (!Double.isNaN(window.getX())
                    && !Double.isNaN(window.getY())
                    && !Double.isNaN(window.getWidth())
                    && !Double.isNaN(window.getHeight())) {
                return Utils.getScreenForRectangle(
                                     new Rectangle2D(window.getX(),
                                                     window.getY(),
                                                     window.getWidth(),
                                                     window.getHeight()));
            }

            window = window.getWindowOwner();
        } while (window != null);

        return Screen.getPrimary();
    }

    private final ReadOnlyObjectWrapper<Screen> screen = new ReadOnlyObjectWrapper<>(Screen.getPrimary());
    private ReadOnlyObjectProperty<Screen> screenProperty() { return screen.getReadOnlyProperty(); }

    private void notifyScreenChanged(Object from, Object to) {
        screen.set(Screen.getScreenForNative(to));
    }

    /**
     * Caches all requested bounds settings and applies them at once during
     * the next pulse.
     */
    private final class TKBoundsConfigurator implements TKPulseListener {
        private double renderScaleX;
        private double renderScaleY;
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

        public void setRenderScaleX(final double renderScaleX) {
            this.renderScaleX = renderScaleX;
            setDirty();
        }

        public void setRenderScaleY(final double renderScaleY) {
            this.renderScaleY = renderScaleY;
            setDirty();
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
                if (peer == null) {
                    reset();
                    return;
                }
                // Snapshot values and then reset() before we call down
                // as we may end up with recursive calls back up with
                // new values that must be recorded as dirty.
                boolean xSet = !Double.isNaN(x);
                float newX = xSet ? (float) x : 0f;
                boolean ySet = !Double.isNaN(y);
                float newY = ySet ? (float) y : 0f;
                float newWW = (float) windowWidth;
                float newWH = (float) windowHeight;
                float newCW = (float) clientWidth;
                float newCH = (float) clientHeight;
                float newXG = xGravity;
                float newYG = yGravity;
                float newRX = (float) renderScaleX;
                float newRY = (float) renderScaleY;
                reset();
                peer.setBounds(newX, newY, xSet, ySet,
                        newWW, newWH, newCW, newCH,
                        newXG, newYG,
                        newRX, newRY);
            }
        }

        @Override
        public void pulse() {
            apply();
        }

        private void reset() {
            renderScaleX = 0.0;
            renderScaleY = 0.0;
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
