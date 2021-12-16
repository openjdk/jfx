/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.css;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;

import static org.junit.Assert.assertEquals;

public class PropertySizeTest {
    private HBox root, p1, p2, p3, p4;
    private Label l0, l1, l2, l3, l4;
    private final static double ROOT_FONT_SIZE = 200;

    private class Property {
        String style;
        double size;
        boolean isRelative;

        Property(String stylee) {
            style = stylee;
            if (!style.equals("")) {
                size = Double.parseDouble(style.substring(0, style.length() - 2));
                isRelative = style.substring(style.length() - 2).equals("em");
            }
        }

        double getValue(double fontSize) {
            if (!style.equals("")) {
                return isRelative ? fontSize * size : size;
            }
            return fontSize;
        }
    }

    private class TestLabel {

        private Label label;
        private Property padding, labelPadding;
        private Property minW, minH;
        private Property maxW, maxH;
        private Property prefW, prefH;
        private Property bgRadius, bgInsets;

        public TestLabel(Label label, String fontSize, String padding, String labelPadding,
                         String maxW, String maxH, String minW, String minH,
                         String prefW, String prefH, String bgRadius, String bgInsets) {

            this.label = label;
            this.padding = new Property(padding);
            this.labelPadding = new Property(labelPadding);
            this.minW = new Property(minW);
            this.minH = new Property(minH);
            this.maxW = new Property(maxW);
            this.maxH = new Property(maxH);
            this.prefW = new Property(prefW);
            this.prefH = new Property(prefH);
            this.bgRadius = new Property(bgRadius);
            this.bgInsets = new Property(bgInsets);

            String style = fontSize.equals("") ? "" : "-fx-font-size: " + fontSize + "; ";
            style += "-fx-padding: " + padding + ";  -fx-label-padding: " + labelPadding + ";" +
                    "-fx-max-width:  " + maxW + "; -fx-max-height:  " + maxH + ";" +
                    "-fx-min-width:  " + minW + "; -fx-min-height:  " + minH + ";" +
                    "-fx-pref-width: " + prefW + "; -fx-pref-height: " + prefH + ";" +
                    "-fx-background-color: red; -fx-background-radius: " + bgRadius + "; " +
                    "-fx-background-insets: " + bgInsets + ";";

            label.setStyle(style);
        }

        public void verifySizes() {
            root.applyCss();
            double fontSize = label.getFont().getSize();
            assertEquals("Incorrect padding", padding.getValue(fontSize), label.getPadding().getLeft(), 0.1);
            assertEquals("Incorrect labelPadding", labelPadding.getValue(fontSize), label.getLabelPadding().getLeft(), 0.1);
            assertEquals("Incorrect max width", maxW.getValue(fontSize), label.getMaxWidth(), 0.1);
            assertEquals("Incorrect min width", minW.getValue(fontSize), label.getMinWidth(), 0.1);
            assertEquals("Incorrect pref width", prefW.getValue(fontSize), label.getPrefWidth(), 0.1);
            assertEquals("Incorrect max Height", maxH.getValue(fontSize), label.getMaxHeight(), 0.1);
            assertEquals("Incorrect min height", minH.getValue(fontSize), label.getMinHeight(), 0.1);
            assertEquals("Incorrect pref height", prefH.getValue(fontSize), label.getPrefHeight(), 0.1);
            assertEquals("Incorrect background radius", bgRadius.getValue(fontSize),
                    label.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0.1);
            assertEquals("Incorrect background insets", bgInsets.getValue(fontSize),
                    label.getBackground().getFills().get(0).getInsets().getLeft(), 0.1);
        }
    }

    private void verifyFontSizes(double l0Font, double l1Font, double l2Font, double l3Font, double l4Font) {
        root.applyCss();
        assertEquals("l0 font size is incorrect.", l0Font, l0.getFont().getSize(), 0.1);
        assertEquals("l1 font size is incorrect.", l1Font, l1.getFont().getSize(), 0.1);
        assertEquals("l2 font size is incorrect.", l2Font, l2.getFont().getSize(), 0.1);
        assertEquals("l3 font size is incorrect.", l3Font, l3.getFont().getSize(), 0.1);
        assertEquals("l4 font size is incorrect.", l4Font, l4.getFont().getSize(), 0.1);
    }

