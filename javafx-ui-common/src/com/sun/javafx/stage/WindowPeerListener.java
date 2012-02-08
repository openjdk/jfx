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
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.TKStageListener;
import javafx.stage.WindowEvent;

/**
 * Listener for the Stage Peer to pass updates and events back to the stage.
 *
 */
public class WindowPeerListener implements TKStageListener, TKPulseListener {

    private Window window;

    protected double local_x;
    protected double local_y;
    protected double local_width;
    protected double local_height;

    public WindowPeerListener(Window window) {
        this.window = window;
    }

    @Override
    public void changedLocation(float x, float y) {
        local_x = x;
        local_y = y;
        window.setX(x);
        window.setY(y);
    }

    @Override
    public void changedSize(float width, float height) {
        local_width = width;
        local_height = height;
        window.setWidth(width);
        window.setHeight(height);
    }

    @Override
    public void pulse() {
        if ((local_x != window.getX()) ||
            (local_y != window.getY()) ||
            (local_width != window.getWidth()) ||
            (local_height != window.getHeight()))
        {
            local_x = window.getX();
            local_y = window.getY();
            local_width = window.getWidth();
            local_height = window.getHeight();
            window.impl_getPeer().setBounds((float)local_x, (float)local_y, true, true,
                    (float)local_width, (float)local_height, -1, -1);
        }
    }

    public void changedFocused(boolean focused, FocusCause cause) {
        // Also overridden in subclasses
        window.setFocused(focused);
    }

    public void changedIconified(boolean iconified) {
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
}
