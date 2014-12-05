/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css.parser;

import com.sun.javafx.css.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javafx.css.ParsedValue;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.junit.Test;
import static org.junit.Assert.*;


public class CSSParserTest {
    
    @Test
    public void testRT_16959() {

        CSSParser instance = CSSParser.getInstance();
        
        // RT-16959 is an infinite loop on incomplete linear gradient
        ParsedValue result = instance.parseExpr("-fx-background-color", "linear-gradient(from 0% 0% to 0% 100%, )");
        assertNull("parseExpr", result);

        // The bad syntax should be skipped. The stylesheet should have one
        // linear gradient with colors red, white, blue.
        Stylesheet ss = instance.parse( 
            "* { "
            +   "-fx-background-color: linear-gradient(from 0% 0% to 0% 100%, ); "
            +   "-fx-background-color: linear-gradient(from 0% 0% to 0% 100%, red, white, blue); "
            + "}" 
        );
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertEquals(1,rules.size(),0);
        List<Declaration> decls = ss.getRules().get(0).getUnobservedDeclarationList();
        assertTrue(decls.size()==1);
        Declaration decl = decls.get(0);
        ParsedValue value = decl.getParsedValue();
        assertTrue(value != null);
        
        Paint[] layers = (Paint[])value.convert(null);
        assertTrue(layers.length == 1);
        
        LinearGradient lg = (LinearGradient)layers[0];
        List<Stop> stops = lg.getStops();
        assertTrue(stops.size()==3);
        assertEquals(Color.RED, stops.get(0).getColor());
        assertEquals(Color.WHITE, stops.get(1).getColor());
        assertEquals(Color.BLUE, stops.get(2).getColor());
        
    }
    
    
    @Test
    public void testRT_17770() {

        // RT-17770 is an infinite loop on a dangling comma.
        // Missing term should be ignored
        String stylesheetText = 
            "* {"
            +   "-fx-background-color: linear-gradient( "
            +   "to right, "
            +   "rgba(141, 138, 125, 0.0), "
            +   "rgba(248, 248, 246, 0.3) 45%, "
            +   "rgba(248, 248, 246, 0.8) 50%, "
            +   "rgba(248, 248, 246, 0.3) 55%, "
            +   "rgba(141, 138, 125, 0.0), "
            +   "); "
            + "}";
                
        CSSParser instance = CSSParser.getInstance();
        
        Stylesheet ss = instance.parse(stylesheetText);
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertEquals(1,rules.size(),0);
        List<Declaration> decls = ss.getRules().get(0).getUnobservedDeclarationList();
        assertTrue(decls.size()==1);
        Declaration decl = decls.get(0);
        ParsedValue value = decl.getParsedValue();
        assertTrue(value != null);
        
        Paint[] layers = (Paint[])value.convert(null);
        assertTrue(layers.length == 1);
        
        LinearGradient lg = (LinearGradient)layers[0];
        List<Stop> stops = lg.getStops();
        assertTrue(stops.size()==5);
        assertEquals(Color.rgb(141, 138, 125, 0.0), stops.get(0).getColor());
        assertEquals(Color.rgb(248, 248, 246, 0.3), stops.get(1).getColor());
        assertEquals(Color.rgb(248, 248, 246, 0.8), stops.get(2).getColor());
        assertEquals(Color.rgb(248, 248, 246, 0.3), stops.get(3).getColor());
        assertEquals(Color.rgb(141, 138, 125, 0.0), stops.get(4).getColor());
        
    }    
    
    @Test
    public void testParseSizeWithInvalidDigits() {

        CSSParser instance = CSSParser.getInstance();
        
        // RT-16959 is an infinite loop on incomplete linear gradient
        ParsedValue result = instance.parseExpr("-fx-font-size", "10ptx");
        assertNull("parseExpr", result);

        // The bad syntax should be skipped.
        Stylesheet ss = instance.parse(
            "* {"
            +  "-fx-font-size: 10ptx; "
            +  "-fx-font-size: 12px; "
            + "}"
        );
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertEquals(1,rules.size(),0);
        List<Declaration> decls = ss.getRules().get(0).getUnobservedDeclarationList();
        assertTrue(decls.size()==1);
        Declaration decl = decls.get(0);
        ParsedValue value = decl.getParsedValue();
        assertTrue(value != null);
        
        Double size = (Double)value.convert(Font.font("Amble", 12));
        assertTrue(Double.compare(size, 12) == 0);
    }
    

    @Test
    public void testRT_17830() {

        CSSParser instance = CSSParser.getInstance();
        
        // The empty declaration should be skipped. The stylesheet should have
        // two declarations.
        Stylesheet ss = instance.parse(".rt17830 {-fx-fill: red;; -fx-stroke: yellow; }");
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertEquals(1,rules.size(),0);
        List<Declaration> decls = ss.getRules().get(0).getUnobservedDeclarationList();
        assertEquals(2,decls.size(),0);
        
        Declaration decl = decls.get(0);
        ParsedValue value = decl.getParsedValue();
        assertTrue(value != null);
        Paint paint = (Paint)value.convert(null);
        assertEquals(Color.RED, paint);
        
        decl = decls.get(1);
        value = decl.getParsedValue();
        assertTrue(value != null);
        paint = (Paint)value.convert(null);
        assertEquals(Color.YELLOW, paint);
    }
    
