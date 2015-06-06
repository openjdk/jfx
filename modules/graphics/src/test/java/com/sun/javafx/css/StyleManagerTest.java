/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.parser.CSSParser;
import javafx.css.StyleOrigin;
import javafx.css.StyleableProperty;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 *
 * @author dgrieve
 */
public class StyleManagerTest {
    
    public StyleManagerTest() {
    }

    @Before
    public void setUp() {
        StyleManager sm = StyleManager.getInstance();
        sm.userAgentStylesheetContainers.clear();
        sm.platformUserAgentStylesheetContainers.clear();
        sm.stylesheetContainerMap.clear();
        sm.cacheContainerMap.clear();
        sm.hasDefaultUserAgentStylesheet = false;
    }
    
    @Test
    public void testMethod_getInstance() {
        Scene scene = new Scene(new Group());
        StyleManager sm = StyleManager.getInstance();
        assertNotNull(sm);
    }

    private static int indexOf(final List<StyleManager.StylesheetContainer> list, final String fname) {

        for (int n=0, nMax=list.size(); n<nMax; n++) {
            StyleManager.StylesheetContainer container = list.get(n);
            if (fname.equals(container.fname)) {
                return n;
            }
        }

        return -1;
    }

    @Test
    public void testAddUserAgentStyleshseet_String() {
        StyleManager sm = StyleManager.getInstance();
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua0.css");
        int index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

    }

    @Test
    public void testAddUserAgentStyleshseet_String_Multiple() {
        StyleManager sm = StyleManager.getInstance();
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua0.css");
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        int index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua1.css");
        assertEquals(1, index);
    }

    @Test
    public void testAddUserAgentStyleshseet_String_Duplicate() {
        StyleManager sm = StyleManager.getInstance();
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua0.css");
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        assertTrue(sm.platformUserAgentStylesheetContainers.size() == 2);

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(1, index);

    }

    @Test
    public void testSetDefaultUserAgentStyleshseet_String() {
        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        int index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua0.css");
        assertEquals(0, index);
    }

    @Test
    public void testSetUserAgentStyleshseet_String_Multiple() {
        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        assertTrue(sm.platformUserAgentStylesheetContainers.size() == 2);

        int index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua1.css");
        assertEquals(1, index);
    }

    @Test
    public void testSetUserAgentStyleshseet_String_Multiple2() {
        StyleManager sm = StyleManager.getInstance();
        // same as before but set default after adding another.
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        assertEquals(2, sm.platformUserAgentStylesheetContainers.size());

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(1, index);
    }

    @Test
    public void testSetUserAgentStyleshseet_String_Duplicate() {
        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        assertEquals(2, sm.platformUserAgentStylesheetContainers.size());

        int index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua1.css");
        assertEquals(1, index);
    }

    @Test
    public void testAddUserAgentStyleshseet_Stylesheet() {

        try {
            StyleManager sm = StyleManager.getInstance();
            URL ua0_url = StyleManagerTest.class.getResource("ua0.css");
            Stylesheet stylesheet = CSSParser.getInstance().parse(ua0_url);
            sm.addUserAgentStylesheet(null,stylesheet);

            assertEquals(1, sm.platformUserAgentStylesheetContainers.size());

            int index = indexOf(sm.platformUserAgentStylesheetContainers,ua0_url.toExternalForm());
            assertEquals(0, index);

        } catch (Exception ioe) {
            fail(ioe.getMessage());
        }

    }

    @Test
    public void testSetDefaultUserAgentStyleshseet_Stylesheet() {

        try {
            StyleManager sm = StyleManager.getInstance();

            URL ua1_url = StyleManagerTest.class.getResource("ua1.css");
            Stylesheet stylesheet = CSSParser.getInstance().parse(ua1_url);
            sm.addUserAgentStylesheet(null,stylesheet);

            URL ua0_url = StyleManagerTest.class.getResource("ua0.css");
            stylesheet = CSSParser.getInstance().parse(ua0_url);
            sm.setDefaultUserAgentStylesheet(stylesheet);

            assertEquals(2, sm.platformUserAgentStylesheetContainers.size());

            int index = indexOf(sm.platformUserAgentStylesheetContainers,ua0_url.toExternalForm());
            assertEquals(0, index);

            index = indexOf(sm.platformUserAgentStylesheetContainers,ua1_url.toExternalForm());
            assertEquals(1, index);

        } catch (Exception ioe) {
            fail(ioe.getMessage());
        }

    }

