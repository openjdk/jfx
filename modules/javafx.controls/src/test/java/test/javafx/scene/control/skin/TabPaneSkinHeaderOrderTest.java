/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.control.skin.TabPaneSkinShim;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Testing sync'ing of tab headers with tabs.
 * https://bugs.openjdk.java.net/browse/JDK-8222457
 * https://bugs.openjdk.java.net/browse/JDK-8237602
 *
 * All basically the same issue: the listChangeListener is not correctly
 * updating the tab headers.
 */
public class TabPaneSkinHeaderOrderTest {

    private Scene scene;
    private Stage stage;
    private VBox root;
    private TabPane tabPane;

    @Test
    public void testSetAllCollectionWithAdditionalTabs() {
        List<Tab> combined = new ArrayList<>(tabPane.getTabs());
        combined.add(combined.size(), new Tab("t6"));
        tabPane.getTabs().setAll(combined);
        assertSyncTabHeaders();
    }

    @Ignore("JDK-8245528")
    @Test
    public void testSetExistingTabAtDifferentIndex() {
        Tab t5 = tabPane.getTabs().get(4);
        tabPane.getTabs().set(0, t5);
        assertSyncTabHeaders();
    }

    @Ignore("JDK-8245528")
    // Does not fail in system test with tab header animations not disabled.
    // Does not fail with even number of tabs.
    // Looks like that it fails in scenarios when an existing tab's position is
    // same before and after the change in tabs list.
    @Test
    public void testSetAllReverse() {
        Tab t1 = tabPane.getTabs().get(0);
        Tab t2 = tabPane.getTabs().get(1);
        Tab t3 = tabPane.getTabs().get(2);
        Tab t4 = tabPane.getTabs().get(3);
        Tab t5 = tabPane.getTabs().get(4);
        tabPane.getTabs().setAll(t5, t4, t3, t2, t1);
        assertSyncTabHeaders();
    }

    @Test
    public void testSetAllRandomwShuffles() {
        Tab t1 = tabPane.getTabs().get(0);
        Tab t2 = tabPane.getTabs().get(1);
        Tab t3 = tabPane.getTabs().get(2);
        Tab t4 = tabPane.getTabs().get(3);
        Tab t5 = tabPane.getTabs().get(4);

        tabPane.getTabs().setAll(t1, t3, t5, t2, t4);
        assertSyncTabHeaders();

        tabPane.getTabs().setAll(t1, t4, t5);
        assertSyncTabHeaders();

        // @Ignore("JDK-8245528")
        // tabPane.getTabs().setAll(t5, t3, t4, t1, t2);
        // assertSyncTabHeaders();

        tabPane.getTabs().setAll(t4, new Tab("T1"), t5, t3);
        assertSyncTabHeaders();

        tabPane.getTabs().setAll(new Tab("T2"), t2, t5, t4);
        assertSyncTabHeaders();

        // @Ignore("JDK-8245528")
        // tabPane.getTabs().setAll(t3, t5, t4, t2, new Tab("T3"));
        // assertSyncTabHeaders();
    }

    @Test
    public void testRetainSingle() {
        tabPane.getTabs().retainAll(tabPane.getTabs().get(2));
        assertSyncTabHeaders();
    }

    @Test
    public void testRetainMultiple() {
        tabPane.getTabs().retainAll(tabPane.getTabs().get(0),
                                    tabPane.getTabs().get(3));
        assertSyncTabHeaders();
    }

    @Test
    public void testRetainMultipleCollection() {
        List<Tab> retain = List.of(tabPane.getTabs().get(1),
                                   tabPane.getTabs().get(3));
        tabPane.getTabs().retainAll(retain);
        assertSyncTabHeaders();
    }

    @Test
    public void testRemoveSingleIndex() {
        tabPane.getTabs().remove(0);
        assertSyncTabHeaders();
    }

    @Test
    public void testRemoveSingleTab() {
        tabPane.getTabs().remove(tabPane.getTabs().get(3));
        assertSyncTabHeaders();
    }

