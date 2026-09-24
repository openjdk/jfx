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
package com.sun.glass.ui.win;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import com.sun.javafx.tk.HeaderAreaType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MS Windows platform implementation class for View.
 * <p>
 * The peer is a {@code GlassView} ({@code native-glass/win/GlassView.cpp}) reached through
 * {@link WinGlassNative}'s {@code gwin_view_*} downcalls; the other direction - the 26 places where
 * {@code GlassView.cpp}, {@code ViewContainer.cpp} and {@code FullScreenWindow.cpp} call back into
 * this class - arrives through the two callback tables {@code WinGlassNative.installViewCallbacks}
 * installs, keyed by a {@code long} id rather than a Java reference. {@link #VIEWS} is that
 * registry: {@link WinGlassNative}'s fifteen upcall targets resolve the id here and call the
 * instance method through a {@code WinView}-typed reference, which is the only way a class in this
 * package can reach the {@code protected} {@code View.notify*} methods (JLS 6.6.2.1), and which is
 * what makes the {@link #notifyResize} and {@link #notifyMenu} overrides below the ones that run.
 * <p>
 * <b>The registry holds strong references</b>, as the JNI's {@code NewGlobalRef} did
 * ({@code GlassView.cpp}), and an entry is removed by {@link #_close} only after
 * {@code gwin_view_close} has returned, because {@code GlassView::Close} can still fire
 * {@code notify_view} on the way out. Ids start at 1 and are never reused: 0 means "no view" to the
 * C (a container with no {@code GlassView} attached, which only the touch and gesture slots can
 * see), and a callback for an id that is no longer here - reachable through the animated
 * fullscreen exit, where the JNI dereferenced a deleted global ref - finds nothing and does nothing,
 * silently.
 */
final class WinView extends View {

    static {
        multiClickTime = WinGlassNative.getDoubleClickTime();
        multiClickMaxX = WinGlassNative.getSystemMetrics(WinGlassNative.SM_CXDOUBLECLK);
        multiClickMaxY = WinGlassNative.getSystemMetrics(WinGlassNative.SM_CYDOUBLECLK);
    }

    // Constants
    private static final long multiClickTime;
    private static final int multiClickMaxX, multiClickMaxY;

    /**
     * Every open view, by the id the callback tables carry. {@code ConcurrentHashMap} although every
     * access is on the JavaFX application thread: the cost is nil, and a {@code WinView} is also
     * reachable from {@code Application.invokeLater} bodies. The lookup boxes the id;
     * {@code Long.valueOf} caches the first 127, which a long-running application will exceed - if a
     * profile ever shows that boxing on the mouse path, this becomes a {@code long}-keyed table and
     * the ABI does not change.
     */
    private static final Map<Long, WinView> VIEWS = new ConcurrentHashMap<>();

    /** Ids from 1: 0 is the C's "no view". Monotone, never reused. */
    private static final AtomicLong NEXT_VIEW_ID = new AtomicLong(1L);

    /** The id this view is registered under; 0 until {@link #register}. */
    private long viewId;

    protected WinView() {
        super();
    }

    static long getMultiClickTime_impl() {
        return multiClickTime;
    }

    static int getMultiClickMaxX_impl() {
        return multiClickMaxX;
    }

    static int getMultiClickMaxY_impl() {
        return multiClickMaxY;
    }

    // ---- the registry the callback tables resolve against ----

    /**
     * Takes the next id and publishes {@code view} under it. Called before the native peer learns the
     * id, so that a callback which arrives inside the creating downcall finds its view.
     */
    static long register(WinView view) {
        long id = NEXT_VIEW_ID.getAndIncrement();
        view.viewId = id;
        VIEWS.put(id, view);
        return id;
    }

    /**
     * Drops a registry entry without closing the view. For the headless views of
     * {@code WinViewNativeTest}, which cannot be closed ({@code gwin_view_close} needs a toolkit
     * thread to marshal to); {@link #_close} is the production path.
     */
    static void unregister(long id) {
        VIEWS.remove(id);
    }

    /** How many views the registry holds; the FFM counterpart of the JNI's live global refs. */
    static int viewRegistrySize() {
        return VIEWS.size();
    }

    /** The id {@link #register} gave this view, 0 if it was never registered. */
    long viewId() {
        return viewId;
    }

    /**
     * The view of {@code viewId}, or {@code null} for 0 and for an id that is gone - the touch and
     * gesture slots pass that through to {@code WinGestureSupport}, whose methods take a
     * {@code View} that the JNI also passed as {@code null} when the container had none.
     */
    static View viewFor(long viewId) {
        return VIEWS.get(viewId);
    }

    /*
     * The dispatch half of the fifteen upcall slots: registry lookup, then the instance method
     * through a WinView reference. An unknown id returns the slot default silently. None of these may
     * take a lock - they run inside DispatchMessage or inside a gwin_view_* downcall, possibly while
     * View.uploadPixels holds View.lock() around gwin_view_upload_pixels.
     */

    static void dispatchView(long viewId, int type) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyView(type);
        }
    }

    static void dispatchResize(long viewId, int width, int height) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyResize(width, height);
        }
    }

    static void dispatchRepaint(long viewId, int x, int y, int width, int height) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyRepaint(x, y, width, height);
        }
    }

    static void dispatchMenu(long viewId, int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyMenu(x, y, xAbs, yAbs, isKeyboardTrigger);
        }
    }

    static void dispatchKey(long viewId, int type, int keyCode, char[] keyChars, int modifiers) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyKey(type, keyCode, keyChars, modifiers);
        }
    }

    /** Hot: per mouse move. Primitives in, one boxed {@code Long} for the lookup, nothing else. */
    static void dispatchMouse(long viewId, int type, int button, int x, int y, int xAbs, int yAbs,
                              int modifiers, boolean isPopupTrigger, boolean isSynthesized) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyMouse(type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger, isSynthesized);
        }
    }

    static void dispatchScroll(long viewId, int x, int y, int xAbs, int yAbs, double deltaX, double deltaY,
                               int modifiers, int lines, int chars, int defaultLines, int defaultChars,
                               double xMultiplier, double yMultiplier) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyScroll(x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars, defaultLines,
                    defaultChars, xMultiplier, yMultiplier);
        }
    }

    static void dispatchInputMethod(long viewId, String text, int[] clauseBoundary, int[] attrBoundary,
                                    byte[] attrValue, int committedTextLength, int caretPos, int visiblePos) {
        WinView view = VIEWS.get(viewId);
        if (view != null) {
            view.notifyInputMethod(text, clauseBoundary, attrBoundary, attrValue, committedTextLength,
                    caretPos, visiblePos);
        }
    }

    /** The candidate position, or {@code null} when the view is gone (the C then keeps its own default). */
    static double[] dispatchInputMethodCandidatePosRequest(long viewId, int offset) {
        WinView view = VIEWS.get(viewId);
        return view == null ? null : view.notifyInputMethodCandidatePosRequest(offset);
    }

    /** The native accessible, or 0 when the view is gone. */
    static long dispatchGetAccessible(long viewId) {
        WinView view = VIEWS.get(viewId);
        return view == null ? 0L : view.getAccessibleForNative();
    }

    /*
     * The dispatch half of the four drag slots of GwinDndCallbacks: the four
     * View.notifyDrag* methods GlassDnD.cpp dialled through javaIDs.View, keyed by the same view id
     * as everything above. They are protected members of another package, reachable only through a
     * WinView reference, which is why the dispatch is here and not in the facade. An unknown id - 0
     * from a drop target whose container has no view, or a view that is gone - answers null, and the
     * facade then writes nothing so that the C's *pdwEffect stays what the C put there
     * (DROPEFFECT_NONE); the JNI's GetView() == NULL early-out itself stays in C. The boxed return
     * costs one Integer per drag event, cached for every ACTION_* value below 128.
     */

    /** {@code drag_enter}: {@code View.notifyDragEnter}, which opens the drop-target {@code ClipboardAssistance}. */
    static Integer dispatchDragEnter(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction) {
        WinView view = VIEWS.get(viewId);
        return view == null ? null : view.notifyDragEnter(x, y, xAbs, yAbs, recommendedAction);
    }

    /** {@code drag_over}: {@code View.notifyDragOver}. */
    static Integer dispatchDragOver(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction) {
        WinView view = VIEWS.get(viewId);
        return view == null ? null : view.notifyDragOver(x, y, xAbs, yAbs, recommendedAction);
    }

    /** {@code drag_drop}: {@code View.notifyDragDrop}, which closes the drop-target assistance. */
    static Integer dispatchDragDrop(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction) {
        WinView view = VIEWS.get(viewId);
        return view == null ? null : view.notifyDragDrop(x, y, xAbs, yAbs, recommendedAction);
    }

    /** {@code drag_leave}: {@code View.notifyDragLeave} (void); whether a view was there to notify. */
    static boolean dispatchDragLeave(long viewId) {
        WinView view = VIEWS.get(viewId);
        if (view == null) {
            return false;
        }
        view.notifyDragLeave();
        return true;
    }

    // ---- the peer ----

    @Override
    protected long _getNativeFrameBuffer(long ptr) {
        return 0;
    }

    @Override
    protected void _enableInputMethodEvents(long ptr, boolean enable) {
        WinGlassNative.viewEnableIme(ptr, enable);
    }

    @Override
    protected void _finishInputMethodComposition(long ptr) {
        WinGlassNative.viewFinishImeComposition(ptr);
    }

    /**
     * Registers this view first, so that a callback arriving inside the creating call already finds
     * it, then hands the id to {@code gwin_view_create} ({@link WinGlassNative#viewCreate}); on any
     * failure the entry is removed again, so a view that never existed is never in the registry.
     * {@code caps} was never read by the C.
     * <p>
     * The export is the statement of the former JNI entry point
     * {@code Java_com_sun_glass_ui_win_WinView__1create}, {@code new GlassView(viewId)}, and nothing
     * else: no OS call, no thread marshal, no {@code jobject} (the drag-and-drop flip retired the global ref
     * {@code GlassDnD.cpp} dereferenced), so it runs without a toolkit. It answers 0 only when the
     * allocation threw, which the JNI let unwind through a JNI frame instead. With it {@code WinView}
     * declares no {@code native} method.
     */
    @Override
    protected long _create(Map caps) {
        long id = register(this);
        long ptr = 0L;
        try {
            ptr = WinGlassNative.viewCreate(id);
        } finally {
            if (ptr == 0L) {
                VIEWS.remove(id);
            }
        }
        return ptr;
    }

    /**
     * The {@code GlassView*} this view holds - {@code View.ptr}, which {@code GlassWindow.cpp}'s
     * {@code _setView} read through {@code javaIDs.View.ptr} - for {@code gwin_window_set_view}; 0
     * once closed. Package-private: the window peer is not a {@code View} subclass and cannot read
     * the protected accessor itself.
     */
    long nativeHandle() {
        return getRawHandle();
    }

    @Override
    protected long _getNativeView(long ptr) {
        return WinGlassNative.viewGetNativeView(ptr);
    }

    @Override
    protected int _getX(long ptr) {
        return WinGlassNative.viewGetX(ptr);
    }

    @Override
    protected int _getY(long ptr) {
        return WinGlassNative.viewGetY(ptr);
    }

    @Override
    protected void _setParent(long ptr, long parentPtr) {
        WinGlassNative.viewSetParent(ptr, parentPtr);
    }

    /**
     * {@code gwin_view_close} first, then the registry: the close marshals through
     * {@code SendMessage} and {@code GlassView::Close} can still fire {@code notify_view} on the way
     * out ({@code GlassView.cpp}), which must still find this view. {@code View.close()} nulls the
     * handle afterwards, so the dangling {@code GlassView*} is never passed again.
     */
    @Override
    protected boolean _close(long ptr) {
        try {
            return WinGlassNative.viewClose(ptr);
        } finally {
            VIEWS.remove(viewId);
        }
    }

    @Override
    protected void _scheduleRepaint(long ptr) {
        WinGlassNative.viewScheduleRepaint(ptr);
    }

    /**
     * {@code _begin} / {@code _end} bracket {@code View.lock()} / {@code unlock()}. Their JNI bodies
     * ({@code Java_com_sun_glass_ui_win_WinView__1begin} / {@code __1end}, {@code GlassView.cpp}) were
     * empty at every revision, so the overrides are.
     */
    @Override
    protected void _begin(long ptr) {
    }

    @Override
    protected void _end(long ptr) {
    }

    @Override
    protected void _uploadPixels(long ptr, Pixels pixels) {
        WinGlassNative.viewUploadPixels(ptr, pixels);
    }

    @Override
    protected boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor) {
        return WinGlassNative.viewEnterFullscreen(ptr, animate, keepRatio, hideCursor);
    }

    @Override
    protected void _exitFullscreen(long ptr, boolean animate) {
        WinGlassNative.viewExitFullscreen(ptr, animate);
    }

    @Override
    protected void notifyResize(int width, int height) {
        super.notifyResize(width, height);

        // After resizing, do a move notification to force the view relocation.
        // When moving to a screen with different DPI settings, its location needs
        // to be recalculated.
        updateLocation();
    }

    @Override
    protected void notifyMenu(int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        // If all of the following conditions are satisfied, we open a system menu at the specified coordinates:
        // 1. The application didn't consume the menu event.
        // 2. The window is an EXTENDED window and is not in full-screen mode.
        // 3. The menu event occurred on a draggable area.
        if (!handleMenuEvent(x, y, xAbs, yAbs, isKeyboardTrigger)) {
            var window = (WinWindow)getWindow();
            if (!window.isExtendedWindow() || isInFullscreen()) {
                return;
            }

            double wx = x / window.getPlatformScaleX();
            double wy = y / window.getPlatformScaleY();

            EventHandler eventHandler = getEventHandler();
            if (eventHandler != null && eventHandler.pickHeaderArea(wx, wy) == HeaderAreaType.DRAGBAR) {
                window.showSystemMenu(x, y);
            }
        }
    }

    @Override
    protected boolean handleNonClientMouseEvent(long time, int type, int button, int x, int y, int xAbs, int yAbs,
                                                int modifiers, int clickCount) {
        if (!shouldHandleEvent()) {
            return false;
        }

        if (getWindow() instanceof WinWindow window) {
            double wx = x / window.getPlatformScaleX();
            double wy = y / window.getPlatformScaleY();

            // Give the header button overlay the first chance to handle the event.
            if (window.headerButtonOverlayProperty().get() instanceof HeaderButtonOverlay overlay
                    && overlay.handleMouseEvent(type, button, wx, wy)) {
                return true;
            }

            if (window.isResizable()
                    && clickCount == 2
                    && type == MouseEvent.DOWN
                    && getEventHandler() instanceof View.EventHandler eventHandler
                    && eventHandler.pickHeaderArea(wx, wy) == HeaderAreaType.DRAGBAR) {
                window.maximize(!window.isMaximized());
            }
        }

        // If the overlay didn't handle the event, we pass it down to the application.
        handleMouseEvent(time, type, button, x, y, xAbs, yAbs, modifiers, false, false);
        return true;
    }
}
