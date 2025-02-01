/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "common.h"

#include "KeyTable.h"
#include "GlassApplication.h"

#include "com_sun_glass_events_KeyEvent.h"
#include "com_sun_glass_ui_win_WinApplication.h"


struct KeyMapEntry
{
    jint javaKey;
    UINT windowsKey;
};

static const KeyMapEntry keyMapTable[] =
{
    // Modifier keys
    {com_sun_glass_events_KeyEvent_VK_CAPS_LOCK,        VK_CAPITAL},
    {com_sun_glass_events_KeyEvent_VK_SCROLL_LOCK,      VK_SCROLL},
    {com_sun_glass_events_KeyEvent_VK_NUM_LOCK,         VK_NUMLOCK},
    {com_sun_glass_events_KeyEvent_VK_SHIFT,            VK_SHIFT},
    {com_sun_glass_events_KeyEvent_VK_CONTROL,          VK_CONTROL},
    {com_sun_glass_events_KeyEvent_VK_ALT,              VK_MENU},
    {com_sun_glass_events_KeyEvent_VK_WINDOWS,          VK_LWIN},
    {com_sun_glass_events_KeyEvent_VK_WINDOWS,          VK_RWIN},
    {com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU,     VK_APPS},
    // Alphabet
    {com_sun_glass_events_KeyEvent_VK_A,                'A'},
    {com_sun_glass_events_KeyEvent_VK_B,                'B'},
    {com_sun_glass_events_KeyEvent_VK_C,                'C'},
    {com_sun_glass_events_KeyEvent_VK_D,                'D'},
    {com_sun_glass_events_KeyEvent_VK_E,                'E'},
    {com_sun_glass_events_KeyEvent_VK_F,                'F'},
    {com_sun_glass_events_KeyEvent_VK_G,                'G'},
    {com_sun_glass_events_KeyEvent_VK_H,                'H'},
    {com_sun_glass_events_KeyEvent_VK_I,                'I'},
    {com_sun_glass_events_KeyEvent_VK_J,                'J'},
    {com_sun_glass_events_KeyEvent_VK_K,                'K'},
    {com_sun_glass_events_KeyEvent_VK_L,                'L'},
    {com_sun_glass_events_KeyEvent_VK_M,                'M'},
    {com_sun_glass_events_KeyEvent_VK_N,                'N'},
    {com_sun_glass_events_KeyEvent_VK_O,                'O'},
    {com_sun_glass_events_KeyEvent_VK_P,                'P'},
    {com_sun_glass_events_KeyEvent_VK_Q,                'Q'},
    {com_sun_glass_events_KeyEvent_VK_R,                'R'},
    {com_sun_glass_events_KeyEvent_VK_S,                'S'},
    {com_sun_glass_events_KeyEvent_VK_T,                'T'},
    {com_sun_glass_events_KeyEvent_VK_U,                'U'},
    {com_sun_glass_events_KeyEvent_VK_V,                'V'},
    {com_sun_glass_events_KeyEvent_VK_W,                'W'},
    {com_sun_glass_events_KeyEvent_VK_X,                'X'},
    {com_sun_glass_events_KeyEvent_VK_Y,                'Y'},
    {com_sun_glass_events_KeyEvent_VK_Z,                'Z'},
    // Standard numeric row
    {com_sun_glass_events_KeyEvent_VK_0,                '0'},
    {com_sun_glass_events_KeyEvent_VK_1,                '1'},
    {com_sun_glass_events_KeyEvent_VK_2,                '2'},
    {com_sun_glass_events_KeyEvent_VK_3,                '3'},
    {com_sun_glass_events_KeyEvent_VK_4,                '4'},
    {com_sun_glass_events_KeyEvent_VK_5,                '5'},
    {com_sun_glass_events_KeyEvent_VK_6,                '6'},
    {com_sun_glass_events_KeyEvent_VK_7,                '7'},
    {com_sun_glass_events_KeyEvent_VK_8,                '8'},
    {com_sun_glass_events_KeyEvent_VK_9,                '9'},

    {com_sun_glass_events_KeyEvent_VK_ENTER,            VK_RETURN},
    {com_sun_glass_events_KeyEvent_VK_SPACE,            VK_SPACE},
    {com_sun_glass_events_KeyEvent_VK_BACKSPACE,        VK_BACK},
    {com_sun_glass_events_KeyEvent_VK_TAB,              VK_TAB},
    {com_sun_glass_events_KeyEvent_VK_ESCAPE,           VK_ESCAPE},

    {com_sun_glass_events_KeyEvent_VK_INSERT,           VK_INSERT},
    {com_sun_glass_events_KeyEvent_VK_DELETE,           VK_DELETE},
    {com_sun_glass_events_KeyEvent_VK_CLEAR,            VK_CLEAR},
    {com_sun_glass_events_KeyEvent_VK_HOME,             VK_HOME},
    {com_sun_glass_events_KeyEvent_VK_END,              VK_END},
    {com_sun_glass_events_KeyEvent_VK_PAGE_UP,          VK_PRIOR},
    {com_sun_glass_events_KeyEvent_VK_PAGE_DOWN,        VK_NEXT},
    {com_sun_glass_events_KeyEvent_VK_LEFT,             VK_LEFT},
    {com_sun_glass_events_KeyEvent_VK_RIGHT,            VK_RIGHT},
    {com_sun_glass_events_KeyEvent_VK_UP,               VK_UP},
    {com_sun_glass_events_KeyEvent_VK_DOWN,             VK_DOWN},

    {com_sun_glass_events_KeyEvent_VK_NUMPAD0,          VK_NUMPAD0},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD1,          VK_NUMPAD1},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD2,          VK_NUMPAD2},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD3,          VK_NUMPAD3},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD4,          VK_NUMPAD4},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD5,          VK_NUMPAD5},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD6,          VK_NUMPAD6},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD7,          VK_NUMPAD7},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD8,          VK_NUMPAD8},
    {com_sun_glass_events_KeyEvent_VK_NUMPAD9,          VK_NUMPAD9},

    {com_sun_glass_events_KeyEvent_VK_MULTIPLY,         VK_MULTIPLY},
    {com_sun_glass_events_KeyEvent_VK_ADD,              VK_ADD},
    {com_sun_glass_events_KeyEvent_VK_SEPARATOR,        VK_SEPARATOR},
    {com_sun_glass_events_KeyEvent_VK_SUBTRACT,         VK_SUBTRACT},
    {com_sun_glass_events_KeyEvent_VK_DECIMAL,          VK_DECIMAL},
    {com_sun_glass_events_KeyEvent_VK_DIVIDE,           VK_DIVIDE},

    {com_sun_glass_events_KeyEvent_VK_EQUALS,           VK_OEM_PLUS},
    {com_sun_glass_events_KeyEvent_VK_MINUS,            VK_OEM_MINUS},

    {com_sun_glass_events_KeyEvent_VK_SEMICOLON,        VK_OEM_1},
    {com_sun_glass_events_KeyEvent_VK_COMMA,            VK_OEM_COMMA},
    {com_sun_glass_events_KeyEvent_VK_PERIOD,           VK_OEM_PERIOD},
    {com_sun_glass_events_KeyEvent_VK_SLASH,            VK_OEM_2},
    {com_sun_glass_events_KeyEvent_VK_BACK_QUOTE,       VK_OEM_3},
    {com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET,     VK_OEM_4},
    {com_sun_glass_events_KeyEvent_VK_BACK_SLASH,       VK_OEM_5},
    {com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET,    VK_OEM_6},
    {com_sun_glass_events_KeyEvent_VK_QUOTE,            VK_OEM_7},
    {com_sun_glass_events_KeyEvent_VK_LESS,             VK_OEM_102},

    {com_sun_glass_events_KeyEvent_VK_F1,               VK_F1},
    {com_sun_glass_events_KeyEvent_VK_F2,               VK_F2},
    {com_sun_glass_events_KeyEvent_VK_F3,               VK_F3},
    {com_sun_glass_events_KeyEvent_VK_F4,               VK_F4},
    {com_sun_glass_events_KeyEvent_VK_F5,               VK_F5},
    {com_sun_glass_events_KeyEvent_VK_F6,               VK_F6},
    {com_sun_glass_events_KeyEvent_VK_F7,               VK_F7},
    {com_sun_glass_events_KeyEvent_VK_F8,               VK_F8},
    {com_sun_glass_events_KeyEvent_VK_F9,               VK_F9},
    {com_sun_glass_events_KeyEvent_VK_F10,              VK_F10},
    {com_sun_glass_events_KeyEvent_VK_F11,              VK_F11},
    {com_sun_glass_events_KeyEvent_VK_F12,              VK_F12},
