/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.linux;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.monocle.input.MouseInput;
import com.sun.glass.ui.monocle.input.MouseState;

public class LinuxMouseProcessor implements LinuxInputProcessor {

    private MouseState state = new MouseState();

    @Override
    public void processEvents(LinuxInputDevice device) {
        MouseInput mouse = MouseInput.getInstance();
        mouse.getState(state);
        while (device.hasNextEvent()) {
            switch (device.getEventType()) {
                case Input.EV_REL:
                    switch (device.getEventCode()) {
                        case Input.REL_X:
                            int x = state.getX();
                            x += device.getEventValue();
                            state.setX(x);
                            break;
                        case Input.REL_Y:
                            int y = state.getY();
                            y += device.getEventValue();
                            state.setY(y);
                            break;
                        default:
                            // Ignore other axes
                    }
                    break;
                case Input.EV_SYN:
                    switch (device.getEventCode()) {
                        case Input.SYN_REPORT:
                            mouse.setState(state);
                            break;
                        default: // ignore
                    }
                    break;
                case Input.EV_KEY: {
                    int button = mouseButtonForKeyCode(device.getEventCode());
                    if (button >= 0) {
                        if (device.getEventValue() == 0) {
                            state.releaseButton(button);
                        } else {
                            state.pressButton(button);
                        }
                    }
                    break;
                }
                default:
                    // Ignore other events
            }
            device.nextEvent();
        }

    }

    private static int mouseButtonForKeyCode(int keyCode) {
        switch (keyCode) {
            case Input.BTN_MOUSE:
                return MouseEvent.BUTTON_LEFT;
            case Input.BTN_MIDDLE:
                return MouseEvent.BUTTON_OTHER;
            case Input.BTN_RIGHT:
                return MouseEvent.BUTTON_RIGHT;
            default:
                return -1;
        }
    }

}
