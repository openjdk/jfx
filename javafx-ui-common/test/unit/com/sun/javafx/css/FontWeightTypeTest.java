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

import static org.junit.Assert.assertEquals;
import javafx.css.ParsedValue;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.junit.Test;

import com.sun.javafx.css.FontUnits.Weight;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.parser.CSSParser;

public class FontWeightTypeTest {

    public FontWeightTypeTest() {
    }

    /**
     * Test of convert method, of class FontWeightType.
     */
    @Test
    public void testConvert() {
        ParsedValue<Weight,FontWeight> value =
                new ParsedValueImpl<Weight,FontWeight>(Weight.BOLD, FontConverter.FontWeightConverter.getInstance());
        Font font = null;
        FontWeight expResult = FontWeight.BOLD;
        FontWeight result = value.convert(font);
        assertEquals(expResult, result);

        value = new ParsedValueImpl<Weight,FontWeight>(Weight.NORMAL, FontConverter.FontWeightConverter.getInstance());
        expResult = FontWeight.NORMAL;
        result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void test_RT_l7607() {
	ParsedValue parsedValue = CSSParser.getInstance().parseExpr("-fx-font-weight", "600");
	FontWeight expected = FontWeight.SEMI_BOLD;
	FontWeight result = (FontWeight)parsedValue.convert(null);
        assertEquals(expected, result);
    }

}
