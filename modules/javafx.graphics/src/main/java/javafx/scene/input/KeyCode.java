/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import java.util.HashMap;
import java.util.Map;

/**
 * Set of key codes for {@link KeyEvent} objects.
 * @since JavaFX 2.0
 */
public enum KeyCode {

    /**
     * Constant for the {@code Enter} key.
     */
    ENTER(0x0A, "Enter", KeyCodeClass.WHITESPACE),

    /**
     * Constant for the {@code Backspace} key.
     */
    BACK_SPACE(0x08, "Backspace"),

    /**
     * Constant for the {@code Tab} key.
     */
    TAB(0x09, "Tab", KeyCodeClass.WHITESPACE),

    /**
     * Constant for the {@code Cancel} key.
     */
    CANCEL(0x03, "Cancel"),

    /**
     * Constant for the {@code Clear} key.
     */
    CLEAR(0x0C, "Clear"),

    /**
     * Constant for the {@code Shift} key.
     */
    SHIFT(0x10, "Shift", KeyCodeClass.MODIFIER),

    /**
     * Constant for the {@code Ctrl} key.
     */
    CONTROL(0x11, "Ctrl", KeyCodeClass.MODIFIER),

    /**
     * Constant for the {@code Alt} key.
     */
    ALT(0x12, "Alt", KeyCodeClass.MODIFIER),

    /**
     * Constant for the {@code Pause} key.
     */
    PAUSE(0x13, "Pause"),

    /**
     * Constant for the {@code Caps Lock} key.
     */
    CAPS(0x14, "Caps Lock"),

    /**
     * Constant for the {@code Esc} key.
     */
    ESCAPE(0x1B, "Esc"),

    /**
     * Constant for the {@code Space} key.
     */
    SPACE(0x20, "Space", KeyCodeClass.WHITESPACE),

    /**
     * Constant for the {@code Page Up} key.
     */
    PAGE_UP(0x21, "Page Up", KeyCodeClass.NAVIGATION),

    /**
     * Constant for the {@code Page Down} key.
     */
    PAGE_DOWN(0x22, "Page Down", KeyCodeClass.NAVIGATION),

    /**
     * Constant for the {@code End} key.
     */
    END(0x23, "End", KeyCodeClass.NAVIGATION),

    /**
     * Constant for the {@code Home} key.
     */
    HOME(0x24, "Home", KeyCodeClass.NAVIGATION),

