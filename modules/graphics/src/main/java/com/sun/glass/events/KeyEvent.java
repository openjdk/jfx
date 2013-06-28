/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.events;

public class KeyEvent {
    /***************************************************************************
     *                                                                         *
     * Native methods                                                          *
     *                                                                         *
     **************************************************************************/
    native private static int _getKeyCodeForChar(char c);

    /***************************************************************************
     *                                                                         *
     * Flags                                                                   *
     *                                                                         *
     **************************************************************************/

    /*
     * Key event type.
     */
    public final static int PRESS   = 111;
    public final static int RELEASE = 112;
    public final static int TYPED   = 113; // synthetic

    /*
     * Key event modifier flags.
     *
     * CONTROL/WINDOWS and OPTION/ALT are equal, because they
     * are mapped to each other on Mac/Windows
     */
    public final static int MODIFIER_NONE              = 0;
    public final static int MODIFIER_SHIFT             = 1 << 0;
    public final static int MODIFIER_FUNCTION          = 1 << 1;
    public final static int MODIFIER_CONTROL           = 1 << 2;

    public final static int MODIFIER_OPTION            = 1 << 3;
    public final static int MODIFIER_ALT               = 1 << 3;

    // The following should be named Meta perhaps?
    public final static int MODIFIER_COMMAND           = 1 << 4;
    public final static int MODIFIER_WINDOWS           = 1 << 4;

    // Mouse buttons
    public final static int MODIFIER_BUTTON_PRIMARY    = 1 << 5;
    public final static int MODIFIER_BUTTON_SECONDARY  = 1 << 6;
    public final static int MODIFIER_BUTTON_MIDDLE     = 1 << 7;

    /*
     * Key event key codes.
     */
    public final static int VK_UNDEFINED      = 0x0;
    // Misc
    public final static int VK_ENTER              = '\n';
    public final static int VK_BACKSPACE          = '\b';
    public final static int VK_TAB                = '\t';
//    public final static int VK_CANCEL             = 0x03;
    public final static int VK_CLEAR              = 0x0C;
    public final static int VK_PAUSE              = 0x13;
    public final static int VK_ESCAPE             = 0x1B;
    public final static int VK_SPACE              = 0x20;
    public final static int VK_DELETE             = 0x7F;
    public final static int VK_PRINTSCREEN        = 0x9A;
    public final static int VK_INSERT             = 0x9B;
    public final static int VK_HELP               = 0x9C;
    // Modifiers
    public final static int VK_SHIFT              = 0x10;
    public final static int VK_CONTROL            = 0x11;
    public final static int VK_ALT                = 0x12;
    public final static int VK_ALT_GRAPH          = 0xFF7E;
    public final static int VK_WINDOWS            = 0x020C;
    public static final int VK_CONTEXT_MENU       = 0x020D;
    public final static int VK_CAPS_LOCK          = 0x14;
    public final static int VK_NUM_LOCK           = 0x90;
    public final static int VK_SCROLL_LOCK        = 0x91;
    public final static int VK_COMMAND            = 0x0300;
    // Navigation keys
    public final static int VK_PAGE_UP            = 0x21;
    public final static int VK_PAGE_DOWN          = 0x22;
    public final static int VK_END                = 0x23;
    public final static int VK_HOME               = 0x24;
    public final static int VK_LEFT               = 0x25;
    public final static int VK_UP                 = 0x26;
    public final static int VK_RIGHT              = 0x27;
    public final static int VK_DOWN               = 0x28;