    @Before
    public void setupTest() {
        l4 = new Label("L4");
        p4 = new HBox(l4);

        l3 = new Label("L3");
        p3 = new HBox(l3, p4);

        l2 = new Label("L2");
        p2 = new HBox(l2, p3);

        l1 = new Label("L1");
        p1 = new HBox(l1, p2);

        l0 = new Label("Text");
        root = new HBox(l0, p1);

        Scene scene = new Scene(root);

        root.setStyle("-fx-font-size: " + ROOT_FONT_SIZE + "px;");
    }

    // -fx-font-size tests - begin
    @Test
    public void defaultFontSizeTest() {
        root.setStyle("");
        double defFontSize = Font.getDefault().getSize();
        verifyFontSizes(defFontSize, defFontSize, defFontSize, defFontSize, defFontSize);
    }

    @Test
    public void absoluteFontSizeSetOnlyOnRootTest() {
        verifyFontSizes(ROOT_FONT_SIZE, ROOT_FONT_SIZE, ROOT_FONT_SIZE, ROOT_FONT_SIZE, ROOT_FONT_SIZE);
    }

    @Test
    public void absoluteFontSizeSetOnControlTest() {
        l1.setStyle("-fx-font-size: 20px");
        l3.setStyle("-fx-font-size: 30px");
        verifyFontSizes(ROOT_FONT_SIZE, 20, ROOT_FONT_SIZE, 30, ROOT_FONT_SIZE);
    }

    @Test
    public void relativeFontSizeNestedControlTest() {
        testRelativeFontSizeSetOnControl(0.9, 0.8, 0.7, 0.6, 0.5);
        testRelativeFontSizeSetOnControl(0.5, 0.5, 0.5, 0.5, 0.5);
        testRelativeFontSizeSetOnControl(1, 1, 1, 1, 1);
    }

    private void testRelativeFontSizeSetOnControl(double l0s, double l1s, double l2s, double l3s, double l4s) {
        l0.setStyle("-fx-font-size: " + l0s + "em");
        l1.setStyle("-fx-font-size: " + l1s + "em");
        l2.setStyle("-fx-font-size: " + l2s + "em");
        l3.setStyle("-fx-font-size: " + l3s + "em");
        l4.setStyle("-fx-font-size: " + l4s + "em");

        verifyFontSizes(ROOT_FONT_SIZE * l0s, ROOT_FONT_SIZE * l1s, ROOT_FONT_SIZE * l2s,
                ROOT_FONT_SIZE * l3s, ROOT_FONT_SIZE * l4s);
    }

    @Test
    public void relativeFontSizeSetOnNestedParentAndControlsExceptRootTest() {
        root.setStyle("-fx-font-size: " + ROOT_FONT_SIZE + "px;");
        p1.setStyle("-fx-font-size: 0.8em");
        p2.setStyle("-fx-font-size: 0.7em");
        p3.setStyle("-fx-font-size: 0.6em");
        p4.setStyle("-fx-font-size: 0.5em");

        double p1FontSize = ROOT_FONT_SIZE * 0.8;
        double p2FontSize = ROOT_FONT_SIZE * 0.7;
        double p3FontSize = p1FontSize * 0.6;
        double p4FontSize = p2FontSize * 0.5;

        l0.setStyle("-fx-font-size: 0.5em");
        l1.setStyle("-fx-font-size: 0.5em");
        l2.setStyle("-fx-font-size: 0.45em");
        l3.setStyle("-fx-font-size: 0.4em");
        l4.setStyle("-fx-font-size: 0.35em");

        verifyFontSizes(ROOT_FONT_SIZE * 0.5, p1FontSize * 0.5, p2FontSize * 0.45,
                p3FontSize * 0.4, p4FontSize * 0.35);
    }

