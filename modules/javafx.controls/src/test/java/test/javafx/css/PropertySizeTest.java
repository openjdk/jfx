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

package test.javafx.css;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;

public class PropertySizeTest {
    private HBox root, p1, p2;
    private Label l0, l1, l2;
    private final static int ROOT_FONT_SIZE = 48;

    @Before
    public void setupTest() {
        l2 = new Label("L2");
        p2 = new HBox(l2);

        l1 = new Label("L1");
        p1 = new HBox(l1, p2);

        l0 = new Label("Text");
        root = new HBox(l0, p1);

        Scene scene = new Scene(root);

        root.setStyle("-fx-font-size: " + ROOT_FONT_SIZE + "px;");
        root.applyCss();
    }

    // -fx-font-size tests
    @Test
    public void defaultFontSizeTest() {
        root.setStyle("");
        root.applyCss();
        assertEquals(Font.getDefault().getSize(), l0.getFont().getSize(), 0);
        assertEquals(Font.getDefault().getSize(), l1.getFont().getSize(), 0);
        assertEquals(Font.getDefault().getSize(), l2.getFont().getSize(), 0);
    }

    @Test
    public void absoluteFontSizeSetOnRootTest() {
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE, l1.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE, l2.getFont().getSize(), 0);
    }

    @Test
    public void absoluteFontSizeSetOnControlTest() {
        l1.setStyle("-fx-font-size: 24px");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);
        assertEquals(24, l1.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE, l2.getFont().getSize(), 0);
    }

    @Test
    public void relativeFontSizeNestedParentTest() {
        p1.setStyle("-fx-font-size: 0.5em");
        p2.setStyle("-fx-font-size: 0.25em");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE * 0.5, l1.getFont().getSize(), 0);
        assertEquals(Font.getDefault().getSize() * 0.25, l2.getFont().getSize(), 0); // def * 0.25
    }

    @Ignore()
    @Test
    public void sameRelativeFontSizeNestedParentTest() {
        p1.setStyle("-fx-font-size: 0.5em");
        p2.setStyle("-fx-font-size: 0.5em");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0); // 48
        assertEquals(ROOT_FONT_SIZE * 0.5, l1.getFont().getSize(), 0); // 24
        // Compared to previous test relativeFontSizeNestedParentTest, there is only
        // one difference in this test: font size of p2 is 0.5em which is same as of p1.
        // In previous test where the font sizes are different for p1 and p2,
        // the font size of l2 is relative to default font size but
        // here in this test, p1 and p2 have same font size.
        // Issue is: Unlike previous test, font size of l2 is not relative to default
        // font size instead it is relative to the font size of root.
        // expected default font size * 0.5 but actual is ROOT_FONT_SIZE * 0.5
        assertEquals(Font.getDefault().getSize() * 0.5, l2.getFont().getSize(), 0);
    }

    @Test
    public void relativeFontSizeNestedControlTest() {
        l1.setStyle("-fx-font-size: 0.5em");
        l2.setStyle("-fx-font-size: 0.25em");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE * 0.50, l1.getFont().getSize(), 0); // 24
        assertEquals(ROOT_FONT_SIZE * 0.25, l2.getFont().getSize(), 0); // 12
    }

    @Test
    public void sameRelativeFontSizeNestedControlTest() {
        l1.setStyle("-fx-font-size: 0.5em");
        l2.setStyle("-fx-font-size: 0.5em");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE * 0.5, l1.getFont().getSize(), 0); // 24
        assertEquals(ROOT_FONT_SIZE * 0.5, l2.getFont().getSize(), 0); // 24
    }

    @Test
    public void relativeFontSizeNestedParentControlTest() {
        p1.setStyle("-fx-font-size: 0.75em"); // 36
        p2.setStyle("-fx-font-size: 0.50em"); // 24

        l1.setStyle("-fx-font-size: 0.25em"); // 9
        l2.setStyle("-fx-font-size: 0.25em"); // 6

        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE * 0.75 * 0.25, l1.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE * 0.50 * 0.25, l2.getFont().getSize(), 0);
    }

    @Ignore()
    @Test
    public void relativeFontSizeDeepNestedParentControlTest() {
        Label l4 = new Label("L4");
        HBox p4 = new HBox(l4);

        Label l3 = new Label("L3");
        HBox p3 = new HBox(l3, p4);

        p2.getChildren().add(p3);

        p1.setStyle("-fx-font-size: 0.75em"); // 36
        p2.setStyle("-fx-font-size: 0.50em"); // 24
        p3.setStyle("-fx-font-size: 0.334em");// ~16
        p4.setStyle("-fx-font-size: 0.25em"); // 12

        l1.setStyle("-fx-font-size: 0.25em"); // 9
        l2.setStyle("-fx-font-size: 0.25em"); // 6
        l3.setStyle("-fx-font-size: 0.25em"); // ~4
        l4.setStyle("-fx-font-size: 0.25em"); // 3

        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE * 0.75 * 0.25,  l1.getFont().getSize(), 0);
        assertEquals(ROOT_FONT_SIZE * 0.50 * 0.25,  l2.getFont().getSize(), 0);
        //expected 4 but is 3 which is l1.getFontSize() * 0.334 => 9 * 0.334
        assertEquals(ROOT_FONT_SIZE * 0.334 * 0.25, l3.getFont().getSize(), 0.1);
        //expected 3 but is 1.5 which is l2.getFontSize() * 0.25 => 6 * 0.25
        assertEquals(ROOT_FONT_SIZE * 0.25 * 0.25,  l4.getFont().getSize(), 0.1);
    }

    // Test the following properties using Label, to verify that
    // 1. The relative size of css properties of a control are computed relative to
    // the -fx-font-size of that control. and,
    // 2. The absolute sized properties remain as specified.
    // -fx-padding,    -fx-label-padding
    // -fx-max-width,  -fx-min-width
    // -fx-max-height, -fx-min-height
    // -fx-pref-width, -fx-pref-height
    // -fx-background-radius, -fx-background-insets
    @Test
    public void absoluteSizePropertiesTest() {
        l1.setStyle("-fx-font-size:  20px; -fx-padding:  5px; -fx-label-padding:  6px;" +
                "-fx-max-width:  200px; -fx-max-height: 100px;" +
                "-fx-min-width:  198px; -fx-min-height:  98px;" +
                "-fx-pref-width: 199px; -fx-pref-height: 99px;" +
                "-fx-background-color: red; -fx-background-radius: 5px; -fx-background-insets: 3px;");
        l2.setStyle("-fx-font-size: 0.5em; -fx-padding: 10px; -fx-label-padding: 11px;" +
                "-fx-max-width:  210px; -fx-max-height:  110px;" +
                "-fx-min-width:  208px; -fx-min-height:  108px;" +
                "-fx-pref-width: 209px; -fx-pref-height: 109px;" +
                "-fx-background-color: red; -fx-background-radius: 4px; -fx-background-insets: 2px;");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);

        assertEquals(20,  l1.getFont().getSize(), 0);
        assertEquals(5,   l1.getPadding().getLeft(), 0);
        assertEquals(6,   l1.getLabelPadding().getLeft(), 0);
        assertEquals(200, l1.getMaxWidth(), 0);
        assertEquals(198, l1.getMinWidth(), 0);
        assertEquals(199, l1.getPrefWidth(), 0);
        assertEquals(100, l1.getMaxHeight(), 0);
        assertEquals(98,  l1.getMinHeight(), 0);
        assertEquals(99,  l1.getPrefHeight(), 0);
        assertEquals(5,   l1.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0);
        assertEquals(3,   l1.getBackground().getFills().get(0).getInsets().getLeft(), 0);

        assertEquals(24,  l2.getFont().getSize(), 0);
        assertEquals(10,  l2.getPadding().getLeft(), 0);
        assertEquals(11,  l2.getLabelPadding().getLeft(), 0);
        assertEquals(210, l2.getMaxWidth(), 0);
        assertEquals(208, l2.getMinWidth(), 0);
        assertEquals(209, l2.getPrefWidth(), 0);
        assertEquals(110, l2.getMaxHeight(), 0);
        assertEquals(108, l2.getMinHeight(), 0);
        assertEquals(109, l2.getPrefHeight(), 0);
        assertEquals(4,   l2.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0);
        assertEquals(2,   l2.getBackground().getFills().get(0).getInsets().getLeft(), 0);
    }

    @Test
    public void propertySizesRelativeToFontSizeOfControlTest() {
        l1.setStyle("-fx-font-size: 0.5em; -fx-padding: 0.5em;  -fx-label-padding: 0.25em;" +
                "-fx-max-width:  20em; -fx-max-height:  10em;" +
                "-fx-min-width:  18em; -fx-min-height:  8em;" +
                "-fx-pref-width: 19em; -fx-pref-height: 9em;" +
                "-fx-background-color: red; -fx-background-radius: 0.2em; -fx-background-insets: 0.1em;");
        l2.setStyle("-fx-font-size:  20px; -fx-padding: 0.25em; -fx-label-padding: 0.2em;" +
                "-fx-max-width:  20em; -fx-max-height:  10em;" +
                "-fx-min-width:  18em; -fx-min-height:  8em;" +
                "-fx-pref-width: 19em; -fx-pref-height: 9em;" +
                "-fx-background-color: red; -fx-background-radius: 0.1em; -fx-background-insets: 0.05em;");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);

        double l1FontSize = ROOT_FONT_SIZE * 0.5;
        assertEquals(l1FontSize, l1.getFont().getSize(), 0);
        assertEquals(l1FontSize * 0.5, l1.getPadding().getLeft(), 0);
        assertEquals(l1FontSize * 0.25,  l1.getLabelPadding().getLeft(), 0);
        assertEquals(l1FontSize * 20, l1.getMaxWidth(), 0);
        assertEquals(l1FontSize * 18, l1.getMinWidth(), 0);
        assertEquals(l1FontSize * 19, l1.getPrefWidth(), 0);
        assertEquals(l1FontSize * 10, l1.getMaxHeight(), 0);
        assertEquals(l1FontSize * 8,  l1.getMinHeight(), 0);
        assertEquals(l1FontSize * 9,  l1.getPrefHeight(), 0);
        assertEquals(l1FontSize * 0.2, l1.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0.01);
        assertEquals(l1FontSize * 0.1, l1.getBackground().getFills().get(0).getInsets().getLeft(), 0.01);

        double l2FontSize = 20;
        assertEquals(l2FontSize, l2.getFont().getSize(), 0);
        assertEquals(5,  l2.getPadding().getLeft(), 0);
        assertEquals(4,  l2.getLabelPadding().getLeft(), 0);
        assertEquals(l2FontSize * 20, l2.getMaxWidth(), 0);
        assertEquals(l2FontSize * 18, l2.getMinWidth(), 0);
        assertEquals(l2FontSize * 19, l2.getPrefWidth(), 0);
        assertEquals(l2FontSize * 10, l2.getMaxHeight(), 0);
        assertEquals(l2FontSize * 8,  l2.getMinHeight(), 0);
        assertEquals(l2FontSize * 9,  l2.getPrefHeight(), 0);
        assertEquals(l2FontSize * 0.1, l2.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0);
        assertEquals(l2FontSize * 0.05, l2.getBackground().getFills().get(0).getInsets().getLeft(), 0.01);
    }

    @Test
    public void propertySizesRelativeToFontSizeOfParentTest() {
        l1.setStyle("-fx-padding: 0.5em;   -fx-label-padding: 0.25em;" +
                "-fx-max-width:  20em; -fx-max-height:  10em;" +
                "-fx-min-width:  18em; -fx-min-height:  8em;" +
                "-fx-pref-width: 19em; -fx-pref-height: 9em;" +
                "-fx-background-color: red; -fx-background-radius: 0.2em; -fx-background-insets: 0.1em;");
        p2.setStyle("-fx-font-size: 0.5em; -fx-label-padding: 0.25em;" +
                "-fx-max-width:  20em; -fx-max-height:  10em;" +
                "-fx-min-width:  18em; -fx-min-height:  8em;" +
                "-fx-pref-width: 19em; -fx-pref-height: 9em;" +
                "-fx-background-color: red; -fx-background-radius: 0.15em; -fx-background-insets: 0.075em;");
        l2.setStyle("-fx-padding: 0.25em;  -fx-label-padding: 0.125em;" +
                "-fx-max-width:  17em; -fx-max-height:  7em;" +
                "-fx-min-width:  15em; -fx-min-height:  5em;" +
                "-fx-pref-width: 16em; -fx-pref-height: 6em;" +
                "-fx-background-color: red; -fx-background-radius: 0.1em; -fx-background-insets: 0.05em;");
        root.applyCss();
        assertEquals(ROOT_FONT_SIZE, l0.getFont().getSize(), 0);

        double l1FontSize = ROOT_FONT_SIZE;
        assertEquals(l1FontSize, l1.getFont().getSize(), 0);
        assertEquals(l1FontSize * 0.5, l1.getPadding().getLeft(), 0);
        assertEquals(l1FontSize * 0.25, l1.getLabelPadding().getLeft(), 0);
        assertEquals(l1FontSize * 20, l1.getMaxWidth(), 0);
        assertEquals(l1FontSize * 18, l1.getMinWidth(), 0);
        assertEquals(l1FontSize * 19, l1.getPrefWidth(), 0);
        assertEquals(l1FontSize * 10, l1.getMaxHeight(), 0);
        assertEquals(l1FontSize * 8,  l1.getMinHeight(), 0);
        assertEquals(l1FontSize * 9,  l1.getPrefHeight(), 0);
        assertEquals(l1FontSize * 0.2, l1.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0.01);
        assertEquals(l1FontSize * 0.1, l1.getBackground().getFills().get(0).getInsets().getLeft(), 0.01);

        double l2FontSize = ROOT_FONT_SIZE * 0.5;
        assertEquals(l2FontSize, l2.getFont().getSize(), 0);
        assertEquals(l2FontSize * 0.25,  l2.getPadding().getLeft(), 0);
        assertEquals(l2FontSize * 0.125,  l2.getLabelPadding().getLeft(), 0);
        assertEquals(l2FontSize * 17, l2.getMaxWidth(), 0);
        assertEquals(l2FontSize * 15, l2.getMinWidth(), 0);
        assertEquals(l2FontSize * 16, l2.getPrefWidth(), 0);
        assertEquals(l2FontSize * 7,  l2.getMaxHeight(), 0);
        assertEquals(l2FontSize * 5,  l2.getMinHeight(), 0);
        assertEquals(l2FontSize * 6,  l2.getPrefHeight(), 0);
        assertEquals(l2FontSize * 0.1, l2.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0.01);
        assertEquals(l2FontSize * 0.05, l2.getBackground().getFills().get(0).getInsets().getLeft(), 0.01);
    }
}
