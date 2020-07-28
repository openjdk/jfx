/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
        entry(KeyCode.ENTER,             WCKeyEvent.VK_RETURN, "Enter"),
        entry(KeyCode.BACK_SPACE,        WCKeyEvent.VK_BACK),
        entry(KeyCode.TAB,               WCKeyEvent.VK_TAB),
        entry(KeyCode.CANCEL,            0x03),
        entry(KeyCode.CLEAR,             0x0C, "Clear"),
        entry(KeyCode.SHIFT,             0x10, "Shift"),
        entry(KeyCode.CONTROL,           0x11, "Control"),
        entry(KeyCode.ALT,               0x12, "Alt"),
        entry(KeyCode.PAUSE,             0x13, "Pause"),
        entry(KeyCode.CAPS,              0x14, "CapsLock"),
        entry(KeyCode.ESCAPE,            WCKeyEvent.VK_ESCAPE),
        entry(KeyCode.SPACE,             0x20),
        entry(KeyCode.PAGE_UP,           WCKeyEvent.VK_PRIOR, "PageUp"),
        entry(KeyCode.PAGE_DOWN,         WCKeyEvent.VK_NEXT, "PageDown"),
        entry(KeyCode.END,               WCKeyEvent.VK_END, "End"),
        entry(KeyCode.HOME,              WCKeyEvent.VK_HOME, "Home"),
        entry(KeyCode.LEFT,              WCKeyEvent.VK_LEFT, "Left"),
        entry(KeyCode.UP,                WCKeyEvent.VK_UP, "Up"),
        entry(KeyCode.RIGHT,             WCKeyEvent.VK_RIGHT, "Right"),
        entry(KeyCode.DOWN,              WCKeyEvent.VK_DOWN, "Down"),
        entry(KeyCode.COMMA,             0xBC),
        entry(KeyCode.MINUS,             0xBD),
        entry(KeyCode.PERIOD,            WCKeyEvent.VK_OEM_PERIOD),
        entry(KeyCode.SLASH,             0xBF),
        entry(KeyCode.DIGIT0,            0x30),
        entry(KeyCode.DIGIT1,            0x31),
        entry(KeyCode.DIGIT2,            0x32),
        entry(KeyCode.DIGIT3,            0x33),
        entry(KeyCode.DIGIT4,            0x34),
        entry(KeyCode.DIGIT5,            0x35),
        entry(KeyCode.DIGIT6,            0x36),
        entry(KeyCode.DIGIT7,            0x37),
        entry(KeyCode.DIGIT8,            0x38),
        entry(KeyCode.DIGIT9,            0x39),
        entry(KeyCode.SEMICOLON,         0xBA),
        entry(KeyCode.EQUALS,            0xBB),
        entry(KeyCode.A,                 0x41),
        entry(KeyCode.B,                 0x42),
        entry(KeyCode.C,                 0x43),
        entry(KeyCode.D,                 0x44),
        entry(KeyCode.E,                 0x45),
        entry(KeyCode.F,                 0x46),
        entry(KeyCode.G,                 0x47),
        entry(KeyCode.H,                 0x48),
        entry(KeyCode.I,                 0x49),
        entry(KeyCode.J,                 0x4A),
        entry(KeyCode.K,                 0x4B),
        entry(KeyCode.L,                 0x4C),
        entry(KeyCode.M,                 0x4D),
        entry(KeyCode.N,                 0x4E),
        entry(KeyCode.O,                 0x4F),
        entry(KeyCode.P,                 0x50),
        entry(KeyCode.Q,                 0x51),
        entry(KeyCode.R,                 0x52),
        entry(KeyCode.S,                 0x53),
        entry(KeyCode.T,                 0x54),
        entry(KeyCode.U,                 0x55),
        entry(KeyCode.V,                 0x56),
        entry(KeyCode.W,                 0x57),
        entry(KeyCode.X,                 0x58),
        entry(KeyCode.Y,                 0x59),
        entry(KeyCode.Z,                 0x5A),
        entry(KeyCode.OPEN_BRACKET,      0xDB),
        entry(KeyCode.BACK_SLASH,        0xDC),
        entry(KeyCode.CLOSE_BRACKET,     0xDD),
        entry(KeyCode.NUMPAD0,           0x60),
        entry(KeyCode.NUMPAD1,           0x61),
        entry(KeyCode.NUMPAD2,           0x62),
        entry(KeyCode.NUMPAD3,           0x63),
        entry(KeyCode.NUMPAD4,           0x64),
        entry(KeyCode.NUMPAD5,           0x65),
        entry(KeyCode.NUMPAD6,           0x66),
        entry(KeyCode.NUMPAD7,           0x67),
        entry(KeyCode.NUMPAD8,           0x68),
        entry(KeyCode.NUMPAD9,           0x69),
        entry(KeyCode.MULTIPLY,          0x6A),
        entry(KeyCode.ADD,               0x6B),
        entry(KeyCode.SEPARATOR,         0x6C),
        entry(KeyCode.SUBTRACT,          0x6D),
        entry(KeyCode.DECIMAL,           0x6E),
        entry(KeyCode.DIVIDE,            0x6F),
        entry(KeyCode.DELETE,            WCKeyEvent.VK_DELETE, "U+007F"),
        entry(KeyCode.NUM_LOCK,          0x90),
        entry(KeyCode.SCROLL_LOCK,       0x91, "Scroll"),
        entry(KeyCode.F1,                0x70, "F1"),
        entry(KeyCode.F2,                0x71, "F2"),
        entry(KeyCode.F3,                0x72, "F3"),
        entry(KeyCode.F4,                0x73, "F4"),
        entry(KeyCode.F5,                0x74, "F5"),
        entry(KeyCode.F6,                0x75, "F6"),
        entry(KeyCode.F7,                0x76, "F7"),
        entry(KeyCode.F8,                0x77, "F8"),
        entry(KeyCode.F9,                0x78, "F9"),
        entry(KeyCode.F10,               0x79, "F10"),
        entry(KeyCode.F11,               0x7A, "F11"),
        entry(KeyCode.F12,               0x7B, "F12"),
        entry(KeyCode.F13,               0x7C, "F13"),
        entry(KeyCode.F14,               0x7D, "F14"),
        entry(KeyCode.F15,               0x7E, "F15"),
        entry(KeyCode.F16,               0x7F, "F16"),
        entry(KeyCode.F17,               0x80, "F17"),
        entry(KeyCode.F18,               0x81, "F18"),
        entry(KeyCode.F19,               0x82, "F19"),
        entry(KeyCode.F20,               0x83, "F20"),
        entry(KeyCode.F21,               0x84, "F21"),
        entry(KeyCode.F22,               0x85, "F22"),
        entry(KeyCode.F23,               0x86, "F23"),
        entry(KeyCode.F24,               0x87, "F24"),
        entry(KeyCode.PRINTSCREEN,       0x2C, "PrintScreen"),
        entry(KeyCode.INSERT,            WCKeyEvent.VK_INSERT, "Insert"),
        entry(KeyCode.HELP,              0x2F, "Help"),
        entry(KeyCode.META,              0x00, "Meta"),
        entry(KeyCode.BACK_QUOTE,        0xC0),
        entry(KeyCode.QUOTE,             0xDE),
        entry(KeyCode.KP_UP,             WCKeyEvent.VK_UP, "Up"),
        entry(KeyCode.KP_DOWN,           WCKeyEvent.VK_DOWN, "Down"),
        entry(KeyCode.KP_LEFT,           WCKeyEvent.VK_LEFT, "Left"),
        entry(KeyCode.KP_RIGHT,          WCKeyEvent.VK_RIGHT, "Right"),
        entry(KeyCode.AMPERSAND,         0x37),
        entry(KeyCode.ASTERISK,          0x38),
        entry(KeyCode.QUOTEDBL,          0xDE),
        entry(KeyCode.LESS,              0xBC),
        entry(KeyCode.GREATER,           WCKeyEvent.VK_OEM_PERIOD),
        entry(KeyCode.BRACELEFT,         0xDB),
        entry(KeyCode.BRACERIGHT,        0xDD),
        entry(KeyCode.AT,                0x32),
        entry(KeyCode.COLON,             0xBA),
        entry(KeyCode.CIRCUMFLEX,        0x36),
        entry(KeyCode.DOLLAR,            0x34),
        entry(KeyCode.EXCLAMATION_MARK,  0x31),
        entry(KeyCode.LEFT_PARENTHESIS,  0x39),
        entry(KeyCode.NUMBER_SIGN,       0x33),
        entry(KeyCode.PLUS,              0xBB),
        entry(KeyCode.RIGHT_PARENTHESIS, 0x30),
        entry(KeyCode.UNDERSCORE,        0xBD),
        entry(KeyCode.WINDOWS,           0x5B, "Win"),
        entry(KeyCode.CONTEXT_MENU,      0x5D),
        entry(KeyCode.FINAL,             0x18),
        entry(KeyCode.CONVERT,           0x1C),
        entry(KeyCode.NONCONVERT,        0x1D),
        entry(KeyCode.ACCEPT,            0x1E),
        entry(KeyCode.MODECHANGE,        0x1F),
        entry(KeyCode.KANA,              0x15),
        entry(KeyCode.KANJI,             0x19),
        entry(KeyCode.ALT_GRAPH,         0xA5),
        entry(KeyCode.PLAY,              0xFA),
        entry(KeyCode.TRACK_PREV,        0xB1),
        entry(KeyCode.TRACK_NEXT,        0xB0),
        entry(KeyCode.VOLUME_UP,         0xAF),
        entry(KeyCode.VOLUME_DOWN,       0xAE),
        entry(KeyCode.MUTE,              0xAD));

    private static Map.Entry<KeyCode, Entry> entry(KeyCode keyCode, int windowsVirtualKeyCode, String keyIdentifier) {
        return Map.entry(keyCode, new Entry(windowsVirtualKeyCode, keyIdentifier));
    }

    private static Map.Entry<KeyCode, Entry> entry(KeyCode keyCode, int windowsVirtualKeyCode) {
        return entry(keyCode, windowsVirtualKeyCode, null);
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
