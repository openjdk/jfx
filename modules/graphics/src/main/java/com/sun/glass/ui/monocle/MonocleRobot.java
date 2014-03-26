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
import com.sun.glass.ui.monocle.input.KeyInput;
import com.sun.glass.ui.monocle.input.KeyState;
import com.sun.glass.ui.monocle.input.MouseInput;
import com.sun.glass.ui.monocle.input.MouseState;
import javafx.application.Platform;

import java.nio.IntBuffer;

public class MonocleRobot extends Robot {
    @Override
    protected void _create() {
    }

    @Override
    protected void _destroy() {
    }

    @Override
    protected void _keyPress(int code) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                KeyState state = new KeyState();
                KeyInput.getInstance().getState(state);
                state.pressKey(code);
                KeyInput.getInstance().setState(state);
            }
        });
    }

    @Override
    protected void _keyRelease(int code) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                KeyState state = new KeyState();
                KeyInput.getInstance().getState(state);
                state.releaseKey(code);
                KeyInput.getInstance().setState(state);
            }
        });
    }

    @Override
    protected void _mouseMove(int x, int y) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                MouseState state = new MouseState();
                MouseInput.getInstance().getState(state);
                state.setX(x);
                state.setY(y);
                MouseInput.getInstance().setState(state, false);
            }
        });
    }

    @Override
    protected void _mousePress(int buttons) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Override
    protected void _mouseRelease(int buttons) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                MouseState state = new MouseState();
                MouseInput.getInstance().getState(state);
                if ((buttons & MOUSE_LEFT_BTN) != 0) {
                    state.releaseButton(MouseEvent.BUTTON_LEFT);
                }
                if ((buttons & MOUSE_MIDDLE_BTN) != 0) {
                    state.releaseButton(MouseEvent.BUTTON_OTHER);
                }
                if ((buttons & MOUSE_RIGHT_BTN) != 0) {
                    state.releaseButton(MouseEvent.BUTTON_RIGHT);
                }
                MouseInput.getInstance().setState(state, false);
            }
        });
    }

    @Override
    protected void _mouseWheel(int wheelAmt) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                MouseState state = new MouseState();
                MouseInput mouse = MouseInput.getInstance();
                mouse.getState(state);
                int direction = wheelAmt < 0
                                ? MouseState.WHEEL_DOWN
                                : MouseState.WHEEL_UP;
                for (int i = 0; i < Math.abs(wheelAmt); i++) {
                    state.setWheel(direction);
                    mouse.setState(state, false);
                    state.setWheel(MouseState.WHEEL_NONE);
                    mouse.setState(state, false);
                }
            }
        });
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
        NativeScreen screen = NativePlatformFactory.getNativePlatform().getScreen();
        IntBuffer buffer = screen.getScreenCapture();
        return buffer.get(x + y * screen.getWidth());
    }

    @Override
    protected Pixels _getScreenCapture(int x, int y, int width, int height,
                                       boolean isHiDPI) {
        NativeScreen screen = NativePlatformFactory.getNativePlatform().getScreen();
        IntBuffer buffer = screen.getScreenCapture();
        buffer.clear();
        if (x == 0 && y == 0 && width == screen.getWidth() && height == screen.getHeight()) {
            return new MonoclePixels(width, height, buffer);
        } else {
            IntBuffer selection = IntBuffer.allocate(width * height);
            for (int i = 0; i < height; i++) {
                int srcPos = x + (y + i) * screen.getWidth();
                buffer.limit(srcPos + width);
                buffer.position(srcPos);
                selection.put(buffer);
            }
            selection.clear();
            return new MonoclePixels(width, height, selection);
        }
    }
}
