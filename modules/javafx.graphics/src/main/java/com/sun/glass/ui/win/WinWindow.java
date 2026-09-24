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

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.HeaderButtonMetrics;
import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import java.lang.annotation.Native;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MS Windows platform implementation class for Window.
 * <p>
 * No native method is left on this class. The twenty-seven {@code GlassWindow.cpp} bodies that were
 * {@code OS-CALL} or native {@code WndProc} state are the {@code gwin_window_*} exports behind the
 * {@link WinGlassNative#windowCreate} family; {@code _getAnchor} is {@link WinGlassNative#windowAnchor}
 * over four {@code user32} calls; {@code _initIDs} cached the twelve method ids that
 * {@code GwinWindowCallbacks} now carries; {@code _setBackground2} had no caller. The handle in
 * {@code Window.ptr} is what it always was, the HWND.
 * <p>
 * The other direction - WM_CLOSE, WM_DESTROY, WM_WINDOWPOSCHANGING, WM_MOVE, WM_SIZE, WM_DPICHANGED,
 * WM_ACTIVATE, the disabled-window and grab notifications, the fullscreen delegate, WM_NCHITTEST for
 * EXTENDED windows, and the destructor - arrives through the twelve slots of that table, each carrying
 * the {@code int64_t} id this class registered itself under before it asked for the HWND
 * ({@link #_createWindow}). The registry is the FFM counterpart of the {@code NewGlobalRef} the C++
 * object held: strong, inserted before {@code gwin_window_create}, removed by {@code notify_dispose}
 * from {@code ~GlassWindow} and by nothing else - not by {@code notifyDestroy}, not by
 * {@code close()}, not by a {@code Cleaner} - because slots can and do fire between WM_DESTROY and the
 * deferred delete, and under JNI the global ref was alive then too. The one exception is the
 * {@code gwin_window_create} that returns 0: then either the C++ object was already deleted (and its
 * {@code notify_dispose} has already removed the entry) or, with no toolkit, none was ever
 * constructed and nothing will ever fire, so {@link #_createWindow} removes the entry itself.
 */
class WinWindow extends Window {
    /*
     * @Native, as Window's constants are, on the two GlassWindow.cpp still reads (RESIZE_DISABLE /
     * RESIZE_AROUND_ANCHOR in HandleWindowPosChangingEvent, every WM_WINDOWPOSCHANGING) from the generated
     * com_sun_glass_ui_win_WinWindow.h: with no native method left in this class the annotation is what
     * keeps javac -h generating that header. RESIZE_TO_FX_ORIGIN and ANCHOR_NO_CAPTURE lost their only C
     * reader when the _getAnchor JNI body, unreachable since its Java flip, was deleted.
     */
    @Native public static final int RESIZE_DISABLE = 0;
    @Native public static final int RESIZE_AROUND_ANCHOR = 1;
    public static final int RESIZE_TO_FX_ORIGIN = 2;

    public static final long ANCHOR_NO_CAPTURE = (1L << 63);

    /**
     * Every window the native side can still call back about, by the id it was created with. The
     * strong reference is deliberate - it is the {@code m_grefThis} global ref of {@code GlassWindow}
     * moved to Java - and its lifetime is the C++ object's, not the peer's disposal state: see the
     * class comment.
     */
    private static final Map<Long, WinWindow> WINDOWS = new ConcurrentHashMap<>();

    /** Ids from 1: 0 is what a JNI-created GlassWindow would deliver, and the registry knows no 0. */
    private static final AtomicLong NEXT_WINDOW_ID = new AtomicLong(1L);

    private float fxReqWidth;
    private float fxReqHeight;
    private int pfReqWidth;
    private int pfReqHeight;

    /**
     * The id this window is registered under. <b>No initializer, on purpose</b>: {@link #_createWindow}
     * assigns it from inside {@code super(...)}, and a subclass field initializer runs after
     * {@code super()} returns and would silently wipe it back to 0.
     */
    private long nativeId;

    protected WinWindow(Window owner, Screen screen, int styleMask) {
        super(owner, screen, styleMask);

        if (isExtendedWindow()) {
            headerButtonHeightProperty().subscribe(this::onPrefHeaderButtonHeightChanged);
        }
    }

    @Override
    public void setBounds(float x, float y, boolean xSet, boolean ySet,
                          float w, float h, float cw, float ch,
                          float xGravity, float yGravity)
    {
        if (xSet || ySet || w > 0 || h > 0 || cw > 0 || ch > 0) {
            long insets = _getInsets(getRawHandle());
            int iLft = (int) (insets >> 48) & 0xffff;
            int iTop = (int) (insets >> 32) & 0xffff;
            int iRgt = (int) (insets >> 16) & 0xffff;
            int iBot = (int) (insets      ) & 0xffff;
            int px, py;
            if (xSet || ySet) {
                // If an origin coordinate is set we need to translate it to the
                // best platform location we can so we can do proper monitor
                // and scale updating.
                // To properly translate to platform space we need a pair of
                // FX coordinates to search for which screen they are on.
                // We know we have at least one FX coordinate available, but
                // we might not have both so we need to reverse calculate the
                // one that is missing in that case.
                // Then we find the best screen for those FX coordinates and
                // use that to calculate accurate platform coordinates, but
                // only for the coordinates that are being set - the other
                // coordinate will be kept from the existing location...
                if (xSet) {
                    px = screen.toPlatformX(x);
                } else {
                    px = this.x;
                    x = screen.fromPlatformX(px);
                }
                if (ySet) {
                    py = screen.toPlatformY(y);
                } else {
                    py = this.y;
                    y = screen.fromPlatformY(py);
                }
            } else {
                px = this.x;
                py = this.y;
            }

            float fx_cw, fx_ch;
            int pw, ph;
            if (w > 0) {
                fx_cw = w - (iLft + iRgt) / platformScaleX;
                pw = (int) Math.ceil(w * platformScaleX);
            } else {
                fx_cw = (cw > 0) ? cw : fxReqWidth;
                pw = iLft + iRgt + (int) Math.ceil(fx_cw * platformScaleX);
            }
            fxReqWidth = fx_cw;
            if (h > 0) {
                fx_ch = h - (iTop + iBot) / platformScaleY;
                ph = (int) Math.ceil(h * platformScaleY);
            } else {
                fx_ch = (ch > 0) ? ch : fxReqHeight;
                ph = iTop + iBot + (int) Math.ceil(fx_ch * platformScaleY);
            }
            fxReqHeight = fx_ch;

            int maxW = getMaximumWidth(), maxH = getMaximumHeight();
            int oldPw = pw;
            int oldPh = ph;
            pw = Math.max(Math.min(pw, maxW > 0 ? maxW : Integer.MAX_VALUE), getMinimumWidth());
            ph = Math.max(Math.min(ph, maxH > 0 ? maxH : Integer.MAX_VALUE), getMinimumHeight());
            boolean minMaxEnforced = (oldPw != pw || oldPh != ph);

            long anchor = _getAnchor(getRawHandle());
            int resizeMode = (anchor == ANCHOR_NO_CAPTURE)
                    ? RESIZE_TO_FX_ORIGIN
                    : RESIZE_AROUND_ANCHOR;
            int anchorX = (int) (anchor >> 32);
            int anchorY = (int) (anchor);

            int overrideDims[] = notifyMoving(px, py, pw, ph,
                                              x, y, anchorX, anchorY, resizeMode,
                                              iLft, iTop, iRgt, iBot);
            if (overrideDims != null) {
                px = overrideDims[0];
                py = overrideDims[1];
                pw = overrideDims[2];
                ph = overrideDims[3];
            }
            // The origin could have changed either due to the actions of
            // the notifyMoving method or by our code to map a single incoming
            // origin coordinate above.  If they don't match the previously
            // recorded location, then we need to make sure to send the new
            // value to the platform code...
            if (!xSet) xSet = (px != this.x);
            if (!ySet) ySet = (py != this.y);
            pfReqWidth = (int) Math.ceil(fxReqWidth * platformScaleX);
            pfReqHeight = (int) Math.ceil(fxReqHeight * platformScaleY);
            boolean alreadyAtSize = (pw == width && ph == height);
            _setBounds(getRawHandle(), px, py, xSet, ySet, pw, ph, 0, 0, xGravity, yGravity);

            if (minMaxEnforced && alreadyAtSize) {
                var eventType = WindowEvent.RESIZE;
                if (isMaximized()) {
                    eventType = WindowEvent.MAXIMIZE;
                } else if (isMinimized()) {
                    eventType = WindowEvent.MINIMIZE;
                }
                notifyResize(eventType, pw, ph);
            }
        }
    }

    /**
     * Called from {@link #setBounds} and, through the {@code notify_moving} slot, from
     * {@code GlassWindow::HandleWindowPosChangingEvent} on every WM_WINDOWPOSCHANGING; the slot
     * always passes {@code 0.0f} for {@code fx_x} / {@code fx_y}, which are read only under
     * {@code RESIZE_TO_FX_ORIGIN}, a mode only the {@link #setBounds} caller uses.
     *
     * @return the platform bounds to use instead, or {@code null} for "no override"
     */
    protected int[] notifyMoving(int x, int y, int w, int h,
                                 float fx_x, float fx_y,
                                 int anchorX, int anchorY,
                                 int resizeMode,
                                 int iLft, int iTop, int iRgt, int iBot)
    {
        final Window owner = getOwner();
        final Screen popupOwnerScreen = isPopup() && owner != null ? owner.getScreen() : null;
        final boolean usePopupScreen = popupOwnerScreen != null;

        if (usePopupScreen && screen == popupOwnerScreen) {
            // Suppress screen switch
            return null;
        }

        if (screen == null || usePopupScreen || !screen.containsPlatformRect(x, y, w, h)) {
            float bestPortion = (screen == null) ? 0.0f
                    : screen.portionIntersectsPlatformRect(x, y, w, h);
            if (usePopupScreen || bestPortion < 0.5f) {
                float relAnchorX = anchorX / platformScaleX;
                float relAnchorY = anchorY / platformScaleY;
                Screen bestScreen = screen;
                int bestx = x;
                int besty = y;
                int bestw = w;
                int besth = h;
                for (Screen scr : Screen.getScreens()) {
                    if (scr == screen) continue;
                    int newx, newy, neww, newh;
                    if (resizeMode == RESIZE_DISABLE) {
                        newx = x;
                        newy = y;
                        neww = w;
                        newh = h;
                    } else {
                        int newcw = (int) Math.ceil(fxReqWidth * scr.getPlatformScaleX());
                        int newch = (int) Math.ceil(fxReqHeight * scr.getPlatformScaleY());
                        neww = newcw + iLft + iRgt;
                        newh = newch + iTop + iBot;
                        if (resizeMode == RESIZE_AROUND_ANCHOR) {
                            newx = x + anchorX - Math.round(relAnchorX * scr.getPlatformScaleX());
                            newy = y + anchorY - Math.round(relAnchorY * scr.getPlatformScaleY());
                        } else {
                            newx = scr.toPlatformX(fx_x);
                            newy = scr.toPlatformY(fx_y);
                        }
                    }
                    float portion = scr.portionIntersectsPlatformRect(newx, newy, neww, newh);
                    if (scr == popupOwnerScreen ||
                            (screen == null || portion > 0.6f && portion > bestPortion) && !usePopupScreen) {

                        bestPortion = portion;
                        bestScreen = scr;
                        bestx = newx;
                        besty = newy;
                        bestw = neww;
                        besth = newh;
                    }
                }
                if (bestScreen != screen) {
                    notifyMoveToAnotherScreen(bestScreen);
                    notifyScaleChanged(bestScreen.getPlatformScaleX(),
                                       bestScreen.getPlatformScaleY(),
                                       bestScreen.getRecommendedOutputScaleX(),
                                       bestScreen.getRecommendedOutputScaleY());
                    if (view != null) {
                        view.updateLocation();
                    }
                    if (resizeMode == RESIZE_DISABLE) {
                        return null;
                    } else {
                        return new int[] { bestx, besty, bestw, besth };
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void notifyResize(int type, int width, int height) {
        float oldScaleX = platformScaleX;
        float oldScaleY = platformScaleY;
        long insets = _getInsets(getRawHandle());
        int iLft = (int) (insets >> 48) & 0xffff;
        int iTop = (int) (insets >> 32) & 0xffff;
        int iRgt = (int) (insets >> 16) & 0xffff;
        int iBot = (int) (insets      ) & 0xffff;
        int pcw = (width - iLft - iRgt);
        int pch = (height - iTop - iBot);
        if (pcw != pfReqWidth || oldScaleX != platformScaleX) {
            fxReqWidth = pcw / platformScaleX;
            pfReqWidth = pcw;
        }
        if (pch != pfReqHeight || oldScaleY != platformScaleY) {
            fxReqHeight = pch / platformScaleY;
            pfReqHeight = pch;
        }
        super.notifyResize(type, width, height);
    }

    @Override
    protected boolean _setBackground(long ptr, float r, float g, float b) {
        // Revert to old behavior for standalone application on Windows as the
        // call to setBackground causes flickering when resizing window.
        // For more details see JDK-8171852: JavaFX Stage flickers on resize on
        // Windows platforms
        return true;
    }

    @Override
    public void setDarkFrame(boolean value) {
        WinGlassNative.windowSetDarkFrame(getRawHandle(), value);
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinWindow__1getInsets}: the packed insets, or 0 when the
     * handle is not a window; not marshalled, so it runs on this thread and can race the WndProc's
     * own {@code UpdateInsets} off the toolkit thread exactly as the JNI did.
     */
    private long _getInsets(long ptr) {
        return WinGlassNative.windowGetInsets(ptr);
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinWindow__1getAnchor}, the one {@code WRAPPER} of this peer,
     * now four {@code user32} calls in {@link WinGlassNative#windowAnchor}: 0 when the handle is not
     * a window, the packed cursor-minus-origin when this window holds the mouse capture, and
     * {@link #ANCHOR_NO_CAPTURE} otherwise - including when either OS query fails.
     */
    private long _getAnchor(long ptr) {
        return WinGlassNative.windowAnchor(ptr);
    }

    // ---- the registry the callback table resolves against ----

    /**
     * Takes the next id and publishes {@code window} under it. Called before {@code gwin_window_create}
     * learns the id, so that a {@code notify_dispose} which fires inside the creating downcall - the
     * {@code CreateWindowEx}-failed path deletes the object before returning - finds its entry.
     */
    static long register(WinWindow window) {
        long id = NEXT_WINDOW_ID.getAndIncrement();
        window.nativeId = id;
        WINDOWS.put(id, window);
        return id;
    }

    /**
     * Drops a registry entry. Product code calls this from exactly one place, the rejected
     * {@link #_createWindow}; {@code notify_dispose} goes through {@link #dispose}. The headless
     * windows of {@code WinWindowNativeTest}, which no C++ object ever backs, use it too.
     */
    static void unregister(long id) {
        WINDOWS.remove(id);
    }

    /** How many windows the registry holds; the FFM counterpart of the JNI's live global refs. */
    static int windowRegistrySize() {
        return WINDOWS.size();
    }

    /** The id {@link #register} gave this window, 0 if it was never registered. */
    long nativeId() {
        return nativeId;
    }

    /** The window of {@code id}, or {@code null} for 0 and for an id that is gone. */
    static WinWindow windowFor(long id) {
        return WINDOWS.get(id);
    }

    /*
     * The dispatch half of the twelve upcall slots: registry lookup, then the protected Window
     * method - reachable only from a Window subclass, which is why these live here and not in the
     * facade - through a WinWindow reference, so the notifyResize override is the one that runs. An
     * unknown id returns the slot default silently. None of these may take a lock: they run inside
     * DispatchMessage, inside the CBT hook, or inside a gwin_window_* downcall, possibly on another
     * window's WndProc frame.
     */

    static void dispatchClose(long id) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyClose();
        }
    }

    static void dispatchDestroy(long id) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyDestroy();
        }
    }

    static int[] dispatchMoving(long id, int x, int y, int w, int h, float fxX, float fxY, int anchorX,
                                int anchorY, int resizeMode, int iLft, int iTop, int iRgt, int iBot) {
        WinWindow window = WINDOWS.get(id);
        return window == null ? null
                : window.notifyMoving(x, y, w, h, fxX, fxY, anchorX, anchorY, resizeMode, iLft, iTop, iRgt, iBot);
    }

    static void dispatchMove(long id, int x, int y) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyMove(x, y);
        }
    }

    static void dispatchResize(long id, int type, int width, int height) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyResize(type, width, height);
        }
    }

    static void dispatchScaleChanged(long id, float platformX, float platformY, float outputX, float outputY) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyScaleChanged(platformX, platformY, outputX, outputY);
        }
    }

    static void dispatchFocus(long id, int event) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyFocus(event);
        }
    }

    static void dispatchFocusDisabled(long id) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyFocusDisabled();
        }
    }

    static void dispatchFocusUngrab(long id) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyFocusUngrab();
        }
    }

    static void dispatchDelegatePtr(long id, long delegateHwnd) {
        WinWindow window = WINDOWS.get(id);
        if (window != null) {
            window.notifyDelegatePtr(delegateHwnd);
        }
    }

    /** {@link WinGlassNative#HTNOWHERE} for an unknown id, as for a throw. */
    static int dispatchNonClientHitTest(long id, int x, int y) {
        WinWindow window = WINDOWS.get(id);
        return window == null ? WinGlassNative.HTNOWHERE : window.nonClientHitTest(x, y);
    }

    /**
     * {@code notify_dispose}: the registry entry goes, and nothing else happens - no event, no
     * listener, nothing that could reach a {@code gwin_window_*} call, because {@code FromHandle} is
     * already NULL inside {@code ~GlassWindow}.
     */
    static void dispose(long id) {
        WINDOWS.remove(id);
    }

    // ---- the former natives, each one facade call ----

    /**
     * Registers this window first, then asks for the HWND: the id is what every slot will carry
     * back, and {@code notify_dispose} can fire before {@code gwin_window_create} returns. A return
     * of 0 - which the {@code Window} constructor rejects - means no C++ object will ever fire that
     * slot for this id, so the entry is removed here; on the {@code CreateWindowEx}-failed path the
     * removal is a no-op because {@code notify_dispose} already ran. With no toolkit the return is
     * indeterminate and is stored as the JNI's was.
     */
    @Override
    protected long _createWindow(long ownerPtr, long screenPtr, int mask) {
        long id = register(this);
        long hwnd = WinGlassNative.windowCreate(ownerPtr, screenPtr, mask, id);
        if (hwnd == 0L) {
            unregister(id);
        }
        return hwnd;
    }

    @Override
    protected boolean _close(long ptr) {
        return WinGlassNative.windowClose(ptr);
    }

    /**
     * The {@code GlassView*} the JNI read from {@code View.ptr} is {@link WinView#nativeHandle()}. A
     * {@code View} that is not a {@code WinView} cannot exist on this platform ({@code WinApplication}
     * creates only those) and would cross as no view.
     */
    @Override
    protected boolean _setView(long ptr, View view) {
        long handle = view instanceof WinView winView ? winView.nativeHandle() : 0L;
        return WinGlassNative.windowSetView(ptr, handle);
    }

    @Override
    protected void _updateViewSize(long ptr) {
        WinGlassNative.windowUpdateViewSize(ptr);
    }

    @Override
    protected boolean _setMenubar(long ptr, long menubarPtr) {
        return WinGlassNative.windowSetMenubar(ptr, menubarPtr);
    }

    @Override
    protected boolean _minimize(long ptr, boolean minimize) {
        return WinGlassNative.windowMinimize(ptr, minimize);
    }

    @Override
    protected boolean _maximize(long ptr, boolean maximize, boolean wasMaximized) {
        return WinGlassNative.windowMaximize(ptr, maximize, wasMaximized);
    }

    @Override
    protected void _setBounds(long ptr, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch,
                              float xGravity, float yGravity) {
        WinGlassNative.windowSetBounds(ptr, x, y, xSet, ySet, w, h, cw, ch, xGravity, yGravity);
    }

    @Override
    protected boolean _setVisible(long ptr, boolean visible) {
        return WinGlassNative.windowSetVisible(ptr, visible);
    }

    @Override
    protected boolean _setResizable(long ptr, boolean resizable) {
        return WinGlassNative.windowSetResizable(ptr, resizable);
    }

    @Override
    protected boolean _requestFocus(long ptr, int event) {
        return WinGlassNative.windowRequestFocus(ptr, event);
    }

    @Override
    protected void _setFocusable(long ptr, boolean isFocusable) {
        WinGlassNative.windowSetFocusable(ptr, isFocusable);
    }

    @Override
    protected boolean _setTitle(long ptr, String title) {
        return WinGlassNative.windowSetTitle(ptr, title);
    }

    @Override
    protected void _setLevel(long ptr, int level) {
        WinGlassNative.windowSetLevel(ptr, level);
    }

    @Override
    protected void _setAlpha(long ptr, float alpha) {
        WinGlassNative.windowSetAlpha(ptr, alpha);
    }

    @Override
    protected void _setEnabled(long ptr, boolean enabled) {
        WinGlassNative.windowSetEnabled(ptr, enabled);
    }

    @Override
    protected boolean _setMinimumSize(long ptr, int width, int height) {
        return WinGlassNative.windowSetMinSize(ptr, width, height);
    }

    @Override
    protected boolean _setMaximumSize(long ptr, int width, int height) {
        return WinGlassNative.windowSetMaxSize(ptr, width, height);
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinWindow__1setIcon}: the {@code HICON} that
     * {@code Pixels::CreateIcon} built is built here ({@link WinGlassNative#iconCreate}, the cursor
     * sequence with {@code fIcon = TRUE}) and handed to the window, which owns it from then on and
     * destroys it on the next call or in its destructor. A null {@code Pixels} installs no icon, as
     * the JNI's {@code !jPixels} branch did. {@code getWidthUnsafe} / {@code getHeightUnsafe} /
     * {@code getBuffer} for the reasons {@code WinCursor} gives.
     */
    @Override
    protected void _setIcon(long ptr, Pixels pixels) {
        long icon = pixels == null ? 0L
                : WinGlassNative.iconCreate(pixels.getWidthUnsafe(), pixels.getHeightUnsafe(), pixels.getBuffer());
        WinGlassNative.windowSetIcon(ptr, icon);
    }

    @Override
    protected void _toFront(long ptr) {
        WinGlassNative.windowToFront(ptr);
    }

    @Override
    protected void _toBack(long ptr) {
        WinGlassNative.windowToBack(ptr);
    }

    @Override
    protected boolean _grabFocus(long ptr) {
        return WinGlassNative.windowGrabFocus(ptr);
    }

    @Override
    protected void _ungrabFocus(long ptr) {
        WinGlassNative.windowUngrabFocus(ptr);
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinWindow__1setCursor}: the {@code Cursor} to {@code HCURSOR}
     * half ({@code JCursorToHCURSOR}) is {@link WinCursor#nativeHandleFor}, the {@code SetCursor}
     * half stays in C.
     */
    @Override
    protected void _setCursor(long ptr, Cursor cursor) {
        WinGlassNative.windowSetCursor(ptr, WinCursor.nativeHandleFor(cursor));
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


    private boolean deferredClosing = false;
    private boolean closingRequested = false;

    /**
     * Defer destroying the window to avoid a crash when using a native dialog
     * (like a file chooser).
     */
    void setDeferredClosing(boolean dc) {
        deferredClosing = dc;
        if (!deferredClosing && closingRequested) {
            close();
        }
    }

    @Override public void close() {
        if (!deferredClosing) {
            if (headerButtonOverlay.get() instanceof HeaderButtonOverlay overlay) {
                overlay.dispose();
            }

            super.close();
        } else {
            closingRequested = true;
            setVisible(false);
        }
    }

    /**
     * Opens a system menu at the specified coordinates.
     *
     * @param x the X coordinate in physical pixels
     * @param y the Y coordinate in physical pixels
     */
    public void showSystemMenu(int x, int y) {
        WinGlassNative.windowShowSystemMenu(getRawHandle(), x, y);
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
        var overlay = new WinHeaderButtonOverlay(
            isModal() || getOwner() != null, isUtilityWindow(),
            (getStyleMask() & RIGHT_TO_LEFT) != 0);

        overlay.buttonPrefHeightProperty().bind(headerButtonHeightProperty());
        overlay.buttonDarkStyleProperty().bind(headerButtonDarkStyleProperty());
        return overlay;
    }

    /**
     * Classifies the window region at the specified physical coordinate.
     * <p>
     * Called from native code through the {@code non_client_hit_test} slot of
     * {@code GwinWindowCallbacks}, on every WM_NCHITTEST of an EXTENDED window; the C
     * post-processes the answer ({@code HandleNCHitTestEvent}) and answers {@code HTNOWHERE} when
     * this throws.
     *
     * @param x the X coordinate in physical pixels
     * @param y the Y coordinate in physical pixels
     */
    private int nonClientHitTest(int x, int y) {
        // https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest
        enum HT {
            CLIENT(1), CAPTION(2), MINBUTTON(8), MAXBUTTON(9), CLOSE(20),
            UNSPECIFIED('H' << 24 | 'T' << 16 | 'U' << 8 | 'N'); // see GlassWindow.cpp:HandleNCHitTestEvent
            HT(int value) { this.value = value; }
            final int value;
        }

        // A full-screen window has no non-client area.
        if (view == null || view.isInFullscreen() || !isExtendedWindow()) {
            return HT.CLIENT.value;
        }

        double wx = x / platformScaleX;
        double wy = y / platformScaleY;

        // If the cursor is over one of the window buttons (minimize, maximize, close), we need to
        // report the value of HTMINBUTTON, HTMAXBUTTON, or HTCLOSE back to the native layer.
        switch (headerButtonOverlay.get() instanceof HeaderButtonOverlay overlay ? overlay.buttonAt(wx, wy) : null) {
            case ICONIFY: return HT.MINBUTTON.value;
            case MAXIMIZE: return HT.MAXBUTTON.value;
            case CLOSE: return HT.CLOSE.value;
            case null: break;
        }

        // Otherwise, pick the header area under the cursor and return the appropriate hit-testing constant.
        View.EventHandler eventHandler = view.getEventHandler();
        return switch (eventHandler != null ? eventHandler.pickHeaderArea(wx, wy) : null) {
            case UNSPECIFIED -> HT.UNSPECIFIED.value;
            case DRAGBAR -> HT.CAPTION.value;
            case ICONIFY -> HT.MINBUTTON.value;
            case MAXIMIZE -> HT.MAXBUTTON.value;
            case CLOSE -> HT.CLOSE.value;
            case null -> HT.CLIENT.value;
        };
    }
}