    @Test
    public void relativeFontSizeSetOnAllNestedParentsAndControlsTest() {
        root.setStyle("-fx-font-size: 100em");
        p1.setStyle("-fx-font-size: 0.8em");
        p2.setStyle("-fx-font-size: 0.7em");
        p3.setStyle("-fx-font-size: 0.6em");
        p4.setStyle("-fx-font-size: 0.5em");

        // Ideally the relative font size of a parent should be relative to the font size of that parent's parent.
        // But the current behavior is that when -fx-font-size of a parent and its parent is specified as a
        // relative size then the font size of that parent is computed relative to font size of its grandparent.
        // For compatibility we are preserving this odd behavior.
        double defFontSize = Font.getDefault().getSize();
        double rootFontSize = defFontSize * 100;
        double p1FontSize = defFontSize * 0.8;
        double p2FontSize = rootFontSize * 0.7;
        double p3FontSize = p1FontSize * 0.6;
        double p4FontSize = p2FontSize * 0.5;

        l0.setStyle("-fx-font-size: 0.5em");
        l1.setStyle("-fx-font-size: 0.45em");
        l2.setStyle("-fx-font-size: 0.4em");
        l3.setStyle("-fx-font-size: 0.35em");
        l4.setStyle("-fx-font-size: 0.3em");

        verifyFontSizes(rootFontSize * 0.5, p1FontSize * 0.45, p2FontSize * 0.4,
                p3FontSize * 0.35, p4FontSize * 0.3);
    }

    @Ignore()
    @Test
    public void ideal_relativeFontSizeNestedParentControlTest() {
        root.setStyle("-fx-font-size: 0.9em");
        p1.setStyle("-fx-font-size: 0.8em");
        p2.setStyle("-fx-font-size: 0.7em");
        p3.setStyle("-fx-font-size: 0.6em");
        p4.setStyle("-fx-font-size: 0.5em");

        l0.setStyle("-fx-font-size: 0.25em");
        l1.setStyle("-fx-font-size: 0.25em");
        l2.setStyle("-fx-font-size: 0.25em");
        l3.setStyle("-fx-font-size: 0.25em");
        l4.setStyle("-fx-font-size: 0.25em");

        // The expected behavior of -fx-font-size calculation with nested set of parents is that
        // the font size of a parent is always calculated relative to font size of its parent.
        // But currently it is calculated relative to the font size of its grandparent.
        // We are not changing current behavior to avoid regressing any applications that rely on this behavior.
        // Current behavior can be observed in other -fx-font-size tests here.
        // see: relativeFontSizeSetOnAllNestedParentsAndControlsTest()
        double defFontSize = Font.getDefault().getSize();
        double rootFontSize = defFontSize * 0.9;
        double p1FontSize = rootFontSize * 0.8;
        double p2FontSize = p1FontSize * 0.7;
        double p3FontSize = p2FontSize * 0.6;
        double p4FontSize = p3FontSize * 0.5;

        verifyFontSizes(rootFontSize * 0.25, p1FontSize * 0.25, p2FontSize * 0.25,
                p3FontSize * 0.25, p4FontSize * 0.25);
    }

    // This test is an extension of relativeFontSizeSetOnNestedParentAndControlsExceptRootTest() and
    // relativeFontSizeSetOnAllNestedParentsAndControlsTest() to test combinations of -fx-font-size.
    @Test
    public void relativeFontSizeOfNestedParentsTest() {

        testFontSizeOfParents("", "", "", "", "");
        testFontSizeOfParents("", "0.9em", "0.7em", "0.5em", "0.3em");

        testFontSizeOfParents(ROOT_FONT_SIZE + "em", "", "", "", "");
        testFontSizeOfParents(ROOT_FONT_SIZE + "px", "", "", "", "");

        testFontSizeOfParents(ROOT_FONT_SIZE + "em", "100px", "80px", "60px", "40px");
        testFontSizeOfParents(ROOT_FONT_SIZE + "px", "100px", "80px", "60px", "40px");

        testFontSizeOfParents(ROOT_FONT_SIZE + "em", "0.9em", "0.8em", "0.7em", "0.6em");
        testFontSizeOfParents(ROOT_FONT_SIZE + "px", "0.9em", "0.8em", "0.7em", "0.6em");

        testFontSizeOfParents(ROOT_FONT_SIZE + "em", "0.9em", "80px", "0.6em", "40px");
        testFontSizeOfParents(ROOT_FONT_SIZE + "px", "0.9em", "80px", "0.6em", "40px");
    }

