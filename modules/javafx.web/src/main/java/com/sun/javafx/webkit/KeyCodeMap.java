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

    private static final Map<KeyCode, Entry> MAP = Map.ofEntries(
        put(KeyCode.ENTER,             WCKeyEvent.VK_RETURN, "Enter"),
        put(KeyCode.BACK_SPACE,        WCKeyEvent.VK_BACK),
        put(KeyCode.TAB,               WCKeyEvent.VK_TAB),
        put(KeyCode.CANCEL,            0x03),
        put(KeyCode.CLEAR,             0x0C, "Clear"),
        put(KeyCode.SHIFT,             0x10, "Shift"),
        put(KeyCode.CONTROL,           0x11, "Control"),
        put(KeyCode.ALT,               0x12, "Alt"),
        put(KeyCode.PAUSE,             0x13, "Pause"),
        put(KeyCode.CAPS,              0x14, "CapsLock"),
        put(KeyCode.ESCAPE,            WCKeyEvent.VK_ESCAPE),
        put(KeyCode.SPACE,             0x20),
        put(KeyCode.PAGE_UP,           WCKeyEvent.VK_PRIOR, "PageUp"),
        put(KeyCode.PAGE_DOWN,         WCKeyEvent.VK_NEXT, "PageDown"),
        put(KeyCode.END,               WCKeyEvent.VK_END, "End"),
        put(KeyCode.HOME,              WCKeyEvent.VK_HOME, "Home"),
        put(KeyCode.LEFT,              WCKeyEvent.VK_LEFT, "Left"),
        put(KeyCode.UP,                WCKeyEvent.VK_UP, "Up"),
        put(KeyCode.RIGHT,             WCKeyEvent.VK_RIGHT, "Right"),
        put(KeyCode.DOWN,              WCKeyEvent.VK_DOWN, "Down"),
        put(KeyCode.COMMA,             0xBC),
        put(KeyCode.MINUS,             0xBD),
        put(KeyCode.PERIOD,            WCKeyEvent.VK_OEM_PERIOD),
        put(KeyCode.SLASH,             0xBF),
        put(KeyCode.DIGIT0,            0x30),
        put(KeyCode.DIGIT1,            0x31),
        put(KeyCode.DIGIT2,            0x32),
        put(KeyCode.DIGIT3,            0x33),
        put(KeyCode.DIGIT4,            0x34),
        put(KeyCode.DIGIT5,            0x35),
        put(KeyCode.DIGIT6,            0x36),
        put(KeyCode.DIGIT7,            0x37),
        put(KeyCode.DIGIT8,            0x38),
        put(KeyCode.DIGIT9,            0x39),
        put(KeyCode.SEMICOLON,         0xBA),
        put(KeyCode.EQUALS,            0xBB),
        put(KeyCode.A,                 0x41),
        put(KeyCode.B,                 0x42),
        put(KeyCode.C,                 0x43),
        put(KeyCode.D,                 0x44),
        put(KeyCode.E,                 0x45),
        put(KeyCode.F,                 0x46),
        put(KeyCode.G,                 0x47),
        put(KeyCode.H,                 0x48),
        put(KeyCode.I,                 0x49),
        put(KeyCode.J,                 0x4A),
        put(KeyCode.K,                 0x4B),
        put(KeyCode.L,                 0x4C),
        put(KeyCode.M,                 0x4D),
        put(KeyCode.N,                 0x4E),
        put(KeyCode.O,                 0x4F),
        put(KeyCode.P,                 0x50),
        put(KeyCode.Q,                 0x51),
        put(KeyCode.R,                 0x52),
        put(KeyCode.S,                 0x53),
        put(KeyCode.T,                 0x54),
        put(KeyCode.U,                 0x55),
        put(KeyCode.V,                 0x56),
        put(KeyCode.W,                 0x57),
        put(KeyCode.X,                 0x58),
        put(KeyCode.Y,                 0x59),
        put(KeyCode.Z,                 0x5A),
        put(KeyCode.OPEN_BRACKET,      0xDB),
        put(KeyCode.BACK_SLASH,        0xDC),
        put(KeyCode.CLOSE_BRACKET,     0xDD),
        put(KeyCode.NUMPAD0,           0x60),
        put(KeyCode.NUMPAD1,           0x61),
        put(KeyCode.NUMPAD2,           0x62),
        put(KeyCode.NUMPAD3,           0x63),
        put(KeyCode.NUMPAD4,           0x64),
        put(KeyCode.NUMPAD5,           0x65),
        put(KeyCode.NUMPAD6,           0x66),
        put(KeyCode.NUMPAD7,           0x67),
        put(KeyCode.NUMPAD8,           0x68),
        put(KeyCode.NUMPAD9,           0x69),
        put(KeyCode.MULTIPLY,          0x6A),
        put(KeyCode.ADD,               0x6B),
        put(KeyCode.SEPARATOR,         0x6C),
        put(KeyCode.SUBTRACT,          0x6D),
        put(KeyCode.DECIMAL,           0x6E),
        put(KeyCode.DIVIDE,            0x6F),
        put(KeyCode.DELETE,            WCKeyEvent.VK_DELETE, "U+007F"),
        put(KeyCode.NUM_LOCK,          0x90),
        put(KeyCode.SCROLL_LOCK,       0x91, "Scroll"),
        put(KeyCode.F1,                0x70, "F1"),
        put(KeyCode.F2,                0x71, "F2"),
        put(KeyCode.F3,                0x72, "F3"),
        put(KeyCode.F4,                0x73, "F4"),
        put(KeyCode.F5,                0x74, "F5"),
        put(KeyCode.F6,                0x75, "F6"),
        put(KeyCode.F7,                0x76, "F7"),
        put(KeyCode.F8,                0x77, "F8"),
        put(KeyCode.F9,                0x78, "F9"),
        put(KeyCode.F10,               0x79, "F10"),
        put(KeyCode.F11,               0x7A, "F11"),
        put(KeyCode.F12,               0x7B, "F12"),
        put(KeyCode.F13,               0x7C, "F13"),
        put(KeyCode.F14,               0x7D, "F14"),
        put(KeyCode.F15,               0x7E, "F15"),
        put(KeyCode.F16,               0x7F, "F16"),
        put(KeyCode.F17,               0x80, "F17"),
        put(KeyCode.F18,               0x81, "F18"),
        put(KeyCode.F19,               0x82, "F19"),
        put(KeyCode.F20,               0x83, "F20"),
        put(KeyCode.F21,               0x84, "F21"),
        put(KeyCode.F22,               0x85, "F22"),
        put(KeyCode.F23,               0x86, "F23"),
        put(KeyCode.F24,               0x87, "F24"),
        put(KeyCode.PRINTSCREEN,       0x2C, "PrintScreen"),
        put(KeyCode.INSERT,            WCKeyEvent.VK_INSERT, "Insert"),
        put(KeyCode.HELP,              0x2F, "Help"),
        put(KeyCode.META,              0x00, "Meta"),
        put(KeyCode.BACK_QUOTE,        0xC0),
        put(KeyCode.QUOTE,             0xDE),
        put(KeyCode.KP_UP,             WCKeyEvent.VK_UP, "Up"),
        put(KeyCode.KP_DOWN,           WCKeyEvent.VK_DOWN, "Down"),
        put(KeyCode.KP_LEFT,           WCKeyEvent.VK_LEFT, "Left"),
        put(KeyCode.KP_RIGHT,          WCKeyEvent.VK_RIGHT, "Right"),
        put(KeyCode.AMPERSAND,         0x37),
        put(KeyCode.ASTERISK,          0x38),
        put(KeyCode.QUOTEDBL,          0xDE),
        put(KeyCode.LESS,              0xBC),
        put(KeyCode.GREATER,           WCKeyEvent.VK_OEM_PERIOD),
        put(KeyCode.BRACELEFT,         0xDB),
        put(KeyCode.BRACERIGHT,        0xDD),
        put(KeyCode.AT,                0x32),
        put(KeyCode.COLON,             0xBA),
        put(KeyCode.CIRCUMFLEX,        0x36),
        put(KeyCode.DOLLAR,            0x34),
        put(KeyCode.EXCLAMATION_MARK,  0x31),
        put(KeyCode.LEFT_PARENTHESIS,  0x39),
        put(KeyCode.NUMBER_SIGN,       0x33),
        put(KeyCode.PLUS,              0xBB),
        put(KeyCode.RIGHT_PARENTHESIS, 0x30),
        put(KeyCode.UNDERSCORE,        0xBD),
        put(KeyCode.WINDOWS,           0x5B, "Win"),
        put(KeyCode.CONTEXT_MENU,      0x5D),
        put(KeyCode.FINAL,             0x18),
        put(KeyCode.CONVERT,           0x1C),
        put(KeyCode.NONCONVERT,        0x1D),
        put(KeyCode.ACCEPT,            0x1E),
        put(KeyCode.MODECHANGE,        0x1F),
        put(KeyCode.KANA,              0x15),
        put(KeyCode.KANJI,             0x19),
        put(KeyCode.ALT_GRAPH,         0xA5),
        put(KeyCode.PLAY,              0xFA),
        put(KeyCode.TRACK_PREV,        0xB1),
        put(KeyCode.TRACK_NEXT,        0xB0),
        put(KeyCode.VOLUME_UP,         0xAF),
        put(KeyCode.VOLUME_DOWN,       0xAE),
        put(KeyCode.MUTE,              0xAD));

    private static Map.Entry<KeyCode, Entry> put(KeyCode keyCode, int windowsVirtualKeyCode, String keyIdentifier) {
        return Map.entry(keyCode, new Entry(windowsVirtualKeyCode, keyIdentifier));
    }

    private static Map.Entry<KeyCode, Entry> put(KeyCode keyCode, int windowsVirtualKeyCode) {
        return put(keyCode, windowsVirtualKeyCode, null);
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
