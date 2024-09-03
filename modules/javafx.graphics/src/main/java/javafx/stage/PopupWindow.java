/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.TreeShowingProperty;
import com.sun.javafx.util.Utils;
import com.sun.javafx.event.DirectEvent;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.event.EventRedirector;
import com.sun.javafx.event.EventUtil;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.stage.FocusUngrabEvent;
import com.sun.javafx.stage.PopupWindowPeerListener;
import com.sun.javafx.stage.WindowCloseRequestHandler;
import com.sun.javafx.stage.WindowEventDispatcher;
import com.sun.javafx.tk.Toolkit;
import static com.sun.javafx.FXPermissions.CREATE_TRANSPARENT_WINDOW_PERMISSION;

import com.sun.javafx.stage.PopupWindowHelper;
import com.sun.javafx.stage.WindowHelper;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

/**
 * PopupWindow is the parent for a variety of different types of popup
 * based windows including {@link Popup} and {@link javafx.scene.control.Tooltip}
 * and {@link javafx.scene.control.ContextMenu}.
 * <p>
 * A PopupWindow is a secondary window which has no window decorations or title bar.
 * It doesn't show up in the OS as a top-level window. It is typically
 * used for tool tip like notification, drop down boxes, menus, and so forth.
 * <p>
 * The PopupWindow <strong>cannot be shown without an owner</strong>.
 * PopupWindows require that an owner window exist in order to be shown. However,
 * it is possible to create a PopupWindow ahead of time and simply set the owner
 * (or change the owner) before first being made visible. Attempting to change
 * the owner while the PopupWindow is visible will result in an IllegalStateException.
 * <p>
 * The PopupWindow encapsulates much of the behavior and functionality common to popups,
 * such as the ability to close when the "esc" key is pressed, or the ability to
 * hide all child popup windows whenever this window is hidden. These abilities can
 * be enabled or disabled via properties.
 * @since JavaFX 2.0
 */
public abstract class PopupWindow extends Window {

     static {
        PopupWindowHelper.setPopupWindowAccessor(new PopupWindowHelper.PopupWindowAccessor() {
            @Override public void doVisibleChanging(Window window, boolean visible) {
                ((PopupWindow) window).doVisibleChanging(visible);
            }

            @Override public void doVisibleChanged(Window window, boolean visible) {
                ((PopupWindow) window).doVisibleChanged(visible);
            }

            @Override
            public ObservableList<Node> getContent(PopupWindow popupWindow) {
                return popupWindow.getContent();
            }

            @Override
            public void applyStylesheetFromOwner(PopupWindow popupWindow, Window owner) {
                popupWindow.applyStylesheetFromOwner(owner);
            }
        });
    }

    /**
     * A private list of all child popups.
     */
    private final List<PopupWindow> children = new ArrayList<>();

    /**
     * Keeps track of the bounds of the content, and adjust the position and
     * size of the popup window accordingly. This way as the popup content
     * changes, the window will be changed to match.
     */
    private final InvalidationListener popupWindowUpdater =
            new InvalidationListener() {
                @Override
                public void invalidated(final Observable observable) {
                    cachedExtendedBounds = null;
                    cachedAnchorBounds = null;
                    updateWindow(getAnchorX(), getAnchorY());
                }
            };

    /**
     * RT-28454: When a parent node or parent window we are associated with is not
     * visible anymore, possibly because the scene was not valid anymore, we should hide.
     */
    private ChangeListener<Boolean> changeListener = (observable, oldValue, newValue) -> {
        if (oldValue && !newValue) {
            hide();
        }
    };

    private WeakChangeListener<Boolean> weakOwnerNodeListener = new WeakChangeListener(changeListener);
    private TreeShowingProperty treeShowingProperty;