//    {com_sun_glass_events_KeyEvent_VK_F13,              VK_F13},
//    {com_sun_glass_events_KeyEvent_VK_F14,              VK_F14},
//    {com_sun_glass_events_KeyEvent_VK_F15,              VK_F15},
//    {com_sun_glass_events_KeyEvent_VK_F16,              VK_F16},
//    {com_sun_glass_events_KeyEvent_VK_F17,              VK_F17},
//    {com_sun_glass_events_KeyEvent_VK_F18,              VK_F18},
//    {com_sun_glass_events_KeyEvent_VK_F19,              VK_F19},
//    {com_sun_glass_events_KeyEvent_VK_F20,              VK_F20},
//    {com_sun_glass_events_KeyEvent_VK_F21,              VK_F21},
//    {com_sun_glass_events_KeyEvent_VK_F22,              VK_F22},
//    {com_sun_glass_events_KeyEvent_VK_F23,              VK_F23},
//    {com_sun_glass_events_KeyEvent_VK_F24,              VK_F24},

    {com_sun_glass_events_KeyEvent_VK_INSERT,           VK_INSERT},
    {com_sun_glass_events_KeyEvent_VK_DELETE,           VK_DELETE},
    {com_sun_glass_events_KeyEvent_VK_PRINTSCREEN,      VK_SNAPSHOT},
    {com_sun_glass_events_KeyEvent_VK_PAUSE,            VK_PAUSE},
