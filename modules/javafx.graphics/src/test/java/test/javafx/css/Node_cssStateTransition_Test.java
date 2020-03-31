/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import javafx.css.CssMetaData;
import javafx.css.CssParser;
import javafx.css.PseudoClass;
import javafx.css.StyleableProperty;
import javafx.css.Stylesheet;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class Node_cssStateTransition_Test {

    public Node_cssStateTransition_Test() {
    }

    private static void resetStyleManager() {
        StyleManager sm = StyleManager.getInstance();
        sm.userAgentStylesheetContainers.clear();
        sm.platformUserAgentStylesheetContainers.clear();
        sm.stylesheetContainerMap.clear();
        sm.cacheContainerMap.clear();
        sm.hasDefaultUserAgentStylesheet = false;
    }

    @Before
    public void setUp() {
        resetStyleManager();
    }

    @AfterClass
    public static void cleanupOnce() {
        resetStyleManager();
    }

    @Test
    public void testPropertiesResetOnStyleclassChange() {

        Rectangle rect = new Rectangle(50,50);
        Paint defaultFill = rect.getFill();
        Paint defaultStroke = rect.getStroke();
        Double defaultStrokeWidth = Double.valueOf(rect.getStrokeWidth());

        CssMetaData metaData = ((StyleableProperty)rect.fillProperty()).getCssMetaData();
        assertEquals(defaultFill, metaData.getInitialValue(rect));
        metaData = ((StyleableProperty)rect.strokeProperty()).getCssMetaData();
        assertEquals(defaultStroke, metaData.getInitialValue(rect));
        metaData = ((StyleableProperty)rect.strokeWidthProperty()).getCssMetaData();
        assertEquals(defaultStrokeWidth, metaData.getInitialValue(rect));

        Stylesheet stylesheet = null;
        try {
            // Note: setDefaultUserAgentStylesheet in StyleManager won't replace the UA stylesheet unless it has a name,
            //       and that name needs to be different from the current one, if any. This matters when running
            //       these tests from the same VM since StyleManager is a singleton.
            stylesheet = new CssParser().parse(
                    "testPropertiesResetOnStyleclassChange",
                    ".rect { -fx-fill: red; -fx-stroke: yellow; -fx-stroke-width: 3px; }" +
                            ".rect.green { -fx-fill: green; }" +
                            ".green { -fx-stroke: green; }"

            );
        } catch(IOException ioe) {
            fail();
        }

        rect.getStyleClass().add("rect");

        Group root = new Group();
        root.getChildren().add(rect);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        Scene scene = new Scene(root);

        root.applyCss();

        assertEquals(Color.RED, rect.getFill());
        assertEquals(Color.YELLOW, rect.getStroke());
        assertEquals(3d, rect.getStrokeWidth(), 1e-6);

        rect.getStyleClass().add("green");
        root.applyCss();

        assertEquals(Color.GREEN, rect.getFill());
        assertEquals(Color.GREEN, rect.getStroke());
        assertEquals(3d, rect.getStrokeWidth(), 1e-6);

        rect.getStyleClass().remove("rect");
        root.applyCss();

        assertEquals(defaultFill, rect.getFill());
        assertEquals(Color.GREEN, rect.getStroke());
        assertEquals(defaultStrokeWidth.doubleValue(), rect.getStrokeWidth(), 1e-6);

        rect.getStyleClass().remove("green");
        root.applyCss();

        assertEquals(defaultFill, rect.getFill());
        assertEquals(defaultStroke, rect.getStroke());
        assertEquals(defaultStrokeWidth.doubleValue(), rect.getStrokeWidth(), 1e-6);
    }

    @Test
    public void testPropertiesResetOnPsedudoClassStateChange() {

        Rectangle rect = new Rectangle(50,50);
        Paint defaultFill = rect.getFill();
        Paint defaultStroke = rect.getStroke();
        Double defaultStrokeWidth = Double.valueOf(rect.getStrokeWidth());

        CssMetaData metaData = ((StyleableProperty)rect.fillProperty()).getCssMetaData();
        assertEquals(defaultFill, metaData.getInitialValue(rect));
        metaData = ((StyleableProperty)rect.strokeProperty()).getCssMetaData();
        assertEquals(defaultStroke, metaData.getInitialValue(rect));
        metaData = ((StyleableProperty)rect.strokeWidthProperty()).getCssMetaData();
        assertEquals(defaultStrokeWidth, metaData.getInitialValue(rect));

        Stylesheet stylesheet = null;
        try {
            // Note: setDefaultUserAgentStylesheet in StyleManager won't replace the UA stylesheet unless it has a name,
            //       and that name needs to be different from the current one, if any. This matters when running
            //       these tests from the same VM since StyleManager is a singleton.
            stylesheet = new CssParser().parse(
                "testPropertiesResetOnPsedudoClassStateChange",
                ".rect:hover { -fx-fill: red; -fx-stroke: yellow; -fx-stroke-width: 3px; }" +
                ".rect:hover:focused { -fx-fill: green; }" +
                ".rect:focused { -fx-stroke: green; }"

            );
        } catch(IOException ioe) {
            fail();
        }

        rect.getStyleClass().add("rect");

        Group root = new Group();
        root.getChildren().add(rect);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        Scene scene = new Scene(root);

        root.applyCss();

        assertEquals(defaultFill, rect.getFill());
        assertEquals(defaultStroke, rect.getStroke());
        assertEquals(defaultStrokeWidth, rect.getStrokeWidth(), 1e-6);

        rect.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        root.applyCss();

        assertEquals(Color.RED, rect.getFill());
        assertEquals(Color.YELLOW, rect.getStroke());
        assertEquals(3d, rect.getStrokeWidth(), 1e-6);

        rect.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
        root.applyCss();

        assertEquals(Color.GREEN, rect.getFill());
        assertEquals(Color.GREEN, rect.getStroke());
        assertEquals(3d, rect.getStrokeWidth(), 1e-6);

        rect.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);
        root.applyCss();

        assertEquals(defaultFill, rect.getFill());
        assertEquals(Color.GREEN, rect.getStroke());
        assertEquals(defaultStrokeWidth.doubleValue(), rect.getStrokeWidth(), 1e-6);

        rect.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false);
        root.applyCss();

        assertEquals(defaultFill, rect.getFill());
        assertEquals(defaultStroke, rect.getStroke());
        assertEquals(defaultStrokeWidth.doubleValue(), rect.getStrokeWidth(), 1e-6);

    }

}