    /**
     * Constant for the non-numpad <b>left</b> arrow key.
     */
    LEFT(0x25, "Left", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /**
     * Constant for the non-numpad <b>up</b> arrow key.
     */
    UP(0x26, "Up", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /**
     * Constant for the non-numpad <b>right</b> arrow key.
     */
    RIGHT(0x27, "Right", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /**
     * Constant for the non-numpad <b>down</b> arrow key.
     */
    DOWN(0x28, "Down", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION),

    /**
     * Constant for the comma key, ","
     */
    COMMA(0x2C, "Comma"),

    /**
     * Constant for the minus key, "-"
     */
    MINUS(0x2D, "Minus"),

    /**
     * Constant for the period key, "."
     */
    PERIOD(0x2E, "Period"),

    /**
     * Constant for the forward slash key, "/"
     */
    SLASH(0x2F, "Slash"),

    /**
     * Constant for the {@code 0} key.
     */
    DIGIT0(0x30, "0", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 1} key.
     */
    DIGIT1(0x31, "1", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 2} key.
     */
    DIGIT2(0x32, "2", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 3} key.
     */
    DIGIT3(0x33, "3", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 4} key.
     */
    DIGIT4(0x34, "4", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 5} key.
     */
    DIGIT5(0x35, "5", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 6} key.
     */
    DIGIT6(0x36, "6", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 7} key.
     */
    DIGIT7(0x37, "7", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 8} key.
     */
    DIGIT8(0x38, "8", KeyCodeClass.DIGIT),

    /**
     * Constant for the {@code 9} key.
     */
    DIGIT9(0x39, "9", KeyCodeClass.DIGIT),

    /**
     * Constant for the semicolon key, ";"
     */
    SEMICOLON(0x3B, "Semicolon"),

    /**
     * Constant for the equals key, "="
     */
    EQUALS(0x3D, "Equals"),

    /**
     * Constant for the {@code A} key.
     */
    A(0x41, "A", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code B} key.
     */
    B(0x42, "B", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code C} key.
     */
    C(0x43, "C", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code D} key.
     */
    D(0x44, "D", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code E} key.
     */
    E(0x45, "E", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code F} key.
     */
    F(0x46, "F", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code G} key.
     */
    G(0x47, "G", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code H} key.
     */
    H(0x48, "H", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code I} key.
     */
    I(0x49, "I", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code J} key.
     */
    J(0x4A, "J", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code K} key.
     */
    K(0x4B, "K", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code L} key.
     */
    L(0x4C, "L", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code M} key.
     */
    M(0x4D, "M", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code N} key.
     */
    N(0x4E, "N", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code O} key.
     */
    O(0x4F, "O", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code P} key.
     */
    P(0x50, "P", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code Q} key.
     */
    Q(0x51, "Q", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code R} key.
     */
    R(0x52, "R", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code S} key.
     */
    S(0x53, "S", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code T} key.
     */
    T(0x54, "T", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code U} key.
     */
    U(0x55, "U", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code V} key.
     */
    V(0x56, "V", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code W} key.
     */
    W(0x57, "W", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code X} key.
     */
    X(0x58, "X", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code Y} key.
     */
    Y(0x59, "Y", KeyCodeClass.LETTER),

    /**
     * Constant for the {@code Z} key.
     */
    Z(0x5A, "Z", KeyCodeClass.LETTER),

    /**
     * Constant for the open bracket key, "["
     */
    OPEN_BRACKET(0x5B, "Open Bracket"),

    /**
     * Constant for the back slash key, "\"
     */
    BACK_SLASH(0x5C, "Back Slash"),

    /**
     * Constant for the close bracket key, "]"
     */
    CLOSE_BRACKET(0x5D, "Close Bracket"),

    /**
     * Constant for the {@code Numpad 0} key.
     */
    NUMPAD0(0x60, "Numpad 0", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 1} key.
     */
    NUMPAD1(0x61, "Numpad 1", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 2} key.
     */
    NUMPAD2(0x62, "Numpad 2", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 3} key.
     */
    NUMPAD3(0x63, "Numpad 3", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 4} key.
     */
    NUMPAD4(0x64, "Numpad 4", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 5} key.
     */
    NUMPAD5(0x65, "Numpad 5", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 6} key.
     */
    NUMPAD6(0x66, "Numpad 6", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 7} key.
     */
    NUMPAD7(0x67, "Numpad 7", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 8} key.
     */
    NUMPAD8(0x68, "Numpad 8", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Numpad 9} key.
     */
    NUMPAD9(0x69, "Numpad 9", KeyCodeClass.DIGIT | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Multiply} key.
     */
    MULTIPLY(0x6A, "Multiply"),

    /**
     * Constant for the {@code Add} key.
     */
    ADD(0x6B, "Add"),

    /**
     * Constant for the Numpad Separator key.
     */
    SEPARATOR(0x6C, "Separator"),

    /**
     * Constant for the {@code Subtract} key.
     */
    SUBTRACT(0x6D, "Subtract"),

    /**
     * Constant for the {@code Decimal} key.
     */
    DECIMAL(0x6E, "Decimal"),

    /**
     * Constant for the {@code Divide} key.
     */
    DIVIDE(0x6F, "Divide"),

    /**
     * Constant for the {@code Delete} key.
     */
    DELETE(0x7F, "Delete"), /* ASCII:Integer   DEL */

    /**
     * Constant for the {@code Num Lock} key.
     */
    NUM_LOCK(0x90, "Num Lock"),

    /**
     * Constant for the {@code Scroll Lock} key.
     */
    SCROLL_LOCK(0x91, "Scroll Lock"),

    /**
     * Constant for the F1 function key.
     */
    F1(0x70, "F1", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F2 function key.
     */
    F2(0x71, "F2", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F3 function key.
     */
    F3(0x72, "F3", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F4 function key.
     */
    F4(0x73, "F4", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F5 function key.
     */
    F5(0x74, "F5", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F6 function key.
     */
    F6(0x75, "F6", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F7 function key.
     */
    F7(0x76, "F7", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F8 function key.
     */
    F8(0x77, "F8", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F9 function key.
     */
    F9(0x78, "F9", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F10 function key.
     */
    F10(0x79, "F10", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F11 function key.
     */
    F11(0x7A, "F11", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F12 function key.
     */
    F12(0x7B, "F12", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F13 function key.
     */
    F13(0xF000, "F13", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F14 function key.
     */
    F14(0xF001, "F14", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F15 function key.
     */
    F15(0xF002, "F15", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F16 function key.
     */
    F16(0xF003, "F16", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F17 function key.
     */
    F17(0xF004, "F17", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F18 function key.
     */
    F18(0xF005, "F18", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F19 function key.
     */
    F19(0xF006, "F19", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F20 function key.
     */
    F20(0xF007, "F20", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F21 function key.
     */
    F21(0xF008, "F21", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F22 function key.
     */
    F22(0xF009, "F22", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F23 function key.
     */
    F23(0xF00A, "F23", KeyCodeClass.FUNCTION),

    /**
     * Constant for the F24 function key.
     */
    F24(0xF00B, "F24", KeyCodeClass.FUNCTION),

    /**
     * Constant for the {@code Print Screen} key.
     */
    PRINTSCREEN(0x9A, "Print Screen"),

    /**
     * Constant for the {@code Insert} key.
     */
    INSERT(0x9B, "Insert"),

    /**
     * Constant for the {@code Help} key.
     */
    HELP(0x9C, "Help"),

    /**
     * Constant for the {@code Meta} key.
     */
    META(0x9D, "Meta", KeyCodeClass.MODIFIER),

    /**
     * Constant for the {@code Back Quote} key.
     */
    BACK_QUOTE(0xC0, "Back Quote"),

    /**
     * Constant for the {@code Quote} key.
     */
    QUOTE(0xDE, "Quote"),

    /**
     * Constant for the numeric keypad <b>up</b> arrow key.
     */
    KP_UP(0xE0, "Numpad Up", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION | KeyCodeClass.KEYPAD),

    /**
     * Constant for the numeric keypad <b>down</b> arrow key.
     */
    KP_DOWN(0xE1, "Numpad Down", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION | KeyCodeClass.KEYPAD),

    /**
     * Constant for the numeric keypad <b>left</b> arrow key.
     */
    KP_LEFT(0xE2, "Numpad Left", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION | KeyCodeClass.KEYPAD),

    /**
     * Constant for the numeric keypad <b>right</b> arrow key.
     */
    KP_RIGHT(0xE3, "Numpad Right", KeyCodeClass.ARROW | KeyCodeClass.NAVIGATION | KeyCodeClass.KEYPAD),

    /**
     * Constant for the {@code Dead Grave} key.
     */
    DEAD_GRAVE(0x80, "Dead Grave"),

    /**
     * Constant for the {@code Dead Acute} key.
     */
    DEAD_ACUTE(0x81, "Dead Acute"),

    /**
     * Constant for the {@code Dead Circumflex} key.
     */
    DEAD_CIRCUMFLEX(0x82, "Dead Circumflex"),

    /**
     * Constant for the {@code Dead Tilde} key.
     */
    DEAD_TILDE(0x83, "Dead Tilde"),

    /**
     * Constant for the {@code Dead Macron} key.
     */
    DEAD_MACRON(0x84, "Dead Macron"),

    /**
     * Constant for the {@code Dead Breve} key.
     */
    DEAD_BREVE(0x85, "Dead Breve"),

    /**
     * Constant for the {@code Dead Abovedot} key.
     */
    DEAD_ABOVEDOT(0x86, "Dead Abovedot"),

    /**
     * Constant for the {@code Dead Diaeresis} key.
     */
    DEAD_DIAERESIS(0x87, "Dead Diaeresis"),

    /**
     * Constant for the {@code Dead Abovering} key.
     */
    DEAD_ABOVERING(0x88, "Dead Abovering"),

    /**
     * Constant for the {@code Dead Doubleacute} key.
     */
    DEAD_DOUBLEACUTE(0x89, "Dead Doubleacute"),

    /**
     * Constant for the {@code Dead Caron} key.
     */
    DEAD_CARON(0x8a, "Dead Caron"),

    /**
     * Constant for the {@code Dead Cedilla} key.
     */
    DEAD_CEDILLA(0x8b, "Dead Cedilla"),

    /**
     * Constant for the {@code Dead Ogonek} key.
     */
    DEAD_OGONEK(0x8c, "Dead Ogonek"),

    /**
     * Constant for the {@code Dead Iota} key.
     */
    DEAD_IOTA(0x8d, "Dead Iota"),

    /**
     * Constant for the {@code Dead Voiced Sound} key.
     */
    DEAD_VOICED_SOUND(0x8e, "Dead Voiced Sound"),

    /**
     * Constant for the {@code Dead Semivoiced Sound} key.
     */
    DEAD_SEMIVOICED_SOUND(0x8f, "Dead Semivoiced Sound"),

    /**
     * Constant for the {@code Ampersand} key.
     */
    AMPERSAND(0x96, "Ampersand"),

    /**
     * Constant for the {@code Asterisk} key.
     */
    ASTERISK(0x97, "Asterisk"),

    /**
     * Constant for the {@code Double Quote} key.
     */
    QUOTEDBL(0x98, "Double Quote"),

    /**
     * Constant for the {@code Less} key.
     */
    LESS(0x99, "Less"),

    /**
     * Constant for the {@code Greater} key.
     */
    GREATER(0xa0, "Greater"),

    /**
     * Constant for the {@code Left Brace} key.
     */
    BRACELEFT(0xa1, "Left Brace"),

    /**
     * Constant for the {@code Right Brace} key.
     */
    BRACERIGHT(0xa2, "Right Brace"),

    /**
     * Constant for the "@" key.
     */
    AT(0x0200, "At"),

    /**
     * Constant for the ":" key.
     */
    COLON(0x0201, "Colon"),

    /**
     * Constant for the "^" key.
     */
    CIRCUMFLEX(0x0202, "Circumflex"),

    /**
     * Constant for the "$" key.
     */
    DOLLAR(0x0203, "Dollar"),

    /**
     * Constant for the Euro currency sign key.
     */
    EURO_SIGN(0x0204, "Euro Sign"),

    /**
     * Constant for the "!" key.
     */
    EXCLAMATION_MARK(0x0205, "Exclamation Mark"),

    /**
     * Constant for the inverted exclamation mark key.
     */
    INVERTED_EXCLAMATION_MARK(0x0206, "Inverted Exclamation Mark"),

    /**
     * Constant for the "(" key.
     */
    LEFT_PARENTHESIS(0x0207, "Left Parenthesis"),

    /**
     * Constant for the "#" key.
     */
    NUMBER_SIGN(0x0208, "Number Sign"),

    /**
     * Constant for the "+" key.
     */
    PLUS(0x0209, "Plus"),

    /**
     * Constant for the ")" key.
     */
    RIGHT_PARENTHESIS(0x020A, "Right Parenthesis"),

    /**
     * Constant for the "_" key.
     */
    UNDERSCORE(0x020B, "Underscore"),

    /**
     * Constant for the Microsoft Windows "Windows" key.
     * It is used for both the left and right version of the key.
     */
    WINDOWS(0x020C, "Windows", KeyCodeClass.MODIFIER),

    /**
     * Constant for the Microsoft Windows Context Menu key.
     */
    CONTEXT_MENU(0x020D, "Context Menu"),

    /**
     * Constant for input method support on Asian Keyboards.
     */
    FINAL(0x0018, "Final"),

    /**
     * Constant for the Convert function key.
     */
    CONVERT(0x001C, "Convert"),

    /**
     * Constant for the Don't Convert function key.
     */
    NONCONVERT(0x001D, "Nonconvert"),

    /**
     * Constant for the Accept or Commit function key.
     */
    ACCEPT(0x001E, "Accept"),

    /**
     * Constant for the {@code Mode Change} key.
     */
    MODECHANGE(0x001F, "Mode Change"),
    /**
     * Constant for the {@code Kana} key.
     */
    KANA(0x0015, "Kana"),
    /**
     * Constant for the {@code Kanji} key.
     */
    KANJI(0x0019, "Kanji"),

    /**
     * Constant for the Alphanumeric function key.
     */
    ALPHANUMERIC(0x00F0, "Alphanumeric"),

    /**
     * Constant for the Katakana function key.
     */
    KATAKANA(0x00F1, "Katakana"),

    /**
     * Constant for the Hiragana function key.
     */
    HIRAGANA(0x00F2, "Hiragana"),

    /**
     * Constant for the Full-Width Characters function key.
     */
    FULL_WIDTH(0x00F3, "Full Width"),

    /**
     * Constant for the Half-Width Characters function key.
     */
    HALF_WIDTH(0x00F4, "Half Width"),

    /**
     * Constant for the Roman Characters function key.
     */
    ROMAN_CHARACTERS(0x00F5, "Roman Characters"),

    /**
     * Constant for the All Candidates function key.
     */
    ALL_CANDIDATES(0x0100, "All Candidates"),

    /**
     * Constant for the Previous Candidate function key.
     */
    PREVIOUS_CANDIDATE(0x0101, "Previous Candidate"),

    /**
     * Constant for the Code Input function key.
     */
    CODE_INPUT(0x0102, "Code Input"),

    /**
     * Constant for the Japanese-Katakana function key.
     * This key switches to a Japanese input method and selects its Katakana input mode.
     */
    JAPANESE_KATAKANA(0x0103, "Japanese Katakana"),

    /**
     * Constant for the Japanese-Hiragana function key.
     * This key switches to a Japanese input method and selects its Hiragana input mode.
     */
    JAPANESE_HIRAGANA(0x0104, "Japanese Hiragana"),

    /**
     * Constant for the Japanese-Roman function key.
     * This key switches to a Japanese input method and selects its Roman-Direct input mode.
     */
    JAPANESE_ROMAN(0x0105, "Japanese Roman"),

    /**
     * Constant for the locking Kana function key.
     * This key locks the keyboard into a Kana layout.
     */
    KANA_LOCK(0x0106, "Kana Lock"),

    /**
     * Constant for the input method on/off key.
     */
    INPUT_METHOD_ON_OFF(0x0107, "Input Method On/Off"),

    /**
     * Constant for the {@code Cut} key.
     */
    CUT(0xFFD1, "Cut"),

    /**
     * Constant for the {@code Copy} key.
     */
    COPY(0xFFCD, "Copy"),

    /**
     * Constant for the {@code Paste} key.
     */
    PASTE(0xFFCF, "Paste"),

    /**
     * Constant for the {@code Undo} key.
     */
    UNDO(0xFFCB, "Undo"),

    /**
     * Constant for the {@code Again} key.
     */
    AGAIN(0xFFC9, "Again"),

    /**
     * Constant for the {@code Find} key.
     */
    FIND(0xFFD0, "Find"),

    /**
     * Constant for the {@code Properties} key.
     */
    PROPS(0xFFCA, "Properties"),

    /**
     * Constant for the {@code Stop} key.
     */
    STOP(0xFFC8, "Stop"),

    /**
     * Constant for the input method on/off key.
     */
    COMPOSE(0xFF20, "Compose"),

    /**
     * Constant for the AltGraph function key.
     */
    ALT_GRAPH(0xFF7E, "Alt Graph", KeyCodeClass.MODIFIER),

    /**
     * Constant for the Begin key.
     */
    BEGIN(0xFF58, "Begin"),

    /**
     * This value is used to indicate that the keyCode is unknown.
     * Key typed events do not have a keyCode value; this value
     * is used instead.
     */
    UNDEFINED(0x0, "Undefined"),


    //--------------------------------------------------------------
    //
    // Mobile and Embedded Specific Key Codes
    //
    //--------------------------------------------------------------

    /**
     * Constant for the {@code Softkey 0} key.
     */
    SOFTKEY_0(0x1000, "Softkey 0"),

    /**
     * Constant for the {@code Softkey 1} key.
     */
    SOFTKEY_1(0x1001, "Softkey 1"),

    /**
     * Constant for the {@code Softkey 2} key.
     */
    SOFTKEY_2(0x1002, "Softkey 2"),

    /**
     * Constant for the {@code Softkey 3} key.
     */
    SOFTKEY_3(0x1003, "Softkey 3"),

    /**
     * Constant for the {@code Softkey 4} key.
     */
    SOFTKEY_4(0x1004, "Softkey 4"),

    /**
     * Constant for the {@code Softkey 5} key.
     */
    SOFTKEY_5(0x1005, "Softkey 5"),

    /**
     * Constant for the {@code Softkey 6} key.
     */
    SOFTKEY_6(0x1006, "Softkey 6"),

    /**
     * Constant for the {@code Softkey 7} key.
     */
    SOFTKEY_7(0x1007, "Softkey 7"),

    /**
     * Constant for the {@code Softkey 8} key.
     */
    SOFTKEY_8(0x1008, "Softkey 8"),

    /**
     * Constant for the {@code Softkey 9} key.
     */
    SOFTKEY_9(0x1009, "Softkey 9"),

    /**
     * Constant for the {@code Game A} key.
     */
    GAME_A(0x100A, "Game A"),

    /**
     * Constant for the {@code Game B} key.
     */
    GAME_B(0x100B, "Game B"),

    /**
     * Constant for the {@code Game C} key.
     */
    GAME_C(0x100C, "Game C"),

    /**
     * Constant for the {@code Game D} key.
     */
    GAME_D(0x100D, "Game D"),

    /**
     * Constant for the {@code Star} key.
     */
    STAR(0x100E, "Star"),

    /**
     * Constant for the {@code Pound} key.
     */
    POUND(0x100F, "Pound"),

    /**
     * Constant for the {@code Power} key.
     */
    POWER(0x199, "Power"),

    /**
     * Constant for the {@code Info} key.
     */
    INFO(0x1C9, "Info"),

    /**
     * Constant for the {@code Colored Key 0} key.
     */
    COLORED_KEY_0(0x193, "Colored Key 0"),

    /**
     * Constant for the {@code Colored Key 1} key.
     */
    COLORED_KEY_1(0x194, "Colored Key 1"),

    /**
     * Constant for the {@code Colored Key 2} key.
     */
    COLORED_KEY_2(0x195, "Colored Key 2"),

    /**
     * Constant for the {@code Colored Key 3} key.
     */
    COLORED_KEY_3(0x196, "Colored Key 3"),

    /**
     * Constant for the {@code Eject} key.
     */
    EJECT_TOGGLE(0x19E, "Eject", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Play} key.
     */
    PLAY(0x19F, "Play", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Record} key.
     */
    RECORD(0x1A0, "Record", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Fast Forward} key.
     */
    FAST_FWD(0x1A1, "Fast Forward", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Rewind} key.
     */
    REWIND(0x19C, "Rewind", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Previous Track} key.
     */
    TRACK_PREV(0x1A8, "Previous Track", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Next Track} key.
     */
    TRACK_NEXT(0x1A9, "Next Track", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Channel Up} key.
     */
    CHANNEL_UP(0x1AB, "Channel Up", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Channel Down} key.
     */
    CHANNEL_DOWN(0x1AC, "Channel Down", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Volume Up} key.
     */
    VOLUME_UP(0x1bf, "Volume Up", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Volume Down} key.
     */
    VOLUME_DOWN(0x1C0, "Volume Down", KeyCodeClass.MEDIA),

    /**
     * Constant for the {@code Mute} key.
     */
    MUTE(0x1C1, "Mute", KeyCodeClass.MEDIA),

    /**
     * Constant for the Apple {@code Command} key.
     * @since JavaFX 2.1
     */
    COMMAND(0x300, "Command", KeyCodeClass.MODIFIER),

    /**
     * Constant for the {@code Shortcut} key.
     */
    SHORTCUT(-1, "Shortcut");

    final int code;
    final String ch;
    final String name;
    private int mask;

    // Need to bundle this in another class to avoid "forward reference" compiler error
    private static class KeyCodeClass {
        private KeyCodeClass() {};

        private static final int FUNCTION = 1;
        private static final int NAVIGATION = 1 << 1;
        private static final int ARROW = 1 << 2;
        private static final int MODIFIER = 1 << 3;
        private static final int LETTER = 1 << 4;
        private static final int DIGIT = 1 << 5;
        private static final int KEYPAD = 1 << 6;
        private static final int WHITESPACE = 1 << 7;
        private static final int MEDIA = 1 << 8;
    }

    private KeyCode(int code, String name, int mask) {
        this.code = code;
        this.name = name;
        this.mask = mask;
        // ch = new String(Character.toChars(code));
        ch = String.valueOf((char)code);
    }

    private KeyCode(int code, String name) {
        this(code, name, 0);
    }

    /**
     * Function keys like F1, F2, etc...
     * @return true if this key code corresponds to a functional key
     * @since JavaFX 2.2
     */
    public final boolean isFunctionKey() {
        return (mask & KeyCodeClass.FUNCTION) != 0;
    }

    /**
     * Navigation keys are arrow keys and Page Down, Page Up, Home, End
     * (including keypad keys)
     * @return true if this key code corresponds to a navigation key
     * @since JavaFX 2.2
     */
    public final boolean isNavigationKey() {
        return (mask & KeyCodeClass.NAVIGATION) != 0;
    }

    /**
     * Left, right, up, down keys (including the keypad arrows)
     * @return true if this key code corresponds to an arrow key
     * @since JavaFX 2.2
     */
    public final boolean isArrowKey() {
        return (mask & KeyCodeClass.ARROW) != 0;
    }

    /**
     * Keys that could act as a modifier
     * @return true if this key code corresponds to a modifier key
     * @since JavaFX 2.2
     */
    public final boolean isModifierKey() {
        return (mask & KeyCodeClass.MODIFIER) != 0;
    }

    /**
     * All keys with letters
     * @return true if this key code corresponds to a letter key
     * @since JavaFX 2.2
     */
    public final boolean isLetterKey() {
        return (mask & KeyCodeClass.LETTER) != 0;
    }

    /**
     * All Digit keys (including the keypad digits)
     * @return true if this key code corresponds to a digit key
     * @since JavaFX 2.2
     */
    public final boolean isDigitKey() {
        return (mask & KeyCodeClass.DIGIT) != 0;
    }

    /**
     * All keys on the keypad
     * @return true if this key code corresponds to a keypad key
     * @since JavaFX 2.2
     */
    public final boolean isKeypadKey() {
        return (mask & KeyCodeClass.KEYPAD) != 0;
    }

    /**
     * Space, tab and enter
     * @return true if this key code corresponds to a whitespace key
     * @since JavaFX 2.2
     */
    public final boolean isWhitespaceKey() {
        return (mask & KeyCodeClass.WHITESPACE) != 0;
    }

    /**
     * All multimedia keys (channel up/down, volume control, etc...)
     * @return true if this key code corresponds to a media key
     * @since JavaFX 2.2
     */
    public final boolean isMediaKey() {
        return (mask & KeyCodeClass.MEDIA) != 0;
    }

    /**
     * Gets name of this key code.
     * @return Name of this key code
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the character element of this key code, which is simply a mapping of the underlying platform code
     * returned by {@link #getCode()}.
     *
     * @return the character element of this key code
     * @since 9
     */
    public final String getChar() {
        return ch;
    }

    /**
     * Returns the underlying platform code used to represent the {@link #getChar() character} in the key code.
     *
     * @return the underlying platform code used to represent the {@link #getChar() character} in the key code
     * @since 9
     */
    public final int getCode() {
        return code;
    }


    private static final Map<String, KeyCode> nameMap;
    static {

        nameMap = new HashMap<String, KeyCode>(KeyCode.values().length);
        for (KeyCode c : KeyCode.values()) {
            nameMap.put(c.name, c);
        }
    }

    /**
     * Parses textual representation of a key.
     * @param name Textual representation of the key
     * @return KeyCode for the key with the given name, null if the string
     *                 is unknown
     */
    public static KeyCode getKeyCode(String name) {
        return nameMap.get(name);
    }

}
