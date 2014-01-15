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

package com.sun.glass.ui.monocle.linux;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.monocle.input.KeyInput;
import com.sun.glass.ui.monocle.input.KeyState;

public class LinuxKeyProcessor implements LinuxInputProcessor {

    private KeyInput key = KeyInput.getInstance();
    private KeyState state = new KeyState();

    @Override
    public void processEvents(LinuxInputDevice device) {
        LinuxEventBuffer buffer = device.getBuffer();
        key.getState(state);
        while (buffer.hasNextEvent()) {
            switch (buffer.getEventType()) {
                case Input.EV_KEY:
                    int vk = getVirtualKeyCode(buffer.getEventCode());
                    if (vk != KeyEvent.VK_UNDEFINED) {
                        if (buffer.getEventValue() == 0) {
                            state.releaseKey(vk);
                        } else {
                            state.pressKey(vk);
                        }
                    }
                    break;
                case Input.EV_SYN:
                    switch (buffer.getEventCode()) {
                        case Input.SYN_REPORT:
                            key.setState(state);
                            break;
                        default: // ignore
                    }
                    break;
                default:
                    // ignore other events
                    break;
            }
            buffer.nextEvent();
        }
    }

    private int getVirtualKeyCode(int linuxKeyCode) {
        if (linuxKeyCode >= Input.KEY_1 && linuxKeyCode <= Input.KEY_9) {
            return linuxKeyCode - Input.KEY_1 + KeyEvent.VK_1;
        } else if (linuxKeyCode >= Input.KEY_NUMERIC_0 && linuxKeyCode <= Input.KEY_NUMERIC_9) {
            return linuxKeyCode - Input.KEY_NUMERIC_0 + KeyEvent.VK_0;
        } else if (linuxKeyCode >= Input.KEY_F1 && linuxKeyCode <= Input.KEY_F10) {
            return linuxKeyCode - Input.KEY_F1 + KeyEvent.VK_F1;
        } else if (linuxKeyCode >= Input.KEY_F11 && linuxKeyCode <= Input.KEY_F12) {
            return linuxKeyCode - Input.KEY_F11 + KeyEvent.VK_F11;
        } else if (linuxKeyCode >= Input.KEY_F13 && linuxKeyCode <= Input.KEY_F24) {
            return linuxKeyCode - Input.KEY_F13 + KeyEvent.VK_F13;
        } else switch (linuxKeyCode) {
            case Input.KEY_1: return KeyEvent.VK_1;
            case Input.KEY_A: return KeyEvent.VK_A;
            case Input.KEY_B: return KeyEvent.VK_B;
            case Input.KEY_C: return KeyEvent.VK_C;
            case Input.KEY_D: return KeyEvent.VK_D;
            case Input.KEY_E: return KeyEvent.VK_E;
            case Input.KEY_F: return KeyEvent.VK_F;
            case Input.KEY_G: return KeyEvent.VK_G;
            case Input.KEY_H: return KeyEvent.VK_H;
            case Input.KEY_I: return KeyEvent.VK_I;
            case Input.KEY_J: return KeyEvent.VK_J;
            case Input.KEY_K: return KeyEvent.VK_K;
            case Input.KEY_L: return KeyEvent.VK_L;
            case Input.KEY_M: return KeyEvent.VK_M;
            case Input.KEY_N: return KeyEvent.VK_N;
            case Input.KEY_O: return KeyEvent.VK_O;
            case Input.KEY_P: return KeyEvent.VK_P;
            case Input.KEY_Q: return KeyEvent.VK_Q;
            case Input.KEY_R: return KeyEvent.VK_R;
            case Input.KEY_S: return KeyEvent.VK_S;
            case Input.KEY_T: return KeyEvent.VK_T;
            case Input.KEY_U: return KeyEvent.VK_U;
            case Input.KEY_V: return KeyEvent.VK_V;
            case Input.KEY_W: return KeyEvent.VK_W;
            case Input.KEY_X: return KeyEvent.VK_X;
            case Input.KEY_Y: return KeyEvent.VK_Y;
            case Input.KEY_Z: return KeyEvent.VK_Z;
            case Input.KEY_LEFTCTRL:
            case Input.KEY_RIGHTCTRL: return KeyEvent.VK_CONTROL;
            case Input.KEY_LEFTSHIFT:
            case Input.KEY_RIGHTSHIFT: return KeyEvent.VK_SHIFT;
            case Input.KEY_CAPSLOCK: return KeyEvent.VK_CAPS_LOCK;
            case Input.KEY_TAB: return KeyEvent.VK_TAB;
            case Input.KEY_GRAVE: return KeyEvent.VK_BACK_QUOTE;
            case Input.KEY_MINUS: return KeyEvent.VK_MINUS;
            case Input.KEY_EQUAL: return KeyEvent.VK_EQUALS;
            case Input.KEY_BACKSPACE: return KeyEvent.VK_BACKSPACE;
            case Input.KEY_LEFTBRACE: return KeyEvent.VK_BRACELEFT;
            case Input.KEY_RIGHTBRACE: return KeyEvent.VK_BRACERIGHT;
            case Input.KEY_BACKSLASH: return KeyEvent.VK_BACK_SLASH;
            case Input.KEY_SEMICOLON: return KeyEvent.VK_SEMICOLON;
            case Input.KEY_APOSTROPHE: return KeyEvent.VK_QUOTE;
            case Input.KEY_COMMA: return KeyEvent.VK_COMMA;
            case Input.KEY_DOT: return KeyEvent.VK_PERIOD;
            case Input.KEY_SLASH: return KeyEvent.VK_SLASH;
            case Input.KEY_LEFTALT:
            case Input.KEY_RIGHTALT: return KeyEvent.VK_ALT;
            case Input.KEY_LEFTMETA:
            case Input.KEY_RIGHTMETA: return KeyEvent.VK_COMMAND;
            case Input.KEY_SPACE: return KeyEvent.VK_SPACE;
            case Input.KEY_MENU: return KeyEvent.VK_CONTEXT_MENU;
            case Input.KEY_ENTER: return KeyEvent.VK_ENTER;
            case Input.KEY_LEFT: return KeyEvent.VK_LEFT;
            case Input.KEY_RIGHT: return KeyEvent.VK_RIGHT;
            case Input.KEY_UP: return KeyEvent.VK_UP;
            case Input.KEY_DOWN: return KeyEvent.VK_DOWN;
            case Input.KEY_HOME: return KeyEvent.VK_HOME;
            case Input.KEY_DELETE: return KeyEvent.VK_DELETE;
            case Input.KEY_INSERT: return KeyEvent.VK_INSERT;
            case Input.KEY_END: return KeyEvent.VK_END;
            case Input.KEY_PAGEDOWN: return KeyEvent.VK_PAGE_DOWN;
            case Input.KEY_PAGEUP: return KeyEvent.VK_PAGE_UP;
            case Input.KEY_NUMLOCK: return KeyEvent.VK_NUM_LOCK;
            case Input.KEY_ESC: return KeyEvent.VK_ESCAPE;
            case Input.KEY_NUMERIC_STAR: return KeyEvent.VK_MULTIPLY;
            default: return KeyEvent.VK_UNDEFINED;
        }
    }

}
