/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

public final class Screen {
    
    public static class EventHandler {
        public void handleSettingsChanged() {
        }
    }
    
    public static double getVideoRefreshPeriod() {
        Application.checkEventThread();
        return Application.GetApplication().staticScreen_getVideoRefreshPeriod();
    }

    public static Screen getDeepestScreen() {
        Application.checkEventThread();
        return Application.GetApplication().staticScreen_getDeepestScreen();
    }

    public static Screen getMainScreen() {
        //Application.checkEventThread(); // Quantum
        return Application.GetApplication().staticScreen_getMainScreen();
    }

    public static Screen getScreenForLocation(int x, int y) {
        Application.checkEventThread();
        return Application.GetApplication().staticScreen_getScreenForLocation(x, y);
    }

    // used by Window.notifyMoveToAnotherScreen
    static Screen getScreenForPtr(long screenPtr) {
        Application.checkEventThread();
        return Application.GetApplication().staticScreen_getScreenForPtr(screenPtr);
    }

    public static List<Screen> getScreens() {
        //Application.checkEventThread(); // Quantum
        return Application.GetApplication().staticScreen_getScreens();
    }

    private static EventHandler eventHandler;
    
    private long ptr;

    private int depth;

    private int x;
    private int y;
    private int width;
    private int height;

    private int visibleX;
    private int visibleY;
    private int visibleWidth;
    private int visibleHeight;

    private int resolutionX;
    private int resolutionY;

    private float scale;

    public Screen() {
        //Application.checkEventThread(); // Quantum
        this.ptr = 0L;

        this.depth = 0;

        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;

        this.visibleX = 0;
        this.visibleY = 0;
        this.visibleWidth = 0;
        this.visibleHeight = 0;

        this.resolutionX = 0;
        this.resolutionY = 0;

        this.scale = 0.0f;
    }

    public int getDepth() {
        Application.checkEventThread();
        return this.depth;
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

    public int getVisibleX() {
        Application.checkEventThread();
        return this.visibleX;
    }

    public int getVisibleY() {
        Application.checkEventThread();
        return this.visibleY;
    }

    public int getVisibleWidth() {
        Application.checkEventThread();
        return this.visibleWidth;
    }

    public int getVisibleHeight() {
        Application.checkEventThread();
        return this.visibleHeight;
    }

    public int getResolutionX() {
        Application.checkEventThread();
        return this.resolutionX;
    }

    public int getResolutionY() {
        Application.checkEventThread();
        return this.resolutionY;
    }

    public float getScale() {
        Application.checkEventThread();
        return this.scale;
    }

    public long getNativeScreen() {
        //Application.checkEventThread(); // Quantum
        return this.ptr;
    }

    public static EventHandler getEventHandler() {
        Application.checkEventThread();
        return eventHandler;
    }
    
    public static void setEventHandler(EventHandler eh) {
        Application.checkEventThread();
        eventHandler = eh;
    }
    
    private static void notifySettingsChanged() {
        if (eventHandler != null) {
            eventHandler.handleSettingsChanged();
        }
    }
    
    @Override public String toString() {
        return  "Screen:"+"\n"+
                "    ptr:"+getNativeScreen()+"\n"+
                "    depth:"+getDepth()+"\n"+
                "    x:"+getX()+"\n"+
                "    y:"+getY()+"\n"+
                "    width:"+getWidth()+"\n"+
                "    height:"+getHeight()+"\n"+
                "    visibleX:"+getVisibleX()+"\n"+
                "    visibleY:"+getVisibleY()+"\n"+
                "    visibleWidth:"+getVisibleWidth()+"\n"+
                "    visibleHeight:"+getVisibleHeight()+"\n"+
                "    scale:"+getScale()+"\n"+
                "    resolutionX:"+getResolutionX()+"\n"+
                "    resolutionY:"+getResolutionY()+"\n";
    }
}