    // This is a specific combination where -fx-font-size of parents is same and test fails.
    // If we fix this then the test can be moved inside previous test.
    @Ignore()
    @Test
    public void sameRelativeFontSizeOfNestedParentsTest() {
        testFontSizeOfParents(ROOT_FONT_SIZE + "px", "0.5em", "0.5em",
                "0.5em", "0.5em");
    }

    private void testFontSizeOfParents(String rtSize, String p1Size, String p2Size, String p3Size, String p4Size) {

        Property rtFont = new Property(rtSize);
        Property p1Font = new Property(p1Size);
        Property p2Font = new Property(p2Size);
        Property p3Font = new Property(p3Size);
        Property p4Font = new Property(p4Size);

        String rtStyle = rtSize.equals("") ? "" : "-fx-font-size: " + rtSize;
        String p1Style = p1Size.equals("") ? "" : "-fx-font-size: " + p1Size;
        String p2Style = p2Size.equals("") ? "" : "-fx-font-size: " + p2Size;
        String p3Style = p3Size.equals("") ? "" : "-fx-font-size: " + p3Size;
        String p4Style = p4Size.equals("") ? "" : "-fx-font-size: " + p4Size;

        root.setStyle(rtStyle);
        p1.setStyle(p1Style);
        p2.setStyle(p2Style);
        p3.setStyle(p3Style);
        p4.setStyle(p4Style);

        double defFontSize = Font.getDefault().getSize();
        double rtFontSize = rtFont.getValue(defFontSize);
        double p1RefeFont = (rtFont.isRelative && p1Font.isRelative) ? defFontSize : rtFontSize;
        double p1FontSize = p1Font.getValue(p1RefeFont);

        double p2RefeFont = (p1Font.isRelative && p2Font.isRelative) ? rtFontSize : p1FontSize;
        double p2FontSize = p2Font.getValue(p2RefeFont);

        double p3RefeFont = (p2Font.isRelative && p3Font.isRelative) ? p1FontSize : p2FontSize;
        double p3FontSize = p3Font.getValue(p3RefeFont);

        double p4RefeFont = (p3Font.isRelative && p4Font.isRelative) ? p2FontSize : p3FontSize;
        double p4FontSize = p4Font.getValue(p4RefeFont);

        testFontSizeOfControls(rtFontSize, "0.55em", p1FontSize, "0.5em",
                p2FontSize, "0.45em",  p3FontSize, "0.4em", p4FontSize, "0.35em");

        testFontSizeOfControls(rtFontSize, "150px", p1FontSize, "0.5em",
                p2FontSize, "0.45em",  p3FontSize, "0.4em", p4FontSize, "0.35em");

        testFontSizeOfControls(rtFontSize, "150px", p1FontSize, "140px",
                p2FontSize, "0.45em",  p3FontSize, "0.4em", p4FontSize, "0.35em");

        testFontSizeOfControls(rtFontSize, "150px", p1FontSize, "140px",
                p2FontSize, "130px",  p3FontSize, "0.4em", p4FontSize, "0.35em");

        testFontSizeOfControls(rtFontSize, "150px", p1FontSize, "140px",
                p2FontSize, "130px",  p3FontSize, "120px", p4FontSize, "0.35em");

        testFontSizeOfControls(rtFontSize, "150px", p1FontSize, "140px",
                p2FontSize, "130px",  p3FontSize, "120px", p4FontSize, "110px");

        testFontSizeOfControls(rtFontSize, "0.55em", p1FontSize, "0.5em",
                p2FontSize, "35px", p3FontSize, "0.4em", p4FontSize, "20px");


        // @Ignore
        // Does not behave like the other tests above. Should be revisited if we plan to change -fx-font-size behavior.
        /*
        testFontSizeOfControls(rtFontSize, "0.55em", p1FontSize, "",
                p2FontSize, "35px", p3FontSize, "", p4FontSize, "20px");

        testFontSizeOfControls(rtFontSize, "", p1FontSize, "",
                p2FontSize, "", p3FontSize, "", p4FontSize, "");
         */
    }

