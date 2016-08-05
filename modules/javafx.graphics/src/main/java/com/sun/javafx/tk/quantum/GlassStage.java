/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.EventLoop;
import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKStageListener;
import com.sun.javafx.tk.Toolkit;

abstract class GlassStage implements TKStage {

    // Need to access via reflection since the SharedSecrets class moved to
    // a new package in JDK 9 and we still build and test with JDK 8u.
    private static Object javaSecurityAccess = null;
    private static Method m_doIntersectionPrivilege = null;

    static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Class cls_SharedSecrets;
            Class cls_JavaSecurityAccess;

            try {
                try {
                    // First look for these classes in their new location
                    cls_SharedSecrets = Class.forName("jdk.internal.misc.SharedSecrets");
                    cls_JavaSecurityAccess = Class.forName("jdk.internal.misc.JavaSecurityAccess");
                } catch (ClassNotFoundException ex) {
                    // As a fallback, look for these classes in their old location
                    cls_SharedSecrets = Class.forName("sun.misc.SharedSecrets");
                    cls_JavaSecurityAccess = Class.forName("sun.misc.JavaSecurityAccess");
                }

                // JavaSecurityAccess jsa = SharedSecrets.getJavaSecurityAccess();
                Method m_getJavaSecurityAccess = cls_SharedSecrets.getMethod("getJavaSecurityAccess",
                        new Class[0]);
                javaSecurityAccess = m_getJavaSecurityAccess.invoke(null);
                m_doIntersectionPrivilege = cls_JavaSecurityAccess.getMethod("doIntersectionPrivilege",
                        PrivilegedAction.class, AccessControlContext.class, AccessControlContext.class);

            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException ex)
            {
                throw new SecurityException(ex);
            }
            return null;
        });
    }

    // A list of all GlassStage objects regardless of visibility.
    private static final List<GlassStage> windows = new ArrayList<>();

    // A list of currently visible important windows. This list is used to
    // check if the application should exit, when idle
    private static List<TKStage> importantWindows = new ArrayList<>();

    private GlassScene scene;

    protected TKStageListener stageListener;

    private boolean visible;

    private boolean important = true;

    private AccessControlContext accessCtrlCtx = null;

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

    // To be used by subclasses to enforce context check
    final AccessControlContext getAccessControlContext() {
        if (accessCtrlCtx == null) {
            throw new RuntimeException("Stage security context has not been set!");
        }
        return accessCtrlCtx;
    }

    static AccessControlContext doIntersectionPrivilege(PrivilegedAction<AccessControlContext> action,
                                                       AccessControlContext stack,
                                                       AccessControlContext context) {
        try {
            return (AccessControlContext) m_doIntersectionPrivilege.invoke(
                    javaSecurityAccess, action, stack, context);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new SecurityException(ex);
        }
    }

    public final void setSecurityContext(AccessControlContext ctx) {
        if (accessCtrlCtx != null) {
            throw new RuntimeException("Stage security context has been already set!");
        }
        AccessControlContext acc = AccessController.getContext();
        // JDK doesn't provide public APIs to get ACC intersection,
        // so using this ugly workaround
        accessCtrlCtx = doIntersectionPrivilege(
                () -> AccessController.getContext(), acc, ctx);
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
        // TODO: Need to solve RT-12605:
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
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    window.stageListener.closing();
                    return null;
                }, window.getAccessControlContext());
            }
        }
    }
}
