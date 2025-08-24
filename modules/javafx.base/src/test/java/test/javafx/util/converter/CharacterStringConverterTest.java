/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import javafx.util.converter.CharacterStringConverter;

public class CharacterStringConverterTest {

    private final CharacterStringConverter converter = new CharacterStringConverter();

    @Test
    void fromString_testValidStringInput_lowercase() {
        assertEquals('c', converter.fromString("c"));
    }

    @Test
    void fromString_testValidStringInput_uppercase() {
        assertEquals('C', converter.fromString("C"));
    }

    @Test
    void fromString_testValidStringInput_differentCase_one() {
        assertNotSame('C', converter.fromString("c"));
    }

    @Test
    void fromString_testValidStringInput_differentCase_two() {
        assertNotSame('c', converter.fromString("C"));
    }

    @Test
    void fromString_testValidStringInputWithWhiteSpace_lowercase() {
        assertEquals('c', converter.fromString("     c     "));
    }

    @Test
    void fromString_testValidStringInputWithWhiteSpace_uppercase() {
        assertEquals('C', converter.fromString("     C     "));
    }

    @Test
    void toString_lowercase() {
        assertEquals("c", converter.toString('c'));
    }

    @Test
    void toString_uppercase() {
        assertEquals("C", converter.toString('C'));
    }
}
