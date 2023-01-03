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
import java.util.Objects;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.TabObservableList;
import com.sun.javafx.tk.Toolkit;

import static javafx.scene.control.skin.TabPaneSkinShim.*;
import static org.junit.Assert.*;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Tests around keeping the selected tab header in visible range of header area.
 */
public class TabPaneHeaderScrollTest {

    // many tabs, exceeding width
    private static final int TAB_COUNT = 30;
    // subset for qualitative scroll asserts
    private static final int THIRD_OF = TAB_COUNT / 3;
    // tabs to just fit
    private static final int FITTING = 7;

    private Scene scene;
    private Stage stage;
    private Pane root;
    private TabPane tabPane;

//-------- tests around JDK-8252236

    @Test
    public void testMoveBySetAll() {
        showTabPane();
        // select last for max scrolling
        int last = tabPane.getTabs().size() - 1;
        tabPane.getSelectionModel().select(last);
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        Toolkit.getToolkit().firePulse();
        // move selected tab to first
        List<Tab> tabs = new ArrayList<>(tabPane.getTabs());
        tabs.remove(selectedTab);
        tabs.add(0, selectedTab);
        tabPane.getTabs().setAll(tabs);
        Toolkit.getToolkit().firePulse();
        assertEquals("scrolled to leading edge", 0, getHeaderAreaScrollOffset(tabPane), 1);
    }

    /**
     * This test passes without the fix, must pass after as well.
     */
    @Test
    public void testMoveByTabObservableList() {
        showTabPane();
        // select last for max scrolling
        int last = tabPane.getTabs().size() - 1;
        tabPane.getSelectionModel().select(last);
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        Toolkit.getToolkit().firePulse();
        // move selected tab to first
        ((TabObservableList<Tab>) tabPane.getTabs()).reorder(selectedTab, tabPane.getTabs().get(0));
        Toolkit.getToolkit().firePulse();
        assertEquals("scrolled to leading edge", 0, getHeaderAreaScrollOffset(tabPane), 1);
    }

    /**
     * Scroll to last (by selecting it) -> remove last.
     *
     * Without fix, fails by not scrolling at all: the gap is increasing every time the
     * last selected (after removal that's the previous) is removed.
     *
     */
    @Test
    public void testRemoveSelectedAsLast() {
        showTabPane();
        int last = tabPane.getTabs().size() - 1;
        Tab secondLastTab = tabPane.getTabs().get(last - 1);
        Tab lastTab = tabPane.getTabs().get(last);
        // select for max scroll
        tabPane.getSelectionModel().select(last);
        Toolkit.getToolkit().firePulse();

        // at this point, the header is scrolled such that the last is at the very right
        double scrollOffset = getHeaderAreaScrollOffset(tabPane);
        double lastTabOffset = getTabHeaderOffset(tabPane, lastTab);
        double secondLastTabOffset = getTabHeaderOffset(tabPane, secondLastTab);
        // expected change in scroll offset
        double expectedDelta = lastTabOffset - secondLastTabOffset;

        // remove last (== selected)
        tabPane.getTabs().remove(last);
        Toolkit.getToolkit().firePulse();

        assertEquals("scrollOffset adjusted: ", scrollOffset + expectedDelta, getHeaderAreaScrollOffset(tabPane), 1);
    }

    /**
     * Scroll to last (by selecting it) -> select previous -> remove last.
     *
     * This test passes without the fix, must pass after as well.
     */
    @Test
    public void testRemoveLastIfSelectedIsSecondLast() {
        showTabPane();
        int last = tabPane.getTabs().size() - 1;
        Tab lastTab = tabPane.getTabs().get(last);
        int secondLast = last - 1;
        Tab secondLastTab = tabPane.getTabs().get(secondLast);

        // select for max scroll
        tabPane.getSelectionModel().select(last);
        Toolkit.getToolkit().firePulse();

        // at this point, the header is scrolled such that the last is at the very right
        double scrollOffset = getHeaderAreaScrollOffset(tabPane);
        double lastTabOffest = getTabHeaderOffset(tabPane, lastTab);
        double secondeLastTabOffset = getTabHeaderOffset(tabPane, secondLastTab);
        // expected change in scroll offset
        double expectedDelta = lastTabOffest - secondeLastTabOffset;

        // select previous tab
        tabPane.getSelectionModel().select(secondLast);
        Toolkit.getToolkit().firePulse();

         // remove last
        tabPane.getTabs().remove(last);
        Toolkit.getToolkit().firePulse();

        assertEquals("scrollOffset adjusted: ", scrollOffset + expectedDelta, getHeaderAreaScrollOffset(tabPane), 1);
    }

