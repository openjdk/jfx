/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.*;
import javafx.css.ParsedValue;
import javafx.scene.Cursor;
import javafx.scene.text.Font;

import org.junit.Test;

import javafx.css.converter.CursorConverter;


public class CursorTypeTest {

    public CursorTypeTest() {
    }

    /**
     * Test of convert method, of class CursorType.
     */
    @Test
    public void testConvert() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>("hand", CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.HAND;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvert_with_hyphen() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>("open-hand", CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.OPEN_HAND;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvert_with_Cursor_dot() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>("Cursor.open-hand", CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.OPEN_HAND;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvert_with_package_name() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>("javafx.scene.Cursor.open-hand", CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.OPEN_HAND;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvert_with_package_name_only() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>("javafx.scene.Cursor.", CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.DEFAULT;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvert_with_empty_string() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>("", CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.DEFAULT;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvert_with_null() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>(null, CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.DEFAULT;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvert_with_bogus_value() {
        ParsedValue<String,Cursor> value = new ParsedValueImpl<>("bogus", CursorConverter.getInstance());
        Font font = null;
        Cursor expResult = Cursor.DEFAULT;
        Cursor result = value.convert(font);
        assertEquals(expResult, result);
    }

}