    private void testFontSizeOfControls(double rtFontSize, String l0s,
                                        double p1FontSize, String l1s,
                                        double p2FontSize, String l2s,
                                        double p3FontSize, String l3s,
                                        double p4FontSize, String l4s) {

        Property l0Font = new Property(l0s);
        Property l1Font = new Property(l1s);
        Property l2Font = new Property(l2s);
        Property l3Font = new Property(l3s);
        Property l4Font = new Property(l4s);

        String l0FontStyle = l0s == "" ? "" : ("-fx-font-size: " + l0s);
        String l1FontStyle = l1s == "" ? "" : ("-fx-font-size: " + l1s);
        String l2FontStyle = l2s == "" ? "" : ("-fx-font-size: " + l2s);
        String l3FontStyle = l3s == "" ? "" : ("-fx-font-size: " + l3s);
        String l4FontStyle = l4s == "" ? "" : ("-fx-font-size: " + l4s);

        l0.setStyle(l0FontStyle);
        l1.setStyle(l1FontStyle);
        l2.setStyle(l2FontStyle);
        l3.setStyle(l3FontStyle);
        l4.setStyle(l4FontStyle);

        double l0FontSize = l0Font.getValue(rtFontSize);
        double l1FontSize = l1Font.getValue(p1FontSize);
        double l2FontSize = l2Font.getValue(p2FontSize);
        double l3FontSize = l3Font.getValue(p3FontSize);
        double l4FontSize = l4Font.getValue(p4FontSize);

        verifyFontSizes(l0FontSize, l1FontSize, l2FontSize, l3FontSize, l4FontSize);
    }
    // -fx-font-size tests - end


    // Test the following properties using Label control, to verify that
    // 1. The relative sized css properties are computed relative to
    // the -fx-font-size of that control. and,
    // 2. The absolute sized properties remain as specified.
    // -fx-padding,    -fx-label-padding
    // -fx-max-width,  -fx-min-width
    // -fx-max-height, -fx-min-height
    // -fx-pref-width, -fx-pref-height
    // -fx-background-radius, -fx-background-insets
    @Test
    public void absolutePropertySizeTest() {

        // absolute font size, absolute property sizes
        TestLabel testL1 = new TestLabel(l1, "20px", "5px", "6px", "200px", "100px",
                "198px", "98px", "199px", "99px", "5px", "3px");

        // relative font size, absolute property sizes
        TestLabel testL2 = new TestLabel(l2, "0.5em", "10px", "11px", "210px", "110px",
                "208px", "108px", "209px", "109px", "4px", "2px");

        testL1.verifySizes();
        testL2.verifySizes();
    }

    @Test
    public void relativePropertySizeTest() {

        // relative font size, relative property sizes
        TestLabel testL1 = new TestLabel(l1, "0.5em", "0.5em", "0.25em", "20em", "10em",
                "18em", "8em", "19em", "9em", "0.2em", "0.1em");

        // absolute font size, relative property sizes
        TestLabel testL2 = new TestLabel(l2, "20px", "0.25em", "0.125em", "17em", "7em",
                "15em", "5em", "16em", "6em", "0.1em", "0.05em");

        testL1.verifySizes();
        testL2.verifySizes();
    }