    @Test
    public void testSceneUAStylesheetAdded() {
        Scene scene = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        // the Scene user-agent stylesheet is not a platform user-agent stylesheet
        index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

    }

    @Test
    public void testSubSceneUAStylesheetAdded() {
        Scene scene = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        // the Scene user-agent stylesheet is not a platform user-agent stylesheet
        index = indexOf(sm.platformUserAgentStylesheetContainers, "/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

    }

    @Test
    public void testForgetParent() {

        Scene scene = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        sm.forget(scene.getRoot());

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

    }

    @Test
    public void testForgetParent_withSceneUAStylesheet() {

        Scene scene = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

        sm.forget(scene.getRoot());

        // forgetting the parent should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        // only forgetting the scene should affect the platform user-agent stylesheets
        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0, index);

    }

    @Test
    public void testForgetParent_withTwoScenes() {
        Scene scene0 = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene0.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        Scene scene1 = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene1.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene0.getRoot().applyCss();
        scene1.getRoot().applyCss();

        // even though there are two scenes using this stylesheet, there should only be one container.
        assertEquals(1, sm.userAgentStylesheetContainers.size());

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0, index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

        sm.forget(scene0.getRoot());

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        // we should still have ua1.css since scene1 is still using it
        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        sm.forget(scene1.getRoot());

        // only forgetting the scene should affect the platform user-agent stylesheets
        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0, index);
    }

    @Test
    public void testForgetParent_withParentStylesheet() {

        Scene scene = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene.getRoot().getStylesheets().add("/com/sun/javafx/css/ua1.css");

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0, index);

        assertTrue(sm.userAgentStylesheetContainers.isEmpty());
        assertTrue(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua1.css"));

        sm.forget(scene.getRoot());

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0, index);

        assertFalse(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua1.css"));

    }

    @Test
    public void testForgetParent_withMultipleParentStylesheets() {

        final Parent parent0 = new Pane(new Rectangle(){{ getStyleClass().add("rect"); }});
        parent0.getStylesheets().add("/com/sun/javafx/css/ua1.css");

        final Parent parent1 = new Pane(new Rectangle(){{ getStyleClass().add("rect"); }});
        parent1.getStylesheets().add("/com/sun/javafx/css/ua1.css");

        Scene scene = new Scene(new Group(parent0, parent1));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        assertTrue(sm.userAgentStylesheetContainers.isEmpty());

        StyleManager.StylesheetContainer container = sm.stylesheetContainerMap.get("/com/sun/javafx/css/ua1.css");
        assertNotNull(container);
        assertTrue(container.parentUsers.contains(parent0));
        assertTrue(container.parentUsers.contains(parent1));

        sm.forget(parent0);

        assertTrue(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua1.css"));
        assertFalse(container.parentUsers.contains(parent0));
        assertTrue(container.parentUsers.contains(parent1));

        sm.forget(parent1);

        assertFalse(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua1.css"));
        assertFalse(container.parentUsers.contains(parent0));
        assertFalse(container.parentUsers.contains(parent1));
    }

    @Test
    public void testForgetScene() {

        Scene scene = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        sm.forget(scene);

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);
    }

    @Test
    public void testForgetScene_withUAStylesheet() {

        Scene scene = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();
        
        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

        sm.forget(scene);

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

    }

    @Test
    public void testForgetScene_withTwoScenes() {
        Scene scene0 = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene0.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        Scene scene1 = new Scene(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        scene1.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene0.getRoot().applyCss();
        scene1.getRoot().applyCss();

        // even though there are two scenes using this stylesheet, there should only be one container.
        assertEquals(1, sm.userAgentStylesheetContainers.size());

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

        sm.forget(scene0);

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        // we should still have ua1.css since scene1 is still using it
        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        sm.forget(scene1);

        // having forgotten scene1, userAgentStylesheetContainers should be empty.
        assertTrue(sm.userAgentStylesheetContainers.isEmpty());
    }

    @Test
    public void testForgetSubScene() {

        Pane subSceneRoot = new Pane(new Rectangle(){{ getStyleClass().add("rect"); }});
        SubScene subScene = new SubScene(subSceneRoot, 100, 100);
        Scene scene = new Scene(new Group(subScene));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        sm.forget(subScene);

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);
    }

    @Test
    public void testForgetSubScene_withUAStylesheet() {

        Pane subSceneRoot = new Pane(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        SubScene subScene = new SubScene(subSceneRoot, 100, 100);
        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        Scene scene = new Scene(new Group(subScene));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

        sm.forget(subScene);

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

    }

    @Test
    public void testForgetSubScene_with_UAStylesheet_and_ParentStylesheet() {

        // make sure forget(SubScene) get's children with stylesheets
        Group group = new Group(new Rectangle(){{ getStyleClass().add("rect"); }});
        group.getStylesheets().add("/com/sun/javafx/css/ua2.css");
        Pane subSceneRoot = new Pane(group);
        SubScene subScene = new SubScene(subSceneRoot, 100, 100);
        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        Scene scene = new Scene(new Group(subScene));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

        sm.forget(subScene);

        // forgetting the scene should not affect the platform user-agent stylesheets
        index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1, index);

    }

    @Test
    public void testChangeSubSceneStylesheet() {

        Pane subSceneRoot = new Pane(new Group(new Rectangle(){{ getStyleClass().add("rect"); }}));
        SubScene subScene = new SubScene(subSceneRoot, 100, 100);
        Scene scene = new Scene(new Group(subScene));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        scene.getRoot().applyCss();

        scene.getRoot().applyCss();

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        sm.forget(subScene);

        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua2.css");
        scene.getRoot().applyCss();

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua2.css");
        assertEquals(0, index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(-1,index);

    }

    @Test
    public void testFindMatchingStyles_defaultStyleSheet() {

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        Rectangle rect = new Rectangle(){{ getStyleClass().add("rect"); }};
        Scene scene = new Scene(new Group(rect));

        StyleMap matchingStyles = sm.findMatchingStyles(rect, null, null);
        Map<String,List<CascadingStyle>> styleMap = matchingStyles.getCascadingStyles();

        assertTrue(styleMap.containsKey("-fx-fill"));

        List<CascadingStyle> styles = styleMap.get("-fx-fill");
        assertEquals(1, styles.size());

        Object obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.RED, obj);
    }

    @Test
    public void testFindMatchingStyles_defaultStyleSheet_sceneUserAgentStylesheet() {

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        Rectangle rect = new Rectangle(){{ getStyleClass().add("rect"); }};
        Scene scene = new Scene(new Group(rect));
        scene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        StyleMap matchingStyles = sm.findMatchingStyles(rect, null, null);
        Map<String,List<CascadingStyle>> styleMap = matchingStyles.getCascadingStyles();

        scene.getRoot().applyCss();

        // scene stylesheet should totally replace default
        assertFalse(styleMap.containsKey("-fx-fill"));
        assertTrue(styleMap.containsKey("-fx-stroke"));

        List<CascadingStyle> styles = styleMap.get("-fx-stroke");
        assertEquals(1, styles.size());

        Object obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.YELLOW, obj);
    }

    @Test
    public void testFindMatchingStyles_defaultStyleSheet_sceneUserAgentStylesheet_sceneAuthorStylesheet() {

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        Rectangle rect = new Rectangle(){{ getStyleClass().add("rect"); }};
        Scene scene = new Scene(new Group(rect));
        scene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        scene.getStylesheets().add("/com/sun/javafx/css/ua2.css");

        StyleMap matchingStyles = sm.findMatchingStyles(rect, null, null);
        Map<String,List<CascadingStyle>> styleMap = matchingStyles.getCascadingStyles();

        scene.getRoot().applyCss();

        // ua2.css has fill
        assertTrue(styleMap.containsKey("-fx-fill"));
        assertTrue(styleMap.containsKey("-fx-stroke"));

        // ua1.css and ua2.css have stroke
        List<CascadingStyle> styles = styleMap.get("-fx-stroke");
        assertEquals(2, styles.size());

        Object obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.GREEN, obj);

        // ua0.css and ua2.css have fill, but we shouldn't get anything from ua0
        // since we have a scene user-agent stylesheet
        styles = styleMap.get("-fx-fill");
        assertEquals(1, styles.size());

        obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.BLUE, obj);
    }

    @Test
    public void testFindMatchingStyles_defaultStyleSheet_subSceneUserAgentStylesheet() {

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        Rectangle rect = new Rectangle(){{ getStyleClass().add("rect"); }};
        Pane subSceneRoot = new Pane(rect);
        SubScene subScene = new SubScene(subSceneRoot, 100, 100);
        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        Scene scene = new Scene(new Group(subScene));

        StyleMap matchingStyles = sm.findMatchingStyles(rect, subScene, null);
        Map<String,List<CascadingStyle>> styleMap = matchingStyles.getCascadingStyles();

        scene.getRoot().applyCss();

        // SubScene stylesheet should totally replace default
        assertFalse(styleMap.containsKey("-fx-fill"));
        assertTrue(styleMap.containsKey("-fx-stroke"));

        List<CascadingStyle> styles = styleMap.get("-fx-stroke");
        assertEquals(1, styles.size());

        Object obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.YELLOW, obj);
    }

    @Test
    public void testFindMatchingStyles_defaultStyleSheet_subSceneUserAgentStylesheet_parentStylesheet() {

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        Rectangle rect = new Rectangle(){{ getStyleClass().add("rect"); }};
        Group group = new Group(rect);
        group.getStylesheets().add("/com/sun/javafx/css/ua2.css");
        Pane subSceneRoot = new Pane(group);
        SubScene subScene = new SubScene(subSceneRoot, 100, 100);
        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        Scene scene = new Scene(new Group(subScene));

        StyleMap matchingStyles = sm.findMatchingStyles(rect, subScene, null);
        Map<String,List<CascadingStyle>> styleMap = matchingStyles.getCascadingStyles();

        scene.getRoot().applyCss();

        // ua2.css has fill
        assertTrue(styleMap.containsKey("-fx-fill"));
        assertTrue(styleMap.containsKey("-fx-stroke"));

        // ua1.css and ua2.css have stroke
        List<CascadingStyle> styles = styleMap.get("-fx-stroke");
        assertEquals(2, styles.size());

        Object obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.GREEN, obj);

        // ua0.css and ua2.css have fill, but we shouldn't get anything from ua0
        // since we have a scene user-agent stylesheet
        styles = styleMap.get("-fx-fill");
        assertEquals(1, styles.size());

        obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.BLUE, obj);
    }

    @Test
    public void testSwitchDefaultUserAgentStylesheets() {

        Rectangle rect = new Rectangle(){{ getStyleClass().add("rect"); }};
        Group group = new Group(rect);
        Pane subSceneRoot = new Pane(group);
        SubScene subScene = new SubScene(subSceneRoot, 100, 100);
        Scene scene = new Scene(new Group(subScene));

        StyleManager sm = StyleManager.getInstance();
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua0.css");

        scene.getRoot().applyCss();

        StyleMap matchingStyles = sm.findMatchingStyles(rect, subScene, null);
        Map<String,List<CascadingStyle>> styleMap = matchingStyles.getCascadingStyles();

        // ua0.css has fill
        assertTrue(styleMap.containsKey("-fx-fill"));
        assertFalse(styleMap.containsKey("-fx-stroke"));

        List<CascadingStyle> styles = styleMap.get("-fx-fill");
        assertEquals(1, styles.size());

        Object obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.RED, obj);

        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        matchingStyles = sm.findMatchingStyles(rect, subScene, null);
        styleMap = matchingStyles.getCascadingStyles();

        // ua1.css has  stroke
        assertTrue(styleMap.containsKey("-fx-stroke"));
        assertFalse(styleMap.containsKey("-fx-fill"));

        styles = styleMap.get("-fx-stroke");
        assertEquals(1, styles.size());

        obj = styles.get(0).getParsedValueImpl().convert(null);
        assertEquals(Color.YELLOW, obj);
    }

    @Test
    public void testGetCacheContainer() {

        Rectangle rectangle = new Rectangle();
        SubScene subScene = new SubScene(null, 0, 0);

        StyleManager sm = StyleManager.getInstance();

        // no scene, should return null
        StyleManager.CacheContainer container = sm.getCacheContainer(rectangle, subScene);

        assertNull(container);

        // has scene, should return non-null
        subScene.setRoot(new Group());
        Scene scene = new Scene(new Group(rectangle,subScene));
        container = sm.getCacheContainer(rectangle, subScene);

        assertNotNull(container);

    }

    @Test
    public void testGetCacheContainer_styleable() {
        Rectangle rectangle = new Rectangle();

        StyleManager sm = StyleManager.getInstance();

        // no scene, should return null
        StyleManager.CacheContainer container = sm.getCacheContainer(rectangle, null);

        assertNull(container);

        // has scene, should return non-null
        Scene scene = new Scene(new Group(rectangle));
        container = sm.getCacheContainer(rectangle, null);

        assertNotNull(container);

    }

    @Test
    public void testGetCacheContainer_subScene() {

        SubScene subScene = new SubScene(null, 0, 0);

        StyleManager sm = StyleManager.getInstance();

        // no scene, should return null
        StyleManager.CacheContainer container = sm.getCacheContainer(null, subScene);

        assertNull(container);

        // has scene, should return non-null
        subScene.setRoot(new Group());
        Scene scene = new Scene(new Group(subScene));
        container = sm.getCacheContainer(null, subScene);

        assertNotNull(container);

    }

    @Test
    public void testRT_37025() {

        //
        // The issue in RT-37025 was that the stylesheet container wasn't getting removed even
        // though the parent had been forgotten. The StyleManager#forget(Parent) method didn't
        // look to see if _any_ stylesheet container had the parent as a reference.
        //
        final StyleManager sm = StyleManager.getInstance();

        // This test needs a bit more complexity to the scene-graph
        Group group = null;
        Pane pane = new Pane(
                new Group(
                        new Pane(
                                // I want these to be a Parent, not a Node
                                new Group(new Pane(){{ getStyleClass().add("rect"); }}),
                                group = new Group(new Pane(){{ getStyleClass().add("rect"); }})
                        )
                )
        );
        pane.getStylesheets().add("/com/sun/javafx/css/ua0.css");
        group.getStylesheets().add("/com/sun/javafx/css/ua1.css");

        Group root = new Group(pane);
        Scene scene = new Scene(root);

        root.applyCss();

        assertTrue(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua0.css"));
        StyleManager.StylesheetContainer container = sm.stylesheetContainerMap.get("/com/sun/javafx/css/ua0.css");
        assertEquals(7, container.parentUsers.list.size());

        assertTrue(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua1.css"));
        container = sm.stylesheetContainerMap.get("/com/sun/javafx/css/ua1.css");
        assertEquals(2, container.parentUsers.list.size());

        ((Pane)group.getParent()).getChildren().remove(group);

        assertFalse(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua1.css"));
        assertTrue(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua0.css"));
        container = sm.stylesheetContainerMap.get("/com/sun/javafx/css/ua0.css");
        assertEquals(5, container.parentUsers.list.size());

        scene.setRoot(new Group());
        assertFalse(sm.stylesheetContainerMap.containsKey("/com/sun/javafx/css/ua0.css"));
        assertFalse(StyleManager.cacheContainerMap.containsKey(root));
        assertTrue(StyleManager.cacheContainerMap.containsKey(scene.getRoot()));

    }

    @Test
    public void test_setUserAgentStylesheets() {

        List<String> uaStylesheets = new ArrayList<>();
        Collections.addAll(uaStylesheets, "/com/sun/javafx/css/ua0.css", "/com/sun/javafx/css/ua1.css");

        final StyleManager sm = StyleManager.getInstance();
        sm.setUserAgentStylesheets(uaStylesheets);

        assertEquals(2, sm.platformUserAgentStylesheetContainers.size());
        assertEquals("/com/sun/javafx/css/ua0.css", sm.platformUserAgentStylesheetContainers.get(0).fname);
        assertEquals("/com/sun/javafx/css/ua1.css", sm.platformUserAgentStylesheetContainers.get(1).fname);
    }

    @Test
    public void test_setUserAgentStylesheets_overwrites_existing() {

        List<String> uaStylesheets = new ArrayList<>();
        Collections.addAll(uaStylesheets, "/com/sun/javafx/css/ua0.css");

        final StyleManager sm = StyleManager.getInstance();

        // 1 - overwrite default user agent stylesheet
        sm.platformUserAgentStylesheetContainers.clear();;
        sm.setDefaultUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        assertEquals(1, sm.platformUserAgentStylesheetContainers.size());
        assertEquals("/com/sun/javafx/css/ua1.css", sm.platformUserAgentStylesheetContainers.get(0).fname);

        sm.setUserAgentStylesheets(uaStylesheets);
        assertEquals(1, sm.platformUserAgentStylesheetContainers.size());
        assertEquals("/com/sun/javafx/css/ua0.css", sm.platformUserAgentStylesheetContainers.get(0).fname);

        // 2 - overwrite other user-agent stylesheets
        sm.platformUserAgentStylesheetContainers.clear();;
        sm.addUserAgentStylesheet("/com/sun/javafx/css/ua1.css");
        assertEquals(1, sm.platformUserAgentStylesheetContainers.size());

        sm.setUserAgentStylesheets(uaStylesheets);
        assertEquals(1, sm.platformUserAgentStylesheetContainers.size());
        assertEquals("/com/sun/javafx/css/ua0.css", sm.platformUserAgentStylesheetContainers.get(0).fname);
    }

    @Test
    public void testRT_38687_with_Scene() {

        Rectangle rect = new Rectangle(50,50) {{ getStyleClass().add("rect"); }};
        Scene scene = new Scene(new Group(rect));
        scene.setUserAgentStylesheet("com/sun/javafx/css/ua0.css");
        scene.getRoot().applyCss();

        StyleableProperty<Paint> fillProperty = (StyleableProperty<Paint>)rect.fillProperty();
        assertEquals(StyleOrigin.USER_AGENT, fillProperty.getStyleOrigin());

        scene.setUserAgentStylesheet("com/sun/javafx/css/ua1.css");
        scene.getRoot().applyCss();

        assertEquals(null, fillProperty.getStyleOrigin());

        rect.setFill(Color.GREEN);

        scene.setUserAgentStylesheet("com/sun/javafx/css/rt38637.css");
        scene.getRoot().applyCss();
        assertEquals(StyleOrigin.USER, fillProperty.getStyleOrigin());

    }

    @Test
    public void testRT_38687_with_SubScene() {

        Rectangle rect = new Rectangle(50,50) {{ getStyleClass().add("rect"); }};
        Group group = new Group(rect);
        SubScene subScene = new SubScene(group, 100, 100);
        subScene.setUserAgentStylesheet("com/sun/javafx/css/ua0.css");

        Scene scene = new Scene(new Group(subScene));
        scene.getRoot().applyCss();

        StyleableProperty<Paint> fillProperty = (StyleableProperty<Paint>)rect.fillProperty();
        assertEquals(StyleOrigin.USER_AGENT, fillProperty.getStyleOrigin());

        subScene.setUserAgentStylesheet("com/sun/javafx/css/ua1.css");
        scene.getRoot().applyCss();

        assertEquals(null, fillProperty.getStyleOrigin());

        rect.setFill(Color.GREEN);

        subScene.setUserAgentStylesheet("com/sun/javafx/css/rt38637.css");
        scene.getRoot().applyCss();
        assertEquals(StyleOrigin.USER, fillProperty.getStyleOrigin());

    }

    @Test
    public void testConcurrentAccess() {
        final int NUM_THREADS = 10;
        final Thread[] bgThreads = new Thread[NUM_THREADS];
        final AtomicBoolean err = new AtomicBoolean(false);
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thr = new Thread(() -> {
                try {
                    for (int j = 0; j < 1000; j++) {
                        Scene scene = new Scene(new Group());
                        scene.setUserAgentStylesheet("com/sun/javafx/css/ua0.css");
                        scene.getRoot().applyCss();
                    }
                } catch (RuntimeException ex) {
                    err.set(true);
                    throw ex;
                }
            });
            thr.setName("MyThread-" + i);
            thr.setDaemon(true);
            bgThreads[i] = thr;
        }

        for (Thread thr : bgThreads) {
            thr.start();
        }

        try {
            for (Thread thr : bgThreads) {
                thr.join();
            }
        } catch (InterruptedException ex) {
            fail("Unexpected exception waiting for threads to finish");
        }

        assertFalse("Exception during CSS processing on BG thread", err.get());
    }

}
