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

import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import com.sun.javafx.tk.HeaderAreaType;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The view peer of the GTK glass. Its natives are the {@code ggtk_view_*} functions of {@code glass_gtk_api.h}
 * ({@link GtkGlassNative}), which hold the bodies of the JNI functions of {@code GlassView.cpp} of commit
 * {@code 033187ad90}; the calls the window, view and drop-target C makes into a view arrive through the view and
 * drag-and-drop tables of the same header ({@code GtkGlassNative.installCallbacks}), keyed by the {@code long} id this
 * class assigns and resolves in {@link #VIEWS}.
 */
final class GtkView extends View {

    /**
     * Every view the C can still name, by id: what the view and drop-target slots resolve, on every input event,
     * without allocating (a primitive-keyed {@link GtkPeerRegistry}). An entry lives while the {@code GlassView}
     * exists ({@link #_create} to {@code _close}) or while a {@code WindowContext} holds the view
     * ({@code GtkWindow._setView}), and holds the view the way the C did at commit {@code 033187ad90}:
     * <ul>
     * <li>strongly while a {@code WindowContext} holds it - the JNI global reference {@code jview} of
     * {@code glass_window.cpp}, which kept a view reachable, and callable even once closed, for as long as a window
     * held it;</li>
     * <li>weakly while no {@code WindowContext} holds it: the {@code GlassView} of {@code GlassView.cpp} kept no
     * reference to its {@code View}, so a view that is dropped without {@code close()} stays collectable. The only
     * C that names such a view is a native of the view itself ({@code _setParent}, {@code _enterFullscreen},
     * {@code _exitFullscreen}), whose receiver is reachable while it runs.</li>
     * </ul>
     * Ids start at 1 and are never reused: 0 is the C's "no view".
     */
    private static final GtkPeerRegistry<GtkView> VIEWS = new GtkPeerRegistry<>();

    private static final AtomicLong NEXT_VIEW_ID = new AtomicLong(1);

    /**
     * The id of this view; set by {@link #_create}, which {@code View}'s constructor calls, so the field has no
     * initializer.
     */
    private long viewId;

    /** Whether {@code _close} has deleted the {@code GlassView}. */
    private boolean peerClosed;

    /** How many {@code WindowContext}s hold this view ({@code GtkWindow._setView}). */
    private int windowHolds;

    private boolean imEnabled = false;

    /**
     * The native copy of the frames this view uploads from a Java array; allocated by the first such upload, freed
     * by {@link #_close}.
     */
    private final GtkGlassNative.UploadStaging uploadStaging = new GtkGlassNative.UploadStaging();

    GtkView() {
        super();
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1create}: the view is registered under a new id before
     * {@code ggtk_view_create} hands that id to its {@code GlassView}, which {@code GtkWindow._setView} copies into
     * the window. No {@code WindowContext} holds the new view yet: registered weakly. The JNI ignored the caps map.
     */
    @Override
    protected long _create(Map caps) {
        long id = NEXT_VIEW_ID.getAndIncrement();
        viewId = id;
        VIEWS.putWeak(id, this);
        long ptr = 0L;
        try {
            ptr = GtkGlassNative.viewCreate(id);
        } finally {
            if (ptr == 0L) {
                VIEWS.remove(id);
            }
        }
        return ptr;
    }

    /**
     * {@code View.ptr}, the {@code GlassView *} of this view, which {@code GlassWindow.cpp}'s {@code _setView} read
     * with {@code GetLongField}; 0 once closed. Package-private for {@code GtkWindow}, which is not a {@code View}
     * subclass and cannot read the protected accessor itself.
     */
    long nativeHandle() {
        return getRawHandle();
    }

    /**
     * {@code View.close}, then, once {@code _close} has deleted the {@code GlassView}, the registry entry goes unless
     * a {@code WindowContext} still holds the view.
     */
    @Override
    public void close() {
        boolean open = !isClosed();
        try {
            super.close();
        } finally {
            if (open && isClosed()) {
                peerClosed = true;
                unregisterIfUnheld();
            }
        }
    }

    private void unregisterIfUnheld() {
        if (peerClosed && windowHolds <= 0) {
            VIEWS.remove(viewId);
        }
    }

    /**
     * A {@code WindowContext} took the view of {@code id} (0: none): from the first hold on, its entry is strong, as
     * the global reference {@code set_view} took.
     */
    static void retainWindowHold(long id) {
        GtkView view = id == 0 ? null : VIEWS.get(id);
        if (view != null) {
            view.windowHolds++;
            if (view.windowHolds == 1) {
                VIEWS.put(id, view);
            }
        }
    }

    /**
     * A {@code WindowContext} let go of the view of {@code id} (0: none), where the C deleted its global reference:
     * after the last hold the entry goes if the view is closed, and turns weak if it is still open.
     */
    static void releaseWindowHold(long id) {
        GtkView view = id == 0 ? null : VIEWS.get(id);
        if (view != null) {
            view.windowHolds--;
            if (view.windowHolds <= 0) {
                if (view.peerClosed) {
                    VIEWS.remove(id);
                } else {
                    VIEWS.putWeak(id, view);
                }
            }
        }
    }

    /** The id this view is registered under. */
    long viewId() {
        return viewId;
    }

    /** The size in bytes of the block this view's frames are copied into, 0 before the first upload and once closed. */
    long uploadStagingBytes() {
        return uploadStaging.capacity();
    }

    /**
     * How many views the registry answers for: those a {@code WindowContext} holds - the JNI's live {@code jview}
     * references - and the open ones no window holds that are still reachable.
     */
    static int viewRegistrySize() {
        return VIEWS.size();
    }

    /** Whether a view is registered under {@code id}. */
    static boolean isRegistered(long id) {
        return VIEWS.containsKey(id);
    }

    /**
     * The view {@code id} names, {@code null} for an id no longer registered. The id 0 is a
     * {@code NullPointerException}: the C passes it where it called Java on a {@code NULL} {@code jview}, which is
     * the exception HotSpot raised for that receiver.
     */
    static GtkView peer(long id) {
        if (id == 0) {
            throw new NullPointerException();
        }
        return VIEWS.get(id);
    }

    /*
     * The dispatch half of the view and drop-target slots: registry lookup, then the protected View method through
     * a GtkView reference - reachable only from a View subclass, which is why they live here - so the overrides
     * below (notifyMenu) run, as they did for the JNI's virtual Call*Method.
     */

    static void dispatchView(long id, int type) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyView(type);
        }
    }

    static void dispatchResize(long id, int width, int height) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyResize(width, height);
        }
    }

    static void dispatchRepaint(long id, int x, int y, int width, int height) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyRepaint(x, y, width, height);
        }
    }

    /** Hot: per motion event. Primitives in, the registry lookup, nothing else. */
    static void dispatchMouse(long id, int type, int button, int x, int y, int xAbs, int yAbs, int modifiers,
                              boolean isPopupTrigger, boolean isSynthesized) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyMouse(type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger, isSynthesized);
        }
    }

    static void dispatchMenu(long id, int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyMenu(x, y, xAbs, yAbs, isKeyboardTrigger);
        }
    }

    static void dispatchScroll(long id, int x, int y, int xAbs, int yAbs, double deltaX, double deltaY,
                               int modifiers, int lines, int chars, int defaultLines, int defaultChars,
                               double xMultiplier, double yMultiplier) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyScroll(x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars, defaultLines, defaultChars,
                    xMultiplier, yMultiplier);
        }
    }

    static void dispatchKey(long id, int type, int keyCode, char[] keyChars, int modifiers) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyKey(type, keyCode, keyChars, modifiers);
        }
    }

    static void dispatchInputMethod(long id, String text, int commitLength, int cursor, byte attr) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyInputMethodLinux(text, commitLength, cursor, attr);
        }
    }

    /** The array the view answered; {@code null} for an id no longer registered. */
    static double[] dispatchCandidatePosRequest(long id, int offset) {
        GtkView view = peer(id);
        return view == null ? null : view.notifyInputMethodCandidateRelativePosRequest(offset);
    }

    /** The action the view answered; {@code null} for an id no longer registered. */
    static Integer dispatchDragEnter(long id, int x, int y, int xAbs, int yAbs, int recommendedAction) {
        GtkView view = peer(id);
        return view == null ? null : view.notifyDragEnter(x, y, xAbs, yAbs, recommendedAction);
    }

    /** The action the view answered; {@code null} for an id no longer registered. */
    static Integer dispatchDragOver(long id, int x, int y, int xAbs, int yAbs, int recommendedAction) {
        GtkView view = peer(id);
        return view == null ? null : view.notifyDragOver(x, y, xAbs, yAbs, recommendedAction);
    }

    static void dispatchDragDrop(long id, int x, int y, int xAbs, int yAbs, int recommendedAction) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyDragDrop(x, y, xAbs, yAbs, recommendedAction);
        }
    }

    static void dispatchDragLeave(long id) {
        GtkView view = peer(id);
        if (view != null) {
            view.notifyDragLeave();
        }
    }

    private void enableInputMethodEventsImpl(long ptr, boolean enable) {
        GtkGlassNative.viewEnableInputMethodEvents(ptr, enable);
    }

    @Override
    protected void _enableInputMethodEvents(long ptr, boolean enable) {
        enableInputMethodEventsImpl(ptr, enable);

        imEnabled = enable;
    }

    @Override
    protected long _getNativeFrameBuffer(long ptr) {
        return 0;
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1getNativeView} ({@code GlassView.cpp}, commit {@code 033187ad90})
     * returned 0.
     */
    @Override
    protected long _getNativeView(long ptr) {
        return 0;
    }

    @Override
    protected int _getX(long ptr) {
        return GtkGlassNative.viewX(ptr);
    }

    @Override
    protected int _getY(long ptr) {
        return GtkGlassNative.viewY(ptr);
    }

    @Override
    protected void _setParent(long ptr, long parentPtr) {
        GtkGlassNative.viewSetParent(ptr, parentPtr);
    }

    /** Deletes the {@code GlassView}, then frees the block the frames were copied into ({@code uploadStaging}). */
    @Override
    protected boolean _close(long ptr) {
        try {
            return GtkGlassNative.viewClose(ptr);
        } finally {
            uploadStaging.release();
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkView__1scheduleRepaint} ({@code GlassView.cpp}, commit {@code 033187ad90})
     * did nothing ("Seems to be unused").
     */
    @Override
    protected void _scheduleRepaint(long ptr) {
    }

    @Override
    protected void _begin(long ptr) {}

    @Override
    protected void _end(long ptr) {}

    @Override
    protected void _uploadPixels(long ptr, Pixels pixels) {
        Buffer data = pixels.getPixels();
        if (data.isDirect() == true) {
            _uploadPixelsDirect(ptr, data, pixels.getWidth(), pixels.getHeight());
        } else if (data.hasArray() == true) {
            if (pixels.getBytesPerComponent() == 1) {
                ByteBuffer bytes = (ByteBuffer)data;
                _uploadPixelsByteArray(ptr, bytes.array(), bytes.arrayOffset(), pixels.getWidth(), pixels.getHeight());
            } else {
                IntBuffer ints = (IntBuffer)data;
                _uploadPixelsIntArray(ptr, ints.array(), ints.arrayOffset(), pixels.getWidth(), pixels.getHeight());
            }
        } else {
            // gznote: what are the circumstances under which this can happen?
            _uploadPixelsDirect(ptr, pixels.asByteBuffer(), pixels.getWidth(), pixels.getHeight());
        }
    }
    private void _uploadPixelsDirect(long viewPtr, Buffer pixels, int width, int height) {
        GtkGlassNative.viewUploadPixelsDirect(viewPtr, pixels, width, height);
    }

    private void _uploadPixelsByteArray(long viewPtr, byte[] pixels, int offset, int width, int height) {
        GtkGlassNative.viewUploadPixelsByte(viewPtr, uploadStaging, pixels, offset, width, height);
    }

    private void _uploadPixelsIntArray(long viewPtr, int[] pixels, int offset, int width, int height) {
        GtkGlassNative.viewUploadPixelsInt(viewPtr, uploadStaging, pixels, offset, width, height);
    }

    @Override
    protected boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor) {
        return GtkGlassNative.viewEnterFullscreen(ptr, animate, keepRatio, hideCursor);
    }

    @Override
    protected void _exitFullscreen(long ptr, boolean animate) {
        GtkGlassNative.viewExitFullscreen(ptr, animate);
    }

    @Override
    protected void _finishInputMethodComposition(long ptr) {
        if (imEnabled) {
            enableInputMethodEventsImpl(ptr, true);
        }
    }

    protected double[] notifyInputMethodCandidateRelativePosRequest(int offset) {
        return convertPosToRelative(super.notifyInputMethodCandidatePosRequest(offset));
    }

    private double[] convertPosToRelative(double[] pos) {
        var w = getWindow();

        if (w != null) {
            pos[0] -= (w.getX() + getX());
            pos[1] -= (w.getY() + getY());
        }

        return pos;
    }

    protected void notifyInputMethodLinux(String str, int commitLength, int cursor, byte attr) {
        if (commitLength > 0) {
            notifyInputMethod(str, null, null, null, commitLength, cursor, 0);
        } else {
            int[] attBounds = new int[] { 0, str.length() };
            byte[] attValues = new byte[] { attr };

            notifyInputMethod(str, attBounds, attBounds, attValues, 0, cursor, 0);
        }
    }

    @Override
    protected void notifyMenu(int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        // If all of the following conditions are satisfied, we open a system menu at the specified coordinates:
        // 1. The application didn't consume the menu event.
        // 2. The window is an EXTENDED window and is not in full-screen mode.
        // 3. The menu event occurred on a draggable area.
        if (!handleMenuEvent(x, y, xAbs, yAbs, isKeyboardTrigger)) {
            var window = (GtkWindow)getWindow();
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
        return getWindow() instanceof GtkWindow window
            && window.headerButtonOverlayProperty().get() instanceof HeaderButtonOverlay overlay
            && overlay.handleMouseEvent(type, button, x / window.getPlatformScaleX(), y / window.getPlatformScaleY());
    }
}
