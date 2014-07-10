/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;

/**
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
 * </ul>
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
     * Creates a new TextBinding instance from the given string.
     *
     * @param s the action text string
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

        // Find the mnemonic if it exists.
        //
        int index = temp.indexOf(MNEMONIC_SYMBOL);
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
