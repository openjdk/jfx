/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.stage;

import com.sun.javafx.tk.TKStage;
import com.sun.javafx.util.Utils;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.stage.Screen;
import javafx.stage.Window;

import java.security.AccessControlContext;

/**
 * Used to access internal window methods.
 */
public class WindowHelper {
    private static final WindowHelper theInstance;
    private static WindowAccessor windowAccessor;

    static {
        theInstance = new WindowHelper();
        Utils.forceInit(Window.class);
    }

    protected WindowHelper() {
    }

    private static WindowHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(Window window) {
        setHelper(window, getInstance());
    }

    private static WindowHelper getHelper(Window window) {
        return windowAccessor.getHelper(window);
    }

    protected static void setHelper(Window window, WindowHelper windowHelper) {
        windowAccessor.setHelper(window, windowHelper);
    }

    /*
     * Static helper methods for cases where the implementation is done in an
     * instance method that is overridden by subclasses.
     * These methods exist in the base class only.
     */
    public static void visibleChanging(Window window, boolean visible) {
        getHelper(window).visibleChangingImpl(window, visible);
    }

    public static void visibleChanged(Window window, boolean visible) {
        getHelper(window).visibleChangedImpl(window, visible);
    }

    /*
     * Methods that will be overridden by subclasses
     */
    protected void visibleChangingImpl(Window window, boolean visible) {
        windowAccessor.doVisibleChanging(window, visible);
    }

    protected void visibleChangedImpl(Window window, boolean visible) {
        windowAccessor.doVisibleChanged(window, visible);
    }

    /*
     * Methods used by Window (base) class only
     */

    public static TKStage getPeer(Window window) {
        return windowAccessor.getPeer(window);
    }

    public static void setPeer(Window window, TKStage peer) {
        windowAccessor.setPeer(window, peer);
    }

    public static WindowPeerListener getPeerListener(Window window) {
        return windowAccessor.getPeerListener(window);
    }

    public static void setPeerListener(Window window, WindowPeerListener peerListener) {
        windowAccessor.setPeerListener(window, peerListener);
    }

    public static void setFocused(Window window, boolean value) {
        windowAccessor.setFocused(window, value);
    }

    public static void notifyLocationChanged(final Window window,
                                             final double x,
                                             final double y) {
        windowAccessor.notifyLocationChanged(window, x, y);
    }

    public static void notifySizeChanged(final Window window,
                                         final double width,
                                         final double height) {
        windowAccessor.notifySizeChanged(window, width, height);
    }

    public static void notifyScaleChanged(final Window window,
                                          final double newOutputScaleX,
                                          final double newOutputScaleY) {
        windowAccessor.notifyScaleChanged(window, newOutputScaleX, newOutputScaleY);
    }

    @SuppressWarnings("removal")
    static AccessControlContext getAccessControlContext(Window window) {
        return windowAccessor.getAccessControlContext(window);
    }

    public static void setWindowAccessor(final WindowAccessor newAccessor) {
        if (windowAccessor != null) {
            throw new IllegalStateException();
        }

        windowAccessor = newAccessor;
    }

    public static WindowAccessor getWindowAccessor() {
        return windowAccessor;
    }

    public interface WindowAccessor {
        WindowHelper getHelper(Window window);
        void setHelper(Window window, WindowHelper windowHelper);
        void doVisibleChanging(Window window, boolean visible);
        void doVisibleChanged(Window window, boolean visible);
        TKStage getPeer(Window window);
        void setPeer(Window window, TKStage peer);
        WindowPeerListener getPeerListener(Window window);
        void setPeerListener(Window window, WindowPeerListener peerListener);
        void setFocused(Window window, boolean value);
        void notifyLocationChanged(Window window, double x, double y);

        void notifySizeChanged(Window window, double width, double height);

        void notifyScreenChanged(Window window, Object from, Object to);

        float getPlatformScaleX(Window window);
        float getPlatformScaleY(Window window);

        void notifyScaleChanged(Window window, double newOutputScaleX, double newOutputScaleY);

        ReadOnlyObjectProperty<Screen> screenProperty(Window window);

        @SuppressWarnings("removal")
        AccessControlContext getAccessControlContext(Window window);
    }
}
