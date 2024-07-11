/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.embed.swt;

import org.eclipse.swt.SWT;
//import org.eclipse.swt.events.MouseEvent;

import java.lang.reflect.Method;

//import com.sun.glass.events.KeyEvent;
import com.sun.javafx.embed.AbstractEvents;
import org.eclipse.swt.widgets.Event;

import java.lang.reflect.InvocationTargetException;

/**
 * An utility class to translate event types between embedded
 * application and SWT.
 *
 */
class SWTEvents {

/*
    static int mouseIDToEmbedMouseType(int id) {
        switch (id) {
            case MouseEvent.MOUSE_PRESSED:
                return AbstractEvents.MOUSEEVENT_PRESSED;
            case MouseEvent.MOUSE_RELEASED:
                return AbstractEvents.MOUSEEVENT_RELEASED;
            case MouseEvent.MOUSE_CLICKED:
                return AbstractEvents.MOUSEEVENT_CLICKED;
            case MouseEvent.MOUSE_MOVED:
                return AbstractEvents.MOUSEEVENT_MOVED;
            case MouseEvent.MOUSE_DRAGGED:
                return AbstractEvents.MOUSEEVENT_DRAGGED;
            case MouseEvent.MOUSE_ENTERED:
                return AbstractEvents.MOUSEEVENT_ENTERED;
            case MouseEvent.MOUSE_EXITED:
                return AbstractEvents.MOUSEEVENT_EXITED;
        }
        return 0;
    }
*/
    static int mouseButtonToEmbedMouseButton(int button, int extModifiers) {
        switch (button) {
            case 1: return AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
            case 2: return AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
            case 3: return AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
            case 4: return AbstractEvents.MOUSEEVENT_BACK_BUTTON;
            case 5: return AbstractEvents.MOUSEEVENT_FORWARD_BUTTON;
        }
        return AbstractEvents.MOUSEEVENT_NONE_BUTTON;
    }

    static double getWheelRotation(Event e) {
        int divisor = 1;
        if ("win32".equals(SWT.getPlatform()) && e.type == SWT.MouseVerticalWheel) {
            int [] linesToScroll = new int [1];
            //OS.SystemParametersInfo (OS.SPI_GETWHEELSCROLLLINES, 0, linesToScroll, 0);
            try {
                Class clazz = Class.forName("org.eclipse.swt.internal.win32.OS");
                Method method = clazz.getDeclaredMethod("SystemParametersInfo", new Class []{int.class, int.class, int [].class, int.class});
                method.invoke(clazz, 104 /*SPI_GETWHEELSCROLLLINES*/, 0, linesToScroll, 0);
            } catch (IllegalAccessException iae) {
            } catch (InvocationTargetException ite) {
            } catch (NoSuchMethodException nme) {
            } catch (ClassNotFoundException cfe) {
                //Fail silently
            }
            if (linesToScroll [0] != -1 /*OS.WHEEL_PAGESCROLL*/) {
                divisor = linesToScroll [0];
            }
        } else if ("gtk".equals(SWT.getPlatform())) {
            divisor = 3;
        }
        else if ("cocoa".equals(SWT.getPlatform())) {
            divisor = Math.abs(e.count);
        }
        return e.count / (double) Math.max(1, divisor);
    }

    static int keyIDToEmbedKeyType(int id) {
        switch (id) {
            case SWT.KeyDown:
                return AbstractEvents.KEYEVENT_PRESSED;
            case SWT.KeyUp:
                return AbstractEvents.KEYEVENT_RELEASED;
//            case KeyEvent.KEY_TYPED:
//                return AbstractEvents.KEYEVENT_TYPED;
        }
        return 0;
    }