    /**
     * Constructor for subclasses to call.
     */
    public PopupWindow() {
        final Pane popupRoot = new Pane();
        popupRoot.setBackground(Background.EMPTY);
        popupRoot.getStyleClass().add("popup");

        final Scene scene = SceneHelper.createPopupScene(popupRoot);
        scene.setFill(null);
        super.setScene(scene);

        popupRoot.layoutBoundsProperty().addListener(popupWindowUpdater);
        popupRoot.boundsInLocalProperty().addListener(popupWindowUpdater);
        scene.rootProperty().addListener(
                new InvalidationListener() {
                    private Node oldRoot = scene.getRoot();

                    @Override
                    public void invalidated(final Observable observable) {
                        final Node newRoot = scene.getRoot();
                        if (oldRoot != newRoot) {
                            if (oldRoot != null) {
                                oldRoot.layoutBoundsProperty()
                                       .removeListener(popupWindowUpdater);
                                oldRoot.boundsInLocalProperty()
                                       .removeListener(popupWindowUpdater);
                                oldRoot.getStyleClass().remove("popup");
                            }

                            if (newRoot != null) {
                                newRoot.layoutBoundsProperty()
                                       .addListener(popupWindowUpdater);
                                newRoot.boundsInLocalProperty()
                                       .addListener(popupWindowUpdater);
                                newRoot.getStyleClass().add("popup");
                            }

                            oldRoot = newRoot;

                            cachedExtendedBounds = null;
                            cachedAnchorBounds = null;
                            updateWindow(getAnchorX(), getAnchorY());
                        }
                    }
                });
        PopupWindowHelper.initHelper(this);
    }

    /*
     * Gets the observable, modifiable list of children which are placed in this
     * PopupWindow.
     *
     * @return the PopupWindow content
     */
    ObservableList<Node> getContent() {
        final Parent rootNode = getScene().getRoot();
        if (rootNode instanceof Group) {
            return ((Group) rootNode).getChildren();
        }

        if (rootNode instanceof Pane) {
            return ((Pane) rootNode).getChildren();
        }

        throw new IllegalStateException(
                "The content of the Popup can't be accessed");
    }

    /**
     * The window which is the parent of this popup. All popups must have an
     * owner window.
     */
    private ReadOnlyObjectWrapper<Window> ownerWindow =
            new ReadOnlyObjectWrapper<>(this, "ownerWindow");
    public final Window getOwnerWindow() {
        return ownerWindow.get();
    }
    public final ReadOnlyObjectProperty<Window> ownerWindowProperty() {
        return ownerWindow.getReadOnlyProperty();
    }

    /**
     * The node which is the owner of this popup. All popups must have an
     * owner window but are not required to be associated with an owner node.
     * If an autohide Popup has an owner node, mouse press inside the owner node
     * doesn't cause the Popup to hide.
     */
    private ReadOnlyObjectWrapper<Node> ownerNode =
            new ReadOnlyObjectWrapper<>(this, "ownerNode");
    public final Node getOwnerNode() {
        return ownerNode.get();
    }
    public final ReadOnlyObjectProperty<Node> ownerNodeProperty() {
        return ownerNode.getReadOnlyProperty();
    }

    /**
     * Note to subclasses: the scene used by PopupWindow is very specifically
     * managed by PopupWindow. This method is overridden to throw
     * UnsupportedOperationException. You cannot specify your own scene.
     *
     * @param scene the scene to be rendered on this window
     */
    @Override protected final void setScene(Scene scene) {
        throw new UnsupportedOperationException();
    }

    /**
     * This convenience variable indicates whether, when the popup is shown,
     * it should automatically correct its position such that it doesn't end
     * up positioned off the screen.
     * @defaultValue true
     */
    private BooleanProperty autoFix =
            new BooleanPropertyBase(true) {
                @Override
                protected void invalidated() {
                    handleAutofixActivation(isShowing(), get());
                }

                @Override
                public Object getBean() {
                    return PopupWindow.this;
                }

                @Override
                public String getName() {
                    return "autoFix";
                }
            };
    public final void setAutoFix(boolean value) { autoFix.set(value); }
    public final boolean isAutoFix() { return autoFix.get(); }
    public final BooleanProperty autoFixProperty() { return autoFix; }

    /**
     * Specifies whether Popups should auto hide. If a popup loses focus and
     * autoHide is true, then the popup will be hidden automatically.
     * <p>
     * The only exception is when owner Node is specified using {@link #show(javafx.scene.Node, double, double)}.
     * Focusing owner Node will not hide the PopupWindow.
     * </p>
     * @defaultValue false
     */
    private BooleanProperty autoHide =
            new BooleanPropertyBase() {
                @Override
                protected void invalidated() {
                    handleAutohideActivation(isShowing(), get());
                }

                @Override
                public Object getBean() {
                    return PopupWindow.this;
                }

                @Override
                public String getName() {
                    return "autoHide";
                }
            };
    public final void setAutoHide(boolean value) { autoHide.set(value); }
    public final boolean isAutoHide() { return autoHide.get(); }
    public final BooleanProperty autoHideProperty() { return autoHide; }

