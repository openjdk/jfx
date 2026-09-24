/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.Cursor;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.HeaderButtonMetrics;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.HeaderButtonOverlay;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The window peer of the GTK glass. Its natives are the {@code ggtk_window_*} functions of {@code glass_gtk_api.h}
 * ({@link GtkGlassNative}), which hold the bodies of the JNI functions of {@code GlassWindow.cpp} of commit
 * {@code 033187ad90}; the calls the window C makes into Java arrive through the window table of the same header
 * ({@code GtkGlassNative.installCallbacks}), keyed by the {@code long} id this class assigns and resolves in
 * {@link #WINDOWS}.
 */
class GtkWindow extends Window {

    /**
     * Every window whose {@code WindowContext} carries its id, by that id: what the window slots resolve, on every
     * input event, without allocating (a primitive-keyed {@link GtkPeerRegistry}). Strong, as the JNI global
     * reference {@code jwindow} of {@code WindowContextTop} was (commit {@code 033187ad90}, {@code glass_window.cpp});
     * an entry goes where {@code WindowContextBase::process_destroy} deleted that reference, after
     * {@code notify_destroy} ({@link #releaseDestroyed}). Ids start at 1 and are never reused: 0 is the C's "no peer".
     */
    private static final GtkPeerRegistry<GtkWindow> WINDOWS = new GtkPeerRegistry<>();

    private static final AtomicLong NEXT_WINDOW_ID = new AtomicLong(1);

    /**
     * The id of this window; set by {@link #_createWindow}, which {@code Window}'s constructor calls, so the field has
     * no initializer.
     */
    private long windowId;

    /**
     * The id of the view this window's {@code WindowContext} holds, as {@code ggtk_window_set_view} answered it for
     * the last {@code _setView}: the view's registry entry is kept while a {@code WindowContext} holds it, as its JNI
     * global reference {@code jview} kept the {@code View} alive.
     */
    private long heldViewId;

    public GtkWindow(Window owner, Screen screen, int styleMask) {
        super(owner, screen, styleMask);
        if (isExtendedWindow()) {
            headerButtonHeightProperty().subscribe(this::onPrefHeaderButtonHeightChanged);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1createWindow}: the window is registered under a new id before
     * {@code ggtk_window_create} hands that id to its {@code WindowContextTop} - no event can then name an id Java does
     * not know - and held from here on as the C held it as {@code jwindow}. The constructor dials no window slot.
     */
    @Override
    protected long _createWindow(long ownerPtr, long screenPtr, int mask) {
        long id = NEXT_WINDOW_ID.getAndIncrement();
        windowId = id;
        WINDOWS.put(id, this);
        long ptr = 0L;
        try {
            ptr = GtkGlassNative.windowCreate(ownerPtr, screenPtr, mask, GtkApplication.visualID, id);
        } finally {
            if (ptr == 0L) {
                WINDOWS.remove(id);
            }
        }
        return ptr;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setView}. {@code WindowContextBase::set_view}
     * ({@code glass_window.cpp}) did not test the exception of the {@code EXIT} it sends the old view: the JNI left
     * it pending, and {@code _setView} threw it to its Java caller after the C had taken the new view. A callback stub
     * cannot let an exception out, so the {@code notify_mouse} stub hands that one back ({@link #claimSetViewExit}) and
     * it is thrown here once the downcall has returned - the same exception at the same point, with the same state on
     * both sides. The view's registry hold moves to the view id the downcall answers, not one read from the
     * {@code WindowContext} again: the {@code EXIT} handler may have closed the window. A {@code View} is a
     * {@code GtkView} on this platform ({@code GtkApplication.createView}); any other crosses as no view.
     */
    @Override
    protected boolean _setView(long ptr, View view) {
        long glassView = view instanceof GtkView gtkView ? gtkView.nativeHandle() : 0L;
        SetViewCall call = new SetViewCall(setViewCall);
        setViewCall = call;
        GtkGlassNative.SetView result;
        try {
            result = GtkGlassNative.windowSetView(ptr, glassView);
        } finally {
            setViewCall = call.outer;
        }
        viewSwapped(result.viewId());
        if (call.held != null) {
            throw GtkWindow.<RuntimeException>sneakyThrow(call.held);
        }
        return result.result();
    }

    /**
     * A {@link #_setView} in progress on the thread that iterates the main context, innermost first. The first
     * {@code notify_mouse} {@code EXIT} inside it is the one of {@code set_view}: nothing else calls Java from that
     * function before it.
     */
    static final class SetViewCall {
        private final SetViewCall outer;
        private boolean exitClaimed;
        private Throwable held;

        private SetViewCall(SetViewCall outer) {
            this.outer = outer;
        }

        /** Keeps what the {@code EXIT} threw, for {@link #_setView} to throw; nothing is reported. */
        void hold(Throwable t) {
            held = t;
        }
    }

    /** The innermost {@link SetViewCall}; only touched on the thread that iterates the main context. */
    private static SetViewCall setViewCall;

    /**
     * The {@code _setView} in progress whose {@code set_view} {@code EXIT} this {@code notify_mouse} of {@code type}
     * is, or {@code null}; claims it, so a mouse event dispatched from inside that {@code EXIT} is reported as usual.
     * On the path of every mouse event: one field read when no {@code _setView} runs.
     */
    static SetViewCall claimSetViewExit(int type) {
        SetViewCall call = setViewCall;
        if (call == null || call.exitClaimed || type != MouseEvent.EXIT) {
            return null;
        }
        call.exitClaimed = true;
        return call;
    }

    /**
     * After a {@code _setView}: moves the hold on the view registry entry from the view the {@code WindowContext}
     * held to {@code now}, the one it holds now ({@code set_view} deleted the old global reference and took a new
     * one).
     */
    private void viewSwapped(long now) {
        if (now != heldViewId) {
            GtkView.retainWindowHold(now);
            GtkView.releaseWindowHold(heldViewId);
            heldViewId = now;
        }
    }

    /** The id this window is registered under, 0 before {@link #_createWindow}. */
    long windowId() {
        return windowId;
    }

    /** How many windows the registry holds; the counterpart of the JNI's live {@code jwindow} references. */
    static int windowRegistrySize() {
        return WINDOWS.size();
    }

    /**
     * The window {@code id} names, {@code null} for an id no longer registered. The id 0 is a
     * {@code NullPointerException}: the C passes it where it called Java on a {@code NULL} {@code jwindow}, which is
     * the exception HotSpot raised for that receiver.
     */
    static GtkWindow peer(long id) {
        if (id == 0) {
            throw new NullPointerException();
        }
        return WINDOWS.get(id);
    }

    /*
     * The dispatch half of the window slots: registry lookup, then the protected Window method through a GtkWindow
     * reference - reachable only from a Window subclass, which is why they live here - so a subclass's override is
     * the one that runs, as it was for the JNI's virtual Call*Method.
     */

    static void dispatchStateChanged(long id, int state) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyStateChanged(state);
        }
    }

    static void dispatchFocus(long id, int event) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyFocus(event);
        }
    }

    static void dispatchFocusDisabled(long id) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyFocusDisabled();
        }
    }

    static void dispatchFocusUngrab(long id) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyFocusUngrab();
        }
    }

    static void dispatchDestroy(long id) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyDestroy();
        }
    }

    static void dispatchClose(long id) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyClose();
        }
    }

    static void dispatchResize(long id, int type, int width, int height) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyResize(type, width, height);
        }
    }

    static void dispatchMove(long id, int x, int y) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyMove(x, y);
        }
    }

    static void dispatchMoveToAnotherScreen(long id, Screen screen) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyMoveToAnotherScreen(screen);
        }
    }

    static void dispatchLevelChanged(long id, int level) {
        GtkWindow window = peer(id);
        if (window != null) {
            window.notifyLevelChanged(level);
        }
    }

    /** {@link #nonClientHitTest(int, int)}, which is private. */
    static int nonClientHitTest(GtkWindow window, int x, int y) {
        return window.nonClientHitTest(x, y);
    }

    /**
     * After {@code notify_destroy}: the window leaves the registry and lets go of its view, where
     * {@code WindowContextBase::process_destroy} deleted the global references {@code jview} and {@code jwindow} and
     * cleared both ids.
     */
    static void releaseDestroyed(long id) {
        GtkWindow window = WINDOWS.remove(id);
        if (window != null) {
            GtkView.releaseWindowHold(window.heldViewId);
            window.heldViewId = 0;
        }
    }

    /** Throws {@code t} unchanged, checked or not, as a JNI native method threw a pending exception. */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    protected boolean _close(long ptr) {
        return GtkGlassNative.windowClose(ptr);
    }

    @Override
    protected void _updateViewSize(long ptr) {
        GtkGlassNative.windowUpdateViewSize(ptr);
    }

    @Override
    protected boolean _setMenubar(long ptr, long menubarPtr) {
        //TODO is it needed?
        return true;
    }

    private void minimizeImpl(long ptr, boolean minimize) {
        GtkGlassNative.windowMinimize(ptr, minimize);
    }

    private void maximizeImpl(long ptr, boolean maximize, boolean wasMaximized) {
        GtkGlassNative.windowMaximize(ptr, maximize, wasMaximized);
    }

    private void setVisibleImpl(long ptr, boolean visible) {
        GtkGlassNative.windowSetVisible(ptr, visible);
    }

    @Override
    protected boolean _setResizable(long ptr, boolean resizable) {
        return GtkGlassNative.windowSetResizable(ptr, resizable);
    }

    @Override
    protected boolean _requestFocus(long ptr, int event) {
        return GtkGlassNative.windowRequestFocus(ptr, event);
    }

    @Override
    protected void _setFocusable(long ptr, boolean isFocusable) {
        GtkGlassNative.windowSetFocusable(ptr, isFocusable);
    }

    @Override
    protected boolean _grabFocus(long ptr) {
        return GtkGlassNative.windowGrabFocus(ptr);
    }

    @Override
    protected void _ungrabFocus(long ptr) {
        GtkGlassNative.windowUngrabFocus(ptr);
    }

    @Override
    protected boolean _setTitle(long ptr, String title) {
        return GtkGlassNative.windowSetTitle(ptr, title);
    }

    @Override
    protected void _setLevel(long ptr, int level) {
        GtkGlassNative.windowSetLevel(ptr, level);
    }

    @Override
    protected void _setAlpha(long ptr, float alpha) {
        GtkGlassNative.windowSetAlpha(ptr, alpha);
    }

    @Override
    protected boolean _setBackground(long ptr, float r, float g, float b) {
        return GtkGlassNative.windowSetBackground(ptr, r, g, b);
    }

    @Override
    protected void _setEnabled(long ptr, boolean enabled) {
        GtkGlassNative.windowSetEnabled(ptr, enabled);
    }

    private boolean _setSystemMinimumSize(long ptr, int width, int height) {
        return GtkGlassNative.windowSetSystemMinimumSize(ptr, width, height);
    }

    @Override
    protected boolean _setMinimumSize(long ptr, int width, int height) {
        return GtkGlassNative.windowSetMinimumSize(ptr, width, height);
    }

    @Override
    protected boolean _setMaximumSize(long ptr, int width, int height) {
        return GtkGlassNative.windowSetMaximumSize(ptr, width, height);
    }

    @Override
    protected void _setIcon(long ptr, Pixels pixels) {
        GtkGlassNative.windowSetIcon(ptr, pixels);
    }

    @Override
    protected void _toFront(long ptr) {
        GtkGlassNative.windowToFront(ptr);
    }

    @Override
    protected void _toBack(long ptr) {
        GtkGlassNative.windowToBack(ptr);
    }

    protected long _getNativeWindowImpl(long ptr) {
        return GtkGlassNative.windowNativeWindow(ptr);
    }

    private void _showSystemMenu(long ptr, int x, int y) {
        GtkGlassNative.windowShowSystemMenu(ptr, x, y);
    }

    private boolean isVisible(long ptr) {
        return GtkGlassNative.windowIsVisible(ptr);
    }

    @Override
    protected boolean _setVisible(long ptr, boolean visible) {
        setVisibleImpl(ptr, visible);
        return isVisible(ptr);
    }

    @Override
    protected boolean _minimize(long ptr, boolean minimize) {
        minimizeImpl(ptr, minimize);
        notifyStateChanged(WindowEvent.MINIMIZE);
        return minimize;
    }

    @Override
    protected boolean _maximize(long ptr, boolean maximize,
                                boolean wasMaximized) {
        maximizeImpl(ptr, maximize, wasMaximized);
        notifyStateChanged(WindowEvent.MAXIMIZE);
        return maximize;
    }

    protected void notifyStateChanged(final int state) {
        switch (state) {
            case WindowEvent.MINIMIZE:
            case WindowEvent.MAXIMIZE:
            case WindowEvent.RESTORE:
                notifyResize(state, getWidth(), getHeight());
                break;
            default:
                System.err.println("Unknown window state: " + state);
                break;
        }
    }

    @Override
    protected void _setCursor(long ptr, Cursor cursor) {
        if (cursor.getType() == Cursor.CURSOR_CUSTOM) {
            _setCustomCursor(ptr, cursor);
        } else {
            _setCursorType(ptr, cursor.getType());
        }
    }

    private void _setCursorType(long ptr, int type) {
        GtkGlassNative.windowSetCursorType(ptr, type);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkWindow__1setCustomCursor}: the {@code GdkCursor *} the JNI read from the
     * cursor's {@code ptr} field. A custom {@code Cursor} is a {@code GtkCursor} on this platform
     * ({@code GtkApplication.createCursor}); any other crosses as no cursor.
     */
    private void _setCustomCursor(long ptr, Cursor cursor) {
        GtkGlassNative.windowSetCursor(ptr, cursor instanceof GtkCursor gtkCursor ? gtkCursor.nativeCursor() : 0L);
    }

    /**
     * The lowest level (X11) window handle.
     * (Used in prism to create GLContext)
     * @return X11 Window handle is returned.
     */
    @Override
    public long getNativeWindow() {
        return _getNativeWindowImpl(super.getNativeWindow());
    }

    @Override
    protected void _setBounds(long ptr, int x, int y, boolean xSet, boolean ySet,
                                       int w, int h, int cw, int ch,
                                       float xGravity, float yGravity) {
        GtkGlassNative.windowSetBounds(ptr, x, y, xSet, ySet, w, h, cw, ch, xGravity, yGravity);
    }

    @Override
    protected void _requestInput(long ptr, String text, int type, double width, double height,
                                    double Mxx, double Mxy, double Mxz, double Mxt,
                                    double Myx, double Myy, double Myz, double Myt,
                                    double Mzx, double Mzy, double Mzz, double Mzt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void _releaseInput(long ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getRawHandle() {
        long ptr = super.getRawHandle();
        return ptr == 0L ? 0L : _getNativeWindowImpl(ptr);
    }

    /**
     * Opens a system menu at the specified coordinates.
     *
     * @param x the X coordinate in physical pixels
     * @param y the Y coordinate in physical pixels
     */
    public void showSystemMenu(int x, int y) {
        _showSystemMenu(super.getRawHandle(), x, y);
    }

    /**
     * Creates or disposes the {@link HeaderButtonOverlay} when the preferred header button height has changed.
     * <p>
     * If the preferred height is zero, the overlay is disposed; if the preferred height is non-zero, the
     * {@link #headerButtonOverlay} and {@link #headerButtonMetrics} properties will hold the overlay and
     * its metrics.
     *
     * @param height the preferred header button height
     */
    private void onPrefHeaderButtonHeightChanged(Number height) {
        // Return early if we can keep the existing overlay instance.
        if (height.doubleValue() != 0 && headerButtonOverlay.get() != null) {
            return;
        }

        if (headerButtonOverlay.get() instanceof HeaderButtonOverlay overlay) {
            overlay.dispose();
        }

        if (height.doubleValue() == 0) {
            headerButtonOverlay.set(null);
            headerButtonMetrics.set(HeaderButtonMetrics.EMPTY);
        } else {
            HeaderButtonOverlay overlay = createHeaderButtonOverlay();
            overlay.metricsProperty().subscribe(headerButtonMetrics::set);
            headerButtonOverlay.set(overlay);
        }
    }

    /**
     * Creates a new {@code HeaderButtonOverlay} instance.
     */
    private HeaderButtonOverlay createHeaderButtonOverlay() {
        var overlay = new HeaderButtonOverlay(
            PlatformThemeObserver.getInstance().stylesheetProperty(),
            isModal() || getOwner() != null, isUtilityWindow(),
            (getStyleMask() & RIGHT_TO_LEFT) != 0);

        // Set the system-defined absolute minimum size to the size of the window buttons area,
        // regardless of whether the application has specified a smaller minimum size.
        overlay.metricsProperty().subscribe(metrics -> {
            int w = (int)(metrics.totalInsetWidth() * platformScaleX);
            int h = (int)(metrics.maxInsetHeight() * platformScaleY);
            _setSystemMinimumSize(super.getRawHandle(), w, h);
        });

        overlay.buttonPrefHeightProperty().bind(headerButtonHeightProperty());
        overlay.buttonDarkStyleProperty().bind(headerButtonDarkStyleProperty());
        return overlay;
    }

    /*
     * The answers of nonClientHitTest, which WindowContextTop::process_mouse_button and
     * ::process_mouse_motion (glass_window.cpp) compare against. The C states them as GGTK_HT_* in
     * native-glass/gtk/glass_gtk_api.h and a test pins the two sides together; up to commit 033187ad90 these
     * fields were @Native and the C read the com_sun_glass_ui_gtk_GtkWindow_HT_* macros of the header javac -h
     * wrote for this class.
     */
    private static final int HT_UNSPECIFIED = 0;
    private static final int HT_CAPTION = 1;
    private static final int HT_CLIENT = 2;

    /**
     * Classifies the window region at the specified physical coordinate.
     * <p>
     * This method is called from native code: the {@code non_client_hit_test} slot, through
     * {@link #nonClientHitTest(GtkWindow, int, int)}.
     *
     * @param x the X coordinate in physical pixels
     * @param y the Y coordinate in physical pixels
     */
    private int nonClientHitTest(int x, int y) {
        // A full-screen window has no draggable area.
        if (view == null || view.isInFullscreen() || !isExtendedWindow()) {
            return HT_CLIENT;
        }

        double wx = x / platformScaleX;
        double wy = y / platformScaleY;

        if (headerButtonOverlay.get() instanceof HeaderButtonOverlay overlay && overlay.buttonAt(wx, wy) != null) {
            return HT_CLIENT;
        }

        View.EventHandler eventHandler = view.getEventHandler();
        if (eventHandler == null) {
            return HT_CLIENT;
        }

        return switch (eventHandler.pickHeaderArea(wx, wy)) {
            case UNSPECIFIED -> HT_UNSPECIFIED;
            case DRAGBAR -> HT_CAPTION;
            case null, default -> HT_CLIENT;
        };
    }
}
