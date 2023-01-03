/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import com.sun.javafx.tk.Toolkit;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.FocusModel;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.com.sun.javafx.scene.control.behavior.TreeViewAnchorRetriever;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

//@Ignore("Disabling tests as they fail with OOM in continuous builds")
public class TreeViewMouseInputTest {
    private TreeView<String> treeView;
    private MultipleSelectionModel<TreeItem<String>> sm;
    private FocusModel<TreeItem<String>> fm;

    private TreeItem<String> root;                  // 0
        private TreeItem<String> child1;            // 1
        private TreeItem<String> child2;            // 2
        private TreeItem<String> child3;            // 3
            private TreeItem<String> subchild1;     // 4
            private TreeItem<String> subchild2;     // 5
            private TreeItem<String> subchild3;     // 6
        private TreeItem<String> child4;            // 7
        private TreeItem<String> child5;            // 8
        private TreeItem<String> child6;            // 9
        private TreeItem<String> child7;            // 10
        private TreeItem<String> child8;            // 11
        private TreeItem<String> child9;            // 12
        private TreeItem<String> child10;           // 13

    @Before public void setup() {
        root = new TreeItem<>("Root");             // 0
        child1 = new TreeItem<>("Child 1");        // 1
        child2 = new TreeItem<>("Child 2");        // 2
        child3 = new TreeItem<>("Child 3");        // 3
        subchild1 = new TreeItem<>("Subchild 1");  // 4
        subchild2 = new TreeItem<>("Subchild 2");  // 5
        subchild3 = new TreeItem<>("Subchild 3");  // 6
        child4 = new TreeItem<>("Child 4");        // 7
        child5 = new TreeItem<>("Child 5");        // 8
        child6 = new TreeItem<>("Child 6");        // 9
        child7 = new TreeItem<>("Child 7");        // 10
        child8 = new TreeItem<>("Child 8");        // 11
        child9 = new TreeItem<>("Child 9");        // 12
        child10 = new TreeItem<>("Child 10");      // 13

        // reset tree structure
        root.getChildren().clear();
        root.setExpanded(true);
        root.getChildren().setAll(child1, child2, child3, child4, child5, child6, child7, child8, child9, child10 );
        child1.getChildren().clear();
        child1.setExpanded(false);
        child2.getChildren().clear();
        child2.setExpanded(false);
        child3.getChildren().clear();
        child3.setExpanded(true);
        child3.getChildren().setAll(subchild1, subchild2, subchild3);
        child4.getChildren().clear();
        child4.setExpanded(false);
        child5.getChildren().clear();
        child5.setExpanded(false);
        child6.getChildren().clear();
        child6.setExpanded(false);
        child7.getChildren().clear();
        child7.setExpanded(false);
        child8.getChildren().clear();
        child8.setExpanded(false);
        child9.getChildren().clear();
        child9.setExpanded(false);
        child10.getChildren().clear();
        child10.setExpanded(false);

        // recreate treeview and gather models
        treeView = new TreeView<>();
        treeView.setRoot(root);
        sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        fm = treeView.getFocusModel();
    }

    @After public void tearDown() {
        treeView.getSkin().dispose();
    }


    /***************************************************************************
     * Util methods
     **************************************************************************/

    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Indices: [");

        List<Integer> indices = sm.getSelectedIndices();
        for (Integer index : indices) {
            sb.append(index);
            sb.append(", ");
        }

        sb.append("] \nFocus: " + fm.getFocusedIndex());
        sb.append(" \nAnchor: " + getAnchor());
        return sb.toString();
    }

    // Returns true if ALL indices are selected
    private boolean isSelected(int... indices) {
        for (int index : indices) {
            if (! sm.isSelected(index)) return false;
        }
        return true;
    }

    // Returns true if ALL indices are NOT selected
    private boolean isNotSelected(int... indices) {
        for (int index : indices) {
            if (sm.isSelected(index)) return false;
        }
        return true;
    }

    private int getAnchor() {
        return TreeViewAnchorRetriever.getAnchor(treeView);
    }

    private boolean isAnchor(int index) {
        return getAnchor() == index;
    }

