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
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import org.junit.Test;

import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.parser.CSSParser;


public class FontTypeTest {

    public FontTypeTest() {
    }

    void checkFont(Font expResult, Font result) {
        assertEquals("family", expResult.getFamily(), result.getFamily());
        assertEquals("size", expResult.getSize(), result.getSize(), 0.001);
        // TODO: how to check for weight and posture?
    }
    /**
     * Test of convert method, of class FontType.
     */
    @Test
    public void testConvert_Value_Font() {
        //System.out.println("convert");
        Font font = Font.getDefault();

        ParsedValue<String,String> family = new ParsedValue<String,String>(font.getFamily(), null);

        ParsedValue<ParsedValue<?,Size>,Double> size =
                new ParsedValue<ParsedValue<?,Size>,Double>(
                    new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                    SizeConverter.getInstance()
                );

        ParsedValue<FontUnits.Style,FontPosture> style =
                new ParsedValue<FontUnits.Style,FontPosture>(FontUnits.Style.NORMAL, FontConverter.StyleConverter.getInstance());

        ParsedValue<FontUnits.Weight,FontWeight> weight =
                new ParsedValue<FontUnits.Weight,FontWeight>(FontUnits.Weight.NORMAL, FontConverter.WeightConverter.getInstance());
        ParsedValue<ParsedValue[],Font> value = new ParsedValue<ParsedValue[],Font>(
                new ParsedValue[] {family, size, weight, style},
                FontConverter.getInstance()
            );

        Font expResult = Font.font(font.getFamily(), font.getSize() * 2);
        Font result = value.convert(font);
        checkFont(expResult, result);

        size =
                new ParsedValue<ParsedValue<?,Size>,Double>(
                    new ParsedValue<Size,Size>(new Size(120, SizeUnits.PERCENT), null),
                    SizeConverter.getInstance()
                );

        value = new ParsedValue<ParsedValue[],Font>(
                new ParsedValue[] {family, size, weight, style},
                FontConverter.getInstance()
            );

        expResult = Font.font(font.getFamily(), font.getSize() * 1.2);
        result = value.convert(font);
        checkFont(expResult, result);

        size =
                new ParsedValue<ParsedValue<?,Size>,Double>(
                    new ParsedValue<Size,Size>(new Size(font.getSize(), SizeUnits.PT), null),
                    SizeConverter.getInstance()
                );

        value = new ParsedValue<ParsedValue[],Font>(
                new ParsedValue[] {family, size, weight, style},
                FontConverter.getInstance()
            );

        expResult = Font.font(font.getFamily(), font.getSize() * (96.0/72.0));
        result = value.convert(font);
        checkFont(expResult, result);
        
    }

    @Test public void test_RT_21960_Bold_Italic() {
        
        ParsedValue pv = CSSParser.getInstance().parseExpr("-fx-font", "italic bold 24 Amble");
        Font f = (Font)pv.convert(null);
        assertEquals("Bold Italic", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(24, f.getSize(),0);
    }
    
    @Test public void test_RT_21960_Bold() {
        
        ParsedValue pv = CSSParser.getInstance().parseExpr("-fx-font", "bold 24 Amble");
        Font f = (Font)pv.convert(null);
        assertEquals("Bold", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(24, f.getSize(),0);
    }

    @Test public void test_RT_21960_Italic() {
        
        ParsedValue pv = CSSParser.getInstance().parseExpr("-fx-font", "italic 24 Amble");
        Font f = (Font)pv.convert(null);
        assertEquals("Italic", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(24, f.getSize(),0);
    }

    @Test public void test_RT_21960_Neither_Bold_Nor_Italic() {
        
        ParsedValue pv = CSSParser.getInstance().parseExpr("-fx-font", "24 Amble");
        Font f = (Font)pv.convert(null);
        assertEquals("Regular", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(24, f.getSize(),0);
    }    

}
