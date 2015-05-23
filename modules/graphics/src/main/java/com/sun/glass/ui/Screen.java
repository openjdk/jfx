/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

    private final int visibleX;
    private final int visibleY;
    private final int visibleWidth;
    private final int visibleHeight;

    private final int resolutionX;
    private final int resolutionY;

    private final float uiScale;
    private final float renderScale;

    protected Screen(
            long nativePtr,

            int depth,
            int x,
            int y,
            int width,
            int height,

            int visibleX,
            int visibleY,
            int visibleWidth,
            int visibleHeight,

            int resolutionX,
            int resolutionY,

            float renderScale
            ) {
        this(nativePtr,
             depth, x, y, width, height,
             visibleX, visibleY, visibleWidth, visibleHeight,
             resolutionX, resolutionY,
             1.0f, renderScale);
    }

    protected Screen(
            long nativePtr,

            int depth,
            int x,
            int y,
            int width,
            int height,

            int visibleX,
            int visibleY,
            int visibleWidth,
            int visibleHeight,

            int resolutionX,
            int resolutionY,

            float uiScale,
            float renderScale
            ) {
        this.ptr = nativePtr;

        this.depth = depth;

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

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

        this.uiScale = uiScale;
        this.renderScale = renderScale;
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
     * Returns the scaling of the UI (window sizes and event coordinates)
     * on the screen.
     * Could be called from any thread
     */
    public float getUIScale() {
        return this.uiScale;
    }

    /**
     * Returns the recommended scaling for rendering an image for this
     * screen, potentially larger than {@link #getUIScale()}.
     * Could be called from any thread
     */
    public float getRenderScale() {
        return this.renderScale;
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
                "    visibleX:"+getVisibleX()+"\n"+
                "    visibleY:"+getVisibleY()+"\n"+
                "    visibleWidth:"+getVisibleWidth()+"\n"+
                "    visibleHeight:"+getVisibleHeight()+"\n"+
                "    uiScale:"+getUIScale()+"\n"+
                "    RenderScale:"+getRenderScale()+"\n"+
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
                && Float.compare(screen.uiScale, uiScale) == 0
                && Float.compare(screen.renderScale, renderScale) == 0;
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
        result = 31 * result + (uiScale != +0.0f ? Float.floatToIntBits(uiScale) : 0);
        result = 31 * result + (renderScale != +0.0f ? Float.floatToIntBits(renderScale) : 0);
        return result;
    }
}