    @Test
    public void testRemoveMultiple() {
        tabPane.getTabs().removeAll(tabPane.getTabs().get(0),
                                    tabPane.getTabs().get(2));
        assertSyncTabHeaders();
    }

    @Test
    public void testRemoveMultipleCollection() {
        List<Tab> remove = List.of(tabPane.getTabs().get(1),
                                   tabPane.getTabs().get(3));
        tabPane.getTabs().removeAll(remove);
        assertSyncTabHeaders();
    }

    @Test
    public void testRemoveFromTo() {
        tabPane.getTabs().remove(1, 3);
        assertSyncTabHeaders();
    }

    @Test
    public void testAddSingleAtBeginning() {
        tabPane.getTabs().add(0, new Tab("t0"));
        assertSyncTabHeaders();
    }

    @Test
    public void testAddSingleAtEnd() {
        tabPane.getTabs().add(new Tab("t6"));
        assertSyncTabHeaders();
    }

    @Test
    public void testAddSingleAtEndIndex() {
        tabPane.getTabs().add(tabPane.getTabs().size(), new Tab("t6"));
        assertSyncTabHeaders();
    }

    @Test
    public void testAddSingleInMiddle() {
        tabPane.getTabs().add(2, new Tab("tm"));
        assertSyncTabHeaders();
    }

    @Ignore("JDK-8245528")
    @Test
    public void testAddSingleExistingTab() {
        tabPane.getTabs().add(1, tabPane.getTabs().get(3));
        assertSyncTabHeaders();
    }

    @Ignore("JDK-8245528")
    @Test
    public void testAddMultipleExistingTabsAtBeginning() {
        List<Tab> added = List.of(tabPane.getTabs().get(3),
                                  tabPane.getTabs().get(4));
        tabPane.getTabs().addAll(0, added);
        assertSyncTabHeaders();
    }

    @Test
    public void testAddCollectionAtBeginning() {
        List<Tab> added = List.of(new Tab("t-1"), new Tab("t0"));
        tabPane.getTabs().addAll(0, added);
        assertSyncTabHeaders();
    }

    @Test
    public void testAddMultipleAtEnd() {
        tabPane.getTabs().addAll(new Tab("t6"), new Tab("t7"));
        assertSyncTabHeaders();
    }

    @Test
    public void testAddCollectionAtEnd() {
        List<Tab> added = List.of(new Tab("t6"), new Tab("t7"));
        tabPane.getTabs().addAll(tabPane.getTabs().size(), added);
        assertSyncTabHeaders();
    }

    @Test
    public void testAddCollectionInMiddle() {
        List<Tab> added = List.of(new Tab("tm1"), new Tab("tm2"));
        tabPane.getTabs().addAll(2, added);
        assertSyncTabHeaders();
    }

    @Test
    public void testInitialTabOrder() {
        assertSyncTabHeaders();
    }

    protected void assertSyncTabHeaders() {
        assertSyncHeaders(tabPane.getTabs(),
                          TabPaneSkinShim.getTabHeaders(tabPane));
    }

    protected void assertSyncHeaders(List<Tab> tabs, List<Node> headers) {
        assertEquals("sanity: same size", tabs.size(), headers.size());
        for (int i = 0; i < tabs.size(); i++) {
            Tab headerTab = (Tab) headers.get(i).getProperties().get(Tab.class);
            assertSame("tab at " + i + ", is: " + tabs.get(i).getText()
                + " but in header it is: " + headerTab.getText(),
                tabs.get(i), headerTab);
        }
    }


//----------------- setup and initial
    @Test
    public void testSetupState() {
        assertNotNull(tabPane);
        List<Node> expected = List.of(tabPane);
        assertEquals(expected, root.getChildren());
    }

    @After
    public void cleanup() {
        stage.hide();
    }

    @Before
    public void setup() {
        root = new VBox();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        tabPane = new TabPane(new Tab("t1"), new Tab("t2"), new Tab("t3"),
                              new Tab("t4"), new Tab("t5"));
        root.getChildren().add(tabPane);
        stage.show();
        TabPaneSkinShim.disableAnimations((TabPaneSkin)tabPane.getSkin());
    }
}
