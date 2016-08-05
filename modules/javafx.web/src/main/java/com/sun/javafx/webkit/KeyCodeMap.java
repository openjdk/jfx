/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit;

import com.sun.webkit.event.WCKeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.input.KeyCode;

/**
 * The static mapping from JavaFX {@code KeyCode}s to Windows virtual key
 * codes and WebKit key identifiers.
 * For why there is a need to map to Windows virtual key codes and WebKit
 * key identifiers, see Source/WebCore/platform/PlatformKeyboardEvent.h,
 * Source/WebKit/chromium/public/WebInputEvent.h
 * (WebKeyboardEvent::windowsKeyCode and WebKeyboardEvent::nativeKeyCode),
 * and Source/WebCore/platform/chromium/KeyboardCodes.h.
 */
public final class KeyCodeMap {

    /**
     * The information associated with a {@code KeyCode}.
     */
    public static final class Entry {
        private final int windowsVirtualKeyCode;
        private final String keyIdentifier;

        private Entry(int windowsVirtualKeyCode, String keyIdentifier) {
            this.windowsVirtualKeyCode = windowsVirtualKeyCode;
            this.keyIdentifier = keyIdentifier;
        }

        public int getWindowsVirtualKeyCode() {
            return windowsVirtualKeyCode;
        }

        public String getKeyIdentifier() {
            return keyIdentifier;
        }
    };


