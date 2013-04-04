/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#include "LensCommon.h"
#include <linux/input.h>
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_KeyEvent.h"

//track keys, for modifiers map,  that have more then one keyborad key.
//for example there are right and left shift, so only when both are not pressed
// the key can be removed from the map
//
static int shiftDown;
static int ctrlDown;
static int altDown = 0;
static int metaDown = 0;

//capslock state
static jboolean capsOn;

//current modifiers mask - bit map of com_sun_glass_events_KeyEvent_MODIFIER_*
static int modifiersMask = com_sun_glass_events_KeyEvent_MODIFIER_NONE;

/**
 * Comment on LensKeyboardMap: Currently *fb ports and DFB are
 * all have access to kernel keycode, so we using one unified
 * map. There is one exception - remote fb codes are different
 * and the port will translate rfb codes to linux kernel codes
 * and also other ports with different key code will have to do
 * the same.
 *
 * In the future we may need to extend the key map to be more
 * dynamic and to support different platform codes, but for know
 * its enough
 *
 * Also the map doesn't include locale support and unicode
 * translations
 */

typedef struct {
    int platformKeyCode; //default to linux kernel key codes
    int jfxKeyCode;
    char *keyStr;
    char *keyStrWithShift;
} LensKeyboardMap;

static LensKeyboardMap keyMap[] = {
    {KEY_RESERVED,  com_sun_glass_events_KeyEvent_VK_UNDEFINED, "", ""},
    {KEY_ESC,   com_sun_glass_events_KeyEvent_VK_ESCAPE, "", ""},
    {KEY_1, com_sun_glass_events_KeyEvent_VK_1, "1", "!"},
    {KEY_2, com_sun_glass_events_KeyEvent_VK_2, "2", "@"},
    {KEY_3, com_sun_glass_events_KeyEvent_VK_3, "3", "#"},
    {KEY_4, com_sun_glass_events_KeyEvent_VK_4, "4", "$"},
    {KEY_5, com_sun_glass_events_KeyEvent_VK_5, "5", "%"},
    {KEY_6, com_sun_glass_events_KeyEvent_VK_6, "6", "^"},
    {KEY_7, com_sun_glass_events_KeyEvent_VK_7, "7", "&"},
    {KEY_8, com_sun_glass_events_KeyEvent_VK_8, "8", "*"},
    {KEY_9, com_sun_glass_events_KeyEvent_VK_9, "9", "("},
    {KEY_0, com_sun_glass_events_KeyEvent_VK_0, "0", ")"},
    {KEY_MINUS, com_sun_glass_events_KeyEvent_VK_MINUS, "-", "_"},
    {KEY_EQUAL, com_sun_glass_events_KeyEvent_VK_EQUALS, "=", "+"},
    {KEY_BACKSPACE, com_sun_glass_events_KeyEvent_VK_BACKSPACE, "", ""},
    {KEY_TAB,   com_sun_glass_events_KeyEvent_VK_TAB, "", ""},
    {KEY_Q,     com_sun_glass_events_KeyEvent_VK_Q, "q", "Q"},
    {KEY_W,     com_sun_glass_events_KeyEvent_VK_W, "w", "W"},
    {KEY_E,     com_sun_glass_events_KeyEvent_VK_E, "e", "E"},
    {KEY_R,     com_sun_glass_events_KeyEvent_VK_R, "r", "R"},
    {KEY_T,     com_sun_glass_events_KeyEvent_VK_T, "t", "T"},
    {KEY_Y,     com_sun_glass_events_KeyEvent_VK_Y, "y", "Y"},
    {KEY_U,     com_sun_glass_events_KeyEvent_VK_U, "u", "U"},
    {KEY_I,     com_sun_glass_events_KeyEvent_VK_I, "i", "I"},
    {KEY_O,     com_sun_glass_events_KeyEvent_VK_O, "o", "O"},
    {KEY_P,     com_sun_glass_events_KeyEvent_VK_P, "p", "P"},
    {KEY_LEFTBRACE, com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET, "[", "{"},
    {KEY_RIGHTBRACE,    com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET, "]", "}"},
    {KEY_ENTER, com_sun_glass_events_KeyEvent_VK_ENTER, "", ""},
    {KEY_LEFTCTRL,  com_sun_glass_events_KeyEvent_VK_CONTROL, "", ""},
    {KEY_A,     com_sun_glass_events_KeyEvent_VK_A, "a", "A"},
    {KEY_S,     com_sun_glass_events_KeyEvent_VK_S, "s", "S"},
    {KEY_D,     com_sun_glass_events_KeyEvent_VK_D, "d", "D"},
    {KEY_F,     com_sun_glass_events_KeyEvent_VK_F, "f", "F"},
    {KEY_G,     com_sun_glass_events_KeyEvent_VK_G, "g", "G"},
    {KEY_H,     com_sun_glass_events_KeyEvent_VK_H, "h", "H"},
    {KEY_J,     com_sun_glass_events_KeyEvent_VK_J, "j", "J"},
    {KEY_K,     com_sun_glass_events_KeyEvent_VK_K, "k", "K"},
    {KEY_L,     com_sun_glass_events_KeyEvent_VK_L, "l", "L"},
    {KEY_SEMICOLON, com_sun_glass_events_KeyEvent_VK_SEMICOLON, ";", ":"},
    {KEY_APOSTROPHE,    com_sun_glass_events_KeyEvent_VK_QUOTE, "'", "\""},
    //tilda is missing
    {KEY_GRAVE, com_sun_glass_events_KeyEvent_VK_BACK_QUOTE, "`", "~"},
    {KEY_LEFTSHIFT, com_sun_glass_events_KeyEvent_VK_SHIFT, "", ""},
    {KEY_BACKSLASH, com_sun_glass_events_KeyEvent_VK_BACK_SLASH, "\\", "|"},
    {KEY_Z,     com_sun_glass_events_KeyEvent_VK_Z, "z", "Z"},
    {KEY_X,     com_sun_glass_events_KeyEvent_VK_X, "x", "X"},
    {KEY_C,     com_sun_glass_events_KeyEvent_VK_C, "c", "C"},
    {KEY_V,     com_sun_glass_events_KeyEvent_VK_V, "v", "V"},
    {KEY_B,     com_sun_glass_events_KeyEvent_VK_B, "b", "B"},
    {KEY_N,     com_sun_glass_events_KeyEvent_VK_N, "n", "N"},
    {KEY_M,     com_sun_glass_events_KeyEvent_VK_M, "m", "M"},
    {KEY_COMMA, com_sun_glass_events_KeyEvent_VK_COMMA, ",", "<"},
    {KEY_DOT,       com_sun_glass_events_KeyEvent_VK_PERIOD, ".", ">"},
    {KEY_SLASH, com_sun_glass_events_KeyEvent_VK_SLASH, "/", "?"},
    {KEY_RIGHTSHIFT,    com_sun_glass_events_KeyEvent_VK_SHIFT, "", ""},
    {KEY_KPASTERISK,    com_sun_glass_events_KeyEvent_VK_MULTIPLY, "*", ""},
    {KEY_LEFTALT,   com_sun_glass_events_KeyEvent_VK_ALT, "", ""},
    {KEY_SPACE, com_sun_glass_events_KeyEvent_VK_SPACE, " ", " "},
    {KEY_CAPSLOCK,  com_sun_glass_events_KeyEvent_VK_CAPS_LOCK, "", ""},
    {KEY_F1,        com_sun_glass_events_KeyEvent_VK_F1, "", ""},
    {KEY_F2,        com_sun_glass_events_KeyEvent_VK_F2, "", ""},
    {KEY_F3,        com_sun_glass_events_KeyEvent_VK_F3, "", ""},
    {KEY_F4,        com_sun_glass_events_KeyEvent_VK_F4, "", ""},
    {KEY_F5,        com_sun_glass_events_KeyEvent_VK_F5, "", ""},
    {KEY_F6,        com_sun_glass_events_KeyEvent_VK_F6, "", ""},
    {KEY_F7,        com_sun_glass_events_KeyEvent_VK_F7, "", ""},
    {KEY_F8,        com_sun_glass_events_KeyEvent_VK_F8, "", ""},
    {KEY_F9,        com_sun_glass_events_KeyEvent_VK_F9, "", ""},
    {KEY_F10,       com_sun_glass_events_KeyEvent_VK_F10, "", ""},
    {KEY_NUMLOCK,   com_sun_glass_events_KeyEvent_VK_NUM_LOCK, "", ""},
    {KEY_SCROLLLOCK,    com_sun_glass_events_KeyEvent_VK_SCROLL_LOCK, "", ""},
    //looks like we will need to return different java keys for the KPs if NUMLOCK is pressed or not.
    {KEY_KP7,       com_sun_glass_events_KeyEvent_VK_NUMPAD7, "7", ""},
    {KEY_KP8,       com_sun_glass_events_KeyEvent_VK_NUMPAD8, "8", ""},
    {KEY_KP9,       com_sun_glass_events_KeyEvent_VK_NUMPAD9, "9", ""},
    {KEY_KPMINUS,   com_sun_glass_events_KeyEvent_VK_SUBTRACT, "-", ""},
    {KEY_KP4,       com_sun_glass_events_KeyEvent_VK_NUMPAD4, "4", ""},
    {KEY_KP5,       com_sun_glass_events_KeyEvent_VK_NUMPAD5, "5", ""},
    {KEY_KP6,       com_sun_glass_events_KeyEvent_VK_NUMPAD6, "6", ""},
    {KEY_KPPLUS,    com_sun_glass_events_KeyEvent_VK_ADD, "+", ""},
    {KEY_KP1,       com_sun_glass_events_KeyEvent_VK_NUMPAD1, "1", ""},
    {KEY_KP2,       com_sun_glass_events_KeyEvent_VK_NUMPAD2, "2", ""},
    {KEY_KP3,       com_sun_glass_events_KeyEvent_VK_NUMPAD3, "3", ""},
    {KEY_KP0,       com_sun_glass_events_KeyEvent_VK_NUMPAD7, "0", ""},
    {KEY_KPDOT,     com_sun_glass_events_KeyEvent_VK_DECIMAL, ".", ""},
    // End of KP
    //{KEY_ZENKAKUHANKAKU,  85
    //{KEY_102ND,   86
    {KEY_F11,       com_sun_glass_events_KeyEvent_VK_F11, "", ""},
    {KEY_F12,       com_sun_glass_events_KeyEvent_VK_F12, "", ""},
    //{KEY_RO,      89
    //{KEY_KATAKANA,    90
    //{KEY_HIRAGANA,    91
    //{KEY_HENKAN,  92
    //{KEY_KATAKANAHIRAGANA,    93
    //{KEY_MUHENKAN,    94
    //{KEY_KPJPCOMMA,   95
    {KEY_KPENTER,   com_sun_glass_events_KeyEvent_VK_ENTER, "", ""},
    {KEY_RIGHTCTRL, com_sun_glass_events_KeyEvent_VK_CONTROL, "", ""},
    {KEY_KPSLASH,   com_sun_glass_events_KeyEvent_VK_DIVIDE, "/", ""},
    {KEY_SYSRQ, com_sun_glass_events_KeyEvent_VK_PRINTSCREEN, "", ""},
    {KEY_RIGHTALT,  com_sun_glass_events_KeyEvent_VK_ALT, "", ""},
    //{KEY_LINEFEED,    101
    {KEY_HOME,  com_sun_glass_events_KeyEvent_VK_HOME, "", ""},
    {KEY_UP,        com_sun_glass_events_KeyEvent_VK_UP, "", ""},
    {KEY_PAGEUP,    com_sun_glass_events_KeyEvent_VK_PAGE_UP, "", ""},
    {KEY_LEFT,  com_sun_glass_events_KeyEvent_VK_LEFT, "", ""},
    {KEY_RIGHT, com_sun_glass_events_KeyEvent_VK_RIGHT, "", ""},
    {KEY_END,       com_sun_glass_events_KeyEvent_VK_END, "", ""},
    {KEY_DOWN,  com_sun_glass_events_KeyEvent_VK_DOWN, "", ""},
    {KEY_PAGEDOWN,  com_sun_glass_events_KeyEvent_VK_PAGE_DOWN, "", ""},
    {KEY_INSERT,    com_sun_glass_events_KeyEvent_VK_INSERT, "", ""},
    {KEY_DELETE,    com_sun_glass_events_KeyEvent_VK_DELETE, "", ""},
    {KEY_LEFTMETA,  com_sun_glass_events_KeyEvent_VK_WINDOWS, "", ""},
    {KEY_RIGHTMETA, com_sun_glass_events_KeyEvent_VK_WINDOWS, "", ""},
    {KEY_COMPOSE,   com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU, "", ""},
};

