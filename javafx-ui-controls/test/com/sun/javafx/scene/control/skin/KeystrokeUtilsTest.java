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

}
