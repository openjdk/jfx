/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertTrue;

import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * @author mickf
 */
public class KeystrokeUtilsTest {

    /*
    ** check that a KeyCombination constructed with a KeyCodeCombination
    ** and one constucted with a KeyCharacterCombination will have
    ** the same text if they are for the same key.
    */
    @Test public void SameDisplayStringKeyCombinationForCharOrCode() {

        KeyCodeCombination acceleratorKeyComboACode =
                new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);

        KeyCharacterCombination acceleratorKeyComboAChar =
                new KeyCharacterCombination("A", KeyCombination.CONTROL_DOWN);

    
        KeyCombination kcACode = acceleratorKeyComboACode;
        KeyCombination kcAChar = acceleratorKeyComboAChar;
        
        String codeAString = KeystrokeUtils.toString(kcACode);
        String charAString = KeystrokeUtils.toString(kcAChar);

        assertTrue(codeAString.equals(charAString));
    }


    /*
    ** check that an accelerator constructed with a Shortcut
    ** displays appropriate platform text.
    */
    @Test public void checkShortcutModifierChangesDisplayString() {
        KeyCombination acceleratorShortcutA = KeyCodeCombination.keyCombination("Shortcut+A");
        String shortcutAString = KeystrokeUtils.toString(acceleratorShortcutA);

        if (com.sun.javafx.PlatformUtil.isMac()) {
            KeyCodeCombination acceleratorMetaA =
                new KeyCodeCombination(KeyCode.A, KeyCombination.META_DOWN);
            String metaAString = KeystrokeUtils.toString(acceleratorMetaA);

            assertTrue(shortcutAString.equals(metaAString));
        }
        else {
            KeyCodeCombination acceleratorControlA =
                new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);
            String controlAString = KeystrokeUtils.toString(acceleratorControlA);

            assertTrue(shortcutAString.equals(controlAString));
        }
    }


    /*
    ** check 
    */
    @Test public void validStringForNonKeyCode() {

        KeyCharacterCombination acceleratorKeyCombo =
            new KeyCharacterCombination("[");

        String comboString = KeystrokeUtils.toString(acceleratorKeyCombo);
        
        assertTrue(comboString.equals("["));
    }


    /*
    ** check that the KeyCodeCombination for KeyCode.DELETE produces something printable.
    ** We only display the unicode DELETE char on mac, otherwise we use "Delete".
    */
    @Test public void validStringForDELETE() {

        KeyCodeCombination keyComboDELETE = new KeyCodeCombination(KeyCode.DELETE);

        String shortcutString = KeyCodeUtils.getAccelerator(keyComboDELETE.getCode());

        if (com.sun.javafx.PlatformUtil.isMac()) {
            assertTrue(shortcutString.equals("\u2326"));
        }
        else {
            assertTrue(shortcutString.equals("Delete"));
        }
    }


}
