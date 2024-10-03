/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css;

import com.sun.javafx.css.ParsedValueImpl;
import javafx.css.ParsedValue;
import javafx.css.SizeUnits;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import javafx.css.converter.EnumConverter;


public class EnumTypeTest {

    public EnumTypeTest() {
    }

    /**
     * Test of convert method, of class EnumType.
     */
    @Test
    public void testConvert() {
        StyleConverter sizeUnitsType = new EnumConverter(SizeUnits.class);
        ParsedValue<String,Enum> value =
                new ParsedValueImpl<String,Enum>("percent", sizeUnitsType);
        Font font = null;
        Enum expResult = SizeUnits.PERCENT;
        Enum result = value.convert(font);
        assertEquals(expResult, result);

        value = new ParsedValueImpl<String,Enum>("SizeUnits.PERCENT", sizeUnitsType);
        result = value.convert(font);
        assertEquals(expResult, result);

        try {
            value = new ParsedValueImpl<String,Enum>("fubar", sizeUnitsType);
            result = value.convert(font);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }

    }

}
