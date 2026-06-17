/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKStageListener;
import com.sun.javafx.tk.Toolkit;

abstract class GlassStage implements TKStage {

    // A list of all GlassStage objects regardless of visibility.
    private static final List<GlassStage> windows = new ArrayList<>();

    // A list of currently visible important windows. This list is used to
    // check if the application should exit, when idle
    private static List<TKStage> importantWindows = new ArrayList<>();

    private GlassScene scene;

    protected TKStageListener stageListener;

    private boolean visible;

    private boolean important = true;

    protected static final AtomicReference<GlassStage> activeFSWindow = new AtomicReference<>();

    protected GlassStage() {
        windows.add(this);
    }

    @Override public void close() {
        assert scene == null; // close() is called after setScene(null)
        windows.remove(this);
        importantWindows.remove(this);
        notifyWindowListeners();
    }

    /**
     * Listener for this stage peer to pass updates and events back to the stage
     *
     * @param listener The listener provided by the stage
     */
    @Override public void setTKStageListener(final TKStageListener listener) {
        this.stageListener = listener;
    }

    @Override
    public void setPrefHeaderButtonHeight(double height) {}

    @Override
    public void setDarkFrame(boolean value) {}

    protected final GlassScene getScene() {
        return scene;
    }

    @Override public void setScene(TKScene scene) {
        if (this.scene != null) {
            this.scene.setStage(null);
        }
        this.scene = (GlassScene)scene;
        if (this.scene != null) {
            this.scene.setStage(this);
        }
    }

    @Override public void requestFocus() {
    }

    @Override public void requestFocus(FocusCause cause) {
    }

    /**
     * Set if the stage is visible on screen
     *
     * @param visible True if the stage should be visible
     */
    @Override public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            if (important) {
                importantWindows.add(this);
                notifyWindowListeners();
            }
        } else {
            if (important) {
                importantWindows.remove(this);
                notifyWindowListeners();
            }
        }
        if (scene != null) {
            scene.stageVisible(visible);
        }
    }

    boolean isVisible() {
        return visible;
    }

    // We do blocking on windows that are backed by WindowStage and EmbeddedStage
    protected void setPlatformEnabled(boolean enabled) {
        // Overridden in subclasses
    }

    void windowsSetEnabled(boolean enabled) {
        // TODO: Need to solve JDK-8087800:
        // If Window #1 pops up an APPLICATION modal dialog #2 it should block
        // Window #1, but will also block Window #3, #4, etc., unless those
        // windows are descendants of #2.

        // Make a copy of the windows list, since it could change as the result
        // of a child window being closed when the parent is closed.
        for (GlassStage window : windows.toArray(new GlassStage[windows.size()])) {
            if (window != this && windows.contains(window)) {
                window.setPlatformEnabled(enabled);
            }
        }
    }

    @Override
    public void setImportant(boolean important) {
        this.important = important;
    }

    private static void notifyWindowListeners() {
        Toolkit.getToolkit().notifyWindowListeners(importantWindows);
    }

    // Cmd+Q action
    static void requestClosingAllWindows() {
        GlassStage fsWindow = activeFSWindow.get();
        if (fsWindow != null) {
            fsWindow.setFullScreen(false);
        }

        for (final GlassStage window : windows.toArray(new GlassStage[windows.size()])) {
            // In case of child windows some of them could already be closed
            // so check if list still contains an object
            if (windows.contains(window) && window.isVisible() && window.stageListener != null) {
                window.stageListener.closing();
            }
        }
    }
}