//    {com_sun_glass_events_KeyEvent_VK_CANCEL,           VK_CANCEL},
    {com_sun_glass_events_KeyEvent_VK_HELP,             VK_HELP},
//    {com_sun_glass_events_KeyEvent_VK_CLEAR,            VK_CLEAR},

    {com_sun_glass_events_KeyEvent_VK_UNDEFINED,        0}
};

jint WindowsKeyToJavaKey(UINT wKey)
{
    for (int i = 0; keyMapTable[i].windowsKey; i++) {
        if (keyMapTable[i].windowsKey == wKey) {
            return keyMapTable[i].javaKey;
        }
    }
    return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
}

static UINT const oemKeys[] = {
    VK_OEM_1,
    VK_OEM_PLUS,
    VK_OEM_COMMA,
    VK_OEM_MINUS,
    VK_OEM_PERIOD,
    VK_OEM_2,
    VK_OEM_3,
    VK_OEM_4,
    VK_OEM_5,
    VK_OEM_6,
    VK_OEM_7,
    VK_OEM_8,
    VK_OEM_102
};
static constexpr size_t numOEMKeys = sizeof(oemKeys) / sizeof(oemKeys[0]);

static BOOL isOEMKey(UINT vkey)
{
    for (size_t i = 0; i < numOEMKeys; ++i) {
        if (oemKeys[i] == vkey) {
            return true;
        }
    }
    return false;
}

