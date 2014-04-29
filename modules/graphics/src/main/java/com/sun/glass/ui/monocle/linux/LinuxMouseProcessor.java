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

    private MouseInput mouse = MouseInput.getInstance();
    private MouseState previousState = new MouseState();
    private MouseState state = new MouseState();
    private boolean processedFirstEvent;

    @Override
    public void processEvents(LinuxInputDevice device) {
        LinuxEventBuffer buffer = device.getBuffer();
        mouse.getState(previousState);
        mouse.getState(state);
        processedFirstEvent = false;
        while (buffer.hasNextEvent()) {
            switch (buffer.getEventType()) {
                case Input.EV_REL:
                    switch (buffer.getEventCode()) {
                        case Input.REL_X:
                            int x = previousState.getX();
                            x += buffer.getEventValue();
                            state.setX(x);
                            break;
                        case Input.REL_Y:
                            int y = previousState.getY();
                            y += buffer.getEventValue();
                            state.setY(y);
                            break;
                        case Input.REL_WHEEL: {
                            int value = buffer.getEventValue();
                            if (value < 0) {
                                state.setWheel(MouseState.WHEEL_DOWN);
                            } else if (value > 0) {
                                state.setWheel(MouseState.WHEEL_UP);
                            } else {
                                state.setWheel(MouseState.WHEEL_NONE);
                            }
                            break;
                        }
                        default:
                            // Ignore other axes
                    }
                    break;
                case Input.EV_SYN:
                    switch (buffer.getEventCode()) {
                        case Input.SYN_REPORT:
                            sendEvent();
                            break;
                        default: // ignore
                    }
                    break;
                case Input.EV_KEY: {
                    int button = mouseButtonForKeyCode(buffer.getEventCode());
                    if (button >= 0) {
                        if (buffer.getEventValue() == 0) {
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
            buffer.nextEvent();
        }
        mouse.setState(previousState, false);
    }

    private void sendEvent() {
        if (!processedFirstEvent) {
            mouse.getState(previousState);
            if (state.canBeFoldedWith(previousState)) {
                processedFirstEvent = true;
            } else {
                mouse.setState(state, false);
            }
        }
        if (processedFirstEvent) {
            // fold together MouseStates that differ only in their coordinates
            if (!state.canBeFoldedWith(previousState)) {
                // the events are different. Send "previousState".
                mouse.setState(previousState, false);
            }
        } else {
            processedFirstEvent = true;
        }
        state.copyTo(previousState);
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
