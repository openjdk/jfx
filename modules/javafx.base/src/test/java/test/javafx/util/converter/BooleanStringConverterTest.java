/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.util.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import javafx.util.converter.BooleanStringConverter;

public class BooleanStringConverterTest {

    private static final BooleanStringConverter CONVERTER = new BooleanStringConverter();

    @Test
    void fromString_testValidStringInput_lowercase_true() {
        assertEquals(Boolean.TRUE, CONVERTER.fromString("true"));
    }

    @Test
    void fromString_testValidStringInput_uppercase_true() {
        assertEquals(Boolean.TRUE, CONVERTER.fromString("TRUE"));
    }

    @Test
    void fromString_testValidStringInput_mixedCase_true() {
        assertEquals(Boolean.TRUE, CONVERTER.fromString("tRUe"));
    }

    @Test
    void fromString_testValidStringInput_lowercase_false() {
        assertEquals(Boolean.FALSE, CONVERTER.fromString("false"));
    }

    @Test
    void fromString_testValidStringInput_uppercase_false() {
        assertEquals(Boolean.FALSE, CONVERTER.fromString("FALSE"));
    }

    @Test
    void fromString_testValidStringInput_mixedCase_false() {
        assertEquals(Boolean.FALSE, CONVERTER.fromString("fALsE"));
    }

    @Test
    void fromString_testValidStringInputWithWhiteSpace_true() {
        assertEquals(Boolean.TRUE, CONVERTER.fromString("      true      "));
    }

    @Test
    void fromString_testValidStringInputWithWhiteSpace_false() {
        assertEquals(Boolean.FALSE, CONVERTER.fromString("     false      "));
    }

    @Test
    void toString_true() {
        assertEquals("true", CONVERTER.toString(true));
    }

    @Test
    void toString_false() {
        assertEquals("false", CONVERTER.toString(false));
    }
}