    @Test
    public void testRT_20311() {

        CSSParser instance = CSSParser.getInstance();
        
        try {
            instance.parse(".rt-20311 {  -fx-background-color:red\n-fx-border-color:black; }");
        } catch (Exception e) {
            fail(e.toString());
        }
        
    }

    @Test public void testFontFace() {

        // http://fonts.googleapis.com/css?family=Bree+Serif
        String css = "@font-face {\n" +
            "font-family: 'Bree Serif';\n" +
            "font-style: normal;\n" +
            "font-weight: 400;\n" +
            "src: local('Bree Serif'), local('BreeSerif-Regular'), url(http://themes.googleusercontent.com/static/fonts/breeserif/v2/LQ7WLTaITDg4OSRuOZCps73hpw3pgy2gAi-Ip7WPMi0.woff) format('woff');\n"+
        "}";

        Stylesheet stylesheet = CSSParser.getInstance().parse(css);

        int nFontFaceSrcs = checkFontFace(stylesheet);

        assertEquals(3, nFontFaceSrcs);
    }

    @Test public void testFontFaceMoreThanOneSrc() {

        // http://fonts.googleapis.com/css?family=Bree+Serif
        String css = "@font-face {\n" +
                "font-family: 'Bree Serif';\n" +
                "font-style: normal;\n" +
                "font-weight: 400;\n" +
                "src: local('Bree Serif'), local('BreeSerif-Regular'), url(http://themes.googleusercontent.com/static/fonts/breeserif/v2/LQ7WLTaITDg4OSRuOZCps73hpw3pgy2gAi-Ip7WPMi0.woff) format('woff'),\n"+
                "     local('Bree Serif'), local('BreeSerif-Regular'), url(http://themes.googleusercontent.com/static/fonts/breeserif/v2/LQ7WLTaITDg4OSRuOZCps73hpw3pgy2gAi-Ip7WPMi0.woff) format('woff');\n"+
                "}";

        Stylesheet stylesheet = CSSParser.getInstance().parse(css);

        int nFontFaceSrcs = checkFontFace(stylesheet);
        assertEquals(6, nFontFaceSrcs);
    }

    public static int checkFontFace(Stylesheet stylesheet) {

        List<FontFace> fontFaces = stylesheet.getFontFaces();
        assertNotNull(fontFaces);
        assertEquals(1, fontFaces.size());

        FontFace fontFace = fontFaces.get(0);

        Map<String,String> descriptors = fontFace.getDescriptors();
        assertEquals("'Bree Serif'", descriptors.get("font-family"));
        assertEquals("normal", descriptors.get("font-style"));
        assertEquals("400", descriptors.get("font-weight"));

        List<FontFace.FontFaceSrc> fontFaceSrcs = fontFace.getSources();

        int nFontFaceSrcs = fontFaceSrcs != null ? fontFaceSrcs.size() : 0;

        for(int n=0; n<nFontFaceSrcs; n++) {
            FontFace.FontFaceSrc fontFaceSrc = fontFaceSrcs.get(n);
            FontFace.FontFaceSrcType type = fontFaceSrc.getType();
            switch(type) {
                case LOCAL: {
                    String src = fontFaceSrc.getSrc();
                    assertTrue("Bree Serif".equals(src) || "BreeSerif-Regular".equals(src));
                    assertNull(fontFaceSrc.getFormat());
                    break;
                }
                case URL: {
                    String src = fontFaceSrc.getSrc();
                    assertEquals(src, "http://themes.googleusercontent.com/static/fonts/breeserif/v2/LQ7WLTaITDg4OSRuOZCps73hpw3pgy2gAi-Ip7WPMi0.woff");
                    assertEquals(fontFaceSrc.getFormat(), "woff");
                    break;
                }
                case REFERENCE:
                default:
                        fail();
            }
        }

        return nFontFaceSrcs;
    }

    @Test public void testRT_32522() {

        ParsedValue value = CSSParser.getInstance().parseExpr("foo", "1 2em 3 4;");
        Object obj = value.convert(Font.font(13));
        assert obj instanceof Number[];
        assertArrayEquals(new Number[] {1d, 26d, 3d, 4d}, (Number[])obj);

        value = CSSParser.getInstance().parseExpr("foo", "1;");
        obj = value.convert(null);
        assert obj instanceof Number;
        assertEquals(1d, (Number)obj);

    }

    @Test public void testRT_38483() {

        Duration expected = Duration.millis(42);
        ParsedValue value = CSSParser.getInstance().parseExpr("foo", "42ms;");
        Object observed = value.convert(null);
        assertEquals(expected, observed);

        value = CSSParser.getInstance().parseExpr("foo", "indefinite;");
        observed = value.convert(null);
        assertEquals(Duration.INDEFINITE, observed);
    }
}
