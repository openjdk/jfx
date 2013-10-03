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

package com.sun.javafx.scene.control.behavior;

import java.util.Locale;
import java.util.StringTokenizer;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;

/**
 * <p>
 * Provides support for embedding a mnemonic, accelerator and text in a single
 * string. A mnemonic is a single character that will either give a control
 * focus or activate it when pressed in conjunction with the base platform's
 * mnemonic modifier (typically the {@code Alt} key). An accelerator is a key
 * combination that will activate the object.
 * </p>
 * 
 * <p>
 * The syntax of the string content is as follows:
 * </p>
 * 
 * <ul>
 * <li>
 * <p>
 * <b>Simple mnemonic</b>: the first character preceded by the first {@code _}
 * character will be treated as the mnemonic. For example, "E_xit" will cause
 * the text to become "Exit" and the mnemonic will be "x". This is the most
 * common designation of a mnemonic, and the skin for a control will present the
 * mnemonic in the string with an underline. To prevent {@code _} from being
 * treated as the mnemonic prefix character, repeat it twice in a row. A
 * mnemonic is not required.
 * </p>
 * </li>
 * <li>
 * <p>
 * <b>Extended mnemonic</b>: an optional representation of a mnemonic is
 * {@code _(c)}, where {@code c} is the mnemonic character. For example,
 * "Exit_(q)" will cause the text to become "Exit" and the
 * {@link #extendedMnemonicText} to become "(q)". This is typically provided in
 * translated strings to support mnemonics where the main text does not have any
 * characters that map to keyboard keys. In these cases, the skin for the
 * control will typically present the mnemonic surrounded by parentheses. The
 * skin will also honor the hiding and presentation of the extended mnemonic
 * string on platforms where the mnemonic is only displayed when the mnemonic
 * modifier key is pressed.
 * <li>
 * <p>
 * <b>Accelerator</b>: an accelerator must be defined as the last element in the
 * string. It is delimited from the rest of the string by {@code @}. The
 * {@code @} must be followed by any number of modifier designations delimited
 * by {@code +} then followed by the accelerator character (e.g.,
 * {@code Ctrl+Shift+K}). The legal modifier designations are as follows (case
 * insensitive, and not to be translated): {@code Ctrl}, {@code Alt},
 * {@code Shift} and {@code Shortcut}. If an {@code @} appears 
 * twice in a row in the string, it will be treated as a single {@code @} and 
 * not as the accelerator delimiter.
 * <p></li>
 * </ul>
 * 
 */
public class TextBinding {

    /**
     * the marker symbol used when parsing for mnemonics
     */
    private String MNEMONIC_SYMBOL = "_";

    /**
     * The text with any markup for the mnemonic and accelerator removed.
     */
    private String text = null;

    /**
     * Returns the text with any markup for the mnemonic and accelerator removed
     * 
     * @return the text with any markup for the mnemonic and accelerator removed
     */
    public String getText() {
        return text;
    }

    /**
     * The mnemonic or {@code null} if there is no mnemonic.
     */
    private String mnemonic = null;
    private KeyCombination mnemonicKeyCombination = null;

    /**
     * Returns the mnemonic or {@code null} if there is no
     * mnemonic.
     * 
     * @return the mnemonic or {@code null} if there is no
     *         mnemonic
     */
    public String getMnemonic() {
        return mnemonic;
    }


    /**
     * Returns the mnemonic KeyCombination or {@code null} if there is no
     * mnemonic.
     * 
     * @return the mnemonic KeyCombination or {@code null} if there is no
     *         mnemonic
     */
    public KeyCombination getMnemonicKeyCombination() {
        if (mnemonic != null && mnemonicKeyCombination == null) {
            mnemonicKeyCombination = new MnemonicKeyCombination(mnemonic);
        }
        return mnemonicKeyCombination;
    }

    /**
     * The index of the mnemonic character in the text property or -1 if there
     * is no mnemonic character in the text. This is only non-negative if the
     * simple {@code _c} syntax was used to specify the mnemonic.
     */
    private int mnemonicIndex = -1;

    /**
     * Returns the index of the mnemonic character in the text property or -1 if
     * there is no mnemonic character in the text. This is only non-negative if
     * the simple {@code _c} syntax was used to specify the mnemonic.
     * 
     * @return the index of the mnemonic character in the text property or -1 if
     *         there is no mnemonic character in the text
     */
    public int getMnemonicIndex() {
        return mnemonicIndex;
    }

    /**
     * The extended mnemonic text (if it exists). This is only non-null if the
     * extended mnemonic syntax was used to specify the mnemonic.
     */
    private String extendedMnemonicText = null;

