/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.events.KeyEvent;

class LinuxKeyProcessor implements LinuxInputProcessor {

    private KeyInput key = KeyInput.getInstance();
    private KeyState state = new KeyState();

    @Override
    public void processEvents(LinuxInputDevice device) {
        LinuxEventBuffer buffer = device.getBuffer();
        key.getState(state);
        while (buffer.hasNextEvent()) {
            switch (buffer.getEventType()) {
                case LinuxInput.EV_KEY:
                    int vk = getVirtualKeyCode(buffer.getEventCode());
                    if (vk != KeyEvent.VK_UNDEFINED) {
                        if (buffer.getEventValue() == 0) {
                            state.releaseKey(vk);
                        } else {
                            state.pressKey(vk);
                        }
                    }
                    break;
                case LinuxInput.EV_SYN:
                    switch (buffer.getEventCode()) {
                        case LinuxInput.SYN_REPORT:
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

    static int getVirtualKeyCode(int linuxKeyCode) {
        if (linuxKeyCode >= LinuxInput.KEY_1 && linuxKeyCode <= LinuxInput.KEY_9) {
            return linuxKeyCode - LinuxInput.KEY_1 + KeyEvent.VK_1;
        } else if (linuxKeyCode >= LinuxInput.KEY_NUMERIC_0 && linuxKeyCode <= LinuxInput.KEY_NUMERIC_9) {
            return linuxKeyCode - LinuxInput.KEY_NUMERIC_0 + KeyEvent.VK_0;
        } else if (linuxKeyCode >= LinuxInput.KEY_F1 && linuxKeyCode <= LinuxInput.KEY_F10) {
            return linuxKeyCode - LinuxInput.KEY_F1 + KeyEvent.VK_F1;
        } else if (linuxKeyCode >= LinuxInput.KEY_F11 && linuxKeyCode <= LinuxInput.KEY_F12) {
            return linuxKeyCode - LinuxInput.KEY_F11 + KeyEvent.VK_F11;
        } else if (linuxKeyCode >= LinuxInput.KEY_F13 && linuxKeyCode <= LinuxInput.KEY_F24) {
            return linuxKeyCode - LinuxInput.KEY_F13 + KeyEvent.VK_F13;
        } else switch (linuxKeyCode) {
            case LinuxInput.KEY_0: return KeyEvent.VK_0;
            case LinuxInput.KEY_A: return KeyEvent.VK_A;
            case LinuxInput.KEY_B: return KeyEvent.VK_B;
            case LinuxInput.KEY_C: return KeyEvent.VK_C;
            case LinuxInput.KEY_D: return KeyEvent.VK_D;
            case LinuxInput.KEY_E: return KeyEvent.VK_E;
            case LinuxInput.KEY_F: return KeyEvent.VK_F;
            case LinuxInput.KEY_G: return KeyEvent.VK_G;
            case LinuxInput.KEY_H: return KeyEvent.VK_H;
            case LinuxInput.KEY_I: return KeyEvent.VK_I;
            case LinuxInput.KEY_J: return KeyEvent.VK_J;
            case LinuxInput.KEY_K: return KeyEvent.VK_K;
            case LinuxInput.KEY_L: return KeyEvent.VK_L;
            case LinuxInput.KEY_M: return KeyEvent.VK_M;
            case LinuxInput.KEY_N: return KeyEvent.VK_N;
            case LinuxInput.KEY_O: return KeyEvent.VK_O;
            case LinuxInput.KEY_P: return KeyEvent.VK_P;
            case LinuxInput.KEY_Q: return KeyEvent.VK_Q;
            case LinuxInput.KEY_R: return KeyEvent.VK_R;
            case LinuxInput.KEY_S: return KeyEvent.VK_S;
            case LinuxInput.KEY_T: return KeyEvent.VK_T;
            case LinuxInput.KEY_U: return KeyEvent.VK_U;
            case LinuxInput.KEY_V: return KeyEvent.VK_V;
            case LinuxInput.KEY_W: return KeyEvent.VK_W;
            case LinuxInput.KEY_X: return KeyEvent.VK_X;
            case LinuxInput.KEY_Y: return KeyEvent.VK_Y;
            case LinuxInput.KEY_Z: return KeyEvent.VK_Z;
            case LinuxInput.KEY_LEFTCTRL:
            case LinuxInput.KEY_RIGHTCTRL: return KeyEvent.VK_CONTROL;
            case LinuxInput.KEY_LEFTSHIFT:
            case LinuxInput.KEY_RIGHTSHIFT: return KeyEvent.VK_SHIFT;
            case LinuxInput.KEY_CAPSLOCK: return KeyEvent.VK_CAPS_LOCK;
            case LinuxInput.KEY_TAB: return KeyEvent.VK_TAB;
            case LinuxInput.KEY_GRAVE: return KeyEvent.VK_BACK_QUOTE;
            case LinuxInput.KEY_MINUS: return KeyEvent.VK_MINUS;
            case LinuxInput.KEY_EQUAL: return KeyEvent.VK_EQUALS;
            case LinuxInput.KEY_BACKSPACE: return KeyEvent.VK_BACKSPACE;
            case LinuxInput.KEY_LEFTBRACE: return KeyEvent.VK_BRACELEFT;
            case LinuxInput.KEY_RIGHTBRACE: return KeyEvent.VK_BRACERIGHT;
            case LinuxInput.KEY_BACKSLASH: return KeyEvent.VK_BACK_SLASH;
            case LinuxInput.KEY_SEMICOLON: return KeyEvent.VK_SEMICOLON;
            case LinuxInput.KEY_APOSTROPHE: return KeyEvent.VK_QUOTE;
            case LinuxInput.KEY_COMMA: return KeyEvent.VK_COMMA;
            case LinuxInput.KEY_DOT: return KeyEvent.VK_PERIOD;
            case LinuxInput.KEY_SLASH: return KeyEvent.VK_SLASH;
            case LinuxInput.KEY_LEFTALT:
            case LinuxInput.KEY_RIGHTALT: return KeyEvent.VK_ALT;
            case LinuxInput.KEY_LEFTMETA:
            case LinuxInput.KEY_RIGHTMETA: return KeyEvent.VK_COMMAND;
            case LinuxInput.KEY_SPACE: return KeyEvent.VK_SPACE;
            case LinuxInput.KEY_MENU: return KeyEvent.VK_CONTEXT_MENU;
            case LinuxInput.KEY_ENTER: return KeyEvent.VK_ENTER;
            case LinuxInput.KEY_LEFT: return KeyEvent.VK_LEFT;
            case LinuxInput.KEY_RIGHT: return KeyEvent.VK_RIGHT;
            case LinuxInput.KEY_UP: return KeyEvent.VK_UP;
            case LinuxInput.KEY_DOWN: return KeyEvent.VK_DOWN;
            case LinuxInput.KEY_HOME: return KeyEvent.VK_HOME;
            case LinuxInput.KEY_DELETE: return KeyEvent.VK_DELETE;
            case LinuxInput.KEY_INSERT: return KeyEvent.VK_INSERT;
            case LinuxInput.KEY_END: return KeyEvent.VK_END;
            case LinuxInput.KEY_PAGEDOWN: return KeyEvent.VK_PAGE_DOWN;
            case LinuxInput.KEY_PAGEUP: return KeyEvent.VK_PAGE_UP;
            case LinuxInput.KEY_NUMLOCK: return KeyEvent.VK_NUM_LOCK;
            case LinuxInput.KEY_ESC: return KeyEvent.VK_ESCAPE;
            case LinuxInput.KEY_NUMERIC_STAR: return KeyEvent.VK_MULTIPLY;
            default: return KeyEvent.VK_UNDEFINED;
        }
    }

}