jint OEMCharToJavaKey(UINT ch, bool deadKey)
{
    jint jKeyCode = com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    if (deadKey) {
        switch (ch) {
            case L'`':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_GRAVE; break;
            case L'\'':  jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ACUTE; break;
            case 0x00B4: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ACUTE; break;
            case L'^':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CIRCUMFLEX; break;
            case L'~':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_TILDE; break;
            case 0x02DC: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_TILDE; break;
            case 0x00AF: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_MACRON; break;
            case 0x02D8: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_BREVE; break;
            case 0x02D9: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ABOVEDOT; break;
            case L'"':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_DIAERESIS; break;
            case 0x00A8: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_DIAERESIS; break;
            case 0x02DA: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ABOVERING; break;
            case 0x02DD: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_DOUBLEACUTE; break;
            case 0x02C7: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CARON; break;            // aka hacek
            case L',':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CEDILLA; break;
            case 0x00B8: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CEDILLA; break;
            case 0x02DB: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_OGONEK; break;
            case 0x037A: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_IOTA; break;             // ASCII ???
            case 0x309B: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_VOICED_SOUND; break;
            case 0x309C: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_SEMIVOICED_SOUND; break;
        }
    }
    else {
        switch (ch) {
            case L'!':   jKeyCode = com_sun_glass_events_KeyEvent_VK_EXCLAMATION; break;
            case L'"':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DOUBLE_QUOTE; break;
            case L'#':   jKeyCode = com_sun_glass_events_KeyEvent_VK_NUMBER_SIGN; break;
            case L'$':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DOLLAR; break;
            case L'&':   jKeyCode = com_sun_glass_events_KeyEvent_VK_AMPERSAND; break;
            case L'\'':  jKeyCode = com_sun_glass_events_KeyEvent_VK_QUOTE; break;
            case L'(':   jKeyCode = com_sun_glass_events_KeyEvent_VK_LEFT_PARENTHESIS; break;
            case L')':   jKeyCode = com_sun_glass_events_KeyEvent_VK_RIGHT_PARENTHESIS; break;
            case L'*':   jKeyCode = com_sun_glass_events_KeyEvent_VK_ASTERISK; break;
            case L'+':   jKeyCode = com_sun_glass_events_KeyEvent_VK_PLUS; break;
            case L',':   jKeyCode = com_sun_glass_events_KeyEvent_VK_COMMA; break;
            case L'-':   jKeyCode = com_sun_glass_events_KeyEvent_VK_MINUS; break;
            case L'.':   jKeyCode = com_sun_glass_events_KeyEvent_VK_PERIOD; break;
            case L'/':   jKeyCode = com_sun_glass_events_KeyEvent_VK_SLASH; break;
            case L':':   jKeyCode = com_sun_glass_events_KeyEvent_VK_COLON; break;
            case L';':   jKeyCode = com_sun_glass_events_KeyEvent_VK_SEMICOLON; break;
            case L'<':   jKeyCode = com_sun_glass_events_KeyEvent_VK_LESS; break;
            case L'=':   jKeyCode = com_sun_glass_events_KeyEvent_VK_EQUALS; break;
            case L'>':   jKeyCode = com_sun_glass_events_KeyEvent_VK_GREATER; break;
            case L'@':   jKeyCode = com_sun_glass_events_KeyEvent_VK_AT; break;
            case L'[':   jKeyCode = com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET; break;
            case L'\\':  jKeyCode = com_sun_glass_events_KeyEvent_VK_BACK_SLASH; break;
            case L']':   jKeyCode = com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET; break;
            case L'^':   jKeyCode = com_sun_glass_events_KeyEvent_VK_CIRCUMFLEX; break;
            case L'_':   jKeyCode = com_sun_glass_events_KeyEvent_VK_UNDERSCORE; break;
            case L'`':   jKeyCode = com_sun_glass_events_KeyEvent_VK_BACK_QUOTE; break;
            case L'{':   jKeyCode = com_sun_glass_events_KeyEvent_VK_BRACELEFT; break;
            case L'}':   jKeyCode = com_sun_glass_events_KeyEvent_VK_BRACERIGHT; break;
            case 0x00A1: jKeyCode = com_sun_glass_events_KeyEvent_VK_INV_EXCLAMATION; break;
            case 0x20A0: jKeyCode = com_sun_glass_events_KeyEvent_VK_EURO_SIGN; break;
        }
    }
    return jKeyCode;
}

void JavaKeyToWindowsKey(jint jkey, UINT &vkey, UINT& modifiers)
{
    vkey = 0;
    modifiers = 0;

    if (jkey == com_sun_glass_events_KeyEvent_VK_UNDEFINED) {
        return;
    }

    for (int i = 0; keyMapTable[i].windowsKey; i++) {
        if (keyMapTable[i].javaKey == jkey) {
            vkey = keyMapTable[i].windowsKey;
            break;
        }
    }

    if (!vkey || isOEMKey(vkey)) {
        // The table is missing entries for keys that don't appear on US
        // layouts, like KeyCode.PLUS. Even if we found a key it may be
        // an OEM key and the relationship between OEM keys and the
        // characters they generate is not fixed even for US English
        // layouts. So in these instances we search through the OEM keys
        // looking for the Java code.
        vkey = 0;
        for (size_t i = 0; i < numOEMKeys; ++i) {
            UINT ch = ::MapVirtualKey(oemKeys[i], 2);
            bool deadKey = (ch & 0x80000000);
            jint trialCode = OEMCharToJavaKey(LOWORD(ch), deadKey);
            if (trialCode == jkey) {
                vkey = oemKeys[i];
                break;
            }
        }
    }
}

