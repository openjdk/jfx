/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.Utils;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.event.EventRedirector;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.stage.PopupEventRedirector;
import com.sun.javafx.stage.PopupWindowPeerListener;
import com.sun.javafx.stage.WindowCloseRequestHandler;
import com.sun.javafx.stage.WindowEventDispatcher;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

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
 */
public abstract class PopupWindow extends Window {
    /**
     * A private list of all child popups.
     */
    private final List<PopupWindow> children = new ArrayList<PopupWindow>();

    /**
     * Keeps track of the bounds of the group, and adjust the size of the
     * popup window accordingly. This way as the popup content changes, the
     * window will be changed to match.
     */
    private final InvalidationListener rootBoundsListener =
            new InvalidationListener() {
                @Override
                public void invalidated(final Observable observable) {
                    updateDimensions();
                }
            };

    public PopupWindow() {
        final Scene scene = new Scene(null);
        scene.setFill(null);
        super.setScene(scene);

        scene.rootProperty().addListener(
                new InvalidationListener() {
                    private Node oldRoot;

                    @Override
                    public void invalidated(final Observable observable) {
                        final Node newRoot = scene.getRoot();
                        if (oldRoot != newRoot) {
                            if (oldRoot != null) {
                                oldRoot.layoutBoundsProperty()
                                       .removeListener(rootBoundsListener);
                            }

                            if (newRoot != null) {
                                newRoot.layoutBoundsProperty()
                                       .addListener(rootBoundsListener);
                            }

                            oldRoot = newRoot;
                            updateDimensions();
                        }
                    }
                });
        scene.setRoot(new Group());
    }

    /**
     * Gets the observable, modifiable list of children which are placed in this
     * PopupWindow.
     *
     * @return the PopupWindow content
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected ObservableList<Node> getContent() {
        final Parent rootNode = getScene().getRoot();
        if (!(rootNode instanceof Group)) {
            throw new IllegalStateException(
                    "The content of the Popup can't be accessed");
        }

        return ((Group) rootNode).getChildren();
    }

    /**
     * The window which is the parent of this popup. All popups must have an
     * owner window.
     */
    private ReadOnlyObjectWrapper<Window> ownerWindow =
            new ReadOnlyObjectWrapper<Window>(this, "ownerWindow");
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
            new ReadOnlyObjectWrapper<Node>(this, "ownerNode");
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
     * @param scene
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
     * @defaultValue false
     */
    private BooleanProperty autoHide =
            new SimpleBooleanProperty(this, "autoHide");
    public final void setAutoHide(boolean value) { autoHide.set(value); }
    public final boolean isAutoHide() { return autoHide.get(); }
    public final BooleanProperty autoHideProperty() { return autoHide; }

    /**
     * Called after autoHide is run.
     */
    private ObjectProperty<EventHandler<Event>> onAutoHide =
            new SimpleObjectProperty<EventHandler<Event>>(this, "onAutoHide");
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
     */
    public void show(Window owner) {
        if (owner == null) throw new NullPointerException();

        // Update the owner field
        this.ownerWindow.set(owner);
        if (owner instanceof PopupWindow) {
            ((PopupWindow)owner).children.add(this);
        }

        // It is required that the root window exist and be visible to show the popup.
        Window rootWindow = getRootWindow(getOwnerWindow());
        if (rootWindow != null && rootWindow.isShowing()) {
            // We do show() first so that the width and height of the
            // popup window are initialized. This way the x,y location of the
            // popup calculated below uses the right width and height values for
            // its calculation. (fix for part of RT-10675).
            show();
            if (!isAutoFix()) {
                final Parent rootNode = getScene().getRoot();
                if (rootNode != null) {
                    final Bounds _bounds = rootNode.getLayoutBounds();
                    setX(this.getX() + _bounds.getMinX());
                    setY(this.getY() + _bounds.getMinY());
                }
            }
        }
    }

    /**
     * Shows the popup at the specified x,y location relative to the screen.
     * The popup is associated with the specified owner node. The {@code Window}
     * which contains the owner node at the time of the call becomes an owner
     * window of the displayed popup.
     * 
     * @param ownerNode The owner Node of the popup. It must not be null
     *        and must be associated with a Window.
     * @param screenX the x location in screen coordinates at which to
     *        show this PopupWindow.
     * @param screenY the y location in screen coordiates at which to
     *        show this PopupWindow.
     */
    public void show(Node ownerNode, double screenX, double screenY) {
        final Scene ownerNodeScene = ownerNode.getScene();
        if ((ownerNodeScene == null)
                || (ownerNodeScene.getWindow() == null)) {
            throw new IllegalStateException(
                    "The owner node needs to be associated with a window");
        }

        this.ownerNode.set(ownerNode);
        show(ownerNodeScene.getWindow(), screenX, screenY);
    }