    /**
     * Called after autoHide is run.
     */
    private ObjectProperty<EventHandler<Event>> onAutoHide =
            new SimpleObjectProperty<>(this, "onAutoHide");
    public final void setOnAutoHide(EventHandler<Event> value) { onAutoHide.set(value); }
    public final EventHandler<Event> getOnAutoHide() { return onAutoHide.get(); }
    public final ObjectProperty<EventHandler<Event>> onAutoHideProperty() { return onAutoHide; }

    /**
     * Specifies whether the PopupWindow should be hidden when an unhandled escape key
     * is pressed while the popup has focus.
     * @defaultValue true
     */
    private BooleanProperty hideOnEscape =
            new SimpleBooleanProperty(this, "hideOnEscape", true);
    public final void setHideOnEscape(boolean value) { hideOnEscape.set(value); }
    public final boolean isHideOnEscape() { return hideOnEscape.get(); }
    public final BooleanProperty hideOnEscapeProperty() { return hideOnEscape; }

    /**
     * Specifies whether the event, which caused the Popup to hide, should be
     * consumed. Having the event consumed prevents it from triggering some
     * additional UI response in the Popup's owner window.
     * @defaultValue true
     * @since JavaFX 2.2
     */
    private BooleanProperty consumeAutoHidingEvents =
            new SimpleBooleanProperty(this, "consumeAutoHidingEvents",
                                      true);

    public final void setConsumeAutoHidingEvents(boolean value) {
        consumeAutoHidingEvents.set(value);
    }

    public final boolean getConsumeAutoHidingEvents() {
        return consumeAutoHidingEvents.get();
    }

    public final BooleanProperty consumeAutoHidingEventsProperty() {
        return consumeAutoHidingEvents;
    }

    /**
     * Show the popup.
     * @param owner The owner of the popup. This must not be null.
     * @throws NullPointerException if owner is null
     * @throws IllegalArgumentException if the specified owner window would
     *      create cycle in the window hierarchy
     */
    public void show(Window owner) {
        validateOwnerWindow(owner);
        showImpl(owner);
    }

    /**
     * Shows the popup at the specified location on the screen. The popup window
     * is positioned in such way that its anchor point ({@link #anchorLocationProperty() anchorLocation})
     * is displayed at the specified {@code anchorX} and {@code anchorY}
     * coordinates.
     * <p>
     * The popup is associated with the specified owner node. The {@code Window}
     * which contains the owner node at the time of the call becomes an owner
     * window of the displayed popup.
     * </p>
     * <p>
     * Note that when {@link #autoHideProperty() autoHide} is set to true, mouse press on the owner Node
     * will not hide the PopupWindow.
     * </p>
     *
     * @param ownerNode The owner Node of the popup. It must not be null
     *        and must be associated with a Window.
     * @param anchorX the x position of the popup anchor in screen coordinates
     * @param anchorY the y position of the popup anchor in screen coordinates
     * @throws NullPointerException if ownerNode is null
     * @throws IllegalArgumentException if the specified owner node is not
     *      associated with a Window or when the window would create cycle
     *      in the window hierarchy
     */
    public void show(Node ownerNode, double anchorX, double anchorY) {
        if (ownerNode == null) {
            throw new NullPointerException("The owner node must not be null");
        }

        final Scene ownerNodeScene = ownerNode.getScene();
        if ((ownerNodeScene == null)
                || (ownerNodeScene.getWindow() == null)) {
            throw new IllegalArgumentException(
                    "The owner node needs to be associated with a window");
        }

        final Window newOwnerWindow = ownerNodeScene.getWindow();
        validateOwnerWindow(newOwnerWindow);

        this.ownerNode.set(ownerNode);

        // PopupWindow should disappear when owner node is not visible
        if (ownerNode != null) {
            treeShowingProperty = new TreeShowingProperty(ownerNode);
            treeShowingProperty.addListener(weakOwnerNodeListener);
        }

        updateWindow(anchorX, anchorY);
        showImpl(newOwnerWindow);
    }

    /**
     * Shows the popup at the specified location on the screen. The popup window
     * is positioned in such way that its anchor point ({@link #anchorLocationProperty() anchorLocation})
     * is displayed at the specified {@code anchorX} and {@code anchorY}
     * coordinates.
     *
     * @param ownerWindow The owner of the popup. This must not be null.
     * @param anchorX the x position of the popup anchor in screen coordinates
     * @param anchorY the y position of the popup anchor in screen coordinates
     * @throws NullPointerException if ownerWindow is null
     * @throws IllegalArgumentException if the specified owner window would
     *      create cycle in the window hierarchy
     */
    public void show(Window ownerWindow, double anchorX, double anchorY) {
        validateOwnerWindow(ownerWindow);

        updateWindow(anchorX, anchorY);
        showImpl(ownerWindow);
    }

