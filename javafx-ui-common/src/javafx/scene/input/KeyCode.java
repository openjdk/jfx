/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
public enum KeyCode {

    /**
     * Constant for the {@code Enter} key.
     */
    ENTER(0x0A, "Enter"),

    /**
     * Constant for the {@code Backspace} key.
     */
    BACK_SPACE(0x08, "Backspace"),

    /**
     * Constant for the {@code Tab} key.
     */
    TAB(0x09, "Tab"),

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
    SHIFT(0x10, "Shift"),

    /**
     * Constant for the {@code Ctrl} key.
     */
    CONTROL(0x11, "Ctrl"),

    /**
     * Constant for the {@code Alt} key.
     */
    ALT(0x12, "Alt"),

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
    SPACE(0x20, "Space"),

    /**
     * Constant for the {@code Page Up} key.
     */
    PAGE_UP(0x21, "Page Up"),

    /**
     * Constant for the {@code Page Down} key.
     */
    PAGE_DOWN(0x22, "Page Down"),

    /**
     * Constant for the {@code End} key.
     */
    END(0x23, "End"),

    /**
     * Constant for the {@code Home} key.
     */
    HOME(0x24, "Home"),

    /**
     * Constant for the non-numpad <b>left</b> arrow key.
     */
    LEFT(0x25, "Left"),

    /**
     * Constant for the non-numpad <b>up</b> arrow key.
     */
    UP(0x26, "Up"),

    /**
     * Constant for the non-numpad <b>right</b> arrow key.
     */
    RIGHT(0x27, "Right"),

    /**
     * Constant for the non-numpad <b>down</b> arrow key.
     */
    DOWN(0x28, "Down"),

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
    DIGIT0(0x30, "0"),

    /**
     * Constant for the {@code 1} key.
     */
    DIGIT1(0x31, "1"),

    /**
     * Constant for the {@code 2} key.
     */
    DIGIT2(0x32, "2"),

    /**
     * Constant for the {@code 3} key.
     */
    DIGIT3(0x33, "3"),

    /**
     * Constant for the {@code 4} key.
     */
    DIGIT4(0x34, "4"),

    /**
     * Constant for the {@code 5} key.
     */
    DIGIT5(0x35, "5"),

    /**
     * Constant for the {@code 6} key.
     */
    DIGIT6(0x36, "6"),

    /**
     * Constant for the {@code 7} key.
     */
    DIGIT7(0x37, "7"),

    /**
     * Constant for the {@code 8} key.
     */
    DIGIT8(0x38, "8"),

    /**
     * Constant for the {@code 9} key.
     */
    DIGIT9(0x39, "9"),

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
    A(0x41, "A"),

    /**
     * Constant for the {@code B} key.
     */
    B(0x42, "B"),

    /**
     * Constant for the {@code C} key.
     */
    C(0x43, "C"),

    /**
     * Constant for the {@code D} key.
     */
    D(0x44, "D"),

    /**
     * Constant for the {@code E} key.
     */
    E(0x45, "E"),

    /**
     * Constant for the {@code F} key.
     */
    F(0x46, "F"),

    /**
     * Constant for the {@code G} key.
     */
    G(0x47, "G"),

    /**
     * Constant for the {@code H} key.
     */
    H(0x48, "H"),

    /**
     * Constant for the {@code I} key.
     */
    I(0x49, "I"),

    /**
     * Constant for the {@code J} key.
     */
    J(0x4A, "J"),

    /**
     * Constant for the {@code K} key.
     */
    K(0x4B, "K"),

    /**
     * Constant for the {@code L} key.
     */
    L(0x4C, "L"),

    /**
     * Constant for the {@code M} key.
     */
    M(0x4D, "M"),

    /**
     * Constant for the {@code N} key.
     */
    N(0x4E, "N"),

    /**
     * Constant for the {@code O} key.
     */
    O(0x4F, "O"),

    /**
     * Constant for the {@code P} key.
     */
    P(0x50, "P"),

    /**
     * Constant for the {@code Q} key.
     */
    Q(0x51, "Q"),

    /**
     * Constant for the {@code R} key.
     */
    R(0x52, "R"),