    @Test
    public void propertySizesCombinationTest() {
        verifyCombinationsWithParentFontSizes("", "", "", "", "");
        verifyCombinationsWithParentFontSizes("200px", "", "", "", "");
        verifyCombinationsWithParentFontSizes("20em", "", "", "", "");

        verifyCombinationsWithParentFontSizes("", "0.9em", "0.8em", "0.7em", "0.6em");
        verifyCombinationsWithParentFontSizes("", "300px", "0.8em", "0.7em", "0.6em");
        verifyCombinationsWithParentFontSizes("", "0.9em", "0.8em", "100px", "0.6em");
        verifyCombinationsWithParentFontSizes("", "180px", "160px", "140px", "120px");

        verifyCombinationsWithParentFontSizes("200px", "0.9em", "0.8em", "0.7em", "0.6em");
        verifyCombinationsWithParentFontSizes("200px", "300px", "0.8em", "0.7em", "0.6em");
        verifyCombinationsWithParentFontSizes("200px", "0.9em", "0.8em", "100px", "0.6em");
        verifyCombinationsWithParentFontSizes("200px", "180px", "160px", "140px", "120px");

        verifyCombinationsWithParentFontSizes("20em", "0.9em", "0.8em", "0.7em", "0.6em");
        verifyCombinationsWithParentFontSizes("20em", "300px", "0.8em", "0.7em", "0.6em");
        verifyCombinationsWithParentFontSizes("20em", "0.9em", "0.8em", "100px", "0.6em");
        verifyCombinationsWithParentFontSizes("20em", "180px", "160px", "140px", "120px");
    }

    private void verifyCombinationsWithParentFontSizes(String rootFont, String p1Font,
                                                       String p2Font, String p3Font, String p4Font) {

        String rootStyle = rootFont.equals("") ? "" : "-fx-font-size: " + rootFont;
        String p1Style = p1Font.equals("") ? "" : "-fx-font-size: " + p1Font;
        String p2Style = p2Font.equals("") ? "" : "-fx-font-size: " + p2Font;
        String p3Style = p3Font.equals("") ? "" : "-fx-font-size: " + p3Font;
        String p4Style = p4Font.equals("") ? "" : "-fx-font-size: " + p4Font;

        root.setStyle(rootStyle);
        p1.setStyle(p1Style);
        p2.setStyle(p2Style);
        p3.setStyle(p3Style);
        p4.setStyle(p4Style);

        verifyCombinationsOfChildrenProperties1();
        verifyCombinationsOfChildrenProperties2();
    }

    TestLabel testL0, testL1, testL2, testL3, testL4;

    private void verifyCombinationsOfChildrenProperties1() {
        testL0 = new TestLabel(l0, "0.5em", "0.5em", "0.5em", "20em",
                "10em", "18em", "8em", "19em", "9em", "0.2em", "0.1em");

        testL1 = new TestLabel(l1, "0.5em", "0.5em", "0.25em", "20em",
                "10em", "18em", "8em", "19em", "9em", "0.2em", "0.1em");

        testL2 = new TestLabel(l2, "0.5em", "0.5em", "0.25em", "20em",
                "10em", "18em", "8em", "19em", "9em", "0.2em", "0.1em");

        testL3 = new TestLabel(l3, "0.5em", "0.5em", "0.25em", "20em",
                "10em", "18em", "8em", "19em", "9em", "0.2em", "0.1em");

        testL4 = new TestLabel(l4, "0.5em", "0.5em", "0.25em", "20em",
                "10em", "18em", "8em", "19em", "9em", "0.2em", "0.1em");

        verifyLabelSizes();
    }

    private void verifyCombinationsOfChildrenProperties2() {
        testL0 = new TestLabel(l0, "0.5em", "0.5em", "0.5em", "20em",
                "10em", "18em", "8em", "19em", "9em", "0.2em", "0.1em");

        testL1 = new TestLabel(l1, "100px", "5px", "2.5px", "90px",
                "10em", "50px", "10px", "19px", "9em", "0.2em", "0.1em");

        testL2 = new TestLabel(l2, "0.5em", "0.5em", "0.25em", "20em",
                "10em", "40px", "8em", "19em", "9em", "6px", "0.1em");

        testL3 = new TestLabel(l3, "200px", "0.5em", "0.25em", "120px",
                "60px", "110px", "40px", "100px", "50px", "0.2em", "2px");

        testL4 = new TestLabel(l4, "200px", "5px", "4px", "180px",
                "30px", "40px", "10px", "120px", "35px", "6px", "2px");

        verifyLabelSizes();
    }

    private void verifyLabelSizes() {
        testL0.verifySizes();
        testL1.verifySizes();
        testL2.verifySizes();
        testL3.verifySizes();
        testL4.verifySizes();
    }
}
