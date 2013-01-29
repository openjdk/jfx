/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.Event;
import javafx.stage.Window;

import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKStageListener;
import javafx.stage.WindowEvent;

/**
 * Listener for the Stage Peer to pass updates and events back to the stage.
 *
 */
public class WindowPeerListener implements TKStageListener {

    private final Window window;

    private WindowBoundsAccessor boundsAccessor;

    public WindowPeerListener(Window window) {
        this.window = window;
    }

    public void setBoundsAccessor(WindowBoundsAccessor boundsAccessor) {
        this.boundsAccessor = boundsAccessor;
    }

    @Override
    public void changedLocation(float x, float y) {
        boundsAccessor.setLocation(window, x, y);
    }

    @Override
    public void changedSize(float width, float height) {
        boundsAccessor.setSize(window, width, height);
    }

    public void changedFocused(boolean focused, FocusCause cause) {
        // Also overridden in subclasses
        window.setFocused(focused);
    }

    public void changedIconified(boolean iconified) {
        // Overridden in subclasses
    }

    public void changedMaximized(boolean maximized) {
        // Overridden in subclasses
    }

    public void changedResizable(boolean resizable) {
        // Overridden in subclasses
    }

    public void changedFullscreen(boolean fs) {
        // Overridden in subclasses
    }

    @Override
    public void closing() {
        Event.fireEvent(window, 
                        new WindowEvent(window,
                                        WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @Override
    public void closed() {
        window.hide();
    }

    @Override public void focusUngrab() {
        Event.fireEvent(window, new FocusUngrabEvent());
    }
   
    /**
     * Initialize accessibility
     */
    @Override
    public void initAccessibleTKStageListener(long ptr) {
        // Overridden in subclasses
    }
}