    /**
     * Constant for the {@code S} key.
     */
    S(0x53, "S"),

    /**
     * Constant for the {@code T} key.
     */
    T(0x54, "T"),

    /**
     * Constant for the {@code U} key.
     */
    U(0x55, "U"),

    /**
     * Constant for the {@code V} key.
     */
    V(0x56, "V"),

    /**
     * Constant for the {@code W} key.
     */
    W(0x57, "W"),

    /**
     * Constant for the {@code X} key.
     */
    X(0x58, "X"),

    /**
     * Constant for the {@code Y} key.
     */
    Y(0x59, "Y"),

    /**
     * Constant for the {@code Z} key.
     */
    Z(0x5A, "Z"),

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
    NUMPAD0(0x60, "Numpad 0"),

    /**
     * Constant for the {@code Numpad 1} key.
     */
    NUMPAD1(0x61, "Numpad 1"),

    /**
     * Constant for the {@code Numpad 2} key.
     */
    NUMPAD2(0x62, "Numpad 2"),

    /**
     * Constant for the {@code Numpad 3} key.
     */
    NUMPAD3(0x63, "Numpad 3"),

    /**
     * Constant for the {@code Numpad 4} key.
     */
    NUMPAD4(0x64, "Numpad 4"),

    /**
     * Constant for the {@code Numpad 5} key.
     */
    NUMPAD5(0x65, "Numpad 5"),

    /**
     * Constant for the {@code Numpad 6} key.
     */
    NUMPAD6(0x66, "Numpad 6"),

    /**
     * Constant for the {@code Numpad 7} key.
     */
    NUMPAD7(0x67, "Numpad 7"),

    /**
     * Constant for the {@code Numpad 8} key.
     */
    NUMPAD8(0x68, "Numpad 8"),

    /**
     * Constant for the {@code Numpad 9} key.
     */
    NUMPAD9(0x69, "Numpad 9"),

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
    F1(0x70, "F1"),

    /**
     * Constant for the F2 function key.
     */
    F2(0x71, "F2"),

    /**
     * Constant for the F3 function key.
     */
    F3(0x72, "F3"),

    /**
     * Constant for the F4 function key.
     */
    F4(0x73, "F4"),

    /**
     * Constant for the F5 function key.
     */
    F5(0x74, "F5"),

    /**
     * Constant for the F6 function key.
     */
    F6(0x75, "F6"),

    /**
     * Constant for the F7 function key.
     */
    F7(0x76, "F7"),

    /**
     * Constant for the F8 function key.
     */
    F8(0x77, "F8"),

    /**
     * Constant for the F9 function key.
     */
    F9(0x78, "F9"),

    /**
     * Constant for the F10 function key.
     */
    F10(0x79, "F10"),

    /**
     * Constant for the F11 function key.
     */
    F11(0x7A, "F11"),

    /**
     * Constant for the F12 function key.
     */
    F12(0x7B, "F12"),

    /**
     * Constant for the F13 function key.
     */
    F13(0xF000, "F13"),

    /**
     * Constant for the F14 function key.
     */
    F14(0xF001, "F14"),

    /**
     * Constant for the F15 function key.
     */
    F15(0xF002, "F15"),

    /**
     * Constant for the F16 function key.
     */
    F16(0xF003, "F16"),

    /**
     * Constant for the F17 function key.
     */
    F17(0xF004, "F17"),

    /**
     * Constant for the F18 function key.
     */
    F18(0xF005, "F18"),

    /**
     * Constant for the F19 function key.
     */
    F19(0xF006, "F19"),

    /**
     * Constant for the F20 function key.
     */
    F20(0xF007, "F20"),

    /**
     * Constant for the F21 function key.
     */
    F21(0xF008, "F21"),

    /**
     * Constant for the F22 function key.
     */
    F22(0xF009, "F22"),

    /**
     * Constant for the F23 function key.
     */
    F23(0xF00A, "F23"),

    /**
     * Constant for the F24 function key.
     */
    F24(0xF00B, "F24"),

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
    META(0x9D, "Meta"),

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
    KP_UP(0xE0, "Numpad Up"),

