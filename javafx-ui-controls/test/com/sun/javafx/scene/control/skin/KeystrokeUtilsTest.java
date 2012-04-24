/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
