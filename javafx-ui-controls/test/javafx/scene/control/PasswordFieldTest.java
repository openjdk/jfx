/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import static javafx.scene.control.ControlTestUtils.*;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class PasswordFieldTest {
    private PasswordField pwdField;//Empty string
    
    @Before public void setup() {
        pwdField = new PasswordField();
    }
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldHaveEmptyString() {
        assertEquals("", pwdField.getText());
    }
    
    /*********************************************************************
     * Tests for overriden cut, copy                                         *
     ********************************************************************/
    
    @Test public void checkCut() {
        pwdField.setText("sample");
        pwdField.selectRange(0, pwdField.getLength());
        pwdField.cut();
        assertNotNull(pwdField.getText());
        assertTrue(pwdField.getLength() != 0);//Because we cut the entire text still its not valid for PasswordField
        assertEquals(pwdField.getText().toString(), pwdField.getContent().get(0, pwdField.getLength()));
    }
    
    @Test public void checkCopy() {
        pwdField.setText("sample");
        pwdField.selectRange(0, pwdField.getLength());
        pwdField.copy();
        assertNotNull(pwdField.getText());
        assertTrue(pwdField.getLength() != 0);//Because we cut the entire text still its not valid for PasswordField
        assertEquals(pwdField.getText().toString(), pwdField.getContent().get(0, pwdField.getLength()));
    }

    
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_passwordfield() {
        assertStyleClassContains(pwdField, "password-field");
    }
    
    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/

    @Test public void lengthMatchesStringLengthExcludingControlCharacters() {
        final String string = "Hello\n";
        pwdField.setText(string);
        assertEquals(string.length()-1, pwdField.getLength());
    }
}
