/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.WindowEvent;
import com.sun.prism.impl.PrismSettings;

import java.lang.annotation.Native;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class Window {

    public static class EventHandler {
        public void handleWindowEvent(Window window, long time, int type) {
        }

        /**
         * Notifies a listener that the screen object for this Window instance
         * has been updated.
         *
         * Note that while the old and new screen objects may be different,
         * they can still represent the same physical screen. This can happen
         * if e.g. only a certain parameter of the screen has been updated such
         * as its scale factor.
         *
         * On some platforms when a window is moved to another physical screen
         * an app can receive this event twice. One representing the physical
         * screen change, and another - the display's parameters change. Note
         * that sending two events instead of just one is platform-specific.
         *
         * The event handler can use the {@link Screen#getNativeScreen} method
         * to determine if this is the same physical screen or not. If the
         * native system always creates new native screen instances, there's no
         * way for the app to distinguish between a real move to another screen
         * or jsut a parameters update event. Since this is a somewhat rare
         * event, an app is advised to always process it the same way.
         *
         * @see Window#getScreen
         */
        public void handleScreenChangedEvent(Window window, long time, Screen oldScreen, Screen newScreen) {
        }

        /**
         * Notifies the listener that the window level has changed. The Level should be one of
         * {@link com.sun.glass.ui.Window.Level#NORMAL}, {@link com.sun.glass.ui.Window.Level#FLOATING},
         * {@link com.sun.glass.ui.Window.Level#TOPMOST}.
         * @param level Level from {@link com.sun.glass.ui.Window.Level} class
         */
        public void handleLevelEvent(int level) {
        }
    }

    // Native object handle (HWND, or NSWindow*, etc.)
    private long ptr;

    // 'Delegate window' ptr. Used in e.g. the Full Screen mode.
    private volatile long delegatePtr = 0L;

    // window list
    static private final LinkedList<Window> visibleWindows = new LinkedList<Window>();
     // Return a list of all visible windows.  Note that on platforms without a native window manager,
     // this list will be sorted in proper z-order
    static public synchronized List<Window> getWindows() {
        Application.checkEventThread();
        return Collections.unmodifiableList(Window.visibleWindows);
    }

    static public List<Window> getWindowsClone() {
        Application.checkEventThread();
        return (List<Window>)visibleWindows.clone();
    }

    // used by Lens Native
    static protected void add(Window window) {
        visibleWindows.add(window);
    }

    static protected void addFirst(Window window) {
        visibleWindows.addFirst(window);
    }

    // used by Lens Native
    static protected void remove(Window window) {
        visibleWindows.remove(window);
    }

    // window style mask

    // visual kind: mutually exclusive
    public static final int UNTITLED        = 0;
    public static final int TITLED          = 1 << 0;
    public static final int TRANSPARENT     = 1 << 1;

    // functional type: mutually exclusive
    /**
     * Normal window.
     *
     * Usual top-level window.
     */
    public static final int NORMAL          = 0;
    /**
     * An utility window.
     *
     * Often used for floating toolbars. It has smaller than usual decorations
     * and doesn't display a taskbar button.
     */
    @Native public static final int UTILITY         = 1 << 2;
    /**
     * A popup window.
     *
     * Used to display popups (tooltips, popup menus, etc.) Note that by
     * default it may display a task-bar button. To hide it the window must be
     * owned.
     */
    @Native public static final int POPUP           = 1 << 3;

    // These affect window decorations as well as system menu actions,
    // so applicable to both decorated and undecorated windows
    @Native public static final int CLOSABLE        = 1 << 4;
    @Native public static final int MINIMIZABLE     = 1 << 5;
    @Native public static final int MAXIMIZABLE     = 1 << 6;

    /**
     * Indicates that the window trim will draw from right to left.
     */
    @Native public static final int RIGHT_TO_LEFT     = 1 << 7;

    /**
     * Indicates that a window will have a client area textured the same way as the platform decorations
     * and will not have a border between decorations and the client area.
     * This is supported not on all platforms, the client should check if the feature is supported by using
     * {@link com.sun.glass.ui.Application#supportsUnifiedWindows()}
     */
    @Native public static final int UNIFIED = 1 << 8;

    final static public class State {
        @Native public static final int NORMAL = 1;
        @Native public static final int MINIMIZED = 2;
        @Native public static final int MAXIMIZED = 3;
    }

    /**
     * Available window levels.
     *
     * Note that on some platforms both {@code FLOATING} and {@code TOPMOST}
     * may represent the same window level.
     *
     * @see #setLevel
     */
    public static final class Level {
        @Native private static final int _MIN = 1;

        /** Normal window level. */
        @Native public static final int NORMAL = 1;

        /** A window level that is above all other NORMAL windows. */
        @Native public static final int FLOATING = 2;

        /** A very topmost window level. May cover system UI elements such as dock, taskbar, etc. */
        @Native public static final int TOPMOST = 3;

        @Native private static final int _MAX = 3;
    }

    private final Window owner;
    private final long parent;
    private final int styleMask;
    private final boolean isDecorated;
    private boolean shouldStartUndecoratedMove = false;

    protected View view = null;
    protected Screen screen = null;
    private MenuBar menubar = null;
    private String title = "";
    private UndecoratedMoveResizeHelper helper = null;

    private int state = State.NORMAL;
    private int level = Level.NORMAL;
    protected int x = 0;
    protected int y = 0;
    protected int width = 0;
    protected int height = 0;
    private float alpha = 1.0f;
    protected float platformScaleX = 1.0f;
    protected float platformScaleY = 1.0f;
    private float outputScaleX = 1.0f;
    private float outputScaleY = 1.0f;
    private float renderScaleX = 1.0f;
    private float renderScaleY = 1.0f;
    private boolean appletMode = false;

    // This is a workaround for RT-15970: as for embedded windows we don't
    // receive any MOVE notifications from the native platform, we poll
    // the window location on screen from timer and post synthetic events
    // if it has changed
    private Timer embeddedLocationTimer = null;
    private int lastKnownEmbeddedX = 0;
    private int lastKnownEmbeddedY = 0;

    private volatile boolean isResizable = false;
    private volatile boolean isVisible = false;
    private volatile boolean isFocused = false;
    private volatile boolean isFocusable = true;
    private volatile boolean isModal = false;

    // Indicates how many times setEnabled(false) has been called.
    // A value of 0 means the window is enabled.
    private volatile int disableCount = 0;

    private int minimumWidth = 0, minimumHeight = 0;
    private int maximumWidth = Integer.MAX_VALUE, maximumHeight = Integer.MAX_VALUE;

    private EventHandler eventHandler;

    protected abstract long _createWindow(long ownerPtr, long screenPtr, int mask);
    protected Window(Window owner, Screen screen, int styleMask) {
        Application.checkEventThread();
        switch (styleMask & (TITLED | TRANSPARENT)) {
            case UNTITLED:
            case TITLED:
            case TRANSPARENT:
                break;
            default:
                throw new RuntimeException("The visual kind should be UNTITLED, TITLED, or TRANSPARENT, but not a combination of these");
        }
        switch (styleMask & (POPUP | UTILITY)) {
            case NORMAL:
            case POPUP:
            case UTILITY:
                break;
            default:
                throw new RuntimeException("The functional type should be NORMAL, POPUP, or UTILITY, but not a combination of these");
        }

        if (((styleMask & UNIFIED) != 0)
                && !Application.GetApplication().supportsUnifiedWindows()) {
           styleMask &= ~UNIFIED;
        }

        if (((styleMask & TRANSPARENT) != 0)
                && !Application.GetApplication().supportsTransparentWindows()) {
            styleMask &= ~TRANSPARENT;
        }


        this.owner = owner;
        this.parent = 0L;
        this.styleMask = styleMask;
        this.isDecorated = (this.styleMask & Window.TITLED) != 0;

        this.screen = screen != null ? screen : Screen.getMainScreen();
        if (PrismSettings.allowHiDPIScaling) {
            this.platformScaleX = this.screen.getPlatformScaleX();
            this.platformScaleY = this.screen.getPlatformScaleY();
            this.outputScaleX = this.screen.getRecommendedOutputScaleX();
            this.outputScaleY = this.screen.getRecommendedOutputScaleY();
        }

        this.ptr = _createWindow(owner != null ? owner.getNativeHandle() : 0L,
                this.screen.getNativeScreen(), this.styleMask);
        if (this.ptr == 0L) {
            throw new RuntimeException("could not create platform window");
        }
    }

    protected abstract long _createChildWindow(long parent);
    /**
     * Constructs a child window of the specified native parent window.
     */
    protected Window(long parent) {
        Application.checkEventThread();
        this.owner = null;
        this.parent = parent;
        this.styleMask = Window.UNTITLED;
        this.isDecorated = false;

        // Note: we can't always catch screen changes when parent is moved...
        this.screen = null; // should infer from the parent

        this.ptr = _createChildWindow(parent);
        if (this.ptr == 0L) {
            throw new RuntimeException("could not create platform window");
        }

        if (screen == null) {
            screen = Screen.getMainScreen(); // start with a default

            if (PrismSettings.allowHiDPIScaling) {
                this.platformScaleX = this.screen.getPlatformScaleX();
                this.platformScaleY = this.screen.getPlatformScaleY();
                this.outputScaleX = this.screen.getRecommendedOutputScaleX();
                this.outputScaleY = this.screen.getRecommendedOutputScaleY();
            }
        }
    }

    public boolean isClosed() {
        Application.checkEventThread();
        return this.ptr == 0L;
    }

    private void checkNotClosed() {
        if (this.ptr == 0L) {
            throw new IllegalStateException("The window has already been closed");
        }
    }

    protected abstract boolean _close(long ptr);
    public void close() {
        Application.checkEventThread();
        if (this.view != null) {
            if (this.ptr != 0L) {
                _setView(this.ptr, null);
            }
            this.view.setWindow(null);
            this.view.close();
            this.view = null;
        }
        if (this.ptr != 0L) {
            _close(this.ptr);
        }
    }

    private boolean isChild() {
        Application.checkEventThread();
        return this.parent != 0L;
    }

    /** This method returns "lowest-level" native window handle
     * (HWND on Windows, NSWindow on Mac, X11 Window handle on linux-gtk etc.)
     */
    public long getNativeWindow() {
        Application.checkEventThread();
        checkNotClosed();
        return this.delegatePtr != 0L ? this.delegatePtr : this.ptr;
    }

    /**
     * This method returns "higher-level" native window handle.
     * glass-mat-lib-gtk GtkWindow.java returns GtkWindow pointer for example.
     */
    public long getNativeHandle() {
        Application.checkEventThread();
        return this.delegatePtr != 0L ? this.delegatePtr : this.ptr;
    }

    /**
     * return the "raw' pointer needed by subclasses to pass to native routines
     * @return the native pointer.
     */
    public long getRawHandle() {
        return ptr;
    }

    public Window getOwner() {
        Application.checkEventThread();
        return this.owner;
    }

    public View getView() {
        Application.checkEventThread();
        return this.view;
    }

    protected abstract boolean _setView(long ptr, View view);
    public void setView(final View view) {
        Application.checkEventThread();
        checkNotClosed();
        View oldView = getView();
        if (oldView == view) {
            return;
        }

        if (oldView != null) {
            oldView.setWindow(null);
        }
        if (view != null) {
            Window host = view.getWindow();
            if (host != null) {
                host.setView(null);
            }
        }

        if (view != null && _setView(this.ptr, view)) {
            this.view = view;
            this.view.setWindow(this);
            if (this.isDecorated == false) {
                this.helper = new UndecoratedMoveResizeHelper();
            }
        } else {
            _setView(this.ptr, null);
            this.view = null;
        }
    }

    public Screen getScreen() {
        Application.checkEventThread();
        return this.screen;
    }

    protected void setScreen(Screen screen) {
        Application.checkEventThread();

        final Screen old = this.screen;
        this.screen = screen;

        if (this.eventHandler != null) {
            if ((old == null && this.screen != null) ||
                (old != null && !old.equals(this.screen))) {
                this.eventHandler.handleScreenChangedEvent(this, System.nanoTime(), old, this.screen);
            }
        }
    }

    public int getStyleMask() {
        Application.checkEventThread();
        return this.styleMask;
    }

    public MenuBar getMenuBar() {
        Application.checkEventThread();
        return this.menubar;
    }

    protected abstract boolean _setMenubar(long ptr, long menubarPtr);
    public void setMenuBar(final MenuBar menubar) {
        Application.checkEventThread();
        checkNotClosed();
        if (_setMenubar(this.ptr, menubar.getNativeMenu())) {
            this.menubar = menubar;
        }
    }

    public boolean isDecorated() {
        Application.checkEventThread();
        return this.isDecorated;
    }

    public boolean isMinimized() {
        Application.checkEventThread();
        return (this.state == State.MINIMIZED);
    }

    protected abstract boolean _minimize(long ptr, boolean minimize);
    public boolean minimize(final boolean minimize) {
        Application.checkEventThread();
        checkNotClosed();
        _minimize(this.ptr, minimize);
        //XXX: this is synchronous? On X11 this may not work
        return isMinimized();
    }

    public boolean isMaximized() {
        Application.checkEventThread();
        return (this.state == State.MAXIMIZED);
    }

    protected abstract boolean _maximize(long ptr, boolean maximize, boolean wasMaximized);
    public boolean maximize(final boolean maximize) {
        Application.checkEventThread();
        checkNotClosed();
        _maximize(ptr, maximize, isMaximized());
        return isMaximized();
    }

    protected void notifyScaleChanged(float platformScaleX, float platformScaleY,
                                      float outputScaleX, float outputScaleY)
    {
        if (!PrismSettings.allowHiDPIScaling) return;
        this.platformScaleX = platformScaleX;
        this.platformScaleY = platformScaleY;
        this.outputScaleX = outputScaleX;
        this.outputScaleY = outputScaleY;
        notifyRescale();
    }

    /**
     * Return the horizontal scale used to communicate window locations,
     * sizes, and event coordinates to/from the platform.
     * @return the horizontal platform scaling for screen locations
     */
    public final float getPlatformScaleX() {
        return platformScaleX;
    }

    /**
     * Return the vertical scale used to communicate window locations,
     * sizes, and event coordinates to/from the platform.
     * @return the vertical platform scaling for screen locations
     */
    public final float getPlatformScaleY() {
        return platformScaleY;
    }

    public void setRenderScaleX(float renderScaleX) {
        if (!PrismSettings.allowHiDPIScaling) return;
        this.renderScaleX = renderScaleX;
    }

    public void setRenderScaleY(float renderScaleY) {
        if (!PrismSettings.allowHiDPIScaling) return;
        this.renderScaleY = renderScaleY;
    }

    /**
     * Return the horizontal scale used for rendering the back buffer.
     * @return the horizontal scaling for rendering
     */
    public final float getRenderScaleX() {
        return renderScaleX;
    }

    /**
     * Return the vertical scale used for rendering to the back buffer.
     * @return the vertical scaling for rendering
     */
    public final float getRenderScaleY() {
        return renderScaleY;
    }

    public float getOutputScaleX() {
        return outputScaleX;
    }

    public float getOutputScaleY() {
        return outputScaleY;
    }

    protected abstract int _getEmbeddedX(long ptr);
    protected abstract int _getEmbeddedY(long ptr);

    private void checkScreenLocation() {
        this.x = _getEmbeddedX(ptr);
        this.y = _getEmbeddedY(ptr);
        if ((this.x != lastKnownEmbeddedX) || (this.y != lastKnownEmbeddedY)) {
            lastKnownEmbeddedX = this.x;
            lastKnownEmbeddedY = this.y;
            handleWindowEvent(System.nanoTime(), WindowEvent.MOVE);
        }
    }

    public int getX() {
        Application.checkEventThread();
        return this.x;
    }

    public int getY() {
        Application.checkEventThread();
        return this.y;
    }

    public int getWidth() {
        Application.checkEventThread();
        return this.width;
    }

    public int getHeight() {
        Application.checkEventThread();
        return this.height;
    }

    protected abstract void _setBounds(long ptr, int x, int y, boolean xSet, boolean ySet,
                                       int w, int h, int cw, int ch,
                                       float xGravity, float yGravity);

    /**
     * Sets the window bounds to the specified values.
     *
     * Gravity values specify how to correct window location if only its size
     * changes (for example when stage decorations are added). User initiated
     * resizing should be ignored and must not influence window location through
     * this mechanism.
     *
     * The corresponding correction formulas are:
     *
     * {@code x -= xGravity * deltaW}
     * {@code y -= yGravity * deltaH}
     *
     * @param x the new window horizontal position, ignored if xSet is set to
     *          false
     * @param y the new window vertical position, ignored if ySet is set to
     *          false
     * @param xSet indicates whether the x parameter is valid
     * @param ySet indicates whether the y parameter is valid
     * @param w the new window width, ignored if set to -1
     * @param h the new window height, ignored if set to -1
     * @param cw the new window content width, ignored if set to -1
     * @param ch the new window content height, ignored if set to -1
     * @param xGravity the xGravity coefficient
     * @param yGravity the yGravity coefficient
     */
    public void setBounds(float x, float y, boolean xSet, boolean ySet,
                          float w, float h, float cw, float ch,
                          float xGravity, float yGravity)
    {
        Application.checkEventThread();
        checkNotClosed();
        float pScaleX = platformScaleX;
        float pScaleY = platformScaleY;
        int px = screen.getPlatformX() + Math.round((x - screen.getX()) * pScaleX);
        int py = screen.getPlatformY() + Math.round((y - screen.getY()) * pScaleY);
        int pw = (int) (w > 0 ? Math.ceil(w * pScaleX) : w);
        int ph = (int) (h > 0 ? Math.ceil(h * pScaleY) : h);
        int pcw = (int) (cw > 0 ? Math.ceil(cw * pScaleX) : cw);
        int pch = (int) (ch > 0 ? Math.ceil(ch * pScaleY) : ch);
        _setBounds(ptr, px, py, xSet, ySet, pw, ph, pcw, pch, xGravity, yGravity);
    }

    public void setPosition(int x, int y) {
        Application.checkEventThread();
        checkNotClosed();
        _setBounds(ptr, x, y, true, true, 0, 0, 0, 0, 0, 0);
    }

    public void setSize(int w, int h) {
        Application.checkEventThread();
        checkNotClosed();
        _setBounds(ptr, 0, 0, false, false, w, h, 0, 0, 0, 0);
    }

    public void setContentSize(int cw, int ch) {
        Application.checkEventThread();
        checkNotClosed();
        _setBounds(ptr, 0, 0, false, false, 0, 0, cw, ch, 0, 0);
    }

    public boolean isVisible() {
        Application.checkEventThread();
        return this.isVisible;
    }

    /**
     * Generates a ViewEvent.MOVE aka insets (might have) changed.
     */
    private void synthesizeViewMoveEvent() {
        final View view = getView();
        if (view != null) {
            view.notifyView(com.sun.glass.events.ViewEvent.MOVE);
        }
    }

    protected abstract boolean _setVisible(long ptr, boolean visible);
    public void setVisible(final boolean visible) {
        Application.checkEventThread();
        if (this.isVisible != visible) {
            if (!visible) {
                if (getView() != null) {
                    getView().setVisible(visible);
                }
                // Avoid native call if the window has been closed already
                if (this.ptr != 0L) {
                    this.isVisible = _setVisible(this.ptr, visible);
                } else {
                    this.isVisible = visible;
                }
                remove(this);
                if (parent != 0) {
                    embeddedLocationTimer.stop();
                }
            } else {
                checkNotClosed();
                this.isVisible = _setVisible(this.ptr, visible);

                if (getView() != null) {
                    getView().setVisible(this.isVisible);
                }
                add(this);
                if (parent != 0) {
                    final Runnable checkRunnable = () -> checkScreenLocation();
                    final Runnable timerRunnable = () -> Application.invokeLater(checkRunnable);
                    embeddedLocationTimer =
                           Application.GetApplication().createTimer(timerRunnable);
                    embeddedLocationTimer.start(16);
                }

                synthesizeViewMoveEvent();
            }
        }
    }

    protected abstract boolean _setResizable(long ptr, boolean resizable);
    public boolean setResizable(final boolean resizable) {
        Application.checkEventThread();
        checkNotClosed();
        if (this.isResizable != resizable) {
            if (_setResizable(this.ptr, resizable)) {
                this.isResizable = resizable;
                synthesizeViewMoveEvent();
            }
        }
        return isResizable;
    }

    public boolean isResizable() {
        Application.checkEventThread();
        return this.isResizable;
    }

    public boolean isUnifiedWindow() {
        //The UNIFIED flag is set only if it is supported
        return (this.styleMask & Window.UNIFIED) != 0;
    }

    public boolean isTransparentWindow() {
        //The TRANSPARENT flag is set only if it is supported
        return (this.styleMask & Window.TRANSPARENT) != 0;
    }

    private static volatile Window focusedWindow = null;
    public static Window getFocusedWindow() {
        Application.checkEventThread();
        return Window.focusedWindow;
    }

    private static void setFocusedWindow(final Window window) {
        Window.focusedWindow = window;
    }

    public boolean isFocused() {
        Application.checkEventThread();
        return this.isFocused;
    }

    protected abstract boolean _requestFocus(long ptr, int event);
    /**
     * Requests or resigns focus on this window.
     *
     * If this is a top-level window (owned or not), then the only possible
     * value for the {@code event} argument is WindowEvent.FOCUS_GAINED.
     * Otherwise, if the window is a child window, the argument may be
     * WindowEvent.FOCUS_LOST, FOCUS_GAINED, FOCUS_GAINED_FORWARD, or
     * FOCUS_GAINED_BACKWARD.
     *
     * @param event one of WindowEvent.FOCUS_LOST, FOCUS_GAINED, FOCUS_GAINED_FORWARD, FOCUS_GAINED_BACKWARD
     *
     * @throws IllegalArgumentException if the argument value is invalid for this window
     *
     * @return {@code true} if the operation succeeded
     */
    public boolean requestFocus(int event) {
        Application.checkEventThread();
        checkNotClosed();

        if (!isChild() && event != WindowEvent.FOCUS_GAINED) {
            throw new IllegalArgumentException("Invalid focus event ID for top-level window");
        }

        if (isChild() && (event < WindowEvent._FOCUS_MIN || event > WindowEvent._FOCUS_MAX)) {
            throw new IllegalArgumentException("Invalid focus event ID for child window");
        }

        if (event == WindowEvent.FOCUS_LOST && !isFocused()) {
            // Already unfocused, nothing to do
            return true;
        }

        // At this point either A) the user requests focus for a focused or unfocused window,
        // or B) the window is focused and the user requests FOCUS_LOST
        if (!this.isFocusable) {
            // It's obviously A). Fail.
            return false;
        }

        return _requestFocus(this.ptr, event);
    }

    public boolean requestFocus() {
        Application.checkEventThread();
        return requestFocus(WindowEvent.FOCUS_GAINED);
    }

    protected abstract void _setFocusable(long ptr, boolean isFocusable);
    /**
     * Sets whether this window is focusable.
     *
     * Clicking an unfocusable window doesn't activate it.
     */
    public void setFocusable(final boolean isFocusable) {
        Application.checkEventThread();
        checkNotClosed();
        this.isFocusable = isFocusable;
        if (isEnabled()) {
            _setFocusable(this.ptr, isFocusable);
        }
    }

    protected abstract boolean _grabFocus(long ptr);
    protected abstract void _ungrabFocus(long ptr);
    /**
     * Grabs focus on this window.
     *
     * All mouse clicks that occur in this window's client area or client-areas
     * of any of its unfocusable owned windows are delivered as usual. Whenever
     * a click occurs on another app's window (not related via the ownership
     * relation with this one, or a focusable owned window), or on non-client
     * area of any window (titlebar, etc.), or any third-party app's window, or
     * native OS GUI (e.g. a taskbar), the grab is automatically reset, and the
     * window that held the grab receives the FOCUS_UNGRAB event.
     *
     * Note that for this functionality to work correctly, the window must have
     * a focus upon calling this method. All owned popup windows that should be
     * operable during the grabbed focus state (e.g. nested popup menus) must
     * be unfocusable (see {@link #setFocusable}). Clicking a focusable owned
     * window will reset the grab due to a focus transfer.
     *
     * The click that occurs in another window and causes resetting of the grab
     * may or may not be delivered to that other window depending on the native
     * OS behavior.
     *
     * If any of the application's windows already holds the grab, it is reset
     * prior to grabbing the focus for this window. The method may be called
     * multiple times for one window. Subsequent calls do not affect the grab
     * status unless it is reset between the calls, in which case the focus
     * is grabbed again.
     *
     * Note that grabbing the focus on an application window may prevent
     * delivering certain events to other applications until the grab is reset.
     * Therefore, if the application has finished showing popup windows based
     * on a user action (e.g. clicking a menu item), and doesn't require the
     * grab any more, it should call the {@link #ungrabFocus} method. The
     * FOCUS_UNGRAB event signals that the grab has been reset.
     *
     * A user event handler associated with a menu item must be invoked after
     * resetting the grab. Otherwise, if a developer debugs the application and
     * has installed a breakpoint in the event handler, the debugger may become
     * unoperable due to events blocking for other applications on some
     * platforms.
     *
     * @return {@code true} if the operation is successful
     * @throws IllegalStateException if the window isn't focused currently
     */
    public boolean grabFocus() {
        Application.checkEventThread();
        checkNotClosed();

        if (!isFocused()) {
            throw new IllegalStateException("The window must be focused when calling grabFocus()");
        }

        return _grabFocus(this.ptr);
    }

    /**
     * Manually ungrabs focus grabbed on this window previously.
     *
     * This method resets the grab, and forces sending of the FOCUS_UNGRAB
     * event. It should be used when popup windows (such as menus) should be
     * dismissed manually, e.g. when a user clicks a menu item which usually
     * causes the menus to hide.
     *
     * @see #grabFocus
     */
    public void ungrabFocus() {
        Application.checkEventThread();
        checkNotClosed();
        _ungrabFocus(this.ptr);
    }

    public String getTitle() {
        Application.checkEventThread();
        return this.title;
    }

    protected abstract boolean _setTitle(long ptr, String title);
    public void setTitle(String title) {
        Application.checkEventThread();
        checkNotClosed();
        if (title == null) {
            title = "";
        }
        if (!title.equals(this.title)) {
            if (_setTitle(this.ptr, title)) {
                this.title = title;
            }
        }
    }

    protected abstract void _setLevel(long ptr, int level);
    /**
     * Set the level of this window in the z-order.
     *
     * @param level one of the constants from {@link Window.Level}
     * @see Window.Level
     */
    public void setLevel(final int level) {
        Application.checkEventThread();
        checkNotClosed();
        if (level < Level._MIN || level > Level._MAX) {
            throw new IllegalArgumentException("Level should be in the range [" + Level._MIN + ".." + Level._MAX + "]");
        }
        if (this.level != level) {
            _setLevel(this.ptr, level);
            this.level = level;
        }
    }

    public int getLevel() {
        Application.checkEventThread();
        return this.level;
    }

    private boolean isInFullscreen() {
        final View view = getView();
        return view == null ? false : view.isInFullscreen();
    }

    // Invoked from the View class before sending FULLSCREEN_ to the View.EventHandler
    void notifyFullscreen(boolean entered) {
        final float alpha = getAlpha();
        if (alpha < 1f) {
            if (entered) {
                // Reset alpha at native level
                _setAlpha(this.ptr, 1f);
            } else {
                // restore the current opacity level
                setAlpha(alpha);
            }
        }
    }

    protected abstract void _setAlpha(long ptr, float alpha);
    /**
     * Sets the uniform translucency level for this window.
     *
     * In the full screen mode the native window is always fully opaque.
     * The requested opacity level is applied upon exiting the full screen
     * mode only.
     *
     * @param alpha a value in the range [0..1f] (transparent..fully-opaque)
     */
    public void setAlpha(final float alpha) {
        Application.checkEventThread();
        checkNotClosed();
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("Alpha should be in the range [0f..1f]");
        }

        this.alpha = alpha;

        if (alpha < 1f && isInFullscreen()) {
            return;
        }

        _setAlpha(this.ptr, this.alpha);
    }

    public float getAlpha() {
        Application.checkEventThread();
        return this.alpha;
    }

    public boolean getAppletMode() {
        return appletMode;
    }

    public void setAppletMode(boolean appletMode) {
        this.appletMode = appletMode;
    }

    protected abstract boolean _setBackground(long ptr, float r, float g, float b);
    /**
     * Set the background of the window.
     *
     * In most cases the View covers the whole window, so the background color
     * of the window is never seen by the user. However, a window w/o a view
     * does display the background color in its content area.
     *
     * On some platforms setting the background color may produce flickering
     * when painting the content area of the View (even though the View covers
     * the whole window).  Therefore it is recommended to set the background
     * color to windows w/o views only.
     */
    public boolean setBackground(final float r, final float g, final float b) {
        Application.checkEventThread();
        checkNotClosed();
        return _setBackground(this.ptr, r, g, b);
    }

    public boolean isEnabled() {
        Application.checkEventThread();
        return this.disableCount == 0;
    }

    protected abstract void _setEnabled(long ptr, boolean enabled);
    /**
     * Enables or disables the window.
     *
     * A disabled window is unfocusable by definition.
     * Also, key or mouse events aren't generated for disabled windows.
     *
     * When a user tries to activate a disabled window, or the window gets
     * accidentally brought to the top of the stacking order, the window
     * generates the FOCUS_DISABLED window event. A Glass client should react
     * to this event and bring the currently active modal blocker of the
     * disabled window to top by calling blocker's minimize(false), toFront(),
     * and requestFocus() methods. It may also 'blink' the blocker window to
     * further attract user's attention.
     *
     * It's strongly recommended to process the FOCUS_DISABLED event
     * synchronously and as fast as possible to avoid any possible visual and
     * behavioral artifacts. Note that a disabled window may by no means gain
     * the input focus. The purpose of this event is to make sure that the
     * current modal blocker window is always visible to the user, and the user
     * understands why he can't interact with a disabled window.
     *
     * The method supports nested calls. If you disable the window twice
     * with two calls to setEnabled(false), you must call setEnabled(true)
     * twice as well in order to enable it afterwards. This is to support
     * 'nested' modal dialogs when one modal dialog opens another one.
     */
    public void setEnabled(boolean enabled) {
        Application.checkEventThread();
        checkNotClosed();
        if (!enabled) {
            if (++this.disableCount > 1) {
                // already disabled
                return;
            }
        } else {
            if (this.disableCount == 0) {
                //should report a warning about an extra enable call ?
                return;
            }
            if (--this.disableCount > 0) {
                // not yet enabled
                return;
            }
        }

        //TODO: on Windows _setFocusable(this.ptr, isEnabled() ? this.isFocusable : false);
        _setEnabled(this.ptr, isEnabled());
    }

    public int getMinimumWidth() {
        Application.checkEventThread();
        return this.minimumWidth;
    }

    public int getMinimumHeight() {
        Application.checkEventThread();
        return this.minimumHeight;
    }

    public int getMaximumWidth() {
        Application.checkEventThread();
        return this.maximumWidth;
    }

    public int getMaximumHeight() {
        Application.checkEventThread();
        return this.maximumHeight;
    }

    protected abstract boolean _setMinimumSize(long ptr, int width, int height);
    /**
     * Sets the minimum size for this window.
     * A value of zero indicates no restriction.
     * If the native platform is unable to apply the constraints,
     * the values returned by getMinimumWidth()/Height() won't change.
     *
     * @throws IllegalArgumentException if width or height < 0
     */
    public void setMinimumSize(int width, int height) {
        Application.checkEventThread();
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("The width and height must be >= 0. Got: width=" + width + "; height=" + height);
        }
        checkNotClosed();
        if (_setMinimumSize(this.ptr, width, height)) {
            this.minimumWidth = width;
            this.minimumHeight = height;
        }
    }

    protected abstract boolean _setMaximumSize(long ptr, int width, int height);
    /**
     * Sets the maximum size for this window.
     * A value of {@code Integer.MAX_VALUE} indicates no restriction.
     * If the native platform is unable to apply the constraints,
     * the values returned by getMaximumWidth()/Height() won't change.
     *
     * @throws IllegalArgumentException if width or height < 0
     */
    public void setMaximumSize(int width, int height) {
        Application.checkEventThread();
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("The width and height must be >= 0. Got: width=" + width + "; height=" + height);
        }
        checkNotClosed();
        if (_setMaximumSize(this.ptr,
                    // for easier handling in native:
                    width == Integer.MAX_VALUE ? -1 : width,
                    height == Integer.MAX_VALUE ? -1 : height))
        {
            this.maximumWidth = width;
            this.maximumHeight = height;
        }
    }


    protected abstract void _setIcon(long ptr, Pixels pixels);

    // In the future we may want to pass a collection of Pixels, so that
    // the native platform could pick up the icon with the best dimensions
    public void setIcon(final Pixels pixels) {
        Application.checkEventThread();
        checkNotClosed();
        _setIcon(this.ptr, pixels);
    }

    protected abstract void _setCursor(long ptr, Cursor cursor);

    /**
     * Sets given cursor as the cursor for this window.
     * If the cursor is NONE, it is automatically hidden,
     * otherwise it is automatically shown.
     * @see Cursor#setVisible(boolean)
     */
    public void setCursor(Cursor cursor) {
        Application.checkEventThread();
        _setCursor(this.ptr, cursor);
    }

    protected abstract void _toFront(long ptr);
    /**
     * Bring the window to front in the z-order.
     * This method DOES NOT activate the window. To make it active use
     * the requestFocus() method right after calling toFront().
     */
    public void toFront() {
        Application.checkEventThread();
        checkNotClosed();
        _toFront(ptr);
    }

    protected abstract void _toBack(long ptr);
    /**
     * Send the window to the bottom of the stacking order.
     * This method may or may not de-focus this window
     * depending on the native platform. To make sure some other
     * window is activated, call requestFocus() on that other window.
     */
    public void toBack() {
        Application.checkEventThread();
        checkNotClosed();
        _toBack(this.ptr);
    }

    // *****************************************************
    // modality (prototype using native platform feature)
    // *****************************************************
    protected abstract void _enterModal(long ptr);
    /**
     * Enter modal state blocking everything except our window.
     */
    public void enterModal() {
        checkNotClosed();
        if (this.isModal == false) {
            this.isModal = true;
            _enterModal(this.ptr);
        }
    }

    protected abstract void _enterModalWithWindow(long dialog, long window);
    /**
     * Enter modal state only blocking the given window.
     * On Mac OS X this is done using a dialog sheet.
     */
    public void enterModal(final Window window) {
        checkNotClosed();
        if (this.isModal == false) {
            this.isModal = true;
            _enterModalWithWindow(this.ptr, window.getNativeHandle());
        }
    }

    protected abstract void _exitModal(long ptr);
    public void exitModal() {
        checkNotClosed();
        if (this.isModal == true) {
            _exitModal(this.ptr);
            this.isModal = false;
        }
    }

    public boolean isModal() {
        return this.isModal;
    }

    /** Only used on Mac when run inside a plugin */
    public void dispatchNpapiEvent(Map eventInfo) {
        Application.checkEventThread();
        throw new RuntimeException("This operation is not supported on this platform");
    }

    public EventHandler getEventHandler() {
        Application.checkEventThread();
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        Application.checkEventThread();
        this.eventHandler = eventHandler;
    }

    /**
     * Enables unconditional start of window move operation when
     * mouse is dragged in the client area.
     */
    public void setShouldStartUndecoratedMove(boolean v) {
        Application.checkEventThread();
        this.shouldStartUndecoratedMove = v;
    }

    // *****************************************************
    // notification callbacks
    // *****************************************************
    protected void notifyClose() {
        handleWindowEvent(System.nanoTime(), WindowEvent.CLOSE);
    }

    protected void notifyDestroy() {
        // Mac is known to send multiple WillClose notifications for some reason
        if (this.ptr == 0) {
            return;
        }

        handleWindowEvent(System.nanoTime(), WindowEvent.DESTROY);

        this.ptr = 0;

        // Do this after setting ptr to 0 to avoid a call to _setVisible()
        setVisible(false);
    }

    protected void notifyMove(final int x, final int y) {
        this.x = x;
        this.y = y;
        handleWindowEvent(System.nanoTime(), WindowEvent.MOVE);
    }

    protected void notifyRescale() {
        handleWindowEvent(System.nanoTime(), WindowEvent.RESCALE);
    }

    protected void notifyMoveToAnotherScreen(Screen newScreen) {
        setScreen(newScreen);
    }

    protected void setState(int state) {
        this.state = state;
    }

    /**
     * type values:
     *   - WindowEvent.RESIZE
     *   - WindowEvent.MINIMIZE
     *   - WindowEvent.MAXIMIZE
     *   - WindowEvent.RESTORE
     */
    protected void notifyResize(final int type, final int width, final int height) {
        if (type == WindowEvent.MINIMIZE) {
            this.state = State.MINIMIZED;
        } else {
            if (type == WindowEvent.MAXIMIZE) {
                this.state = State.MAXIMIZED;
            } else { // WindowEvent.RESIZE or WindowEvent.RESTORE
                this.state = State.NORMAL;
            }
            this.width = width;
            this.height = height;

            // update moveRect/resizeRect
            if (this.helper != null){
                this.helper.updateRectangles();
            }
        }
        handleWindowEvent(System.nanoTime(), type);

        /*
         * Send RESIZE notification as MAXIMIZE and RESTORE change the window size
         */
        if (type == WindowEvent.MAXIMIZE || type == WindowEvent.RESTORE) {
            handleWindowEvent(System.nanoTime(), WindowEvent.RESIZE);
        }
    }

    protected void notifyFocus(int event) {
        final boolean focused = event != WindowEvent.FOCUS_LOST;

        if (this.isFocused != focused) {
            this.isFocused = focused;
            if (this.isFocused) {
                setFocusedWindow(this);
            } else {
                setFocusedWindow(null);
            }
            handleWindowEvent(System.nanoTime(), event);
        }
    }

    protected void notifyFocusDisabled() {
        handleWindowEvent(System.nanoTime(), WindowEvent.FOCUS_DISABLED);
    }

    protected void notifyFocusUngrab() {
        handleWindowEvent(System.nanoTime(), WindowEvent.FOCUS_UNGRAB);
    }

    protected void notifyDelegatePtr(long ptr) {
        this.delegatePtr = ptr;
    }

    // *****************************************************
    // window event handlers
    // *****************************************************
    protected void handleWindowEvent(long time, int type) {
        if (this.eventHandler != null) {
            this.eventHandler.handleWindowEvent(this, time, type);
        }
    }

    // *****************************************************
    // programmatical move/resize
    // *****************************************************
    /** Sets "programmatical move" rectangle.
     * The rectangle is measured from top of the View:
     * width is View.width, height is size.
     *
     * throws RuntimeException for decorated window.
     */
    public void setUndecoratedMoveRectangle(int size) {
        Application.checkEventThread();
        if (this.isDecorated == true) {
            //throw new RuntimeException("setUndecoratedMoveRectangle is only valid for Undecorated Window");
            System.err.println("Glass Window.setUndecoratedMoveRectangle is only valid for Undecorated Window. In the future this will be hard error.");
            Thread.dumpStack();
            return;
        }

        if (this.helper != null) {
            this.helper.setMoveRectangle(size);
        }
    }
    /** The method called only for undecorated windows
     * x, y: mouse coordinates (in View space).
     *
     * throws RuntimeException for decorated window.
     */
    public boolean shouldStartUndecoratedMove(final int x, final int y) {
        Application.checkEventThread();
        if (this.shouldStartUndecoratedMove == true) {
            return true;
        }
        if (this.isDecorated == true) {
            return false;
        }

        if (this.helper != null) {
            return this.helper.shouldStartMove(x, y);
        } else {
            return false;
        }
    }

    /** Sets "programmatical resize" rectangle.
     * The rectangle is measured from top of the View:
     * width is View.width, height is size.
     *
     * throws RuntimeException for decorated window.
     */
    public void setUndecoratedResizeRectangle(int size) {
        Application.checkEventThread();
        if ((this.isDecorated == true) || (this.isResizable == false)) {
            //throw new RuntimeException("setUndecoratedMoveRectangle is only valid for Undecorated Resizable Window");
            System.err.println("Glass Window.setUndecoratedResizeRectangle is only valid for Undecorated Resizable Window. In the future this will be hard error.");
            Thread.dumpStack();
            return;
        }

        if (this.helper != null) {
            this.helper.setResizeRectangle(size);
        }
    }

    /** The method called only for undecorated windows
     * x, y: mouse coordinates (in View space).
     *
     * throws RuntimeException for decorated window.
     */
    public boolean shouldStartUndecoratedResize(final int x, final int y) {
        Application.checkEventThread();
        if ((this.isDecorated == true) || (this.isResizable == false)) {
            return false;
        }

        if (this.helper != null) {
            return this.helper.shouldStartResize(x, y);
        }  else {
            return false;
        }
    }

    /** Mouse event handler for processing programmatical resize/move
     * (for undecorated windows only).
     * Must be called by View.
     * x & y are View coordinates.
     * NOTE: it's package private!
     * @return true if the event is processed by the window,
     *         false if it has to be delivered to the app
     */
    boolean handleMouseEvent(int type, int button, int x, int y, int xAbs, int yAbs) {
        if (this.isDecorated == false) {
            return this.helper.handleMouseEvent(type, button, x, y, xAbs, yAbs);
        }
        return false;
    }

    @Override
    public String toString() {
        Application.checkEventThread();
        return  "Window:"+"\n"
                + "    ptr: " + getNativeWindow() + "\n"
                + "    screen ptr: " + (screen != null ? screen.getNativeScreen() : "null") + "\n"
                + "    isDecorated: " + isDecorated() + "\n"
                + "    title: " + getTitle() + "\n"
                + "    visible: " + isVisible() + "\n"
                + "    focused: " + isFocused() + "\n"
                + "    modal: " + isModal() + "\n"
                + "    state: " + state + "\n"
                + "    x: " + getX() + ", y: " + getY() + ", w: " + getWidth() + ", h: " + getHeight() + "\n"
                + "";
    }

    // "programmical" move/resize support for undecorated windows

    static private class TrackingRectangle {
        int size = 0;
        int x = 0, y = 0, width = 0, height = 0;
        boolean contains(final int x, final int y) {
            return ((size > 0) &&
                    (x >= this.x) && (x < (this.x + this.width)) &&
                        (y >= this.y) && (y < (this.y + this.height)));
        }
    }

    protected void notifyLevelChanged(int level) {
        this.level = level;
        if (this.eventHandler != null) {
            this.eventHandler.handleLevelEvent(level);
        }
    }

    private class UndecoratedMoveResizeHelper {
        TrackingRectangle moveRect = null;
        TrackingRectangle resizeRect = null;

        boolean inMove = false;         // we are in "move" mode
        boolean inResize = false;       // we are in "resize" mode

        int startMouseX, startMouseY;   // start mouse coords
        int startX, startY;             // start window location (for move)
        int startWidth, startHeight;    // start window size (for resize)

        UndecoratedMoveResizeHelper() {
            this.moveRect = new TrackingRectangle();
            this.resizeRect = new TrackingRectangle();
        }

        void setMoveRectangle(final int size) {
            this.moveRect.size = size;

            this.moveRect.x = 0;
            this.moveRect.y = 0;
            this.moveRect.width = getWidth();
            this.moveRect.height = this.moveRect.size;
        }

        boolean shouldStartMove(final int x, final int y) {
            return this.moveRect.contains(x, y);
        }

        boolean inMove() {
            return this.inMove;
        }

        void startMove(final int x, final int y) {
            this.inMove = true;

            this.startMouseX = x;
            this.startMouseY = y;

            this.startX = getX();
            this.startY = getY();
        }

        void deltaMove(final int x, final int y) {
            int deltaX = x - this.startMouseX;
            int deltaY = y - this.startMouseY;

            setPosition(this.startX + deltaX, this.startY + deltaY);
        }

        void stopMove() {
            this.inMove = false;
        }

        void setResizeRectangle(final int size) {
            this.resizeRect.size = size;

            // set the rect (bottom right corner of the Window)
            this.resizeRect.x = getWidth() - this.resizeRect.size;
            this.resizeRect.y = getHeight() - this.resizeRect.size;
            this.resizeRect.width = this.resizeRect.size;
            this.resizeRect.height = this.resizeRect.size;
        }

        boolean shouldStartResize(final int x, final int y) {
            return this.resizeRect.contains(x, y);
        }

        boolean inResize() {
            return this.inResize;
        }

        void startResize(final int x, final int y) {
            this.inResize = true;

            this.startMouseX = x;
            this.startMouseY = y;

            this.startWidth = getWidth();
            this.startHeight = getHeight();
        }

        void deltaResize(final int x, final int y) {
            int deltaX = x - this.startMouseX;
            int deltaY = y - this.startMouseY;

            setSize(this.startWidth + deltaX, this.startHeight + deltaY);
        }

        protected void stopResize() {
            this.inResize = false;
        }

        void updateRectangles() {
            if (this.moveRect.size > 0) {
                setMoveRectangle(this.moveRect.size);
            }
            if (this.resizeRect.size > 0) {
                setResizeRectangle(this.resizeRect.size);
            }
        }

        boolean handleMouseEvent(final int type, final int button, final int x, final int y, final int xAbs, final int yAbs) {
            switch (type) {
                case MouseEvent.DOWN:
                    if (button == MouseEvent.BUTTON_LEFT) {
                        if (shouldStartUndecoratedMove(x, y) == true) {
                            startMove(xAbs, yAbs);
                            return true;
                        } else if (shouldStartUndecoratedResize(x, y) == true) {
                            startResize(xAbs, yAbs);
                            return true;
                        }
                    }
                    break;

                case MouseEvent.MOVE:
                case MouseEvent.DRAG:
                    if (inMove() == true) {
                        deltaMove(xAbs, yAbs);
                        return true;
                    } else if (inResize() == true) {
                        deltaResize(xAbs, yAbs);
                        return true;
                    }
                    break;

                case MouseEvent.UP:
                    boolean wasProcessed = inMove() || inResize();
                    stopResize();
                    stopMove();
                    return wasProcessed;
            }
            return false;
        }
    }

    /**
     * Requests text input in form of native keyboard for text component
     * contained by this Window. Native text input component is drawn on the place
     * of JavaFX component to cover it completely and to provide native text editing
     * techniques. Any change of text is immediately reflected in JavaFX text component.
     *
     * @param text text to be shown in the native text input component
     * @param type type of text input component @see com.sun.javafx.scene.control.behavior.TextInputTypes
     * @param width width of JavaFX text input component
     * @param height height of JavaFX text input component
     * @param M standard transformation matrix for drawing the native text component derived from JavaFX component
     */
    public void requestInput(String text, int type, double width, double height,
                                double Mxx, double Mxy, double Mxz, double Mxt,
                                double Myx, double Myy, double Myz, double Myt,
                                double Mzx, double Mzy, double Mzz, double Mzt) {
        Application.checkEventThread();
        _requestInput(this.ptr, text, type, width, height,
                        Mxx, Mxy, Mxz, Mxt,
                        Myx, Myy, Myz, Myt,
                        Mzx, Mzy, Mzz, Mzt);
    }

    /**
     * Native keyboard for text input is no longer necessary.
     * Keyboard will be hidden and native text input component too.
     */
    public void releaseInput() {
        Application.checkEventThread();
        _releaseInput(this.ptr);
    }

    protected abstract void _requestInput(long ptr, String text, int type, double width, double height,
                                            double Mxx, double Mxy, double Mxz, double Mxt,
                                            double Myx, double Myy, double Myz, double Myt,
                                            double Mzx, double Mzy, double Mzz, double Mzt);

    protected abstract void _releaseInput(long ptr);

}
