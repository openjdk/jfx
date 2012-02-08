/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.io.*;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
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
        Stylesheet ss = instance.parseStyle(
              "-fx-background-color: linear-gradient(from 0% 0% to 0% 100%, ); "
            + "-fx-background-color: linear-gradient(from 0% 0% to 0% 100%, red, white, blue);" 
        );
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertTrue(rules.size()==1);
        List<Declaration> decls = ss.getRules().get(0).getDeclarations();
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
              "-fx-background-color: linear-gradient( "
            + "to right, "
            + "rgba(141, 138, 125, 0.0), "
            + "rgba(248, 248, 246, 0.3) 45%, "
            + "rgba(248, 248, 246, 0.8) 50%, "
            + "rgba(248, 248, 246, 0.3) 55%, "
            + "rgba(141, 138, 125, 0.0), "
            + "); ";
                
        CSSParser instance = CSSParser.getInstance();
        
        Stylesheet ss = instance.parseStyle(stylesheetText);
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertTrue(rules.size()==1);
        List<Declaration> decls = ss.getRules().get(0).getDeclarations();
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
        Stylesheet ss = instance.parseStyle(
              "-fx-font-size: 10ptx; "
            + "-fx-font-size: 12px; "
        );
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertTrue(rules.size()==1);
        List<Declaration> decls = ss.getRules().get(0).getDeclarations();
        assertTrue(decls.size()==1);
        Declaration decl = decls.get(0);
        ParsedValue value = decl.getParsedValue();
        assertTrue(value != null);
        
        Double size = (Double)value.convert(null);
        assertTrue(Double.compare(size, 12) == 0);
    }
    
    @Test public void test_RT_18126() {
        // CSS cannot write binary -fx-background-repeat: repeat, no-repeat;
        String data = "#rt18126 {"
                + "-fx-background-repeat: repeat, no-repeat;"
                + "-fx-border-image-repeat: repeat, no-repeat;"
                + "}";
        
        try {
            Stylesheet stylesheet = CSSParser.getInstance().parse(data);

            StringStore stringStore = new StringStore();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            stylesheet.writeBinary(dos, stringStore);
            dos.flush();
            dos.close();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            DataInputStream dis = new DataInputStream(bais);
            
            Stylesheet restored = new Stylesheet();
            restored.readBinary(dis, stringStore.strings.toArray(new String[stringStore.strings.size()]));
            
            List<Rule> cssRules = stylesheet.getRules();
            List<Rule> bssRules = restored.getRules();
            
            // Rule does not have an equals method
            assert(cssRules.size() == bssRules.size());
            for (int n=0; n<cssRules.size(); n++) {
                Rule expected = cssRules.get(n);
                Rule actual = bssRules.get(n);
                assertEquals(Integer.toString(n), expected.getDeclarations(), actual.getDeclarations());                
            }
            
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
        
    }
    
    @Test
    public void testRT_17830() {

        CSSParser instance = CSSParser.getInstance();
        
        // The empty declaration should be skipped. The stylesheet should have
        // two declarations.
        Stylesheet ss = null;
        try { 
            ss = instance.parse(".rt17830 {-fx-fill: red;; -fx-stroke: yellow; }");
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
        
        assertNotNull(ss);
        List<Rule> rules = ss.getRules();
        assertTrue(rules.size()==1);
        List<Declaration> decls = ss.getRules().get(0).getDeclarations();
        assertTrue(decls.size()==2);
        
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
    
}
