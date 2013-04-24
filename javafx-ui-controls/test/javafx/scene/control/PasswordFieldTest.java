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

package javafx.scene.control;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
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
