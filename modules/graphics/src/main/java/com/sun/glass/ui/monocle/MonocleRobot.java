/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Robot;
import com.sun.glass.ui.monocle.input.MouseInput;
import com.sun.glass.ui.monocle.input.MouseState;

public class MonocleRobot extends Robot {
    @Override
    protected void _create() {
    }

    @Override
    protected void _destroy() {
    }

    @Override
    protected void _keyPress(int code) {
        // TODO: robot key press
    }

    @Override
    protected void _keyRelease(int code) {
        // TODO: robot key release
    }

    @Override
    protected void _mouseMove(int x, int y) {
        MouseState state = new MouseState();
        MouseInput.getInstance().getState(state);
        state.setX(x);
        state.setY(y);
        MouseInput.getInstance().setState(state, false);
    }

    @Override
    protected void _mousePress(int buttons) {
        MouseState state = new MouseState();
        MouseInput.getInstance().getState(state);
        if ((buttons & MOUSE_LEFT_BTN) != 0) {
            state.pressButton(MouseEvent.BUTTON_LEFT);
        }
        if ((buttons & MOUSE_MIDDLE_BTN) != 0) {
            state.pressButton(MouseEvent.BUTTON_OTHER);
        }
        if ((buttons & MOUSE_RIGHT_BTN) != 0) {
            state.pressButton(MouseEvent.BUTTON_RIGHT);
        }
        MouseInput.getInstance().setState(state, false);
    }

    @Override
    protected void _mouseRelease(int buttons) {
        MouseState state = new MouseState();
        MouseInput.getInstance().getState(state);
        if ((buttons & MOUSE_LEFT_BTN) != 0) {
            state.pressButton(MouseEvent.BUTTON_LEFT);
        }
        if ((buttons & MOUSE_MIDDLE_BTN) != 0) {
            state.pressButton(MouseEvent.BUTTON_OTHER);
        }
        if ((buttons & MOUSE_RIGHT_BTN) != 0) {
            state.pressButton(MouseEvent.BUTTON_RIGHT);
        }
        MouseInput.getInstance().setState(state, false);
    }

    @Override
    protected void _mouseWheel(int wheelAmt) {
        // TODO: Mouse wheel robot
    }

    @Override
    protected int _getMouseX() {
        MouseState state = new MouseState();
        MouseInput.getInstance().getState(state);
        return state.getX();
    }

    @Override
    protected int _getMouseY() {
        MouseState state = new MouseState();
        MouseInput.getInstance().getState(state);
        return state.getY();
    }

    @Override
    protected int _getPixelColor(int x, int y) {
        return 0;
    }

    @Override
    protected Pixels _getScreenCapture(int x, int y, int width, int height,
                                       boolean isHiDPI) {
        // TODO: screen capture
        return null;
    }
}