    static final int [] [] KeyTable = {

        {0x0 /*KeyEvent.VK_UNDEFINED*/,     SWT.NULL},

        // SWT only
        {'\n' /*KeyEvent.VK_?????*/,         SWT.CR},

        // Misc
        {'\n' /*KeyEvent.VK_ENTER*/,         SWT.LF},
        {'\b' /*KeyEvent.VK_BACKSPACE*/,     SWT.BS},
        {'\t' /*KeyEvent.VK_TAB*/,           SWT.TAB},
//      {KeyEvent.VK_CANCEL         SWT.???},
//      {KeyEvent.VK_CLEAR          SWT.???},
//      {KeyEvent.VK_PAUSE          SWT.???},
        {0x1B /*KeyEvent.VK_ESCAPE*/,        SWT.ESC},
        {0x20 /*KeyEvent.VK_SPACE*/,         0x20},
        {0x7F /*KeyEvent.VK_DELETE*/,        SWT.DEL},
//      {KeyEvent.VK_PRINTSCREEN    SWT.???;
        {0x9B /*KeyEvent.VK_INSERT*/,        SWT.INSERT},
        {0x9C /*KeyEvent.VK_HELP*/,          SWT.HELP},

        // Modifiers
        {0x10 /*KeyEvent.VK_SHIFT*/,         SWT.SHIFT},
        {0x11 /*KeyEvent.VK_CONTROL*/,       SWT.CONTROL},
        {0x12 /*KeyEvent.VK_ALT*/,           SWT.ALT},
        {0x020C /*(KeyEvent.VK_WINDOWS*/,       SWT.COMMAND},
 //     {KeyEvent.VK_CONTEXT_MENU,  SWT.???},
        {0x14 /*KeyEvent.VK_CAPS_LOCK*/,     SWT.CAPS_LOCK},
        {0x90 /*KeyEvent.VK_NUM_LOCK*/,      SWT.NUM_LOCK},
        {0x91 /*KeyEvent.VK_SCROLL_LOCK*/,   SWT.SCROLL_LOCK},

        // Navigation keys
        {0x21 /*KeyEvent.VK_PAGE_UP*/,       SWT.PAGE_UP},
        {0x22 /*KeyEvent.VK_PAGE_DOWN*/,     SWT.PAGE_DOWN},
        {0x23 /*KeyEvent.VK_END*/,           SWT.END},
        {0x24 /*KeyEvent.VK_HOME*/,          SWT.HOME},
        {0x25 /*KeyEvent.VK_LEFT*/,          SWT.ARROW_LEFT},
        {0x26 /*KeyEvent.VK_UP*/,            SWT.ARROW_UP},
        {0x27 /*KeyEvent.VK_RIGHT*/,         SWT.ARROW_RIGHT},
        {0x28 /*KeyEvent.VK_DOWN*/,          SWT.ARROW_DOWN},

        // Misc 2
        // NOTE: suspect this only works for English keyboard
        {0x2C /*KeyEvent.VK_COMMA*/,                 ','}, // ','
        {0x2D /*KeyEvent.VK_MINUS*/,                 '-'}, // '-'
        {0x2E /*KeyEvent.VK_PERIOD*/,                '.'}, // '.'
        {0x2F /*KeyEvent.VK_SLASH*/,                 '/'}, // '/'
        {0x3B /*KeyEvent.VK_SEMICOLON*/,             ';'}, // ';'
        {0x3D /*KeyEvent.VK_EQUALS*/,                '='}, // '='
        {0x5B /*KeyEvent.VK_OPEN_BRACKET*/,          '['}, // '['
        {0x5C /*KeyEvent.VK_BACK_SLASH*/,            '\\'}, // '\'
        {0x5D /*KeyEvent.VK_CLOSE_BRACKET*/,         ']'}, // ']'

        // Numeric key pad keys
        {0x6A /*KeyEvent.VK_MULTIPLY*/,     SWT.KEYPAD_MULTIPLY}, // '*'
        {0x6B /*KeyEvent.VK_ADD*/,          SWT.KEYPAD_ADD}, // '+'
//        {0x6C /*KeyEvent.VK_SEPARATOR*/,    SWT.???},
        {0x6D /*KeyEvent.VK_SUBTRACT*/,     SWT.KEYPAD_SUBTRACT},
        {0x6E /*KeyEvent.VK_DECIMAL*/,      SWT.KEYPAD_DECIMAL},
        {0x6F /*KeyEvent.VK_DIVIDE*/,       SWT.KEYPAD_DIVIDE},
//        {0x?? /*KeyEvent.VK_????*/,         SWT.KEYPAD_EQUAL},
//        {0x?? /*KeyEvent.VK_????*/,         SWT.KEYPAD_CR},

        {0x96 /*KeyEvent.VK_AMPERSAND*/,             '@'},
        {0x97 /*KeyEvent.VK_ASTERISK*/,              '*'},

        {0x98 /*KeyEvent.VK_DOUBLE_QUOTE*/,          '"'}, // '"'
        {0x99 /*KeyEvent.VK_LESS*/,                  '<'}, // '<'
        {0xa0 /*KeyEvent.VK_GREATER*/,               '>'}, // '>'
        {0xa1 /*KeyEvent.VK_BRACELEFT*/,             '{'}, // '{'
        {0xa2 /*KeyEvent.VK_BRACERIGHT*/,            '}'}, // '}'
        {0xC0 /*KeyEvent.VK_BACK_QUOTE*/,            '`'}, // '`'
        {0xDE /*KeyEvent.VK_QUOTE*/,                 '\''}, // '''
        {0x0200 /*KeyEvent.VK_AT*/,                    '@'}, // '@'
        {0x0201 /*KeyEvent.VK_COLON*/,                 ':'}, // ':'
        {0x0202 /*KeyEvent.VK_CIRCUMFLEX*/,            '^'}, // '^'
        {0x0203 /*KeyEvent.VK_DOLLAR*/,                '$'}, // '$'
//        {KeyEvent.VK_EURO_SIGN,             0x0204},
        {0x0205 /*KeyEvent.VK_EXCLAMATION*/,           '!'}, // '!'
//        {KeyEvent.VK_INV_EXCLAMATION,       0x0206},
        {0x0207 /*KeyEvent.VK_LEFT_PARENTHESIS*/,      '('}, // '('
        {0x0208 /*KeyEvent.VK_NUMBER_SIGN*/,           '#'}, // '#'
        {0x0209 /*KeyEvent.VK_PLUS*/,                  '+'}, // '+'
        {0x020A /*KeyEvent.VK_RIGHT_PARENTHESIS*/,      ')'}, // ')'
        {0x020B /*KeyEvent.VK_UNDERSCORE*/,             '_'}, // '_'

        // Numeric keys
        // NOTE: suspect this only works for English keyboard
        {0x30 /*KeyEvent.VK_0*/, '0'}, // '0'
        {0x31 /*KeyEvent.VK_1*/, '1'}, // '1'
        {0x32 /*KeyEvent.VK_2*/, '2'}, // '2'
        {0x33 /*KeyEvent.VK_3*/, '3'}, // '3'
        {0x34 /*KeyEvent.VK_4*/, '4'}, // '4'
        {0x35 /*KeyEvent.VK_5*/, '5'}, // '5'
        {0x36 /*KeyEvent.VK_6*/, '6'}, // '6'
        {0x37 /*KeyEvent.VK_7*/, '7'}, // '7'
        {0x38 /*KeyEvent.VK_8*/, '8'}, // '8'
        {0x39 /*KeyEvent.VK_9*/, '9'}, // '9'

        // Alpha keys
        // NOTE: suspect this only works for English keyboard
        {0x41 /*KeyEvent.VK_A*/, 'a'}, // 'A'
        {0x42 /*KeyEvent.VK_B*/, 'b'}, // 'B'
        {0x43 /*KeyEvent.VK_C*/, 'c'}, // 'C'
        {0x44 /*KeyEvent.VK_D*/, 'd'}, // 'D'
        {0x45 /*KeyEvent.VK_E*/, 'e'}, // 'E'
        {0x46 /*KeyEvent.VK_F*/, 'f'}, // 'F'
        {0x47 /*KeyEvent.VK_G*/, 'g'}, // 'G'
        {0x48 /*KeyEvent.VK_H*/, 'h'}, // 'H'
        {0x49 /*KeyEvent.VK_I*/, 'i'}, // 'I'
        {0x4A /*KeyEvent.VK_J*/, 'j'}, // 'J'
        {0x4B /*KeyEvent.VK_K*/, 'k'}, // 'K'
        {0x4C /*KeyEvent.VK_L*/, 'l'}, // 'L'
        {0x4D /*KeyEvent.VK_M*/, 'm'}, // 'M'
        {0x4E /*KeyEvent.VK_N*/, 'n'}, // 'N'
        {0x4F /*KeyEvent.VK_O*/, 'o'}, // 'O'
        {0x50 /*KeyEvent.VK_P*/, 'p'}, // 'P'
        {0x51 /*KeyEvent.VK_Q*/, 'q'}, // 'Q'
        {0x52 /*KeyEvent.VK_R*/, 'r'}, // 'R'
        {0x53 /*KeyEvent.VK_S*/, 's'}, // 'S'
        {0x54 /*KeyEvent.VK_T*/, 't'}, // 'T'
        {0x55 /*KeyEvent.VK_U*/, 'u'}, // 'U'
        {0x56 /*KeyEvent.VK_V*/, 'v'}, // 'V'
        {0x57 /*KeyEvent.VK_W*/, 'w'}, // 'W'
        {0x58 /*KeyEvent.VK_X*/, 'x'}, // 'X'
        {0x59 /*KeyEvent.VK_Y*/, 'y'}, // 'Y'
        {0x5A /*KeyEvent.VK_Z*/, 'z'}, // 'Z'

        // Numpad keys
        {0x60 /*KeyEvent.VK_NUMPAD0*/,   SWT.KEYPAD_0},
        {0x61 /*KeyEvent.VK_NUMPAD1*/,   SWT.KEYPAD_1},
        {0x62 /*KeyEvent.VK_NUMPAD2*/,   SWT.KEYPAD_2},
        {0x63 /*KeyEvent.VK_NUMPAD3*/,   SWT.KEYPAD_3},
        {0x64 /*KeyEvent.VK_NUMPAD4*/,   SWT.KEYPAD_4},
        {0x65 /*KeyEvent.VK_NUMPAD5*/,   SWT.KEYPAD_5},
        {0x66 /*KeyEvent.VK_NUMPAD6*/,   SWT.KEYPAD_6},
        {0x67 /*KeyEvent.VK_NUMPAD7*/,   SWT.KEYPAD_7},
        {0x68 /*KeyEvent.VK_NUMPAD8*/,   SWT.KEYPAD_8},
        {0x69 /*KeyEvent.VK_NUMPAD9*/,   SWT.KEYPAD_9},

        // Function keys
        {0x70 /*KeyEvent.VK_F1*/,    SWT.F1},
        {0x71 /*KeyEvent.VK_F2*/,    SWT.F2},
        {0x72 /*KeyEvent.VK_F3*/,    SWT.F3},
        {0x73 /*KeyEvent.VK_F4*/,    SWT.F4},
        {0x74 /*KeyEvent.VK_F5*/,    SWT.F5},
        {0x75 /*KeyEvent.VK_F6*/,    SWT.F6},
        {0x76 /*KeyEvent.VK_F7*/,    SWT.F7},
        {0x77 /*KeyEvent.VK_F8*/,    SWT.F8},
        {0x78 /*KeyEvent.VK_F9*/,    SWT.F9},
        {0x79 /*KeyEvent.VK_F10*/,   SWT.F10},
        {0x7A /*KeyEvent.VK_F11*/,   SWT.F11},
        {0x7B /*KeyEvent.VK_F12*/,   SWT.F12},
    };

