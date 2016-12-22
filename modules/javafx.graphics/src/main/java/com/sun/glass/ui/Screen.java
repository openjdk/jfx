/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Screen {

    // the list of attached screens provided by native.
    // screens[0] is the default/main Screen
    private static volatile List<Screen> screens = null ;

    // If dpiOverride is non-zero, use its value as screen DPI
    private static final int dpiOverride;

    static {
        dpiOverride = AccessController.doPrivileged((PrivilegedAction<Integer>) () -> Integer.getInteger("com.sun.javafx.screenDPI", 0)).intValue();
    }

    public static class EventHandler {
        public void handleSettingsChanged() {
        }
    }

    public static double getVideoRefreshPeriod() {
        Application.checkEventThread();
        return Application.GetApplication().staticScreen_getVideoRefreshPeriod();
    }

    /**
     * Could be called from any thread
     * @return the main screen
     */
    public static Screen getMainScreen() {
        return getScreens().get(0);
    }

    /**
     * Could be called from any thread
     * @return list of all available screens
     */
    public static List<Screen> getScreens() {
        if (screens == null) {
            throw new RuntimeException("Internal graphics not initialized yet");
        }

        return screens;
    }

    private static EventHandler eventHandler;

    private volatile long ptr;
    private volatile int adapter;

    private final int depth;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final int platformX;
    private final int platformY;
    private final int platformWidth;
    private final int platformHeight;

    private final int visibleX;
    private final int visibleY;
    private final int visibleWidth;
    private final int visibleHeight;

    private final int resolutionX;
    private final int resolutionY;

    private final float platformScaleX;
    private final float platformScaleY;
    private final float outputScaleX;
    private final float outputScaleY;

    public Screen(
            long nativePtr,

            int depth,
            int x,
            int y,
            int width,
            int height,

            int platformX,
            int platformY,
            int platformWidth,
            int platformHeight,

            int visibleX,
            int visibleY,
            int visibleWidth,
            int visibleHeight,

            int resolutionX,
            int resolutionY,

            float platformScaleX,
            float platformScaleY,
            float outputScaleX,
            float outputScaleY
            ) {
        this.ptr = nativePtr;

        this.depth = depth;

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.platformX = platformX;
        this.platformY = platformY;
        this.platformWidth = platformWidth;
        this.platformHeight = platformHeight;

        this.visibleX = visibleX;
        this.visibleY = visibleY;
        this.visibleWidth = visibleWidth;
        this.visibleHeight = visibleHeight;

        if (dpiOverride > 0) {
            this.resolutionX = this.resolutionY = dpiOverride;
        } else {
            this.resolutionX = resolutionX;
            this.resolutionY = resolutionY;
        }

        this.platformScaleX = platformScaleX;
        this.platformScaleY = platformScaleY;
        this.outputScaleX = outputScaleX;
        this.outputScaleY = outputScaleY;
    }

    /**
     * Could be called from any thread
     */
    public int getDepth() {
        return this.depth;
    }

    /**
     * Could be called from any thread
     */
    public int getX() {
        return this.x;
    }

    /**
     * Could be called from any thread
     */
    public int getY() {
        return this.y;
    }

    /**
     * Could be called from any thread
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Could be called from any thread
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Could be called from any thread
     */
    public int getPlatformX() {
        return this.platformX;
    }

    /**
     * Could be called from any thread
     */
    public int getPlatformY() {
        return this.platformY;
    }

    /**
     * Could be called from any thread
     */
    public int getPlatformWidth() {
        return this.platformWidth;
    }

    /**
     * Could be called from any thread
     */
    public int getPlatformHeight() {
        return this.platformHeight;
    }

    public float fromPlatformX(int px) {
        return this.x + (px - platformX) / platformScaleX;
    }

    public float fromPlatformY(int py) {
        return this.y + (py - platformY) / platformScaleY;
    }

    public int toPlatformX(float ux) {
        return platformX + Math.round((ux - this.x) * platformScaleX);
    }

    public int toPlatformY(float uy) {
        return platformY + Math.round((uy - this.y) * platformScaleY);
    }

    public float portionIntersectsPlatformRect(int x, int y, int w, int h) {
        int x0 = Math.max(x, platformX);
        int y0 = Math.max(y, platformY);
        int x1 = Math.min(x + w, platformX + platformWidth);
        int y1 = Math.min(y + h, platformY + platformHeight);
        if ((x1 -= x0) <= 0) return 0.0f;
        if ((y1 -= y0) <= 0) return 0.0f;
        float ret = x1 * y1;
        return (ret / w) / h;
    }

    public boolean containsPlatformRect(int x, int y, int w, int h) {
        if (!containsPlatformCoords(x, y)) return false;
        if (w <= 0 || h <= 0) return true;
        return (x + w <= platformX + platformWidth &&
                y + h <= platformY + platformHeight);
    }

    public boolean containsPlatformCoords(int x, int y) {
        x -= platformX;
        y -= platformY;
        return (x >= 0 && x < platformWidth &&
                y >= 0 && y < platformHeight);
    }

    /**
     * Returns the horizontal scaling of the UI (window sizes and event
     * coordinates) from FX logical units to the platform units.
     * Could be called from any thread
     * @return platform X scaling
     */
    public float getPlatformScaleX() {
        return this.platformScaleX;
    }

    /**
     * Returns the vertical scaling of the UI (window sizes and event
     * coordinates) from FX logical units to the platform units.
     * Could be called from any thread
     * @return platform Y scaling
     */
    public float getPlatformScaleY() {
        return this.platformScaleY;
    }

    /**
     * Returns the recommended horizontal scaling for the rendered frames.
     * Could be called from any thread
     * @return recommended render X scaling
     */
    public float getRecommendedOutputScaleX() {
        return this.outputScaleX;
    }

    /**
     * Returns the recommended vertical scaling for the rendered frames.
     * Could be called from any thread
     * @return recommended render Y scaling
     */
    public float getRecommendedOutputScaleY() {
        return this.outputScaleY;
    }

    /**
     * Could be called from any thread
     */
    public int getVisibleX() {
        return this.visibleX;
    }

    /**
     * Could be called from any thread
     */
    public int getVisibleY() {
        return this.visibleY;
    }

    /**
     * Could be called from any thread
     */
    public int getVisibleWidth() {
        return this.visibleWidth;
    }

    /**
     * Could be called from any thread
     */
    public int getVisibleHeight() {
        return this.visibleHeight;
    }

    /**
     * Could be called from any thread
     */
    public int getResolutionX() {
        return this.resolutionX;
    }

    /**
     * Could be called from any thread
     */
    public int getResolutionY() {
        return this.resolutionY;
    }

    /**
     * Could be called from any thread
     */
    public long getNativeScreen() {
        return this.ptr;
    }

    private void dispose() {
        this.ptr = 0L;
    }

    public int getAdapterOrdinal() {
        return this.adapter;
    }

    public void setAdapterOrdinal(int adapter) {
        this.adapter = adapter;
    }

    public static void setEventHandler(EventHandler eh) {
        Application.checkEventThread();
        eventHandler = eh;
    }

    /**
     * Called from native when the Screen definitions change.
     */
    public static void notifySettingsChanged() {
        // Save the old screens in order to dispose them later
        List<Screen> oldScreens = screens;

        // Get the new screens
        initScreens();

        if (eventHandler != null) {
            eventHandler.handleSettingsChanged();
        }

        // Update the screen for each window to match the new instance.
        // Note that if a window has moved to another screen, the window
        // will be notified separately of that from native code and the
        // new screen will be updated there
        List<Window> windows = Window.getWindows();
        for (Window w : windows) {
            Screen oldScreen = w.getScreen();
            for (Screen newScreen : screens) {
                if (oldScreen.getNativeScreen() == newScreen.getNativeScreen()) {
                    w.setScreen(newScreen);
                    break;
                }
            }
        }

        // Dispose the old screens
        if (oldScreens != null) {
            for (Screen screen : oldScreens) {
                screen.dispose();
            }
        }
    }

    static void initScreens() {
        Application.checkEventThread();
        Screen[] newScreens = Application.GetApplication().staticScreen_getScreens();
        if (newScreens == null) {
            throw new RuntimeException("Internal graphics failed to initialize");
        }
        screens = Collections.unmodifiableList(Arrays.asList(newScreens));
    }

    @Override public String toString() {
        return  "Screen:"+"\n"+
                "    ptr:"+getNativeScreen()+"\n"+
                "    adapter:"+getAdapterOrdinal()+"\n"+
                "    depth:"+getDepth()+"\n"+
                "    x:"+getX()+"\n"+
                "    y:"+getY()+"\n"+
                "    width:"+getWidth()+"\n"+
                "    height:"+getHeight()+"\n"+
                "    platformX:"+getPlatformX()+"\n"+
                "    platformY:"+getPlatformY()+"\n"+
                "    platformWidth:"+getPlatformWidth()+"\n"+
                "    platformHeight:"+getPlatformHeight()+"\n"+
                "    visibleX:"+getVisibleX()+"\n"+
                "    visibleY:"+getVisibleY()+"\n"+
                "    visibleWidth:"+getVisibleWidth()+"\n"+
                "    visibleHeight:"+getVisibleHeight()+"\n"+
                "    platformScaleX:"+getPlatformScaleX()+"\n"+
                "    platformScaleY:"+getPlatformScaleY()+"\n"+
                "    outputScaleX:"+getRecommendedOutputScaleX()+"\n"+
                "    outputScaleY:"+getRecommendedOutputScaleY()+"\n"+
                "    resolutionX:"+getResolutionX()+"\n"+
                "    resolutionY:"+getResolutionY()+"\n";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Screen screen = (Screen) o;
        return ptr == screen.ptr
                && adapter == screen.adapter
                && depth == screen.depth
                && x == screen.x
                && y == screen.y
                && width == screen.width
                && height == screen.height
                && visibleX == screen.visibleX
                && visibleY == screen.visibleY
                && visibleWidth == screen.visibleWidth
                && visibleHeight == screen.visibleHeight
                && resolutionX == screen.resolutionX
                && resolutionY == screen.resolutionY
                && Float.compare(screen.platformScaleX, platformScaleX) == 0
                && Float.compare(screen.platformScaleY, platformScaleY) == 0
                && Float.compare(screen.outputScaleX, outputScaleX) == 0
                && Float.compare(screen.outputScaleY, outputScaleY) == 0;
    }

    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + (int) (ptr ^ (ptr >>> 32));
        result = 31 * result + adapter;
        result = 31 * result + depth;
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + visibleX;
        result = 31 * result + visibleY;
        result = 31 * result + visibleWidth;
        result = 31 * result + visibleHeight;
        result = 31 * result + resolutionX;
        result = 31 * result + resolutionY;
        result = 31 * result + (platformScaleX != +0.0f ? Float.floatToIntBits(platformScaleX) : 0);
        result = 31 * result + (platformScaleY != +0.0f ? Float.floatToIntBits(platformScaleY) : 0);
        result = 31 * result + (outputScaleX != +0.0f ? Float.floatToIntBits(outputScaleX) : 0);
        result = 31 * result + (outputScaleY != +0.0f ? Float.floatToIntBits(outputScaleY) : 0);
        return result;
    }
}
