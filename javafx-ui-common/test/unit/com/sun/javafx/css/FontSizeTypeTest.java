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
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistribution of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistribution in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.

 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND
 * ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A
 * RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 * IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.sun.javafx.css;

import static org.junit.Assert.assertEquals;
import javafx.scene.text.Font;

import org.junit.Test;

import com.sun.javafx.css.converters.SizeConverter;


public class FontSizeTypeTest {

    public FontSizeTypeTest() {
    }

    /**
     * Test of convert method, of class FontSizeType.
     */
    @Test
    public void testConvertToPixels() {
        ParsedValue<Size,Size> size = new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null);
        ParsedValue<ParsedValue<?,Size>,Double> value = new ParsedValue<ParsedValue<?,Size>,Double>(size, SizeConverter.getInstance());
        Font font = Font.getDefault();
        double expResult = SizeUnits.EM.pixels(2, 1, font);
        double result = SizeConverter.getInstance().convert(value, font);
        assertEquals(expResult, result, 0.01);

        size = new ParsedValue<Size,Size>(new Size(120.0f, SizeUnits.PERCENT), null);
        value = new ParsedValue<ParsedValue<?,Size>,Double>(size, SizeConverter.getInstance());
        expResult = SizeUnits.PERCENT.pixels(120, 1, font);
        result = SizeConverter.getInstance().convert(value, font);
        assertEquals(expResult, result, 0.01);

        size = new ParsedValue<Size,Size>(new Size(12.0f, SizeUnits.PT), null);
        value = new ParsedValue<ParsedValue<?,Size>,Double>(size, SizeConverter.getInstance());
        expResult = SizeUnits.PT.pixels(12, 1, font);
        result = SizeConverter.getInstance().convert(value, font);
        assertEquals(expResult, result, 0.01);

        size = new ParsedValue<Size,Size>(new Size(12.0f, SizeUnits.PX), null);
        value = new ParsedValue<ParsedValue<?,Size>,Double>(size, SizeConverter.getInstance());
        expResult = SizeUnits.PX.pixels(12, 1, font);
        result = SizeConverter.getInstance().convert(value, font);
        assertEquals(expResult, result, 0.01);
    }

//    @Test
//    public void testConvertToPoints() {
//
//        Font font = Font.getDefault();
//        // font size is in pixels. convert to points
//        double pointSize = font.getSize() / javafx.stage.Screen.getPrimary().getDpi() * 72;
//
//        ParsedValue<Size,Size> size = new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null);
//        ParsedValue<Value<?,Size>,Double> value = new ParsedValue<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        double expResult = SizeUnits.EM.points(2, 1, font);
//        double result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//        size = new ParsedValue<Size,Size>(new Size(120.0f, SizeUnits.PERCENT), null);
//        value = new ParsedValue<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        expResult = SizeUnits.PERCENT.points(120, 1, font);
//        result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//        size = new ParsedValue<Size,Size>(new Size(12.0f, SizeUnits.PT), null);
//        value = new ParsedValue<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        expResult = SizeUnits.PT.points(12, 1, font);
//        result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//        size = new ParsedValue<Size,Size>(new Size(12.0f, SizeUnits.PX), null);
//        value = new ParsedValue<Value<?,Size>,Double>(size, StyleConverter.POINTS);
//        expResult = SizeUnits.PX.points(12, 1, font);
//        result = StyleConverter.POINTS.convert(value, font);
//        assertEquals(expResult, result, 0.01);
//
//    }

}