    /**
     * Constant for the numeric keypad <b>down</b> arrow key.
     */
    KP_DOWN(0xE1, "Numpad Down"),

    /**
     * Constant for the numeric keypad <b>left</b> arrow key.
     */
    KP_LEFT(0xE2, "Numpad Left"),

    /**
     * Constant for the numeric keypad <b>right</b> arrow key.
     */
    KP_RIGHT(0xE3, "Numpad Right"),

    /**
     * Constant for the {@code Dead Grave} key.
     */
    DEAD_GRAVE(0x80, "Dead Grave"),

    /**
     * Constant for the {@code Dead Acute} key.
     */
    DEAD_ACUTE(0x81, "Dead Acute"),

    /**
     * Constant for the {@code Circumflex} key.
     */
    DEAD_CIRCUMFLEX(0x82, "Circumflex"),

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
    WINDOWS(0x020C, "Windows"),

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
    ALT_GRAPH(0xFF7E, "Alt Graph"),

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
     * Set of TV Specific Key Codes
     *
     * @since JavaFX 1.3
     */

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
    EJECT_TOGGLE(0x19E, "Eject"),

    /**
     * Constant for the {@code Play} key.
     */
    PLAY(0x19F, "Play"),

    /**
     * Constant for the {@code Record} key.
     */
    RECORD(0x1A0, "Record"),

    /**
     * Constant for the {@code Fast Forward} key.
     */
    FAST_FWD(0x1A1, "Fast Forward"),

    /**
     * Constant for the {@code Rewind} key.
     */
    REWIND(0x19C, "Rewind"),

    /**
     * Constant for the {@code Previous Track} key.
     */
    TRACK_PREV(0x1A8, "Previous Track"),

    /**
     * Constant for the {@code Next Track} key.
     */
    TRACK_NEXT(0x1A9, "Next Track"),

    /**
     * Constant for the {@code Channel Up} key.
     */
    CHANNEL_UP(0x1AB, "Channel Up"),

    /**
     * Constant for the {@code Channel Down} key.
     */
    CHANNEL_DOWN(0x1AC, "Channel Down"),

    /**
     * Constant for the {@code Volume Up} key.
     */
    VOLUME_UP(0x1bf, "Volume Up"),

    /**
     * Constant for the {@code Volume Down} key.
     */
    VOLUME_DOWN(0x1C0, "Volume Down"),

    /**
     * Constant for the {@code Mute} key.
     */
    MUTE(0x1C1, "Mute"),

    /**
     * Constant for the Apple {@code Command} key. 
     */
    COMMAND(0x300, "Command"),
    
    /**
     * Constant for the {@code Shortcut} key.
     */
    SHORTCUT(-1, "Shortcut");

    final int code;
    final String ch;
    final String name;

    private KeyCode(int code, String name) {
        this.code = code;
        this.name = name;
        // ch = new String(Character.toChars(code));
        ch = String.valueOf((char)code);
    }

    /**
     * Gets name of this key code.
     * @return Name of this key code
     */
    public final String getName() {
        return name;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public String impl_getChar() {
        return ch;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public int impl_getCode() {
        return code;
    }

    private static final Map<Integer, KeyCode> charMap;
    private static final Map<String, KeyCode> nameMap;
    static {
        charMap = new HashMap<Integer, KeyCode>(KeyCode.values().length);
        nameMap = new HashMap<String, KeyCode>(KeyCode.values().length);
        for (KeyCode c : KeyCode.values()) {
            charMap.put(c.code, c);
            nameMap.put(c.name, c);
        }
    }

    /**
     * Returns KeyCode object for the given numeric code
     * @param code Numeric code of the key
     * @return KeyCode object for the given numeric code, null if no such key
     *                 code exists
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    static KeyCode impl_valueOf(int code) {
        return charMap.get(code);
    }

    /**
     * Parses textual representation of a key.
     * @param name Textual representation of the key
     * @return KeyCode for the key with the given name, null if the string
     *                 is unknown.
     */
    public static KeyCode getKeyCode(String name) {
        return nameMap.get(name);
    }

}