    /**
     * Returns the extended mnemonic text (if it exists). This is only non-null
     * if the extended mnemonic syntax was used to specify the mnemonic.
     * 
     * @return the extended mnemonic text (if it exists) or null
     */
    public String getExtendedMnemonicText() {
        return extendedMnemonicText;
    }

    /**
     * The KeyBinding that describes the accelerator or null if there is no
     * accelerator.
     */
    private KeyBinding accelerator = null;

    /**
     * Returns the KeyBinding that describes the accelerator or null if there is
     * no accelerator
     * 
     * @return the KeyBinding that describes the accelerator or null if there is
     *         no accelerator
     */
    public KeyBinding getAccelerator() {
        return accelerator;
    }

    /**
     * The localized accelerator text for use in presenting in an interface.
     */
    private String acceleratorText = null;

    /**
     * Returns the localized accelerator text for use in presenting in an
     * interface or null if there is no accelerator text.
     * 
     * @return the localized accelerator text for use in presenting in an
     *         interface or null if there is no accelerator text
     */
    public String getAcceleratorText() {
        return acceleratorText;
    }


    /**
     * for accelerator : is the control key needed?
     */ 
    private boolean ctrl = false;
    public boolean getCtrl() {
        return ctrl;
    }

    /**
     * for accelerator : is the shift key needed?
     */ 
    private boolean shift = false;
    public boolean getShift() {
        return shift;
    }

    /**
     * for accelerator : is the control key needed?
     */ 
    private boolean alt = false;
    public boolean getAlt() {
        return alt;
    }
    
    /**
     * for accelerator : is the shortcut key needed?
     */ 
    private boolean shortcut = false;
    public boolean getShortcut() {
        return shortcut;
    }

    /**
     * Creates a new TextBinding instance from the given string.
     * 
     * @param s the action text string
     * 
     * @see getText
     * @see getMnemonic
     * @see getMnemonicIndex
     * @see getExtendedMnemonicText
     * @see getAccelerator
     * @see getAcceleratorText
     */
    public TextBinding(String s) {
        parseAndSplit(s);
    }

    /**
     * Parse and split the given string into the appropriate segments.
     */
    private void parseAndSplit(String s) {
        if (s == null || s.length() == 0) {
            text = s;
            return;
        }

        // We will use temp as a working copy of the string and will pull
        // mnemonic and accelerator text out of it as we find those things.
        //
        StringBuffer temp = new StringBuffer(s);
        
        // Find the accelerator if it exists.
        //
        int index = temp.indexOf("@");
        while (index >= 0 && index < (temp.length() - 1)) {
            if (temp.charAt(index + 1) == '@') {
                temp.delete(index, index + 1); // delete the extra '@'
            } else {
                // Only strip out the accelerator text if we can parse it.
                // [[[TODO: WDW - provide a localized form that concatenates
                // the localized strings for the modifier with the key.]]]
                acceleratorText = temp.substring(index + 1);
                accelerator = parseAcceleratorText(acceleratorText);
                if (accelerator != null) {
                    temp.delete(index, temp.length());
                    break;
                } else {
                    acceleratorText = null;
                }
            }
            index = temp.indexOf("@", index + 1);
        }
        
        // Find the mnemonic if it exists.
        //
        index = temp.indexOf(MNEMONIC_SYMBOL);
        while (index >= 0 && index < (temp.length() - 1)) {
            // Skip two _'s in a row
            if (MNEMONIC_SYMBOL.equals(temp.substring(index + 1, index + 2))) {
                temp.delete(index, index + 1); // delete the extra MNEMONIC_SYMBOL
            } else if (temp.charAt(index + 1) != '('
                       || index == temp.length() - 2) {
                mnemonic = temp.substring(index + 1, index + 2);
                if (mnemonic != null) {
                    mnemonicIndex = index;
                }
                temp.delete(index, index + 1);
                break;
            } else {
                int endIndex = temp.indexOf(")", index + 3);
                if (endIndex == -1) { // "(" is actually the mnemonic
                    mnemonic = temp.substring(index + 1, index + 2);
                    if (mnemonic != null) {
                        mnemonicIndex = index;
                    }
                    temp.delete(index, index + 1);
                    break;
                } else if (endIndex == index + 3) {
                    mnemonic = temp.substring(index + 2, index + 3);
                    extendedMnemonicText = temp.substring(index + 1, index + 4);
                    temp.delete(index, endIndex + 3);
                    break;
                }
            }
            index = temp.indexOf(MNEMONIC_SYMBOL, index + 1);
        }
        text = temp.toString();
    }
    