    // RT-27940: map these to Fx keys
//    /* Numeric Keypad Keys */
//    {KeyEvent.VK_MULTIPLY,    SWT.KEYPAD_MULTIPLY},
//    {KeyEvent.VK_ADD,         SWT.KEYPAD_ADD},
//    {KeyEvent.VK_RETURN,      SWT.KEYPAD_CR},
//    {KeyEvent.VK_SUBTRACT,    SWT.KEYPAD_SUBTRACT},
//    {KeyEvent.VK_DECIMAL,     SWT.KEYPAD_DECIMAL},
//    {KeyEvent.VK_DIVIDE,      SWT.KEYPAD_DIVIDE},
//--  {KeyEvent.VK_????,        SWT.KEYPAD_EQUAL},

    static int keyCodeToEmbedKeyCode(int keyCode) {
        for (int i=0; i<KeyTable.length; i++) {
            if (KeyTable [i] [1] == keyCode) return KeyTable [i] [0];
        }
        return 0;
    }

    static int keyModifiersToEmbedKeyModifiers(int extModifiers) {
        int embedModifiers = 0;
        if ((extModifiers & SWT.SHIFT) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
        }
        if ((extModifiers & SWT.CTRL) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
        }
        if ((extModifiers & SWT.ALT) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_ALT;
        }
        // NOTE: can't get Windows key from SWT
        if ((extModifiers & SWT.COMMAND) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_META;
        }
        return embedModifiers;
    }
}
