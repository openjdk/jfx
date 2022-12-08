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
import javafx.css.converter.SizeConverter;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.SizeUnitsShim;
import javafx.scene.text.Font;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class FontSizeTypeTest {

    public FontSizeTypeTest() {
    }

    /**
     * Test of convert method, of class FontSizeType.
     */
    @Test
    public void testConvertToPixels() {
        ParsedValue<Size,Size> size = new ParsedValueImpl<>(new Size(2.0f, SizeUnits.EM), null);
        ParsedValue<ParsedValue<?,Size>,Number> value = new ParsedValueImpl<>(size, SizeConverter.getInstance());
        Font font = Font.getDefault();
        double expResult = SizeUnitsShim.pixels(SizeUnits.EM, 2, 1, font);
        double result = SizeConverter.getInstance().convert(value, font).doubleValue();
        assertEquals(expResult, result, 0.01);

        size = new ParsedValueImpl<>(new Size(120.0f, SizeUnits.PERCENT), null);
        value = new ParsedValueImpl<>(size, SizeConverter.getInstance());
        expResult = SizeUnitsShim.pixels(SizeUnits.PERCENT, 120, 1, font);
        result = SizeConverter.getInstance().convert(value, font).doubleValue();
        assertEquals(expResult, result, 0.01);

        size = new ParsedValueImpl<>(new Size(12.0f, SizeUnits.PT), null);
        value = new ParsedValueImpl<>(size, SizeConverter.getInstance());
        expResult = SizeUnitsShim.pixels(SizeUnits.PT, 12, 1, font);
        result = SizeConverter.getInstance().convert(value, font).doubleValue();
        assertEquals(expResult, result, 0.01);

        size = new ParsedValueImpl<>(new Size(12.0f, SizeUnits.PX), null);
        value = new ParsedValueImpl<>(size, SizeConverter.getInstance());
        expResult = SizeUnitsShim.pixels(SizeUnits.PX, 12, 1, font);
        result = SizeConverter.getInstance().convert(value, font).doubleValue();
        assertEquals(expResult, result, 0.01);
    }

//    @Test
//    public void testConvertToPoints() {
//
//        Font font = Font.getDefault();
//        // font size is in pixels. convert to points
//        double pointSize = font.getSize() / javafx.stage.Screen.getPrimary().getDpi() * 72;
//
//        ParsedValue<Size,Size> size = new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null);
//        ParsedValue<Value<?,Size>,Double> value = new ParsedValueImpl<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        double expResult = SizeUnits.EM.points(2, 1, font);
//        double result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//        size = new ParsedValueImpl<Size,Size>(new Size(120.0f, SizeUnits.PERCENT), null);
//        value = new ParsedValueImpl<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        expResult = SizeUnits.PERCENT.points(120, 1, font);
//        result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//        size = new ParsedValueImpl<Size,Size>(new Size(12.0f, SizeUnits.PT), null);
//        value = new ParsedValueImpl<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        expResult = SizeUnits.PT.points(12, 1, font);
//        result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//        size = new ParsedValueImpl<Size,Size>(new Size(12.0f, SizeUnits.PX), null);
//        value = new ParsedValueImpl<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        expResult = SizeUnits.PX.points(12, 1, font);
//        result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//    }

}