    /**
     * Returns a KeyBinding for the given accelerator text or {@code null} if
     * the accelerator text cannot be parsed.
     * 
     * @return a KeyBinding for the given accelerator text or {@code null} if
     *         the accelerator text cannot be parsed
     */
    private KeyBinding parseAcceleratorText(String s) {
        KeyBinding result = null;
        String text = null;
        boolean controlDown = false;
        boolean altDown = false;
        boolean shiftDown = false;
        boolean shortcutDown = false;
        boolean parseFail = false;
        StringTokenizer tokenizer = new StringTokenizer(s, "+");
        while (!parseFail && tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!tokenizer.hasMoreTokens()) {
                text = token;
            } else {
                KeyCode code = KeyCode.getKeyCode(token.toUpperCase(Locale.ROOT));
                if (code != null) {
                    switch (code) {
                      case CONTROL:
                        controlDown = true;
                        break;
                      case ALT:
                        altDown = true;
                        break;
                      case SHIFT:
                        shiftDown = true;
                        break;
                      case SHORTCUT:
                        shortcutDown = true;
                        break;
                      default:
                        text = null;
                        parseFail = true;
                        break;
                    }
                } else {
                    text = null;
                    parseFail = true;
                }
            }
        }
        if (text != null) {
            KeyCode code = KeyCode.getKeyCode(text.toUpperCase());
            if (code != null) {
                if (code != KeyCode.UNDEFINED) {
                    result = new KeyBinding(code, null);
                    if (controlDown) {
                        ctrl = true;
                        result.ctrl();
                    }
                    if (altDown) {
                        alt = true;
                        result.alt();
                    }
                    if (shiftDown) {
                        shift = true;
                        result.shift();
                    }
                    if (shortcutDown) {
                        shortcut = true;
                        result.shortcut();
                    }
                }
            }
        }
        return result;
    }
    
    @Override public String toString() {
        return "TextBinding [text=" + getText() + ",mnemonic=" + getMnemonic()
            + ", mnemonicIndex=" + getMnemonicIndex()
            + ", extendedMnemonicText=" + getExtendedMnemonicText()
            + ", accelerator=" + getAccelerator() + ", acceleratorText="
            + getAcceleratorText() + "]";
    }

    /**
     * A modified version of KeyCharacterCombination, which matches
     * on the text property of a KeyEvent instead of on the KeyCode.
     */
    public static class MnemonicKeyCombination extends KeyCombination {
        private String character = "";

        /**
         * Constructs a {@code MnemonicKeyCombination} for the specified main key
         * character.
         *
         * @param character the main key character
         */
        public MnemonicKeyCombination(String character) {
            super(com.sun.javafx.PlatformUtil.isMac()
                                  ? KeyCombination.META_DOWN
                                  : KeyCombination.ALT_DOWN);
            this.character = character;
        }

        /** 
         * Gets the key character associated with this key combination. 
         * @return The key character associated with this key combination
         */
        public final String getCharacter() {
            return character;
        }

        /**
         * Tests whether this key combination matches the key combination in the
         * given {@code KeyEvent}.
         *
         * @param event the key event
         * @return {@code true} if the key combinations match, {@code false}
         *      otherwise
         */
        @Override public boolean match(final KeyEvent event) {
            String text = event.getText();
            return (text != null
                    && !text.isEmpty()
                    && text.equalsIgnoreCase(getCharacter())
                    && super.match(event));
        }

        /**
         * Returns a string representation of this {@code MnemonicKeyCombination}.
         * <p>
         * The string representation consists of sections separated by plus
         * characters. Each section specifies either a modifier key or the main key.
         * <p>
         * A modifier key section contains the {@code KeyCode} name of a modifier
         * key. It can be prefixed with the {@code Ignored} keyword. A non-prefixed
         * modifier key implies its {@code PRESSED} value while the prefixed version
         * implies the {@code IGNORED} value. If some modifier key is not specified
         * in the string at all, it means it has the default {@code RELEASED} value.
         * <p>
         * The main key section contains the main key character enclosed in single
         * quotes and is the last section in the returned string.
         *
         * @return the string representation of this {@code MnemonicKeyCombination}
         */
        @Override public String getName() {
            StringBuilder sb = new StringBuilder();

            sb.append(super.getName());
            if (sb.length() > 0) {
                sb.append("+");
            }

            return sb.append('\'').append(character.replace("'", "\\'"))
                    .append('\'').toString();
        }

        /**
         * Tests whether this {@code MnemonicKeyCombination} equals to the
         * specified object.
         *
         * @param obj the object to compare to
         * @return {@code true} if the objects are equal, {@code false} otherwise
         */
        @Override public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof MnemonicKeyCombination)) {
                return false;
            }

            return (this.character.equals(((MnemonicKeyCombination)obj).getCharacter())
                    && super.equals(obj));
        }

        /**
         * Returns a hash code value for this {@code MnemonicKeyCombination}.
         *
         * @return the hash code value
         */
        @Override public int hashCode() {
            return 23 * super.hashCode() + character.hashCode();
        }
    }
}