BOOL IsExtendedKey(UINT vkey) {
    switch (vkey) {
        case VK_INSERT:
        case VK_DELETE:
        case VK_HOME:
        case VK_END:
        case VK_PRIOR:
        case VK_NEXT:
        case VK_LEFT:
        case VK_UP:
        case VK_RIGHT:
        case VK_DOWN:
        case VK_NUMLOCK:
        case VK_NUMPAD0:
        case VK_NUMPAD1:
        case VK_NUMPAD2:
        case VK_NUMPAD3:
        case VK_NUMPAD4:
        case VK_NUMPAD5:
        case VK_NUMPAD6:
        case VK_NUMPAD7:
        case VK_NUMPAD8:
        case VK_NUMPAD9:
        case VK_SNAPSHOT:
            return true;
        default:
            return false;
    }
}

BOOL IsNumericKeypadCode(int javaCode) {
    switch (javaCode) {
        case com_sun_glass_events_KeyEvent_VK_DIVIDE:
        case com_sun_glass_events_KeyEvent_VK_MULTIPLY:
        case com_sun_glass_events_KeyEvent_VK_SUBTRACT:
        case com_sun_glass_events_KeyEvent_VK_ADD:
        case com_sun_glass_events_KeyEvent_VK_DECIMAL:
        case com_sun_glass_events_KeyEvent_VK_SEPARATOR:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD0:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD1:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD2:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD3:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD4:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD5:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD6:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD7:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD8:
        case com_sun_glass_events_KeyEvent_VK_NUMPAD9:
            return true;
    }
    return false;
}

/*
 * Class:     Java_com_sun_glass_ui_win_WinApplication
 * Method:    _getKeyCodeForChar
 * Signature: (CI)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinApplication__1getKeyCodeForChar
  (JNIEnv * env, jobject jApplication, jchar c, jint hint)
{
    // The Delete key doesn't generate a character so ViewContainer::HandleViewKeyEvent
    // synthesizes one. Here we reverse that process.
    if ((TCHAR)c == 0x7F) {
        return com_sun_glass_events_KeyEvent_VK_DELETE;
    }

    HKL layout = ::GetKeyboardLayout(GlassApplication::GetMainThreadId());

    // If the system is trying to match against the numeric keypad verify that
    // the key generates the expected character.
    if (IsNumericKeypadCode(hint)) {
        UINT vkey = 0, modifiers = 0;
        JavaKeyToWindowsKey(hint, vkey, modifiers);
        if (vkey != 0) {
            UINT mapped = ::MapVirtualKeyEx(vkey, MAPVK_VK_TO_CHAR, layout);
            if (mapped != 0 && mapped == c) {
                return hint;
            }
        }
    }

    BYTE vkey = 0xFF & ::VkKeyScanEx((TCHAR)c, layout);

    if (!vkey || vkey == 0xFF) {
        return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    }

    // Duplicate the encoding used in ViewContainer::HandleViewKeyEvent
    if (isOEMKey(vkey)) {
        UINT mapped = ::MapVirtualKeyEx(vkey, MAPVK_VK_TO_CHAR, layout);
        if (mapped == 0) {
            return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
        }
        bool deadKey = (mapped & 0x80000000);
        return OEMCharToJavaKey(LOWORD(mapped), deadKey);
    }

    return WindowsKeyToJavaKey(vkey);
}

/*
 * Class:     com_sun_glass_ui_win_WinApplication
 * Method:    _isKeyLocked
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinApplication__1isKeyLocked
  (JNIEnv * env, jobject obj, jint keyCode)
{
    SHORT keyState = 0;
    switch (keyCode) {
        case com_sun_glass_events_KeyEvent_VK_CAPS_LOCK:
            keyState = ::GetKeyState(VK_CAPITAL);
            break;

        case com_sun_glass_events_KeyEvent_VK_NUM_LOCK:
            keyState = ::GetKeyState(VK_NUMLOCK);
            break;

        default:
            return com_sun_glass_events_KeyEvent_KEY_LOCK_UNKNOWN;
    }
    return (keyState & 0x1) ? com_sun_glass_events_KeyEvent_KEY_LOCK_ON
                            : com_sun_glass_events_KeyEvent_KEY_LOCK_OFF;
}
