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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.HeaderButtonMetrics;
import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

/**
 * MS Windows platform implementation class for Window.
 */
class WinWindow extends Window {
    public static final int RESIZE_DISABLE = 0;
    public static final int RESIZE_AROUND_ANCHOR = 1;
    public static final int RESIZE_TO_FX_ORIGIN = 2;

    public static final long ANCHOR_NO_CAPTURE = (1L << 63);

    private float fxReqWidth;
    private float fxReqHeight;
    private int pfReqWidth;
    private int pfReqHeight;

    private native static void _initIDs();
    static {
        _initIDs();
    }

    protected WinWindow(Window owner, Screen screen, int styleMask) {
        super(owner, screen, styleMask);

        if (isExtendedWindow()) {
            prefHeaderButtonHeightProperty().subscribe(this::onPrefHeaderButtonHeightChanged);
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
            pw = Math.max(Math.min(pw, maxW > 0 ? maxW : Integer.MAX_VALUE), getMinimumWidth());
            ph = Math.max(Math.min(ph, maxH > 0 ? maxH : Integer.MAX_VALUE), getMinimumHeight());

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
            _setBounds(getRawHandle(), px, py, xSet, ySet, pw, ph, 0, 0, xGravity, yGravity);
        }
    }

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

    native protected boolean _setBackground2(long ptr, float r, float g, float b);
    @Override
    protected boolean _setBackground(long ptr, float r, float g, float b) {
        // Revert to old behavior for standalone application on Windows as the
        // call to setBackground causes flickering when resizing window.
        // For more details see JDK-8171852: JavaFX Stage flickers on resize on
        // Windows platforms
        return true;
    }

    private native void _setDarkFrame(long ptr, boolean value);

    @Override
    public void setDarkFrame(boolean value) {
        _setDarkFrame(getRawHandle(), value);
    }

    native private long _getInsets(long ptr);
    native private long _getAnchor(long ptr);
    native private void _showSystemMenu(long ptr, int x, int y);
    @Override native protected long _createWindow(long ownerPtr, long screenPtr, int mask);
    @Override native protected boolean _close(long ptr);
    @Override native protected boolean _setView(long ptr, View view);
    @Override native protected void _updateViewSize(long ptr);
    @Override native protected boolean _setMenubar(long ptr, long menubarPtr);
    @Override native protected boolean _minimize(long ptr, boolean minimize);
    @Override native protected boolean _maximize(long ptr, boolean maximize, boolean wasMaximized);
    @Override native protected void _setBounds(long ptr, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch, float xGravity, float yGravity);
    @Override native protected boolean _setVisible(long ptr, boolean visible);
    @Override native protected boolean _setResizable(long ptr, boolean resizable);
    @Override native protected boolean _requestFocus(long ptr, int event);
    @Override native protected void _setFocusable(long ptr, boolean isFocusable);
    @Override native protected boolean _setTitle(long ptr, String title);
    @Override native protected void _setLevel(long ptr, int level);
    @Override native protected void _setAlpha(long ptr, float alpha);
    @Override native protected void _setEnabled(long ptr, boolean enabled);
    @Override native protected boolean _setMinimumSize(long ptr, int width, int height);
    @Override native protected boolean _setMaximumSize(long ptr, int width, int height);
    @Override native protected void _setIcon(long ptr, Pixels pixels);
    @Override native protected void _toFront(long ptr);
    @Override native protected void _toBack(long ptr);
    @Override native protected boolean _grabFocus(long ptr);
    @Override native protected void _ungrabFocus(long ptr);
    @Override native protected void _setCursor(long ptr, Cursor cursor);

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
        _showSystemMenu(getRawHandle(), x, y);
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

        overlay.prefButtonHeightProperty().bind(prefHeaderButtonHeightProperty());
        return overlay;
    }

    /**
     * Classifies the window region at the specified physical coordinate.
     * <p>
     * This method is called from native code.
     *
     * @param x the X coordinate in physical pixels
     * @param y the Y coordinate in physical pixels
     */
    @SuppressWarnings("unused")
    private int nonClientHitTest(int x, int y) {
        // https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest
        enum HT {
            CLIENT(1), CAPTION(2), MINBUTTON(8), MAXBUTTON(9), CLOSE(20);
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

        // Otherwise, test if the cursor is over a draggable area and return HTCAPTION.
        View.EventHandler eventHandler = view.getEventHandler();
        return switch (eventHandler != null ? eventHandler.pickHeaderArea(wx, wy) : null) {
            case DRAGBAR -> HT.CAPTION.value;
            case ICONIFY -> HT.MINBUTTON.value;
            case MAXIMIZE -> HT.MAXBUTTON.value;
            case CLOSE -> HT.CLOSE.value;
            case null -> HT.CLIENT.value;
        };
    }
}
