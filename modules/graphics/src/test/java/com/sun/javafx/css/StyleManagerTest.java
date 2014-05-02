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
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;

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
        assertEquals(0,index);
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

//        sm.findMatchingStyles(scene.getRoot(), null, null);

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

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

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

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

        int index = indexOf(sm.platformUserAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(0,index);

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua0.css");
        assertEquals(-1, index);

        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua1.css");

        index = indexOf(sm.userAgentStylesheetContainers,"/com/sun/javafx/css/ua1.css");
        assertEquals(0,index);

        sm.forget(subScene);

        subScene.setUserAgentStylesheet("/com/sun/javafx/css/ua2.css");

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

}