    private void showImpl(final Window owner) {
        // Update the owner field
        this.ownerWindow.set(owner);
        if (owner instanceof PopupWindow) {
            ((PopupWindow)owner).children.add(this);
        }
        // PopupWindow should disappear when owner node is not visible
        if (owner != null) {
            owner.showingProperty().addListener(weakOwnerNodeListener);
        }

        final Scene sceneValue = getScene();
        SceneHelper.parentEffectiveOrientationInvalidated(sceneValue);

        // JDK-8116444
        applyStylesheetFromOwner(owner);

        final Scene ownerScene = getRootWindow(owner).getScene();
        if (ownerScene != null) {
            if (sceneValue.getCursor() == null) {
                sceneValue.setCursor(ownerScene.getCursor());
            }
        }

        // It is required that the root window exist and be visible to show the popup.
        if (getRootWindow(owner).isShowing()) {
            // We do show() first so that the width and height of the
            // popup window are initialized. This way the x,y location of the
            // popup calculated below uses the right width and height values for
            // its calculation. (fix for part of RT-10675).
            show();
        }
    }

    /**
     * Applies the stylesheet from the scene of the root owner {@link Window} to the {@link Scene}
     * associated with that window.
     *
     * @param owner the owner {@link Window}
     */
    void applyStylesheetFromOwner(Window owner) {
        Scene scene = getScene();
        final Scene ownerScene = getRootWindow(owner).getScene();
        if (ownerScene != null) {
            if (ownerScene.getUserAgentStylesheet() != null) {
                scene.setUserAgentStylesheet(ownerScene.getUserAgentStylesheet());
            }
            scene.getStylesheets().setAll(ownerScene.getStylesheets());
        }
    }

    /**
     * Hide this Popup and all its children
     */
    @Override public void hide() {
        for (PopupWindow c : children) {
            if (c.isShowing()) {
                c.hide();
            }
        }
        children.clear();
        super.hide();

        // When popup hides, remove listeners; these are added when the popup shows.
        if (getOwnerWindow() != null) getOwnerWindow().showingProperty().removeListener(weakOwnerNodeListener);
        if (treeShowingProperty != null) {
            treeShowingProperty.removeListener(weakOwnerNodeListener);
            treeShowingProperty.dispose();
            treeShowingProperty = null;
        }
    }

    /*
     * This can be replaced by listening for the onShowing/onHiding events
     * Note: This method MUST only be called via its accessor method.
     */
    private void doVisibleChanging(boolean visible) {
        PerformanceTracker.logEvent("PopupWindow.storeVisible for [PopupWindow]");

        Toolkit toolkit = Toolkit.getToolkit();
        if (visible && (getPeer() == null)) {
            // Setup the peer
            StageStyle popupStyle;
            try {
                @SuppressWarnings("removal")
                final SecurityManager securityManager =
                        System.getSecurityManager();
                if (securityManager != null) {
                    securityManager.checkPermission(CREATE_TRANSPARENT_WINDOW_PERMISSION);
                }
                popupStyle = StageStyle.TRANSPARENT;
            } catch (final SecurityException e) {
                popupStyle = StageStyle.UNDECORATED;
            }
            setPeer(toolkit.createTKPopupStage(this, popupStyle, getOwnerWindow().getPeer(), acc));
            setPeerListener(new PopupWindowPeerListener(PopupWindow.this));
        }
    }

    private Window rootWindow;

    /*
     * This can be replaced by listening for the onShown/onHidden events
     * Note: This method MUST only be called via its accessor method.
     */
    private void doVisibleChanged(boolean visible) {
        final Window ownerWindowValue = getOwnerWindow();
        if (visible) {
            rootWindow = getRootWindow(ownerWindowValue);

            startMonitorOwnerEvents(ownerWindowValue);
            // currently we consider popup window to be focused when it is
            // visible and its owner window is focused (we need to track
            // that through listener on owner window focused property)
            // a better solution would require some focus manager, which can
            // track focus state across multiple windows
            bindOwnerFocusedProperty(ownerWindowValue);
            WindowHelper.setFocused(this, ownerWindowValue.isFocused());
            handleAutofixActivation(true, isAutoFix());
            handleAutohideActivation(true, isAutoHide());
        } else {
            stopMonitorOwnerEvents(ownerWindowValue);
            unbindOwnerFocusedProperty(ownerWindowValue);
            WindowHelper.setFocused(this, false);
            handleAutofixActivation(false, isAutoFix());
            handleAutohideActivation(false, isAutoHide());
            rootWindow = null;
        }

        PerformanceTracker.logEvent("PopupWindow.storeVisible for [PopupWindow] finished");
    }

