/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.skin;

import javafx.scene.input.KeyCode;

/**
 * Utility functions related to KeyCodes
 */
public class KeyCodeUtils {
    /**
     * Return the accelerator string associated with a KeyCode.
     * (This doesn't include the modifier keys you would find in a
     * Keystroke - for those, you would call {@link KeystrokeUtils#toString}
     * which incidentally uses this method to compute the key portion
     * of the accelerator string.
     *
     * @param code The keycode to compute a menu accelerator string for
     * @return The accelerator string, typically a single character (quite possibly
     * *  a unicode character).
     */
    public static String getAccelerator(KeyCode code) {
        char c = getSingleChar(code);
        if (c != 0) {
            return String.valueOf(c);
        }

        // Compute a name based on the enum name, e.g. F13 becomes F13 and
        // NUM_LOCK becomes Num Lock

        String name = code.toString();

        // We only want the first letter to be upper-case, so we convert 'ENTER'
        // to 'Enter' -- and we also convert multiple words separated by _
        // such that each individual word is capitalized and the underline replaced
        // by spaces.
        StringBuilder sb = new StringBuilder();
        String[] words = com.sun.javafx.Utils.split(name, "_");
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.charAt(0));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /** Compute a single suitable char summarizing the code, if any, and 0 otherwise. */
    private static char getSingleChar(KeyCode code) {
        switch (code) {
            case ENTER: return '\u21B5';
            case LEFT: return '\u2190';
            case UP: return '\u2191';
            case RIGHT: return '\u2192';
            case DOWN: return '\u2193';
            case COMMA: return ',';
            case MINUS: return '-';
            case PERIOD: return '.';
            case SLASH: return '/';
            case SEMICOLON: return ';';
            case EQUALS: return '=';
            case OPEN_BRACKET: return '[';
            case BACK_SLASH: return '\\';
            case CLOSE_BRACKET: return ']';
            case MULTIPLY: return '*';
            case ADD: return '+';
            case SUBTRACT: return '-';
            case DECIMAL: return '.';
            case DIVIDE: return '/';
            case BACK_QUOTE: return '`';
            case QUOTE: return '"';
            case AMPERSAND: return '&';
            case ASTERISK: return '*';
            case LESS: return '<';
            case GREATER: return '>';
            case BRACELEFT: return '{';
            case BRACERIGHT: return '}';
            case AT: return '@';
            case COLON: return ':';
            case CIRCUMFLEX: return '^';
            case DOLLAR: return '$';
            case EURO_SIGN: return '\u20AC';
            case EXCLAMATION_MARK: return '!';
            case LEFT_PARENTHESIS: return '(';
            case NUMBER_SIGN: return '#';
            case PLUS: return '+';
            case RIGHT_PARENTHESIS: return ')';
            case UNDERSCORE: return '_';
            case DIGIT0: return '0';
            case DIGIT1: return '1';
            case DIGIT2: return '2';
            case DIGIT3: return '3';
            case DIGIT4: return '4';
            case DIGIT5: return '5';
            case DIGIT6: return '6';
            case DIGIT7: return '7';
            case DIGIT8: return '8';
            case DIGIT9: return '9';
        default:
            break;
        }

        /*
        ** On Mac we display these unicode symbols,
        ** otherwise we default to the Text version of the char.
        */
        if (com.sun.javafx.PlatformUtil.isMac()) {
            switch (code) {
                case BACK_SPACE: return '\u232B';
                case ESCAPE: return '\u238B';
                case DELETE: return '\u2326';
            default:
                break;
            }
        }
        return 0;
    }
}
