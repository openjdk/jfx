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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKStageListener;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;

abstract class GlassStage implements TKStage {

    // A list of all GlassStage objects regardless of visibility. Used in WindowStage.
    protected static final List<GlassStage> windows = new ArrayList<GlassStage>();

    // A list of currently visible TKStage objects.
    private static List<TKStage> topLevelWindows = new ArrayList<TKStage>();

    private List<PopupStage> popups = new LinkedList<PopupStage>();

    // An active window is visible && enabled && focusable.
    // The list is maintained in the z-order, so that the last element
    // represents the topmost window (or more accurately, the last
    // focused window, which we assume is very close to the last topmost one).
    private static List<GlassStage> activeWindows = new LinkedList<GlassStage>();

    protected boolean verbose;

    protected GlassScene scene;

    protected TKStageListener stageListener;

    private boolean visible;

    private boolean important = true;

    private AccessControlContext accessCtrlCtx = null;

    protected GlassStage(boolean verbose) {
        this.verbose = verbose;
        windows.add(this);
    }

    /**
     * Listener for this stage peer to pass updates and events back to the stage
     *
     * @param listener The listener provided by the stage
     */
    @Override public void setTKStageListener(final TKStageListener listener) {
        this.stageListener = listener;
    }

    @Override public void setScene(TKScene scene) {
        if (this.scene != null) {
            this.scene.setGlassStage(null);
        }
        this.scene = (GlassScene)scene;
        if (this.scene != null) {
            this.scene.setGlassStage(this);
        }
    }

    protected void setPlatformWindowClosed() {
    }

    // To be used by subclasses to enforce context check
    final AccessControlContext getAccessControlContext() {
        if (accessCtrlCtx == null) {
            throw new RuntimeException("Stage security context has not been set!");
        }
        return accessCtrlCtx;
    }

    @Override public final void setSecurityContext(AccessControlContext ctx) {
        if (accessCtrlCtx != null) {
            throw new RuntimeException("Stage security context has been already set!");
        }
        accessCtrlCtx = ctx;
    }

    @Override public void requestFocus() {
    }

    @Override public void requestFocus(FocusCause cause) {
    }

    static void addActiveWindow(GlassStage window) {
        GlassStage.activeWindows.remove(window);
        GlassStage.activeWindows.add(window);
    }

    static void removeActiveWindow(GlassStage window) {
        GlassStage.activeWindows.remove(window);
    }

    final void handleFocusDisabled() {
        if (GlassStage.activeWindows.isEmpty()) {
            return;
        }
        GlassStage window = GlassStage.activeWindows.get(GlassStage.activeWindows.size() - 1);

        window.setIconified(false);
        window.requestToFront();
        window.requestFocus();
    }

    /**
     * Set if the stage is visible on screen
     *
     * @param visible True if the stage should be visible
     */
    @Override public void setVisible(boolean visible) {
        boolean isTopLevel = this.isImportant() && this.isTopLevel();
        boolean visibilityChanged = this.visible != visible;

        this.visible = visible;
        if (visible) {
            if (visibilityChanged && isTopLevel) {
                topLevelWindows.add(this);
                notifyWindowListeners();
            }
        }
        if (!visible) {
            GlassStage.removeActiveWindow(this);
            if (visibilityChanged && isTopLevel) {
                topLevelWindows.remove(this);
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
    protected abstract void setPlatformEnabled(boolean enabled);

    protected abstract void requestToFront();

    @Override public void close() {
        windows.remove(this);
        topLevelWindows.remove(this);
        notifyWindowListeners();
    }

    static boolean windowsAreOpen() {
        for (GlassStage w : windows) {
            if (w.isVisible()) {
                return true;
            }
        }
        return false;
    }

    // True for unowned, non-Popup stage, or embedded stage
    public boolean isTopLevel() {
        return true;
    }

    @Override public void setImportant(boolean important) {
        this.important = important;
    }

    public boolean isImportant() {
        return important;
    }

    private static void notifyWindowListeners() {
        Toolkit.getToolkit().notifyWindowListeners(topLevelWindows);
    }

    protected void addPopup(final PopupStage popup) {
        popups.add(popup);
    }

    protected void removePopup(final PopupStage popup) {
        popups.remove(popup);
    }

    // Cmd+Q action
    static void requestClosingAllWindows() {
        for (final GlassStage window : windows.toArray(new GlassStage[windows.size()])) {
            if (window.isVisible() && window.stageListener != null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        window.stageListener.closing();
                        return null;
                    }
                }, window.getAccessControlContext());
            }
        }
    }
}