    /**
     * Specifies the x coordinate of the popup anchor point on the screen. If
     * the {@code anchorLocation} is set to {@code WINDOW_TOP_LEFT} or
     * {@code WINDOW_BOTTOM_LEFT} the {@code x} and {@code anchorX} values will
     * be identical.
     *
     * @since JavaFX 8.0
     */
    private final ReadOnlyDoubleWrapper anchorX =
            new ReadOnlyDoubleWrapper(this, "anchorX", Double.NaN);

    public final void setAnchorX(final double value) {
        updateWindow(value, getAnchorY());
    }
    public final double getAnchorX() {
        return anchorX.get();
    }
    public final ReadOnlyDoubleProperty anchorXProperty() {
        return anchorX.getReadOnlyProperty();
    }

    /**
     * Specifies the y coordinate of the popup anchor point on the screen. If
     * the {@code anchorLocation} is set to {@code WINDOW_TOP_LEFT} or
     * {@code WINDOW_TOP_RIGHT} the {@code y} and {@code anchorY} values will
     * be identical.
     *
     * @since JavaFX 8.0
     */
    private final ReadOnlyDoubleWrapper anchorY =
            new ReadOnlyDoubleWrapper(this, "anchorY", Double.NaN);

    public final void setAnchorY(final double value) {
        updateWindow(getAnchorX(), value);
    }
    public final double getAnchorY() {
        return anchorY.get();
    }
    public final ReadOnlyDoubleProperty anchorYProperty() {
        return anchorY.getReadOnlyProperty();
    }

    /**
     * Specifies the popup anchor point which is used in popup positioning. The
     * point can be set to a corner of the popup window or a corner of its
     * content. In this context the content corners are derived from the popup
     * root node's layout bounds.
     * <p>
     * In general changing of the anchor location won't change the current
     * window position. Instead of that, the {@code anchorX} and {@code anchorY}
     * values are recalculated to correspond to the new anchor point.
     * </p>
     * @since JavaFX 8.0
     */
    private final ObjectProperty<AnchorLocation> anchorLocation =
            new ObjectPropertyBase<AnchorLocation>(
                    AnchorLocation.WINDOW_TOP_LEFT) {
                @Override
                protected void invalidated() {
                    cachedAnchorBounds = null;
                    updateWindow(windowToAnchorX(getX()),
                                 windowToAnchorY(getY()));
                }

                @Override
                public Object getBean() {
                    return PopupWindow.this;
                }

                @Override
                public String getName() {
                    return "anchorLocation";
                }
            };
    public final void setAnchorLocation(final AnchorLocation value) {
        anchorLocation.set(value);
    }
    public final AnchorLocation getAnchorLocation() {
        return anchorLocation.get();
    }
    public final ObjectProperty<AnchorLocation> anchorLocationProperty() {
        return anchorLocation;
    }

    /**
     * Anchor location constants for popup anchor point selection.
     *
     * @since JavaFX 8.0
     */
    public enum AnchorLocation {
        /** Represents top left window corner. */
        WINDOW_TOP_LEFT(0, 0, false),
        /** Represents top right window corner. */
        WINDOW_TOP_RIGHT(1, 0, false),
        /** Represents bottom left window corner. */
        WINDOW_BOTTOM_LEFT(0, 1, false),
        /** Represents bottom right window corner. */
        WINDOW_BOTTOM_RIGHT(1, 1, false),
        /** Represents top left content corner. */
        CONTENT_TOP_LEFT(0, 0, true),
        /** Represents top right content corner. */
        CONTENT_TOP_RIGHT(1, 0, true),
        /** Represents bottom left content corner. */
        CONTENT_BOTTOM_LEFT(0, 1, true),
        /** Represents bottom right content corner. */
        CONTENT_BOTTOM_RIGHT(1, 1, true);

        private final double xCoef;
        private final double yCoef;
        private final boolean contentLocation;

