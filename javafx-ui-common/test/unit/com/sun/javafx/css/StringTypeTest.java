/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.css.converters.StringConverter;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;


public class StringTypeTest {

    public StringTypeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of convert method, of class StringType.
     */
    @Test
    public void testConvert() {
        //System.out.println("convert");
        ParsedValue<String,String> value = new ParsedValueImpl<String,String>("test", StyleConverter.getStringConverter());
        Font font = null;
        String expResult = "test";
        String result = value.convert(font);
        assertEquals(expResult, result);

        ParsedValue<String,String>[] values = new ParsedValue[] {
            new ParsedValueImpl<String,String>("hello", StyleConverter.getStringConverter()),
            new ParsedValueImpl<String,String>("world", StyleConverter.getStringConverter())
        };

        ParsedValue<ParsedValue<String,String>[], String[]> seq =
            new ParsedValueImpl<ParsedValue<String,String>[], String[]>(values, StringConverter.SequenceConverter.getInstance());

        String[] strings = seq.convert(font);
        assertEquals("hello", strings[0]);
        assertEquals("world", strings[1]);

    }

}