    private int getItemCount() {
        return root.getChildren().size() + child3.getChildren().size();
    }


    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/

    @Test public void test_rt29833_mouse_select_upwards() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(9);

        // select all from 9 - 7
        VirtualFlowTestUtils.clickOnRow(treeView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(7,8,9));

        // select all from 9 - 7 - 5
        VirtualFlowTestUtils.clickOnRow(treeView, 5, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }

    @Test public void test_rt29833_mouse_select_downwards() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(5);

        // select all from 5 - 7
        VirtualFlowTestUtils.clickOnRow(treeView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(5,6,7));

        // select all from 5 - 7 - 9
        VirtualFlowTestUtils.clickOnRow(treeView, 9, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }

    private int rt30394_count = 0;
    @Test public void test_rt30394() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        final FocusModel fm = treeView.getFocusModel();
        fm.focus(-1);

        fm.focusedIndexProperty().addListener((observable, oldValue, newValue) -> {
            rt30394_count++;
            assertEquals(0, fm.getFocusedIndex());
        });

        // test pre-conditions
        assertEquals(0,rt30394_count);
        assertFalse(fm.isFocused(0));

        // select the first row with the shift key held down. The focus event
        // should only fire once - for focus on 0 (never -1 as this bug shows).
        VirtualFlowTestUtils.clickOnRow(treeView, 0, KeyModifier.SHIFT);
        assertEquals(1, rt30394_count);
        assertTrue(fm.isFocused(0));
    }

    @Test public void test_rt32119() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        // select rows 2, 3, and 4
        VirtualFlowTestUtils.clickOnRow(treeView, 2);
        VirtualFlowTestUtils.clickOnRow(treeView, 4, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertTrue(sm.isSelected(3));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));

        // now shift click on the 2nd row - this should make only row 2 be
        // selected. The bug is that row 4 remains selected also.
        VirtualFlowTestUtils.clickOnRow(treeView, 2, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
        assertFalse(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt21444_up() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int selectRow = 3;

        treeView.setShowRoot(false);
        final MultipleSelectionModel<TreeItem<String>> sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem().getValue());

        VirtualFlowTestUtils.clickOnRow(treeView, selectRow - 1, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 2", sm.getSelectedItem().getValue());
        assertEquals("Row 2", sm.getSelectedItems().get(0).getValue());
    }

    @Test public void test_rt21444_down() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int selectRow = 3;

        treeView.setShowRoot(false);
        final MultipleSelectionModel<TreeItem<String>> sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem().getValue());

        VirtualFlowTestUtils.clickOnRow(treeView, selectRow + 1, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 4", sm.getSelectedItem().getValue());
        assertEquals("Row 4", sm.getSelectedItems().get(1).getValue());
    }

    @Test public void test_rt32560() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(root, sm.getSelectedItem());
        assertEquals(0, fm.getFocusedIndex());

        VirtualFlowTestUtils.clickOnRow(treeView, 5, KeyModifier.SHIFT);
        assertEquals(5, sm.getSelectedIndex());
        assertEquals(5, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(treeView, 0, KeyModifier.SHIFT);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedItems().size());
    }

    @Test public void test_rt_32963() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        treeView.setRoot(root);

        treeView.setShowRoot(true);
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(9, treeView.getExpandedItemCount());

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(root, sm.getSelectedItem());
        assertEquals(1, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(treeView, 5, KeyModifier.SHIFT);
        assertEquals("Actual selected index: " + sm.getSelectedIndex(), 5, sm.getSelectedIndex());
        assertEquals("Actual focused index: " + fm.getFocusedIndex(), 5, fm.getFocusedIndex());
        assertTrue("Selected indices: " + sm.getSelectedIndices(), sm.getSelectedIndices().contains(0));
        assertTrue("Selected items: " + sm.getSelectedItems(), sm.getSelectedItems().contains(root));
        assertEquals(6, sm.getSelectedItems().size());
    }

    private int rt_30626_count = 0;
    @Test public void test_rt_30626() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        treeView.setRoot(root);

        treeView.setShowRoot(true);
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener) c -> {
            while (c.next()) {
                rt_30626_count++;
            }
        });

        assertEquals(0, rt_30626_count);
        VirtualFlowTestUtils.clickOnRow(treeView, 1);
        assertEquals(1, rt_30626_count);

        VirtualFlowTestUtils.clickOnRow(treeView, 1);
        assertEquals(1, rt_30626_count);
    }

    @Test public void test_rt_34649() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        treeView.setRoot(root);

        final MultipleSelectionModel sm = treeView.getSelectionModel();
        final FocusModel fm = treeView.getFocusModel();
        sm.setSelectionMode(SelectionMode.SINGLE);

        assertFalse(sm.isSelected(4));
        assertFalse(fm.isFocused(4));
        VirtualFlowTestUtils.clickOnRow(treeView, 4, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(4));
        assertTrue(fm.isFocused(4));

        VirtualFlowTestUtils.clickOnRow(treeView, 4, KeyModifier.getShortcutKey());
        assertFalse(sm.isSelected(4));
        assertTrue(fm.isFocused(4));
    }

    @Test public void test_rt_36509() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(false);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        treeView.setRoot(root);

        // expand
        assertFalse(root.isExpanded());
        VirtualFlowTestUtils.clickOnRow(treeView, 0, 2);
        assertTrue(root.isExpanded());

        // collapse
        VirtualFlowTestUtils.clickOnRow(treeView, 0, 2);
        assertFalse(root.isExpanded());

        // expand again
        VirtualFlowTestUtils.clickOnRow(treeView, 0, 2);
        assertTrue(root.isExpanded());
    }

    @Test public void test_rt_37069() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(false);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        treeView.setRoot(root);
        treeView.setFocusTraversable(false);

        Button btn = new Button("Button");
        VBox vbox = new VBox(btn, treeView);

        StageLoader sl = new StageLoader(vbox);
        sl.getStage().requestFocus();
        btn.requestFocus();
        Toolkit.getToolkit().firePulse();
        Scene scene = sl.getStage().getScene();

        assertTrue(btn.isFocused());
        assertFalse(treeView.isFocused());

        ScrollBar vbar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(treeView);
        MouseEventFirer mouse = new MouseEventFirer(vbar);
        mouse.fireMousePressAndRelease();

        assertTrue(btn.isFocused());
        assertFalse(treeView.isFocused());

        sl.dispose();
    }

    @Test public void test_jdk_8147823() {
        final int items = 3;
        root.getChildren().clear();
        root.setExpanded(false);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        treeView.setFocusTraversable(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        AtomicInteger hitCount = new AtomicInteger();
        sm.getSelectedItems().addListener((ListChangeListener<TreeItem<String>>) c -> {
            hitCount.incrementAndGet();

            // The overarching issue this test ensures is that we do not end up with a null in the items list.
            // Because of the nature of the bug, we copy the items into a temporary list, and analyse that for nulls
            List<TreeItem<String>> copy = new ArrayList<>(sm.getSelectedItems());
            assertFalse(copy.contains(null));
        });

        // select all
        VirtualFlowTestUtils.clickOnRow(treeView, 0, KeyModifier.getShortcutKey());
        assertEquals(1, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(treeView, 1, KeyModifier.getShortcutKey());
        assertEquals(2, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(treeView, 2, KeyModifier.getShortcutKey());
        assertEquals(3, hitCount.get());
        assertEquals(3, sm.getSelectedIndices().size());
        isSelected(0,1,2);

        // start deselecting, row 1, and then row 0
        VirtualFlowTestUtils.clickOnRow(treeView, 1, KeyModifier.getShortcutKey());
        assertEquals(4, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(treeView, 0, KeyModifier.getShortcutKey());
        assertEquals(5, hitCount.get());

        // we should not have null / -1 selected, we should have row 2 selected
        assertEquals(1, sm.getSelectedIndices().size());
        isSelected(2);
        isNotSelected(-1, 0, 1);

        assertNotNull(sm.getSelectedItems().get(0));
        assertNotNull(sm.getSelectedItem());
    }
}