    /**
     * Show the Popup at the specified x,y location relative to the screen
     * @param ownerWindow The owner of the popup. This must not be null.
     * @param screenX the x location in screen coordinates at which to
     *        show this PopupWindow.
     * @param screenY the y location in screen coordiates at which to
     *        show this PopupWindow.
     */
    public void show(Window ownerWindow, double screenX, double screenY) {
        setX(screenX);
        setY(screenY);
        show(ownerWindow);
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
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_visibleChanging(boolean visible) {
        super.impl_visibleChanging(visible);
        PerformanceTracker.logEvent("PopupWindow.storeVisible for [PopupWindow]");

        Toolkit toolkit = Toolkit.getToolkit();
        if (visible && (impl_peer == null)) {
            // Setup the peer
            impl_peer = toolkit.createTKPopupStage(StageStyle.TRANSPARENT, getOwnerWindow().impl_getPeer());
            peerListener = new PopupWindowPeerListener(PopupWindow.this);
        }
    }

    private Window focusGrabWindow;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_visibleChanged(boolean visible) {
        super.impl_visibleChanged(visible);
        if (!visible && (impl_peer != null)) {
            peerListener = null;
            impl_peer = null;
        }

        final Window ownerWindowValue = getOwnerWindow();
        if (visible) {
            startMonitorOwnerEvents(ownerWindowValue);
            // currently we consider popup window to be focused when it is
            // visible and its owner window is focused (we need to track
            // that through listener on owner window focused property)
            // a better solution would require some focus manager, which can
            // track focus state across multiple windows
            bindOwnerFocusedProperty(ownerWindowValue);
            setFocused(ownerWindowValue.isFocused());
            focusGrabWindow = increaseFocusGrabInRootWindow(ownerWindowValue);
            handleAutofixActivation(true, isAutoFix());
        } else {
            stopMonitorOwnerEvents(ownerWindowValue);
            unbindOwnerFocusedProperty(ownerWindowValue);
            setFocused(false);
            if (focusGrabWindow != null) {
                focusGrabWindow.impl_decreaseFocusGrabCounter();
                focusGrabWindow = null;
            }
            handleAutofixActivation(false, isAutoFix());
        }

        PerformanceTracker.logEvent("PopupWindow.storeVisible for [PopupWindow] finished");
    }

    private void updateDimensions() {
        final Parent rootNode = getScene().getRoot();
        if (rootNode != null) {
            final Bounds layoutBounds = rootNode.getLayoutBounds();

            // update popup dimensions
            setWidth(layoutBounds.getMaxX() - layoutBounds.getMinX());
            setHeight(layoutBounds.getMaxY() - layoutBounds.getMinY());
            // update transform
            rootNode.setTranslateX(-layoutBounds.getMinX());
            rootNode.setTranslateY(-layoutBounds.getMinY());
        }
   }

   /**
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

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void doAutoHide() {
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

    private Window increaseFocusGrabInRootWindow(final Window ownerWindow) {
        final Window rootWindow = getRootWindow(ownerWindow);
        if (rootWindow != null) {
            rootWindow.impl_increaseFocusGrabCounter();
        }

        return rootWindow;
    }

    private ChangeListener<Boolean> ownerFocusedListener;

    private void bindOwnerFocusedProperty(final Window ownerWindowValue) {
        ownerFocusedListener =
            new ChangeListener<Boolean>() {
                @Override
                public void changed(
                        ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    setFocused(newValue);
                }
            };
        ownerWindowValue.focusedProperty().addListener(ownerFocusedListener);
    }

    private void unbindOwnerFocusedProperty(final Window ownerWindowValue) {
        ownerWindowValue.focusedProperty().removeListener(ownerFocusedListener);
        ownerFocusedListener = null;
    }

    private boolean autofixActive;
    private AutofixHandler autofixHandler;
    private void handleAutofixActivation(final boolean visible,
                                         final boolean autofix) {
        final boolean newAutofixActive = visible && autofix;
        if (autofixActive != newAutofixActive) {
            autofixActive = newAutofixActive;
            if (newAutofixActive) {
                autofixHandler = new AutofixHandler();
                widthProperty().addListener(autofixHandler);
                heightProperty().addListener(autofixHandler);
                Screen.getScreens().addListener(autofixHandler);
                autofixHandler.adjustPosition();
            } else {
                widthProperty().removeListener(autofixHandler);
                heightProperty().removeListener(autofixHandler);
                Screen.getScreens().removeListener(autofixHandler);
                autofixHandler = null;
            }
        }
    }

    private final class AutofixHandler implements InvalidationListener {
        @Override
        public void invalidated(final Observable observable) {
            adjustPosition();
        }

        public void adjustPosition() {
            final Screen currentScreen =
                    Utils.getScreenForPoint(getX(), getY());
            final Rectangle2D screenBounds = currentScreen.getVisualBounds();
            double _x = Math.min(getX(), screenBounds.getMaxX() - getWidth());
            double _y = Math.min(getY(), screenBounds.getMaxY() - getHeight());
            _x = Math.max(_x, screenBounds.getMinX());
            _y = Math.max(_y, screenBounds.getMinY());
            setX(_x);
            setY(_y);
        }
    }
}
