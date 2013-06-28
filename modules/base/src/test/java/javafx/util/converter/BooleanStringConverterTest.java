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

package javafx.util.converter;

import java.math.BigInteger;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class BooleanStringConverterTest {
    private BooleanStringConverter converter;
    
    @Before public void setup() {
        converter = new BooleanStringConverter();
    }
    
    @Test public void fromString_testValidStringInput_lowercase_true() {
        assertEquals(Boolean.TRUE, converter.fromString("true"));
    }
    
    @Test public void fromString_testValidStringInput_uppercase_true() {
        assertEquals(Boolean.TRUE, converter.fromString("TRUE"));
    }
    
    @Test public void fromString_testValidStringInput_mixedCase_true() {
        assertEquals(Boolean.TRUE, converter.fromString("tRUe"));
    }
    
    @Test public void fromString_testValidStringInput_lowercase_false() {
        assertEquals(Boolean.FALSE, converter.fromString("false"));
    }
    
    @Test public void fromString_testValidStringInput_uppercase_false() {
        assertEquals(Boolean.FALSE, converter.fromString("FALSE"));
    }
    
    @Test public void fromString_testValidStringInput_mixedCase_false() {
        assertEquals(Boolean.FALSE, converter.fromString("fALsE"));
    }
    
    @Test public void fromString_testValidStringInputWithWhiteSpace_true() {
        assertEquals(Boolean.TRUE, converter.fromString("      true      "));
    }
    
    @Test public void fromString_testValidStringInputWithWhiteSpace_false() {
        assertEquals(Boolean.FALSE, converter.fromString("     false      "));
    }
    
    @Test public void toString_true() {
        assertEquals("true", converter.toString(true));
    }
    
    @Test public void toString_false() {
        assertEquals("false", converter.toString(false));
    }
}
