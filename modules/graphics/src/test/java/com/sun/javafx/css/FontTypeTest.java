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

package com.sun.javafx.css;

import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.parser.CSSParser;
import javafx.css.ParsedValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static org.junit.Assert.*;
import org.junit.Test;


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

        ParsedValue<String,String> family = new ParsedValueImpl<String,String>(font.getFamily(), null);

        ParsedValue<ParsedValue<?,Size>,Number> size =
                new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                    new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                    SizeConverter.getInstance()
                );

        ParsedValue<String,FontPosture> style =
                new ParsedValueImpl<String,FontPosture>(FontPosture.REGULAR.name(), FontConverter.FontStyleConverter.getInstance());

        ParsedValue<String,FontWeight> weight =
                new ParsedValueImpl<String,FontWeight>(FontWeight.NORMAL.name(), FontConverter.FontWeightConverter.getInstance());
        ParsedValue<ParsedValue[],Font> value = new ParsedValueImpl<ParsedValue[],Font>(
                new ParsedValue[] {family, size, weight, style},
                FontConverter.getInstance()
            );

        Font expResult = Font.font(font.getFamily(), font.getSize() * 2);
        Font result = value.convert(font);
        checkFont(expResult, result);

        size =
                new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                    new ParsedValueImpl<Size,Size>(new Size(120, SizeUnits.PERCENT), null),
                    SizeConverter.getInstance()
                );

        value = new ParsedValueImpl<ParsedValue[],Font>(
                new ParsedValue[] {family, size, weight, style},
                FontConverter.getInstance()
            );

        expResult = Font.font(font.getFamily(), font.getSize() * 1.2);
        result = value.convert(font);
        checkFont(expResult, result);

        size =
                new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                    new ParsedValueImpl<Size,Size>(new Size(font.getSize(), SizeUnits.PT), null),
                    SizeConverter.getInstance()
                );

        value = new ParsedValueImpl<ParsedValue[],Font>(
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

    @Test public void test_RT_25355_shorthandLast() {

        Text txt = new Text("test_RT_25355");
        txt.setStyle("-fx-font-weight: bold; -fx-font: 16 Amble;");

        Scene scene  = new Scene(new Group());
        ((Group)scene.getRoot()).getChildren().add(txt);

        txt.impl_processCSS(true);

        Font f = txt.getFont();
        assertEquals("Regular", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(16, f.getSize(),0);
    }

    @Test public void test_RT_25355_shorthandFirst() {

        Text txt = new Text("test_RT_25355");
        txt.setStyle("-fx-font: 16 Amble; -fx-font-weight: bold;");

        Scene scene  = new Scene(new Group());
        ((Group)scene.getRoot()).getChildren().add(txt);

        txt.impl_processCSS(true);

        Font f = txt.getFont();
        assertEquals("Bold", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(16, f.getSize(),0);

    }

    @Test public void test_RT_25355_shorthandFirstInheritedWeight() {

        Text txt = new Text("test_RT_25355");
        txt.setStyle("-fx-font: 16 Amble;");

        Group g = new Group();
        g.setStyle("-fx-font-weight: bold");

        Scene scene  = new Scene(g);
        g.getChildren().add(txt);

        g.impl_processCSS(true);

        Font f = txt.getFont();
        assertEquals("Regular", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(16, f.getSize(),0);

    }

    @Test public void test_RT_25355_weightFirstInheritedShorthand() {

        Text txt = new Text("test_RT_25355");
        txt.setStyle("-fx-font-weight: bold;");

        Group g = new Group();
        g.setStyle("-fx-font: 16 Amble;");

        Scene scene  = new Scene(g);
        g.getChildren().add(txt);

        g.impl_processCSS(true);

        Font f = txt.getFont();
        assertEquals("Bold", f.getStyle());
        assertEquals("Amble", f.getFamily());
        assertEquals(16, f.getSize(),0);

    }

    @Test public void testInheritedFontDoesNotOverrideUserSetFont() {

        Text txt = new Text("testInheritedFontDoesNotOverrideUserSetFont");
        txt.setFont(Font.font("Amble", 32));

        Group g = new Group();
        g.setStyle("-fx-font: 16 Amble;");

        Scene scene  = new Scene(g);
        g.getChildren().add(txt);

        g.impl_processCSS(true);

        Font f = txt.getFont();
        assertEquals("Amble", f.getFamily());
        assertEquals(32, f.getSize(),0);

    }

    @Test public void testRT_32551() {

        Text txt = new Text("testRT_32551");
        txt.setId("test-rt-32551");
        txt.setStyle("-fx-font-weight:bold;");

        Group g = new Group();

        Scene scene  = new Scene(g);
        scene.getStylesheets().add(FontTypeTest.class.getResource("HonorDeveloperSettingsTest_AUTHOR.css").toExternalForm());
        g.getChildren().add(txt);

        g.impl_processCSS(true);

        Font f = txt.getFont();
        // should get size and amble from .root, 'italic' from #test-rt-32551, bold from inline.
        assertEquals("Amble Bold Italic", f.getName());
        assertEquals(20, f.getSize(),0);

    }

    @Test public void testRT_29773() {

        Text txt = new Text("testRT_29773");
        txt.setId("test-rt-29773");

        Group g = new Group();

        Scene scene  = new Scene(g);
        scene.getStylesheets().add(FontTypeTest.class.getResource("HonorDeveloperSettingsTest_AUTHOR.css").toExternalForm());
        g.getChildren().add(txt);

        g.impl_processCSS(true);

        Font f = txt.getFont();
        // should get size and amble from .root, 'italic' from #test-rt-32551, bold from inline.
        assertEquals("Amble Condensed", f.getName());
        assertEquals(20, f.getSize(),0);

    }
}