    @Test
    public void testRemoveBefore() {
        showTabPane();
        int selected = 4;
        tabPane.getSelectionModel().select(selected);
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        Toolkit.getToolkit().firePulse();
        // state before tabs modification
        double selectedTabOffset = getTabHeaderOffset(tabPane, selectedTab);
        double scrollOffset = getHeaderAreaScrollOffset(tabPane);
        assertEquals("sanity: tab visible but not scrolled", 0, scrollOffset, 1);

        // scroll selected to leading edge
        setHeaderAreaScrollOffset(tabPane, - selectedTabOffset);
        Toolkit.getToolkit().firePulse();
        assertEquals("sanity: really scrolled", - selectedTabOffset, getHeaderAreaScrollOffset(tabPane), 1);
        tabPane.getTabs().remove(0);
        Toolkit.getToolkit().firePulse();
        assertEquals("scroll offset", - getTabHeaderOffset(tabPane, selectedTab), getHeaderAreaScrollOffset(tabPane), 1);
    }

    @Test
    public void testAddBefore() {
        showTabPane();
        int last = tabPane.getTabs().size() - 1;
        tabPane.getSelectionModel().select(last);
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        Toolkit.getToolkit().firePulse();
        // state before tabs modification
        double selectedTabOffset = getTabHeaderOffset(tabPane, selectedTab);
        double scrollOffset = getHeaderAreaScrollOffset(tabPane);

        Tab added = new Tab("added", new Label("added"));
        tabPane.getTabs().add(0, added);
        Toolkit.getToolkit().firePulse();
        Node addedHeader = getTabHeaderFor(tabPane, added);
        double addedWidth = addedHeader.prefWidth(-1);
        assertEquals("sanity", selectedTabOffset + addedWidth, getTabHeaderOffset(tabPane, selectedTab), 1);
        assertEquals("scroll offset", scrollOffset - addedWidth, getHeaderAreaScrollOffset(tabPane), 1);
    }

    /**
     * Test scroll on changing tabPane width.
     */
    @Test
    public void testDecreaseWidth() {
        assertScrollOnDecreaseSize(Side.TOP);
    }

    /**
     * Test scroll on changing tabPane height.
     */
    @Test
    public void testDecreaseHeight() {
        assertScrollOnDecreaseSize(Side.RIGHT);
    }

    private void assertScrollOnDecreaseSize(Side side) {
        // init and configure tabPane with fitting # of tabs
        TabPane tabPane = createTabPane(FITTING);
        tabPane.setSide(side);
        showTabPane(tabPane);
        tabPane.getSelectionModel().select(tabPane.getTabs().size() - 1);
        Toolkit.getToolkit().firePulse();
        Node header = getSelectedTabHeader(tabPane);
        double tabOffset = getTabHeaderOffset(tabPane, tabPane.getSelectionModel().getSelectedItem());

        // assert scroll state (== no scroll) before resize
        double noScrollOffset = getHeaderAreaScrollOffset(tabPane);
        assertEquals("scrollOffset for fitting tabs", 0, noScrollOffset, 1);
        assertEquals("bounds minX", tabOffset, header.getBoundsInParent().getMinX(), 1);

        // force resize of tabPane by decreasing max size.
        if (side.isHorizontal()) {
            tabPane.setMaxWidth(stage.getWidth()/2);
        } else {
            tabPane.setMaxHeight(stage.getHeight()/2);
        }
        Toolkit.getToolkit().firePulse();

        // assert scroll state after resize
        double scrollOffset = getHeaderAreaScrollOffset(tabPane);
        assertFalse("sanity: not fitting after resize", isTabsFit(tabPane));
        assertTrue("header must be scrolled", scrollOffset < 0);
        assertEquals("bounds minX", tabOffset, - scrollOffset + header.getBoundsInParent().getMinX(), 0);
    }

    /**
     * Sanity test of tabPane configured with FITTING count of tabs.
     * Beware: this might be context dependent - if this fails,
     * testing of change of width/height most probably are unreliable.
     */
    @Test
    public void testTabsFitHorizontal() {
        assertTabsFit(Side.TOP);
    }

    @Test
    public void testTabsFitVertical() {
        assertTabsFit(Side.RIGHT);
    }

    private void assertTabsFit(Side side) {
        TabPane tabPane = createTabPane(FITTING);
        tabPane.setSide(side);
        showTabPane(tabPane);
        assertTrue(isTabsFit(tabPane));
        tabPane.getTabs().add(new Tab("tab + x"));
        Toolkit.getToolkit().firePulse();
        assertFalse(isTabsFit(tabPane));
    }

    /**
     * Test scroll on change side.
     */
    @Test
    public void testChangeSide() {
        showTabPane();
        tabPane.getSelectionModel().select(tabPane.getTabs().size() - 1);
        Toolkit.getToolkit().firePulse();
        tabPane.setSide(Side.BOTTOM);
        Toolkit.getToolkit().firePulse();
        assertScrolledToLastAndBack();
    }

    /**
     * Test scroll to initially selected tab.
     */
    @Test
    public void testInitialSelect() {
        tabPane.getSelectionModel().select(tabPane.getTabs().size() - 1);
        showTabPane();
        assertScrolledToLastAndBack();
    }

    /**
     * Sanity test: selecting tab after showing.
     */
    @Test
    public void testSelect() {
        showTabPane();
        assertEquals(0, getHeaderAreaScrollOffset(tabPane), 1);
        tabPane.getSelectionModel().select(tabPane.getTabs().size() - 1);
        Toolkit.getToolkit().firePulse();
        assertScrolledToLastAndBack();
    }