static const unsigned int gKeyMapSize = sizeof(keyMap) / sizeof(LensKeyboardMap);

static inline jboolean glass_inputEvents_isAlpha(int javaKeyCode) {
    if (javaKeyCode >= com_sun_glass_events_KeyEvent_VK_A &&
            javaKeyCode <= com_sun_glass_events_KeyEvent_VK_Z) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

jboolean glass_inputEvents_checkForShift(int keyCode) {
    if (shiftDown || (capsOn && glass_inputEvents_isAlpha(keyCode))) {
        GLASS_LOG_FINER("Shift state is true");
        return JNI_TRUE;
    }
    GLASS_LOG_FINER("Shift state is false");
    return JNI_FALSE;
}

void glass_inputEvents_updateKeyModifiers(int key, int eventType) {
    jboolean isPressed = JNI_FALSE;
    int mask;

    if (eventType == com_sun_glass_events_KeyEvent_PRESS) {
        isPressed = JNI_TRUE;
    } else if (eventType == com_sun_glass_events_KeyEvent_RELEASE) {
        isPressed = JNI_FALSE;
    } else {
        GLASS_LOG_FINER("skipping - event %d not handled", eventType);
        return;
    }

    GLASS_LOG_FINER("updating modifiers for event[%d] on key[%d]", eventType, key);

    //screen none modifiers keys
    switch (key) {
        case com_sun_glass_events_KeyEvent_VK_SHIFT:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_SHIFT;
            //update counter
            if (isPressed) {
                shiftDown++;
                GLASS_LOG_FINER("SHIFT was pressed shiftDown = %d", shiftDown);
            } else {
       #ifdef ANDROID_NDK  
                shiftDown = 0;
       #else
                shiftDown--;
       #endif
                GLASS_LOG_FINER("SHIFT was released shiftDown = %d", shiftDown);
            }
            //update mask
            if (shiftDown) {
                modifiersMask |= mask;
                GLASS_LOG_FINER("SHIFT is pressed");
            } else {
                modifiersMask &= ~mask;
                GLASS_LOG_FINER("SHIFT is not pressed");
            }
            break;
        case com_sun_glass_events_KeyEvent_VK_CONTROL:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_CONTROL;
            //update counter
            if (isPressed) {
                ctrlDown++;
                GLASS_LOG_FINER("CTRL was pressed");
            } else {
       #ifdef ANDROID_NDK
                ctrlDown = 0;
       #else
                ctrlDown--;
       #endif
                GLASS_LOG_FINER("CTRL was released");
            }
            //update mask
            if (ctrlDown) {
                modifiersMask |= mask;
                GLASS_LOG_FINER("CTRL is pressed");
            } else {
                modifiersMask &= ~mask;
                GLASS_LOG_FINER("CTRL is not  pressed");
            }
            break;
        case com_sun_glass_events_KeyEvent_VK_ALT:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_ALT;
            //update counter
            if (isPressed) {
                altDown++;
                GLASS_LOG_FINER("ALT was pressed");
            } else {                
       #ifdef ANDROID_NDK
                altDown = 0;
       #else
                altDown--;
       #endif
                GLASS_LOG_FINER("ALT was released");
            }
            //update mask
            if (altDown) {
                modifiersMask |= mask;
                GLASS_LOG_FINER("ALT is pressed");
            } else {
                modifiersMask &= ~mask;
                GLASS_LOG_FINER("ALT is not pressed");
            }
            break;
        case com_sun_glass_events_KeyEvent_VK_WINDOWS:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_WINDOWS;
            //update counter
            if (isPressed) {
                metaDown++;
                GLASS_LOG_FINER("META was pressed");
            } else {
       #ifdef ANDROID_NDK
                metaDown = 0;
       #else
                metaDown--;
       #endif
                GLASS_LOG_FINER("META was released");
            }
            //update mask
            if (metaDown) {
                modifiersMask |= mask;
                GLASS_LOG_FINER("META is pressed");
            } else {
                modifiersMask &= ~mask;
                GLASS_LOG_FINER("META is not pressed");
            }
            break;
        case com_sun_glass_events_KeyEvent_VK_CAPS_LOCK:
            if (isPressed) {
                //reverse state
                capsOn = (capsOn) ? JNI_FALSE : JNI_TRUE;
                GLASS_LOG_FINER("Capslock was pressed and its now %s",
                                capsOn ? "ON" : "OFF");
                break;
            }
        default:
            GLASS_LOG_FINER("Key %d ignored - not a modifier", key);
    }
}

void glass_inputEvents_updateMouseButtonModifiers(int button, int eventType) {

    jboolean isPressed;
    int mask;

    if (eventType == com_sun_glass_events_MouseEvent_DOWN) {
        isPressed = JNI_TRUE;
    } else if (eventType == com_sun_glass_events_MouseEvent_UP) {
        isPressed = JNI_FALSE;
    } else {
        GLASS_LOG_FINER("skipping - event %d not handled", eventType);
        return;
    }

    switch (button) {
        case com_sun_glass_events_MouseEvent_BUTTON_LEFT:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY;
            break;
        case com_sun_glass_events_MouseEvent_BUTTON_RIGHT:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY;
            break;
        case com_sun_glass_events_MouseEvent_BUTTON_OTHER:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE;
            break;
        default:
            mask = com_sun_glass_events_KeyEvent_MODIFIER_NONE;

    }

    if (mask != com_sun_glass_events_KeyEvent_MODIFIER_NONE) {
        if (isPressed) {
            modifiersMask |= mask;
        } else {
            modifiersMask &= ~mask;
        }
    }

}

int glass_inputEvents_getModifiers() {
    return modifiersMask;
}

int glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(int platformKey) {
    unsigned int i;
    GLASS_LOG_FINER("Searching for platform key[%d]...", platformKey);
    for (i = 0; i < gKeyMapSize; i++) {
        if (keyMap[i].platformKeyCode == platformKey) {
            GLASS_LOG_FINER("Found jfx key[%d]", keyMap[i].jfxKeyCode);
            return keyMap[i].jfxKeyCode;
        }
    }
    GLASS_LOG_FINER("No key found");
    return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
}

int glass_inputEvents_getJavaKeyCodeFromJChar(jchar c) {
    unsigned int i;
    for (i = 0; i < gKeyMapSize; i++) {
        if (keyMap[i].keyStr[0] == c || keyMap[i].keyStrWithShift[0] == c) {
            return keyMap[i].jfxKeyCode;
        }
    }
    return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
}

LensResult glass_inputEvents_getKeyChar(int jfxKeyCode, char **keyStr) {

    unsigned int i;

    GLASS_LOG_FINER("searching char for key[%d]", jfxKeyCode);
    if (keyStr == NULL) {
        GLASS_LOG_WARNING("keyStr == NULL - abort");
        return LENS_FAILED;
    }

    for (i = 0; i < gKeyMapSize; i++) {
        if (keyMap[i].jfxKeyCode == jfxKeyCode) {
            if (keyMap[i].jfxKeyCode == jfxKeyCode) {
                if (glass_inputEvents_checkForShift(jfxKeyCode)) {
                    *keyStr = keyMap[i].keyStrWithShift;
                } else {
                    *keyStr = keyMap[i].keyStr;
                }
                GLASS_LOG_FINER("key char = %s", *keyStr);
                return LENS_OK;
            }
        }
    }

    return LENS_FAILED;

}

jboolean glass_inputEvents_isKeyModifier(int jfxKeyCode) {
    switch (jfxKeyCode) {
        case com_sun_glass_events_KeyEvent_VK_SHIFT:
        case com_sun_glass_events_KeyEvent_VK_ALT:
        case com_sun_glass_events_KeyEvent_VK_CONTROL:
        case com_sun_glass_events_KeyEvent_VK_WINDOWS:
        case com_sun_glass_events_KeyEvent_VK_CAPS_LOCK:
            return JNI_TRUE;
        default:
            return JNI_FALSE;
    }
}