    private static final Map<KeyCode,Entry> MAP;
    static {
        Map<KeyCode,Entry> map = new HashMap<KeyCode,Entry>();

        put(map, KeyCode.ENTER, WCKeyEvent.VK_RETURN, "Enter");
        put(map, KeyCode.BACK_SPACE, WCKeyEvent.VK_BACK);
        put(map, KeyCode.TAB, WCKeyEvent.VK_TAB);
        put(map, KeyCode.CANCEL, 0x03);
        put(map, KeyCode.CLEAR, 0x0C, "Clear");
        put(map, KeyCode.SHIFT, 0x10, "Shift");
        put(map, KeyCode.CONTROL, 0x11, "Control");
        put(map, KeyCode.ALT, 0x12, "Alt");
        put(map, KeyCode.PAUSE, 0x13, "Pause");
        put(map, KeyCode.CAPS, 0x14, "CapsLock");
        put(map, KeyCode.ESCAPE, WCKeyEvent.VK_ESCAPE);
        put(map, KeyCode.SPACE, 0x20);
        put(map, KeyCode.PAGE_UP, WCKeyEvent.VK_PRIOR, "PageUp");
        put(map, KeyCode.PAGE_DOWN, WCKeyEvent.VK_NEXT, "PageDown");
        put(map, KeyCode.END, WCKeyEvent.VK_END, "End");
        put(map, KeyCode.HOME, WCKeyEvent.VK_HOME, "Home");
        put(map, KeyCode.LEFT, WCKeyEvent.VK_LEFT, "Left");
        put(map, KeyCode.UP, WCKeyEvent.VK_UP, "Up");
        put(map, KeyCode.RIGHT, WCKeyEvent.VK_RIGHT, "Right");
        put(map, KeyCode.DOWN, WCKeyEvent.VK_DOWN, "Down");
        put(map, KeyCode.COMMA, 0xBC);
        put(map, KeyCode.MINUS, 0xBD);
        put(map, KeyCode.PERIOD, WCKeyEvent.VK_OEM_PERIOD);
        put(map, KeyCode.SLASH, 0xBF);
        put(map, KeyCode.DIGIT0, 0x30);
        put(map, KeyCode.DIGIT1, 0x31);
        put(map, KeyCode.DIGIT2, 0x32);
        put(map, KeyCode.DIGIT3, 0x33);
        put(map, KeyCode.DIGIT4, 0x34);
        put(map, KeyCode.DIGIT5, 0x35);
        put(map, KeyCode.DIGIT6, 0x36);
        put(map, KeyCode.DIGIT7, 0x37);
        put(map, KeyCode.DIGIT8, 0x38);
        put(map, KeyCode.DIGIT9, 0x39);
        put(map, KeyCode.SEMICOLON, 0xBA);
        put(map, KeyCode.EQUALS, 0xBB);
        put(map, KeyCode.A, 0x41);
        put(map, KeyCode.B, 0x42);
        put(map, KeyCode.C, 0x43);
        put(map, KeyCode.D, 0x44);
        put(map, KeyCode.E, 0x45);
        put(map, KeyCode.F, 0x46);
        put(map, KeyCode.G, 0x47);
        put(map, KeyCode.H, 0x48);
        put(map, KeyCode.I, 0x49);
        put(map, KeyCode.J, 0x4A);
        put(map, KeyCode.K, 0x4B);
        put(map, KeyCode.L, 0x4C);
        put(map, KeyCode.M, 0x4D);
        put(map, KeyCode.N, 0x4E);
        put(map, KeyCode.O, 0x4F);
        put(map, KeyCode.P, 0x50);
        put(map, KeyCode.Q, 0x51);
        put(map, KeyCode.R, 0x52);
        put(map, KeyCode.S, 0x53);
        put(map, KeyCode.T, 0x54);
        put(map, KeyCode.U, 0x55);
        put(map, KeyCode.V, 0x56);
        put(map, KeyCode.W, 0x57);
        put(map, KeyCode.X, 0x58);
        put(map, KeyCode.Y, 0x59);
        put(map, KeyCode.Z, 0x5A);
        put(map, KeyCode.OPEN_BRACKET, 0xDB);
        put(map, KeyCode.BACK_SLASH, 0xDC);
        put(map, KeyCode.CLOSE_BRACKET, 0xDD);
        put(map, KeyCode.NUMPAD0, 0x60);
        put(map, KeyCode.NUMPAD1, 0x61);
        put(map, KeyCode.NUMPAD2, 0x62);
        put(map, KeyCode.NUMPAD3, 0x63);
        put(map, KeyCode.NUMPAD4, 0x64);
        put(map, KeyCode.NUMPAD5, 0x65);
        put(map, KeyCode.NUMPAD6, 0x66);
        put(map, KeyCode.NUMPAD7, 0x67);
        put(map, KeyCode.NUMPAD8, 0x68);
        put(map, KeyCode.NUMPAD9, 0x69);
        put(map, KeyCode.MULTIPLY, 0x6A);
        put(map, KeyCode.ADD, 0x6B);
        put(map, KeyCode.SEPARATOR, 0x6C);
        put(map, KeyCode.SUBTRACT, 0x6D);
        put(map, KeyCode.DECIMAL, 0x6E);
        put(map, KeyCode.DIVIDE, 0x6F);
        put(map, KeyCode.DELETE, WCKeyEvent.VK_DELETE, "U+007F");
        put(map, KeyCode.NUM_LOCK, 0x90);
        put(map, KeyCode.SCROLL_LOCK, 0x91, "Scroll");
        put(map, KeyCode.F1, 0x70, "F1");
        put(map, KeyCode.F2, 0x71, "F2");
        put(map, KeyCode.F3, 0x72, "F3");
        put(map, KeyCode.F4, 0x73, "F4");
        put(map, KeyCode.F5, 0x74, "F5");
        put(map, KeyCode.F6, 0x75, "F6");
        put(map, KeyCode.F7, 0x76, "F7");
        put(map, KeyCode.F8, 0x77, "F8");
        put(map, KeyCode.F9, 0x78, "F9");
        put(map, KeyCode.F10, 0x79, "F10");
        put(map, KeyCode.F11, 0x7A, "F11");
        put(map, KeyCode.F12, 0x7B, "F12");
        put(map, KeyCode.F13, 0x7C, "F13");
        put(map, KeyCode.F14, 0x7D, "F14");
        put(map, KeyCode.F15, 0x7E, "F15");
        put(map, KeyCode.F16, 0x7F, "F16");
        put(map, KeyCode.F17, 0x80, "F17");
        put(map, KeyCode.F18, 0x81, "F18");
        put(map, KeyCode.F19, 0x82, "F19");
        put(map, KeyCode.F20, 0x83, "F20");
        put(map, KeyCode.F21, 0x84, "F21");
        put(map, KeyCode.F22, 0x85, "F22");
        put(map, KeyCode.F23, 0x86, "F23");
        put(map, KeyCode.F24, 0x87, "F24");
        put(map, KeyCode.PRINTSCREEN, 0x2C, "PrintScreen");
        put(map, KeyCode.INSERT, WCKeyEvent.VK_INSERT, "Insert");
        put(map, KeyCode.HELP, 0x2F, "Help");
        put(map, KeyCode.META, 0x00, "Meta");
        put(map, KeyCode.BACK_QUOTE, 0xC0);
        put(map, KeyCode.QUOTE, 0xDE);
        put(map, KeyCode.KP_UP, WCKeyEvent.VK_UP, "Up");
        put(map, KeyCode.KP_DOWN, WCKeyEvent.VK_DOWN, "Down");
        put(map, KeyCode.KP_LEFT, WCKeyEvent.VK_LEFT, "Left");
        put(map, KeyCode.KP_RIGHT, WCKeyEvent.VK_RIGHT, "Right");
        put(map, KeyCode.AMPERSAND, 0x37);
        put(map, KeyCode.ASTERISK, 0x38);
        put(map, KeyCode.QUOTEDBL, 0xDE);
        put(map, KeyCode.LESS, 0xBC);
        put(map, KeyCode.GREATER, WCKeyEvent.VK_OEM_PERIOD);
        put(map, KeyCode.BRACELEFT, 0xDB);
        put(map, KeyCode.BRACERIGHT, 0xDD);
        put(map, KeyCode.AT, 0x32);
        put(map, KeyCode.COLON, 0xBA);
        put(map, KeyCode.CIRCUMFLEX, 0x36);
        put(map, KeyCode.DOLLAR, 0x34);
        put(map, KeyCode.EXCLAMATION_MARK, 0x31);
        put(map, KeyCode.LEFT_PARENTHESIS, 0x39);
        put(map, KeyCode.NUMBER_SIGN, 0x33);
        put(map, KeyCode.PLUS, 0xBB);
        put(map, KeyCode.RIGHT_PARENTHESIS, 0x30);
        put(map, KeyCode.UNDERSCORE, 0xBD);
        put(map, KeyCode.WINDOWS, 0x5B, "Win");
        put(map, KeyCode.CONTEXT_MENU, 0x5D);
        put(map, KeyCode.FINAL, 0x18);
        put(map, KeyCode.CONVERT, 0x1C);
        put(map, KeyCode.NONCONVERT, 0x1D);
        put(map, KeyCode.ACCEPT, 0x1E);
        put(map, KeyCode.MODECHANGE, 0x1F);
        put(map, KeyCode.KANA, 0x15);
        put(map, KeyCode.KANJI, 0x19);
        put(map, KeyCode.ALT_GRAPH, 0xA5);
        put(map, KeyCode.PLAY, 0xFA);
        put(map, KeyCode.TRACK_PREV, 0xB1);
        put(map, KeyCode.TRACK_NEXT, 0xB0);
        put(map, KeyCode.VOLUME_UP, 0xAF);
        put(map, KeyCode.VOLUME_DOWN, 0xAE);
        put(map, KeyCode.MUTE, 0xAD);

        MAP = Collections.unmodifiableMap(map);
    }


    private static void put(Map<KeyCode,Entry> map, KeyCode keyCode,
                            int windowsVirtualKeyCode, String keyIdentifier)
    {
        map.put(keyCode, new Entry(windowsVirtualKeyCode, keyIdentifier));
    }

    private static void put(Map<KeyCode,Entry> map, KeyCode keyCode,
                            int windowsVirtualKeyCode)
    {
        put(map, keyCode, windowsVirtualKeyCode, null);
    }

    /**
     * Returns an {@link Entry} object containing the Windows
     * virtual key code and the key identifier associated with
     * a given {@link KeyCode}.
     */
    public static Entry lookup(KeyCode keyCode) {
        Entry entry = MAP.get(keyCode);
        if (entry == null || entry.getKeyIdentifier() == null) {
            int windowsVirtualKeyCode = entry != null
                    ? entry.getWindowsVirtualKeyCode() : 0;
            String keyIdentifier =
                    String.format("U+%04X", windowsVirtualKeyCode);
            entry = new Entry(windowsVirtualKeyCode, keyIdentifier);
        }
        return entry;
    }
}