        private AnchorLocation(final double xCoef, final double yCoef,
                               final boolean contentLocation) {
            this.xCoef = xCoef;
            this.yCoef = yCoef;
            this.contentLocation = contentLocation;
        }

        double getXCoef() {
            return xCoef;
        }

        double getYCoef() {
            return yCoef;
        }

        boolean isContentLocation() {
            return contentLocation;
        }
    }

    @Override
    void setXInternal(final double value) {
        updateWindow(windowToAnchorX(value), getAnchorY());
    }

    @Override
    void setYInternal(final double value) {
        updateWindow(getAnchorX(), windowToAnchorY(value));
    }

    @Override
    void notifyLocationChanged(final double newX, final double newY) {
        super.notifyLocationChanged(newX, newY);
        anchorX.set(windowToAnchorX(newX));
        anchorY.set(windowToAnchorY(newY));
    }

    private Bounds cachedExtendedBounds;
    private Bounds cachedAnchorBounds;

    private Bounds getExtendedBounds() {
        if (cachedExtendedBounds == null) {
            final Parent rootNode = getScene().getRoot();
            cachedExtendedBounds = union(rootNode.getLayoutBounds(),
                                         rootNode.getBoundsInLocal());
        }

        return cachedExtendedBounds;
    }

    private Bounds getAnchorBounds() {
        if (cachedAnchorBounds == null) {
            cachedAnchorBounds = getAnchorLocation().isContentLocation()
                                         ? getScene().getRoot()
                                                     .getLayoutBounds()
                                         : getExtendedBounds();
        }

        return cachedAnchorBounds;
    }

    private void updateWindow(final double newAnchorX,
                              final double newAnchorY) {
        final AnchorLocation anchorLocationValue = getAnchorLocation();
        final Parent rootNode = getScene().getRoot();
        final Bounds extendedBounds = getExtendedBounds();
        final Bounds anchorBounds = getAnchorBounds();

        final double anchorXCoef = anchorLocationValue.getXCoef();
        final double anchorYCoef = anchorLocationValue.getYCoef();
        final double anchorDeltaX = anchorXCoef * anchorBounds.getWidth();
        final double anchorDeltaY = anchorYCoef * anchorBounds.getHeight();
        double anchorScrMinX = newAnchorX - anchorDeltaX;
        double anchorScrMinY = newAnchorY - anchorDeltaY;

        if (autofixActive) {
            final Screen currentScreen =
                    Utils.getScreenForPoint(newAnchorX, newAnchorY);
            final Rectangle2D screenBounds =
                    Utils.hasFullScreenStage(currentScreen)
                            ? currentScreen.getBounds()
                            : currentScreen.getVisualBounds();

            if (anchorXCoef <= 0.5) {
                // left side of the popup is more important, try to keep it
                // visible if the popup width is larger than screen width
                anchorScrMinX = Math.min(anchorScrMinX,
                                         screenBounds.getMaxX()
                                             - anchorBounds.getWidth());
                anchorScrMinX = Math.max(anchorScrMinX, screenBounds.getMinX());
            } else {
                // right side of the popup is more important
                anchorScrMinX = Math.max(anchorScrMinX, screenBounds.getMinX());
                anchorScrMinX = Math.min(anchorScrMinX,
                                         screenBounds.getMaxX()
                                             - anchorBounds.getWidth());
            }

            if (anchorYCoef <= 0.5) {
                // top side of the popup is more important
                anchorScrMinY = Math.min(anchorScrMinY,
                                         screenBounds.getMaxY()
                                             - anchorBounds.getHeight());
                anchorScrMinY = Math.max(anchorScrMinY, screenBounds.getMinY());
            } else {
                // bottom side of the popup is more important
                anchorScrMinY = Math.max(anchorScrMinY, screenBounds.getMinY());
                anchorScrMinY = Math.min(anchorScrMinY,
                                         screenBounds.getMaxY()
                                             - anchorBounds.getHeight());
            }
        }

        final double windowScrMinX =
                anchorScrMinX - anchorBounds.getMinX()
                              + extendedBounds.getMinX();
        final double windowScrMinY =
                anchorScrMinY - anchorBounds.getMinY()
                              + extendedBounds.getMinY();

        // update popup dimensions
        setWidth(extendedBounds.getWidth());
        setHeight(extendedBounds.getHeight());
        // update transform
        rootNode.setTranslateX(-extendedBounds.getMinX());
        rootNode.setTranslateY(-extendedBounds.getMinY());

        // update popup position
        // don't set Window.xExplicit unnecessarily
        if (!Double.isNaN(windowScrMinX)) {
            super.setXInternal(windowScrMinX);
        }
        // don't set Window.yExplicit unnecessarily
        if (!Double.isNaN(windowScrMinY)) {
            super.setYInternal(windowScrMinY);
        }

        // set anchor x, anchor y
        anchorX.set(anchorScrMinX + anchorDeltaX);
        anchorY.set(anchorScrMinY + anchorDeltaY);
    }

