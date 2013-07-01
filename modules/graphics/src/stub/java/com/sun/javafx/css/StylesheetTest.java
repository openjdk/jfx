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

package com.sun.javafx.css;

import javafx.css.StyleOrigin;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import javafx.css.StyleableProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.*;
import static org.junit.Assert.*;


public class StylesheetTest {

    URL testURL = null;
    
    public StylesheetTest() {
        testURL = getClass().getResource("HonorDeveloperSettingsTest_UA.css");
    }

    /**
     * Test of getUrl method, of class Stylesheet.
     */
    @Test
    public void testGetUrl() {
        Stylesheet instance = new Stylesheet();
        URL expResult = null;
        URL result = instance.getUrl();
        assertEquals(expResult, result);
        
        instance = new Stylesheet(testURL);
        result = instance.getUrl();
        assertEquals(testURL, result);
    }

    /**
     * Test of getSource method, of class Stylesheet.
     */
    @Test
    public void testGetStylesheetSourceGetterAndSetter() {
        Stylesheet instance = new Stylesheet();
        StyleOrigin expResult = StyleOrigin.AUTHOR;
        StyleOrigin result = instance.getOrigin();
        assertEquals(expResult, result);
        
        instance.setOrigin(StyleOrigin.INLINE);
        expResult = StyleOrigin.INLINE;
        result = instance.getOrigin();
        assertEquals(expResult, result);

        instance.setOrigin(StyleOrigin.USER);
        expResult = StyleOrigin.USER;
        result = instance.getOrigin();
        assertEquals(expResult, result);

        instance.setOrigin(StyleOrigin.USER_AGENT);
        expResult = StyleOrigin.USER_AGENT;
        result = instance.getOrigin();
        assertEquals(expResult, result);
    }

    /**
     * Test of addRule method, of class Stylesheet.
     */
    @Test
    public void testStylesheetAddAndGetRule() {
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new Stylesheet();
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        List<Rule> rules = instance.getRules();
        assert(rules.size() == 5);
        for(Rule r : rules) assertEquals(r, rule);
    }
    
    @Test
    public void testAddingRuleSetsStylesheetOnRule() {
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new Stylesheet();
        instance.getRules().add(rule);
        assert(rule.getStylesheet() == instance);        
    }

    @Test
    public void testRemovingRuleSetsStylesheetNullOnRule() {
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new Stylesheet();
        instance.getRules().add(rule);
        instance.getRules().remove(rule);
        assertNull(rule.getStylesheet());
    }
    
    /**
     * Test of equals method, of class Stylesheet.
     */
    @Test
    public void testStylesheetEquals() {
        Object obj = new Stylesheet();
        Stylesheet instance = new Stylesheet();
        boolean expResult = true;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);

        obj = new Stylesheet(testURL);
        instance = new Stylesheet(testURL);
        expResult = true;
        result = instance.equals(obj);
        assertEquals(expResult, result);

        obj = new Stylesheet();
        instance = new Stylesheet(testURL);
        expResult = false;
        result = instance.equals(obj);
        assertEquals(expResult, result);
        
        obj = instance = new Stylesheet(testURL);
        expResult = true;
        result = instance.equals(obj);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of toString method, of class Stylesheet.
     */
    @Test
    public void testStylesheetToString() {
        Stylesheet instance = new Stylesheet();
        String expResult = "/*  */";
        String result = instance.toString();
        assertEquals(expResult, result);

        instance = new Stylesheet(testURL);
        expResult = "/* " + testURL.toExternalForm() + " */";
        result = instance.toString();
        assertEquals(expResult, result);

        instance = new Stylesheet(testURL);
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        instance.getRules().add(rule);
        expResult = "/* " + testURL.toExternalForm() + " */\n{\n}\n";
        result = instance.toString();
        assertEquals(expResult, result);
    }

    /**
     * Test of writeBinary method, of class Stylesheet.
    @Test
    public void testWriteAndReadBinary() throws Exception {
        DataOutputStream os = null;
        StringStore stringStore = null;
        Stylesheet instance = new Stylesheet(testURL);
        instance.writeBinary(os, stringStore);
    }
     */

