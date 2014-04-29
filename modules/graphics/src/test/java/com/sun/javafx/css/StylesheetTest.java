/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.css.parser.CSSParser;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;


public class StylesheetTest {

    String testURL = null;
    
    public StylesheetTest() {
        testURL = getClass().getResource("HonorDeveloperSettingsTest_UA.css").toExternalForm();
    }

    /**
     * Test of getUrl method, of class Stylesheet.
     */
    @Test
    public void testGetUrl() {
        Stylesheet instance = new Stylesheet();
        URL expResult = null;
        String result = instance.getUrl();
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
        expResult = "/* " + testURL + " */";
        result = instance.toString();
        assertEquals(expResult, result);

        instance = new Stylesheet(testURL);
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        instance.getRules().add(rule);
        expResult = "/* " + testURL + " */\n{\n}\n";
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
            restored.readBinary(Stylesheet.BINARY_CSS_VERSION, dis, stringStore.strings.toArray(new String[stringStore.strings.size()]));

            List<Rule> cssRules = stylesheet.getRules();
            List<Rule> bssRules = restored.getRules();

            // Rule does not have an equals method
            assert(cssRules.size() == bssRules.size());
            for (int n=0; n<cssRules.size(); n++) {
                Rule expected = cssRules.get(n);
                Rule actual = bssRules.get(n);
                assertEquals(Integer.toString(n), expected.getUnobservedDeclarationList(), actual.getUnobservedDeclarationList());
            }

        } catch (IOException ioe) {
            fail(ioe.toString());
        }

    }

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

            root.applyCss();

            // Shows inline style works.
            assertEquals(Color.RED, rect.getFill());

            // reset fill
            ((StyleableProperty<Paint>)rect.fillProperty()).applyStyle(null, null);

            // loop in style!
            rect.setStyle("-fx-base: -fx-fill; -fx-fill: -fx-base;");
            root.applyCss();

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

            root.applyCss();

            // Shows inline style works.
            assertTrue(rect.getFill() instanceof RadialGradient);

            // reset fill
            ((StyleableProperty<Paint>)rect.fillProperty()).applyStyle(null, null);

            // loop in style!
            rect.setStyle("-fx-base: -fx-color; -fx-color: -fx-base; -fx-fill: radial-gradient(radius 100%, red, -fx-color);");

            root.applyCss();

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

            root.applyCss();

            // Shows inline style works.
            assertTrue(rect.getFill() instanceof RadialGradient);

            // reset fill
            ((StyleableProperty<Paint>)rect.fillProperty()).applyStyle(null, null);

            // loop in style
            root.setStyle("-fx-base: -fx-color;");

            root.applyCss();

            // Shows value was left alone
            assertNull(rect.getFill());

        } catch (Exception e) {
            // The code generates an IllegalArgumentException that should never reach here
            fail("Exception not expected: " + e.toString());
        }

    }

    @Test public void testRT_32229() {

        try {

            Rectangle rect = new Rectangle(50,50);
            rect.setStyle("-fx-base: red; -fx-fill: radial-gradient(radius 100%, derive(-fx-base, -25%), derive(-fx-base, 25%));");
            rect.setFill(Color.GREEN);

            Group root = new Group();
            root.getChildren().add(rect);
            Scene scene = new Scene(root, 500, 500);

            root.applyCss();

            // Shows inline style works.
            assertTrue(rect.getFill() instanceof RadialGradient);

            // reset fill
            ((StyleableProperty<Paint>)rect.fillProperty()).applyStyle(null, null);

            // loop in style!
            root.setStyle("-fx-base: -fx-fill;");
            rect.setStyle("-fx-fill: radial-gradient(radius 100%, derive(-fx-base, -25%), derive(-fx-base, 25%));");


            root.applyCss();

            // Shows value was left alone
            assertNull(rect.getFill());

        } catch (Exception e) {
            // The code generates an IllegalArgumentException that should never reach here
            fail("Exception not expected: " + e.toString());
        }
    }

    @Test
    public void testRT_30953_parse() {

        try {
            // Make sure RT-30953.css can be parsed, serialized and deserialized with the current code,
            // no matter the bss version
            URL url = StylesheetTest.class.getResource("RT-30953.css");
            if (url == null) {
                fail("Can't find RT-30953.css");
            }

            Stylesheet ss = CSSParser.getInstance().parse(url);
            int nFontFaceSrcs = checkFontFace(ss);
            assertEquals(3, nFontFaceSrcs);
            checkConvert(ss);

        } catch (Exception e) {
            fail(e.toString());
        }

    }

    @Test public void testRT_30953_deserialize_from_v4() {
        // RT-30953-v4.bss was generated with version 4
        Stylesheet ss = deserialize("RT-30953-v4.bss");
        checkConvert(ss);
    }

    @Test
    public void testRT_30953_deserialize_from_2_2_45() {

        // RT-30953-2.2.4bss was generated with javafx version 2.2.45 from 7u??
        Stylesheet ss = deserialize("RT-30953-2.2.45.bss");
        checkConvert(ss);
    }

    @Test
    public void testRT_30953_deserialize_from_2_2_4() {

        // RT-30953-2.2.4bss was generated with javafx version 2.2.4 from 7u10
        Stylesheet ss = deserialize("RT-30953-2.2.4.bss");
        checkConvert(ss);
    }

    @Test
    public void testRT_30953_deserialize_from_2_2_21() {

        // RT-30953-2.2.21.bss was generated with javafx version 2.2.21 from 7u21
        Stylesheet ss = deserialize("RT-30953-2.2.21.bss");
        checkConvert(ss);

    }

    private Stylesheet deserialize(String bssFile) {
        Stylesheet ss = null;
        try {
            URL url = StylesheetTest.class.getResource(bssFile);
            if (url == null) {
                fail(bssFile);
            }
            ss = Stylesheet.loadBinary(url);
        } catch (IOException ioe) {
            fail(ioe.toString());
        } catch (Exception e) {
            fail(e.toString());
        }
        return ss;
    }

    private void checkConvert(Stylesheet ss) {
        Declaration decl = null;
        StyleConverter converter = null;
        try {
            for (Rule r : ss.getRules()) {
                for (Declaration d : r.getDeclarations()) {
                    decl = d;
                    ParsedValue pv = decl.getParsedValue();
                    converter = pv.getConverter();
                    if (converter == null) {

                        if ("inherit".equals(pv.getValue())) continue;

                        String prop = d.getProperty().toLowerCase(Locale.ROOT);
                        if ("-fx-shape".equals(prop)) {
                            StringConverter.getInstance().convert(pv, null);
                        } else if ("-fx-font-smoothing-type".equals(prop)) {
                            (new EnumConverter<FontSmoothingType>(FontSmoothingType.class)).convert(pv, null);
                        } else if ("-fx-text-alignment".equals(prop)) {
                            (new EnumConverter<TextAlignment>(TextAlignment.class)).convert(pv, null);
                        } else if ("-fx-alignment".equals(prop)) {
                            (new EnumConverter<Pos>(Pos.class)).convert(pv, null);
                        } else if ("-fx-text-origin".equals(prop)) {
                            (new EnumConverter<VPos>(VPos.class)).convert(pv, null);
                        } else if ("-fx-text-overrun".equals(prop)) {
                            Class cl = null;
                            try {
                                cl = Class.forName("javafx.scene.control.OverrunStyle");
                            } catch (Exception ignored) {
                                // just means we're running ant test from javafx-ui-common
                            }
                            if (cl != null) {
                                (new EnumConverter(cl)).convert(pv, null);
                            }
                        } else if ("-fx-orientation".equals(prop)) {
                            (new EnumConverter<Orientation>(Orientation.class)).convert(pv, null);
                        } else if ("-fx-content-display".equals(prop)) {
                            Class cl = null;
                            try {
                                cl = Class.forName("javafx.scene.control.CpntentDisplay");
                            } catch (Exception ignored) {
                                // just means we're running ant test from javafx-ui-common
                            }
                            if (cl != null) {
                                (new EnumConverter(cl)).convert(pv, null);
                            }
                        } else if ("-fx-hbar-policy".equals(prop)) {
                            Class cl = null;
                            try {
                                cl = Class.forName("javafx.scene.control.ScrollPane.ScrollBarPolicy");
                            } catch (Exception ignored) {
                                // just means we're running ant test from javafx-ui-common
                            }
                            if (cl != null) {
                                (new EnumConverter(cl)).convert(pv, null);
                            }
                        } else {
                            System.out.println("No converter for " + d.toString() + ". Skipped conversion.");
                        }
                        continue;
                    }
                    Object value = converter.convert(pv, Font.getDefault());
                }
            }
        } catch (Exception e) {
            if (decl == null) fail(e.toString());
            else if (converter != null) fail(decl.getProperty() + ", " + converter + ", " + e.toString());
            else fail(decl.getProperty() + ", " + e.toString());
        }

    }

    private int checkFontFace(Stylesheet stylesheet) {
        return com.sun.javafx.css.parser.CSSParserTest.checkFontFace(stylesheet);
    }

}