    // Misc 2
    public final static int VK_COMMA              = 0x2C; // ','
    public final static int VK_MINUS              = 0x2D; // '-'
    public final static int VK_PERIOD             = 0x2E; // '.'
    public final static int VK_SLASH              = 0x2F; // '/'
    public final static int VK_SEMICOLON          = 0x3B; // ';'
    public final static int VK_EQUALS             = 0x3D; // '='
    public final static int VK_OPEN_BRACKET       = 0x5B; // '['
    public final static int VK_BACK_SLASH         = 0x5C; // '\'
    public final static int VK_CLOSE_BRACKET      = 0x5D; // ']'
    public final static int VK_MULTIPLY           = 0x6A; // '*'
    public final static int VK_ADD                = 0x6B; // '+'
    public final static int VK_SEPARATOR          = 0x6C;
    public final static int VK_SUBTRACT           = 0x6D;
    public final static int VK_DECIMAL            = 0x6E;
    public final static int VK_DIVIDE             = 0x6F;
    public final static int VK_AMPERSAND          = 0x96;
    public final static int VK_ASTERISK           = 0x97;
    public final static int VK_DOUBLE_QUOTE       = 0x98; // '"'
    public final static int VK_LESS               = 0x99; // '<'
    public final static int VK_GREATER            = 0xa0; // '>'
    public final static int VK_BRACELEFT          = 0xa1; // '{'
    public final static int VK_BRACERIGHT         = 0xa2; // '}'
    public final static int VK_BACK_QUOTE         = 0xC0; // '`'
    public final static int VK_QUOTE              = 0xDE; // '''
    public final static int VK_AT                 = 0x0200; // '@'
    public final static int VK_COLON              = 0x0201; // ':'
    public final static int VK_CIRCUMFLEX         = 0x0202; // '^'
    public final static int VK_DOLLAR             = 0x0203; // '$'
    public final static int VK_EURO_SIGN          = 0x0204;
    public final static int VK_EXCLAMATION        = 0x0205; // '!'
    public final static int VK_INV_EXCLAMATION    = 0x0206;
    public final static int VK_LEFT_PARENTHESIS   = 0x0207; // '('
    public final static int VK_NUMBER_SIGN        = 0x0208; // '#'
    public final static int VK_PLUS               = 0x0209; // '+'
    public final static int VK_RIGHT_PARENTHESIS  = 0x020A; // ')'
    public final static int VK_UNDERSCORE         = 0x020B; // '_'
    // Numeric keys
    public final static int VK_0                  = 0x30; // '0'
    public final static int VK_1                  = 0x31; // '1'
    public final static int VK_2                  = 0x32; // '2'
    public final static int VK_3                  = 0x33; // '3'
    public final static int VK_4                  = 0x34; // '4'
    public final static int VK_5                  = 0x35; // '5'
    public final static int VK_6                  = 0x36; // '6'
    public final static int VK_7                  = 0x37; // '7'
    public final static int VK_8                  = 0x38; // '8'
    public final static int VK_9                  = 0x39; // '9'
    // Alpha keys
    public final static int VK_A                  = 0x41; // 'A'
    public final static int VK_B                  = 0x42; // 'B'
    public final static int VK_C                  = 0x43; // 'C'
    public final static int VK_D                  = 0x44; // 'D'
    public final static int VK_E                  = 0x45; // 'E'
    public final static int VK_F                  = 0x46; // 'F'
    public final static int VK_G                  = 0x47; // 'G'
    public final static int VK_H                  = 0x48; // 'H'
    public final static int VK_I                  = 0x49; // 'I'
    public final static int VK_J                  = 0x4A; // 'J'
    public final static int VK_K                  = 0x4B; // 'K'
    public final static int VK_L                  = 0x4C; // 'L'
    public final static int VK_M                  = 0x4D; // 'M'
    public final static int VK_N                  = 0x4E; // 'N'
    public final static int VK_O                  = 0x4F; // 'O'
    public final static int VK_P                  = 0x50; // 'P'
    public final static int VK_Q                  = 0x51; // 'Q'
    public final static int VK_R                  = 0x52; // 'R'
    public final static int VK_S                  = 0x53; // 'S'
    public final static int VK_T                  = 0x54; // 'T'
    public final static int VK_U                  = 0x55; // 'U'
    public final static int VK_V                  = 0x56; // 'V'
    public final static int VK_W                  = 0x57; // 'W'
    public final static int VK_X                  = 0x58; // 'X'
    public final static int VK_Y                  = 0x59; // 'Y'
    public final static int VK_Z                  = 0x5A; // 'Z'
    // Numpad keys
    public final static int VK_NUMPAD0            = 0x60;
    public final static int VK_NUMPAD1            = 0x61;
    public final static int VK_NUMPAD2            = 0x62;
    public final static int VK_NUMPAD3            = 0x63;
    public final static int VK_NUMPAD4            = 0x64;
    public final static int VK_NUMPAD5            = 0x65;
    public final static int VK_NUMPAD6            = 0x66;
    public final static int VK_NUMPAD7            = 0x67;
    public final static int VK_NUMPAD8            = 0x68;
    public final static int VK_NUMPAD9            = 0x69;
    // Function keys
    public final static int VK_F1                 = 0x70;
    public final static int VK_F2                 = 0x71;
    public final static int VK_F3                 = 0x72;
    public final static int VK_F4                 = 0x73;
    public final static int VK_F5                 = 0x74;
    public final static int VK_F6                 = 0x75;
    public final static int VK_F7                 = 0x76;
    public final static int VK_F8                 = 0x77;
    public final static int VK_F9                 = 0x78;
    public final static int VK_F10                = 0x79;
    public final static int VK_F11                = 0x7A;
    public final static int VK_F12                = 0x7B;
    public final static int VK_F13                = 0xF000;
    public final static int VK_F14                = 0xF001;
    public final static int VK_F15                = 0xF002;
    public final static int VK_F16                = 0xF003;
    public final static int VK_F17                = 0xF004;
    public final static int VK_F18                = 0xF005;
    public final static int VK_F19                = 0xF006;
    public final static int VK_F20                = 0xF007;
    public final static int VK_F21                = 0xF008;
    public final static int VK_F22                = 0xF009;
    public final static int VK_F23                = 0xF00A;
    public final static int VK_F24                = 0xF00B;

    /***************************************************************************
     *                                                                         *
     * Static Methods                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns a VK_ code of a key capable of producing the given unicode
     * character with respect to the currently active keyboard layout or
     * VK_UNDEFINED if the character isn't present in the current layout.
     *
     * @return integer code for the given char
     */
    public static int getKeyCodeForChar(char c) {
        return _getKeyCodeForChar(c);
    }

    /**
     * Gets a string representation of the KeyEvent type (PRESS, RELEASE, TYPED,
     * or UNKNOWN).
     * @param type Should be one of PRESS, RELEASE, or TYPED.
     * @return A string representation of the key event type. This will be
     *           UNKNOWN if the type is something other than what was expected.
     */
    public final static String getTypeString(int type) {
        switch (type) {
            case PRESS:   return "PRESS";
            case RELEASE: return "RELEASE";
            case TYPED:   return "TYPED";
            default:      return "UNKNOWN";
        }
    }
}