    /**
     * Test of loadBinary method, of class Stylesheet.
    @Test
    public void testLoadBinary() {
        System.out.println("loadBinary");
        URL url = null;
        Stylesheet expResult = null;
        Stylesheet result = Stylesheet.loadBinary(url);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
    
    @Test
    public void testRT_23140() {

        try {
            Group root = new Group();
            root.getChildren().add(new Rectangle(50,50));        
            Scene scene = new Scene(root, 500, 500);
            root.getStylesheets().add("bogus.css");
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
        } catch (NullPointerException e) {
            // RT-23140 is supposed to fix the NPE. Did it?
            fail("Test purpose failed: " + e.toString());
        } catch (Exception e) {
            // Something other than an NPE should still raise a red flag,
            // but the exception is not what RT-23140 fixed.

            fail("Exception not expected: " + e.toString());
        }
        
    }

    @Test public void testRT_31316() {

        try {

            Rectangle rect = new Rectangle(50,50);
            rect.setStyle("-fx-base: red; -fx-fill: -fx-base;");
            rect.setFill(Color.GREEN);

            Group root = new Group();
            root.getChildren().add(rect);
            Scene scene = new Scene(root, 500, 500);

            root.impl_processCSS(true);

            // Shows inline style works.
            assertEquals(Color.RED, rect.getFill());

            // reset fill
            ((StyleableProperty<Paint>)rect.fillProperty()).applyStyle(null, null);

            // loop in style!
            rect.setStyle("-fx-base: -fx-fill; -fx-fill: -fx-base;");
            root.impl_processCSS(true);

            // Shows value was left alone
            assertNull(rect.getFill());


        } catch (Exception e) {
            // The code generates an IllegalArgumentException that should never reach here
            fail("Exception not expected: " + e.toString());
        }

    }

    @Test public void testRT_31316_with_complex_value() {

        try {

            Rectangle rect = new Rectangle(50,50);
            rect.setStyle("-fx-base: red; -fx-color: -fx-base; -fx-fill: radial-gradient(radius 100%, red, -fx-color);");
            rect.setFill(Color.GREEN);

            Group root = new Group();
            root.getChildren().add(rect);
            Scene scene = new Scene(root, 500, 500);

            root.impl_processCSS(true);

            // Shows inline style works.
            assertTrue(rect.getFill() instanceof RadialGradient);

            // reset fill
            ((StyleableProperty<Paint>)rect.fillProperty()).applyStyle(null, null);

            // loop in style!
            rect.setStyle("-fx-base: -fx-color; -fx-color: -fx-base; -fx-fill: radial-gradient(radius 100%, red, -fx-color);");

            root.impl_processCSS(true);

            // Shows value was left alone
            assertNull(rect.getFill());

        } catch (Exception e) {
            // The code generates an IllegalArgumentException that should never reach here
            fail("Exception not expected: " + e.toString());
        }
    }


    @Test public void testRT_31316_with_complex_scenegraph() {

        try {

            Rectangle rect = new Rectangle(50,50);
            rect.setStyle("-fx-fill: radial-gradient(radius 100%, red, -fx-color);");
            rect.setFill(Color.GREEN);

            StackPane pane = new StackPane();
            pane.setStyle("-fx-color: -fx-base;");
            pane.getChildren().add(rect);

            Group root = new Group();
            // loop in style!
            root.setStyle("-fx-base: red;");
            root.getChildren().add(pane);
            Scene scene = new Scene(root, 500, 500);

            root.impl_processCSS(true);

            // Shows inline style works.
            assertTrue(rect.getFill() instanceof RadialGradient);

            // reset fill
            ((StyleableProperty<Paint>)rect.fillProperty()).applyStyle(null, null);

            // loop in style
            root.setStyle("-fx-base: -fx-color;");

            root.impl_processCSS(true);

            // Shows value was left alone
            assertNull(rect.getFill());

        } catch (Exception e) {
            // The code generates an IllegalArgumentException that should never reach here
            fail("Exception not expected: " + e.toString());
        }

    }

    
}
