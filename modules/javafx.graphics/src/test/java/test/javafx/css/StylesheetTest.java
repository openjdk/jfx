/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.css;

import com.sun.javafx.css.StyleManager;
import javafx.css.StyleConverter.StringStore;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.StringConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javafx.css.CssParser;
import javafx.css.Declaration;
import javafx.css.ParsedValue;
import javafx.css.Rule;
import javafx.css.RuleShim;
import javafx.css.Selector;
import javafx.css.SimpleSelector;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.StyleableProperty;
import javafx.css.Stylesheet;
import javafx.css.StylesheetShim;

import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
    final String EXPECTED_WARNING = "EXPECTED WARNING: This is a negative test"
        + " to verify loop detection in CSS. A Loop detected warning message is expected.";

    public StylesheetTest() {
        testURL = getClass().getResource("HonorDeveloperSettingsTest_UA.css").toExternalForm();
    }

    /**
     * Test of getUrl method, of class Stylesheet.
     */
    @Test
    public void testGetUrl() {
        Stylesheet instance = new StylesheetShim();
        URL expResult = null;
        String result = instance.getUrl();
        assertEquals(expResult, result);

        instance = new StylesheetShim(testURL);
        result = instance.getUrl();
        assertEquals(testURL, result);
    }

    /**
     * Test of getSource method, of class Stylesheet.
     */
    @Test
    public void testGetStylesheetSourceGetterAndSetter() {
        Stylesheet instance = new StylesheetShim();
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
        Rule rule = RuleShim.getRule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new StylesheetShim();
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
        Rule rule = RuleShim.getRule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new StylesheetShim();
        instance.getRules().add(rule);
        assert(rule.getStylesheet() == instance);
    }

    @Test
    public void testRemovingRuleSetsStylesheetNullOnRule() {
        Rule rule = RuleShim.getRule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new StylesheetShim();
        instance.getRules().add(rule);
        instance.getRules().remove(rule);
        assertNull(rule.getStylesheet());
    }

    /**
     * Test of equals method, of class Stylesheet.
     */
    @Test
    public void testStylesheetEquals() {
        Object obj = new StylesheetShim();
        Stylesheet instance = new StylesheetShim();
        boolean expResult = true;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);

        obj = new StylesheetShim(testURL);
        instance = new StylesheetShim(testURL);
        expResult = true;
        result = instance.equals(obj);
        assertEquals(expResult, result);

        obj = new StylesheetShim();
        instance = new StylesheetShim(testURL);
        expResult = false;
        result = instance.equals(obj);
        assertEquals(expResult, result);

        obj = instance = new StylesheetShim(testURL);
        expResult = true;
        result = instance.equals(obj);
        assertEquals(expResult, result);

    }

    /**
     * Test of toString method, of class Stylesheet.
     */
    @Test
    public void testStylesheetToString() {
        Stylesheet instance = new StylesheetShim();
        String expResult = "/*  */";
        String result = instance.toString();
        assertEquals(expResult, result);

        instance = new StylesheetShim(testURL);
        expResult = "/* " + testURL + " */";
        result = instance.toString();
        assertEquals(expResult, result);

        instance = new StylesheetShim(testURL);
        Rule rule = RuleShim.getRule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
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
        Stylesheet instance = new StylesheetShim(testURL);
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
            Stylesheet stylesheet = new CssParser().parse(data);

            StringStore stringStore = new StringStore();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            StylesheetShim.writeBinary(stylesheet, dos, stringStore);
            dos.flush();
            dos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            DataInputStream dis = new DataInputStream(bais);

            Stylesheet restored = new StylesheetShim();
            StylesheetShim.readBinary(restored, StylesheetShim.BINARY_CSS_VERSION, dis, stringStore.strings.toArray(new String[stringStore.strings.size()]));

            List<Rule> cssRules = stylesheet.getRules();
            List<Rule> bssRules = restored.getRules();

            // Rule does not have an equals method
            assert(cssRules.size() == bssRules.size());
            for (int n=0; n<cssRules.size(); n++) {
                Rule expected = cssRules.get(n);
                Rule actual = bssRules.get(n);
                assertEquals(Integer.toString(n),
                        RuleShim.getUnobservedDeclarationList(expected),
                        RuleShim.getUnobservedDeclarationList(actual));
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

            System.err.println(EXPECTED_WARNING);

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

            System.err.println(EXPECTED_WARNING);

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

            System.err.println(EXPECTED_WARNING);

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

            System.err.println(EXPECTED_WARNING);

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

            Stylesheet ss = new CssParser().parse(url);
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
        return test.javafx.css.CssParserTest.checkFontFace(stylesheet);
    }

   @Test
   public void testRT_37122() {
       try {
           URL url = StylesheetTest.class.getResource("RT-37122.css");
           File source = new File(url.toURI());
           File target = File.createTempFile("RT_37122_", "bss");
           Stylesheet.convertToBinary(source, target);
           Stylesheet.convertToBinary(source, target);
       } catch (URISyntaxException | IOException e) {
           fail(e.toString());
       }
   }

    @SuppressWarnings("deprecation")
    @Test
    public void testRT_37301() {
        try {
            File source = File.createTempFile("RT_37301_", "css");
            FileWriter writer = new FileWriter(source);
            writer.write("A:dir(rtl) {} B:dir(ltr) {} C {}");
            writer.flush();
            writer.close();
            File target = File.createTempFile("RT_37301_", "bss");
            Stylesheet.convertToBinary(source, target);
            Stylesheet stylesheet = Stylesheet.loadBinary(target.toURL());
            int good = 0;
            for (Rule rule : stylesheet.getRules()) {
                for (Selector sel : rule.getSelectors()) {
                    SimpleSelector simpleSelector = (SimpleSelector)sel;
                    if ("A".equals(simpleSelector.getName())) {
                        assertEquals(NodeOrientation.RIGHT_TO_LEFT, simpleSelector.getNodeOrientation());
                    } else if ("B".equals(simpleSelector.getName())) {
                        assertEquals(NodeOrientation.LEFT_TO_RIGHT, simpleSelector.getNodeOrientation());
                    } else if ("C".equals(simpleSelector.getName())) {
                        assertEquals(NodeOrientation.INHERIT, simpleSelector.getNodeOrientation());
                    } else {
                        fail(simpleSelector.toString());
                    }
                }
            }
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    private byte[] convertCssTextToBinary(String cssText) throws IOException {
        var stylesheet = new CssParser().parse(cssText);
        var stream = new ByteArrayOutputStream();
        var stringStore = new StringStore();
        StylesheetShim.writeBinary(stylesheet, new DataOutputStream(stream), stringStore);
        var stylesheetData = stream.toByteArray();
        stream = new ByteArrayOutputStream();
        var dataStream = new DataOutputStream(stream);
        dataStream.writeShort(StylesheetShim.BINARY_CSS_VERSION);
        stringStore.writeBinary(dataStream);
        dataStream.write(stylesheetData);
        return stream.toByteArray();
    }

    @Test
    public void testLoadBinaryStylesheetFromStream() throws IOException {
        byte[] stylesheetData = convertCssTextToBinary(".rect { -fx-fill: blue; }");

        var rules = Stylesheet.loadBinary(new ByteArrayInputStream(stylesheetData)).getRules();
        assertEquals(1, rules.size());

        var rule = rules.get(0);
        assertEquals(1, rule.getDeclarations().size());

        var decl = rule.getDeclarations().get(0);
        assertEquals("-fx-fill", decl.getProperty());
        assertEquals("0x0000ffff", decl.getParsedValue().getValue().toString());
    }

    @Test
    public void testLoadStylesheetFromDataURI() {
        var rect = new Rectangle();
        var root = new StackPane(rect);
        rect.getStyleClass().add("rect");

        // Stylesheet content: .rect { -fx-fill: blue; }
        root.getStylesheets().add("data:base64,LnJlY3QgeyAtZngtZmlsbDogYmx1ZTsgfQ==");
        Scene scene = new Scene(root);
        scene.getRoot().applyCss();

        assertEquals(Color.BLUE, rect.getFill());
    }

    @Test
    public void testLoadStylesheetFromTextCssDataURI() {
        var rect = new Rectangle();
        var root = new StackPane(rect);
        rect.getStyleClass().add("rect");

        // Stylesheet content: .rect { -fx-fill: blue; }
        root.getStylesheets().add("data:text/css;charset=utf-8;base64,LnJlY3QgeyAtZngtZmlsbDogYmx1ZTsgfQ==");
        Scene scene = new Scene(root);
        scene.getRoot().applyCss();

        assertEquals(Color.BLUE, rect.getFill());
    }

    @Test
    public void testLoadStylesheetFromBinaryDataURI() throws IOException {
        byte[] stylesheetData = convertCssTextToBinary(".rect { -fx-fill: blue; }");

        var rect = new Rectangle();
        var root = new StackPane(rect);
        rect.getStyleClass().add("rect");
        root.getStylesheets().add("data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(stylesheetData));
        Scene scene = new Scene(root);
        scene.getRoot().applyCss();

        assertEquals(Color.BLUE, rect.getFill());
    }

    @Test
    public void testLoadStylesheetFromDataURIFailsForUnsupportedMimeType() {
        var errors = StyleManager.errorsProperty();
        errors.clear();

        var rect = new Rectangle();
        var root = new StackPane(rect);
        rect.getStyleClass().add("rect");
        root.getStylesheets().add("data:text/html;base64,LnJlY3QgeyAtZngtZmlsbDogYmx1ZTsgfQ==");
        Scene scene = new Scene(root);
        scene.getRoot().applyCss();

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().startsWith("Unexpected MIME type \"text/html\""));
    }

    @Test
    public void testLoadStylesheetFromDataURIFailsForUnsupportedCharset() {
        var errors = StyleManager.errorsProperty();
        errors.clear();

        var rect = new Rectangle();
        var root = new StackPane(rect);
        rect.getStyleClass().add("rect");
        root.getStylesheets().add("data:charset=ABC-321;base64,LnJlY3QgeyAtZngtZmlsbDogYmx1ZTsgfQ==");
        Scene scene = new Scene(root);
        scene.getRoot().applyCss();

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().startsWith("Unsupported charset \"ABC-321\""));
    }

    @Test
    public void testStyleRevertsWhenDataURIStylesheetIsRemoved() {
        var rect = new Rectangle();
        var root = new StackPane(rect);
        rect.getStyleClass().add("rect");

        // Stylesheet content: .rect { -fx-fill: blue; }
        root.getStylesheets().add("data:base64,LnJlY3QgeyAtZngtZmlsbDogYmx1ZTsgfQ==");

        // Stylesheet content: .rect { -fx-fill: red; }
        String stylesheet = "data:base64,LnJlY3QgeyAtZngtZmlsbDogcmVkOyB9";
        root.getStylesheets().add(stylesheet);

        Scene scene = new Scene(root);
        scene.getRoot().applyCss();

        assertEquals(Color.RED, rect.getFill());

        root.getStylesheets().remove(stylesheet);
        scene.getRoot().applyCss();

        assertEquals(Color.BLUE, rect.getFill());
    }

}
