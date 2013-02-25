/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.View;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.desktop.AppletWindow;
import java.lang.ref.WeakReference;
import java.util.Map;
import javafx.stage.Stage;

/**
 * Implementation class for AppletWindow using a Glass Window
 */
class GlassAppletWindow implements AppletWindow {
    private final Window glassWindow;
    private WeakReference<Stage> topStage;
    private String serverName;
    
    GlassAppletWindow(long nativeParent, String serverName) {
        if (0 == nativeParent) {
            if (serverName != null) {
                throw new RuntimeException("GlassAppletWindow constructor used incorrectly.");
            }
            glassWindow = Application.GetApplication().createWindow(null, Window.NORMAL);
        } else {
            this.serverName = serverName;
            glassWindow = Application.GetApplication().createWindow(nativeParent);
        }
    }
    
    Window getGlassWindow() {
        return glassWindow;
    }
    
    @Override
    public void setBackgroundColor(int color) {
        float RR = (float)((color >> 16) & 0xff) / 255f;
        float GG = (float)((color >> 8)  & 0xff) / 255f;
        float BB = (float)(color & 0xff) / 255f;
        glassWindow.setBackground(RR, GG, BB);
    }

    @Override
    public void setForegroundColor(int color) {
        // Do nothing, foreground color is not supported
    }

    @Override
    public void setVisible(boolean state) {
        glassWindow.setVisible(state);
    }

    @Override
    public void setSize(int width, int height) {
        glassWindow.setSize(width, height);
    }

    @Override
    public int getWidth() {
        return glassWindow.getWidth();
    }

    @Override
    public int getHeight() {
        return glassWindow.getHeight();
    }

    @Override
    public void setPosition(int x, int y) {
        glassWindow.setPosition(x, y);
    }

    @Override
    public int getPositionX() {
        return glassWindow.getX();
    }

    @Override
    public int getPositionY() {
        return glassWindow.getY();
    }

    void dispose() {
        AbstractPainter.renderLock.lock();
        try {
            glassWindow.close();
            //TODO - should update glass scene view state
            //TODO - doesn't matter because we are disposing
        } finally {
            AbstractPainter.renderLock.unlock();
        }
    }

    @Override
    public void setStageOnTop(Stage topStage) {
        if (null != topStage) {
            this.topStage = new WeakReference<Stage>(topStage);
        } else {
            this.topStage = null;
        }
    }
    
    @Override
    public int getRemoteLayerId() {
        View view = this.glassWindow.getView();
        if (view != null) {
            return view.getNativeRemoteLayerId(this.serverName);
        } else {
            return -1;
        }
    }
    
    @Override
    public void dispatchEvent(Map eventInfo) {
        this.glassWindow.dispatchNpapiEvent(eventInfo);
    }
    
    /**
     * Call when a child stage becomes visible so we can make sure topStage
     * is pushed to the front where it should be.
     */
    void assertStageOrder() {
        if (null != topStage) {
            Stage ts = topStage.get();
            if (null != ts) {
                TKStage tsp = ts.impl_getPeer();
                if (tsp instanceof WindowStage && ((WindowStage)tsp).isVisible()) {
                    // call the underlying Glass window toFront to bypass
                    // the check in WindowStage.toFront or we'll create an
                    // infinite loop
                    Window pw = ((WindowStage)tsp).getPlatformWindow();
                    if (null != pw) {
                        pw.toFront();
                    }
                }
            }
        }
    }
}