    private Bounds union(final Bounds bounds1, final Bounds bounds2) {
        final double minX = Math.min(bounds1.getMinX(), bounds2.getMinX());
        final double minY = Math.min(bounds1.getMinY(), bounds2.getMinY());
        final double maxX = Math.max(bounds1.getMaxX(), bounds2.getMaxX());
        final double maxY = Math.max(bounds1.getMaxY(), bounds2.getMaxY());

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private double windowToAnchorX(final double windowX) {
        final Bounds anchorBounds = getAnchorBounds();
        return windowX - getExtendedBounds().getMinX()
                       + anchorBounds.getMinX()
                       + getAnchorLocation().getXCoef()
                             * anchorBounds.getWidth();
    }

    private double windowToAnchorY(final double windowY) {
        final Bounds anchorBounds = getAnchorBounds();
        return windowY - getExtendedBounds().getMinY()
                       + anchorBounds.getMinY()
                       + getAnchorLocation().getYCoef()
                             * anchorBounds.getHeight();
    }

    /**
     *
     * Gets the root (non PopupWindow) Window for the provided window.
     *
     * @param win the Window for which to get the root window
     */
    private static Window getRootWindow(Window win) {
        // should be enough to traverse PopupWindow hierarchy here to get to the
        // first non-popup focusable window
        while (win instanceof PopupWindow) {
            win = ((PopupWindow) win).getOwnerWindow();
        }
        return win;
    }

    void doAutoHide() {
        // There is a timing problem here. I would like to have this isVisible
        // check, such that we don't send an onAutoHide event if it was already
        // invisible. However, visible is already false by the time this method
        // gets called, when done by certain code paths.
//        if (isVisible()) {
        // hide this popup
        hide();
        if (getOnAutoHide() != null) {
            getOnAutoHide().handle(new Event(this, this, Event.ANY));
        }
//        }
    }

    @Override
    WindowEventDispatcher createInternalEventDispatcher() {
        return new WindowEventDispatcher(new PopupEventRedirector(this),
                                         new WindowCloseRequestHandler(this),
                                         new EventHandlerManager(this));

    }

    @Override
    Window getWindowOwner() {
        return getOwnerWindow();
    }

    private void startMonitorOwnerEvents(final Window ownerWindowValue) {
        final EventRedirector parentEventRedirector =
                ownerWindowValue.getInternalEventDispatcher()
                                .getEventRedirector();
        parentEventRedirector.addEventDispatcher(getEventDispatcher());
    }

    private void stopMonitorOwnerEvents(final Window ownerWindowValue) {
        final EventRedirector parentEventRedirector =
                ownerWindowValue.getInternalEventDispatcher()
                                .getEventRedirector();
        parentEventRedirector.removeEventDispatcher(getEventDispatcher());
    }

    private ChangeListener<Boolean> ownerFocusedListener;

    private void bindOwnerFocusedProperty(final Window ownerWindowValue) {
        ownerFocusedListener =
                (observable, oldValue, newValue) -> WindowHelper.setFocused(this, newValue);
        ownerWindowValue.focusedProperty().addListener(ownerFocusedListener);
    }

    private void unbindOwnerFocusedProperty(final Window ownerWindowValue) {
        ownerWindowValue.focusedProperty().removeListener(ownerFocusedListener);
        ownerFocusedListener = null;
    }

    private boolean autofixActive;
    private void handleAutofixActivation(final boolean visible,
                                         final boolean autofix) {
        final boolean newAutofixActive = visible && autofix;
        if (autofixActive != newAutofixActive) {
            autofixActive = newAutofixActive;
            if (newAutofixActive) {
                Screen.getScreens().addListener(popupWindowUpdater);
                updateWindow(getAnchorX(), getAnchorY());
            } else {
                Screen.getScreens().removeListener(popupWindowUpdater);
            }
        }
    }

    private boolean autohideActive;
    private void handleAutohideActivation(final boolean visible,
                                          final boolean autohide) {
        final boolean newAutohideActive = visible && autohide;
        if (autohideActive != newAutohideActive) {
            // assert rootWindow != null;
            autohideActive = newAutohideActive;
            if (newAutohideActive) {
                rootWindow.increaseFocusGrabCounter();
            } else {
                rootWindow.decreaseFocusGrabCounter();
            }
        }
    }

    private void validateOwnerWindow(final Window owner) {
        if (owner == null) {
            throw new NullPointerException("Owner window must not be null");
        }

        if (wouldCreateCycle(owner, this)) {
            throw new IllegalArgumentException(
                    "Specified owner window would create cycle"
                        + " in the window hierarchy");
        }

        if (isShowing() && (getOwnerWindow() != owner)) {
            throw new IllegalStateException(
                    "Popup is already shown with different owner window");
        }
    }

    private static boolean wouldCreateCycle(Window parent, final Window child) {
       while (parent != null) {
           if (parent == child) {
               return true;
           }

           parent = parent.getWindowOwner();
       }

       return false;
    }

    static class PopupEventRedirector extends EventRedirector {

        private static final KeyCombination ESCAPE_KEY_COMBINATION =
                KeyCombination.keyCombination("Esc");
        private final PopupWindow popupWindow;

        public PopupEventRedirector(final PopupWindow popupWindow) {
            super(popupWindow);
            this.popupWindow = popupWindow;
        }

        @Override
        protected void handleRedirectedEvent(final Object eventSource,
                final Event event) {
            if (event instanceof KeyEvent) {
                handleKeyEvent((KeyEvent) event);
                return;
            }

            final EventType<?> eventType = event.getEventType();

            if (eventType == MouseEvent.MOUSE_PRESSED
                    || eventType == ScrollEvent.SCROLL) {
                handleAutoHidingEvents(eventSource, event);
                return;
            }

            if (eventType == FocusUngrabEvent.FOCUS_UNGRAB) {
                handleFocusUngrabEvent();
                return;
            }
        }

        private void handleKeyEvent(final KeyEvent event) {
            if (event.isConsumed()) {
                return;
            }

            final Scene scene = popupWindow.getScene();
            if (scene != null) {
                final Node sceneFocusOwner = scene.getFocusOwner();
                final EventTarget eventTarget =
                        (sceneFocusOwner != null) ? sceneFocusOwner : scene;
                if (EventUtil.fireEvent(eventTarget, new DirectEvent(event.copyFor(popupWindow, eventTarget)))
                        == null) {
                    event.consume();
                    return;
                }
            }

            if ((event.getEventType() == KeyEvent.KEY_PRESSED)
                    && ESCAPE_KEY_COMBINATION.match(event)) {
                handleEscapeKeyPressedEvent(event);
            }
        }

        private void handleEscapeKeyPressedEvent(final Event event) {
            if (popupWindow.isHideOnEscape()) {
                popupWindow.doAutoHide();

                if (popupWindow.getConsumeAutoHidingEvents()) {
                    event.consume();
                }
            }
        }

        private void handleAutoHidingEvents(final Object eventSource,
                                            final Event event) {
            // we handle mouse pressed only for the immediate parent window,
            // where we can check whether the mouse press is inside of the owner
            // control or not, we will force possible child popups to close
            // by sending the FOCUS_UNGRAB event
            if (popupWindow.getOwnerWindow() != eventSource) {
                return;
            }

            if (popupWindow.isAutoHide() && !isOwnerNodeEvent(event)) {
                // the mouse press is outside of the owner control,
                // fire FOCUS_UNGRAB to child popups
                Event.fireEvent(popupWindow, new FocusUngrabEvent());

                popupWindow.doAutoHide();

                if (popupWindow.getConsumeAutoHidingEvents()) {
                    event.consume();
                }
            }
        }

        private void handleFocusUngrabEvent() {
            if (popupWindow.isAutoHide()) {
                popupWindow.doAutoHide();
            }
        }

        private boolean isOwnerNodeEvent(final Event event) {
            final Node ownerNode = popupWindow.getOwnerNode();
            if (ownerNode == null) {
                return false;
            }

            final EventTarget eventTarget = event.getTarget();
            if (!(eventTarget instanceof Node)) {
                return false;
            }

            Node node = (Node) eventTarget;
            do {
                if (node == ownerNode) {
                    return true;
                }
                node = node.getParent();
            } while (node != null);

            return false;
        }
    }
}