    /**
     * Qualitative assert that tabHeaderArea of default TabPane is scrolled as expected.
     * The tabPane must be configured to have the last tab selected.
     * Then selects second tab and asserts that headerArea is scrolled back
     * to show the tab at the leading edge.
     */
    private void assertScrolledToLastAndBack() {
        Node firstHeader = getTabHeaderFor(tabPane, tabPane.getTabs().get(0));
        // rough measure for one header
        double scrollPerTab = firstHeader.prefWidth(-1);
        double scrollPerThirdOfTabs = THIRD_OF * scrollPerTab;
        double scrollOffset = getHeaderAreaScrollOffset(tabPane);
        assertTrue("scrollOffset must be negative", scrollOffset < 0);
        assertTrue("scrollOffset " + scrollOffset + "must be much greater than multiple tab widths " + scrollPerThirdOfTabs ,
                scrollPerThirdOfTabs < - scrollOffset);
        tabPane.getSelectionModel().select(1);
        Toolkit.getToolkit().firePulse();
        // scrolled back such that second header is at the leading edge of the header area
        assertEquals("scrollOffset", scrollPerTab, - getHeaderAreaScrollOffset(tabPane), 1);
    }

//------------- setup and initial state testing

    /**
     * Show the default TabPane.
     */
    protected void showTabPane() {
        showTabPane(tabPane);
    }

    /**
     * Ensures the control is shown in an active scenegraph.
     *
     * @param control the control to show
     */
    protected void showTabPane(TabPane tabPane) {
        if (root == null) {
            root = new VBox();
            // need to bound the size here, otherwise it's just as big as needed
            scene = new Scene(root, 600, 600);
            stage = new Stage();
            stage.setScene(scene);
            stage.show();
        }
        // need single child, otherwise the outcome might depend on layout
        root.getChildren().setAll(tabPane);
        // needed if the hierarchy is changed after showing the stage
        Toolkit.getToolkit().firePulse();
        disableAnimations((TabPaneSkin) tabPane.getSkin());
    }

//----------------- setup and initial

    @Test
    public void testShowAlternativeTabPane() {
        // show default tabPane
        showTabPane();
        List<Node> expected = List.of(tabPane);
        assertEquals(expected, root.getChildren());
        // show alternative
        TabPane alternativeTabPane = createTabPane();
        showTabPane(alternativeTabPane);
        List<Node> alternative = List.of(alternativeTabPane);
        assertEquals(alternative, root.getChildren());
    }

    @Test
    public void testShowTabPane() {
        assertNotNull(tabPane);
        assertSame(Side.TOP, tabPane.getSide());
        showTabPane();
        List<Node> expected = List.of(tabPane);
        assertEquals(expected, root.getChildren());
    }

    protected TabPane createTabPane() {
        return createTabPane(TAB_COUNT);
    }

    protected TabPane createTabPane(int max) {
        TabPane tabPane = new TabPane();
        for (int i = 0; i < max; i++) {
            Tab tab = new Tab("Tab " + i, new Label("Content for " + i));
            tabPane.getTabs().add(tab);
        }
        return tabPane;
    }

    @Before
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
        tabPane = createTabPane();
    }

    @After
    public void cleanup() {
        if (stage != null) stage.hide();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

//------------------- helpers to access tab headers

    public static double getTabHeaderOffset(TabPane tabPane, Tab tab) {
        Objects.requireNonNull(tabPane, "tabPane must not be null");
        Objects.requireNonNull(tab, "tab must not be null");
        if (!tabPane.getTabs().contains(tab)) throw new IllegalStateException("tab must be contained");
        List<Node> headers = getTabHeaders(tabPane);
        double offset = 0;
        for (Node node : headers) {
            if (getTabFor(node) == tab) break;
            offset += node.prefWidth(-1);
        }
        return offset;
    }

    public static Node getSelectedTabHeader(TabPane tabPane) {
        Objects.requireNonNull(tabPane, "tabPane must not be null");
        if (tabPane.getTabs().isEmpty()) throw new IllegalStateException("tabs must not be empty");
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        return getTabHeaderFor(tabPane, tab);
    }

    public static Node getTabHeaderFor(TabPane tabPane, Tab tab) {
        Objects.requireNonNull(tabPane, "tabPane must not be null");
        Objects.requireNonNull(tab, "tab must not be null");
        if (!tabPane.getTabs().contains(tab)) throw new IllegalStateException("tab must be contained");
        List<Node> headers = getTabHeaders(tabPane);
        Optional<Node> tabHeader = headers.stream()
                .filter(node -> getTabFor(node) == tab)
                .findFirst();
        return tabHeader.get();
    }

    public static Tab getTabFor(Node tabHeader) {
        Objects.requireNonNull(tabHeader, "tabHeader must not be null");
        Object tab = tabHeader.getProperties().get(Tab.class);
        if (tab instanceof Tab) return (Tab) tab;
        throw new IllegalStateException("node is not a tabHeader " + tabHeader);
    }

}
