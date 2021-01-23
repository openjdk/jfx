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

void JavaKeyToWindowsKey(jint jkey, UINT &vkey, UINT &modifiers)
{
    for (int i = 0; keyMapTable[i].windowsKey; i++) {
        if (keyMapTable[i].javaKey == jkey) {
            vkey = keyMapTable[i].windowsKey;
            modifiers = 0;
            return;
        }
    }

    vkey = 0;
    modifiers = 0;
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

/*
 * Class:     Java_com_sun_glass_ui_win_WinApplication
 * Method:    _getKeyCodeForChar
 * Signature: (C)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinApplication__1getKeyCodeForChar
  (JNIEnv * env, jobject jApplication, jchar c)
{
    BYTE vkey = 0xFF & ::VkKeyScanEx((TCHAR)c,
            ::GetKeyboardLayout(GlassApplication::GetMainThreadId()));

    if (!vkey || vkey == 0xFF) {
        return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
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
