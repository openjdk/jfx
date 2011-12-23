/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.pgstub;

import java.util.List;

import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKStageListener;

/**
 * @author Richard Bair
 */
public class StubStage implements TKStage {
    protected TKStageListener listener;

    public void setTKStageListener(TKStageListener listener) {
        this.listener = listener;
    }

    public TKScene createTKScene(boolean depthBuffer) {
        return new StubScene();
    }

    public void setScene(TKScene scene) {
        if (scene != null) {
            StubScene s = (StubScene) scene;
            s.stage = this;
        }
    }

    public int numTimesSetSizeAndLocation;

    // Platform place/resize the window with some
    // "default" value. Pretending that the values
    // below are those platform defaults.
    public float x = 16;
    public float y = 12;
    public float width = 256;
    public float height = 192;

    public boolean visible;
    public float opacity;

    public void setBounds(float x, float y, boolean xSet, boolean ySet,
                          float width, float height, float contentWidth, float contentHeight)
    {
        numTimesSetSizeAndLocation++;
        if (xSet) {
            this.x = x;
        }
        if (ySet) {
            this.y = y;
        }
        if (xSet || ySet) {
            if (listener != null) {
                listener.changedLocation(x, y);
            }
        }
        boolean widthChanged = true;
        if (width > 0) {
            this.width = width;
        } else if (contentWidth > 0) {
            this.width = contentWidth;
        } else {
            widthChanged = false;
        }
        boolean heightChanged = true;
        if (height > 0) {
            this.height = height;
        } else if (contentHeight > 0) {
            this.height = contentHeight;
        } else {
            heightChanged = false;
        }
        if (widthChanged || heightChanged) {
            if (listener != null) {
                listener.changedSize(width, height);
            }
        }
    }

    // Just a helper method
    public void setSize(float w, float h) {
        setBounds(0, 0, false, false, w, h, 0, 0);
    }

    // Just a helper method
    public void setLocation(float x, float y) {
        setBounds(x, y, true, true, 0, 0, 0, 0);
    }

    public void setIcons(List icons) {
    }

    public void setTitle(String title) {
    }

    public void setVisible(boolean visible) {
        this.visible = visible;

        if (!visible) {
            listener.changedFocused(false, FocusCause.DEACTIVATED);
        }
        if (listener != null) {
            listener.changedLocation(x, y);
            listener.changedSize(width, height);
        }
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public void setIconified(boolean iconified) {
    }

    public void setResizable(boolean resizable) {
    }

    public void setImportant(boolean important) {
    }

    public void initSecurityContext() {
    }

    public void setFullScreen(boolean fullScreen) {
    }

    public void requestFocus() {
        listener.changedFocused(true, FocusCause.ACTIVATED);
    }
    
    public void requestFocus(FocusCause cause) {
        listener.changedFocused(true, cause);
    }

    public void toBack() {
    }

    public void toFront() {
    }

    public void close() {
    }

    public boolean grabFocus() {
        return false;
    }

    public void ungrabFocus() {
    }
}
