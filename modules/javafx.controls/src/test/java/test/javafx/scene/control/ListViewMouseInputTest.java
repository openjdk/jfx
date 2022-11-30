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
import javafx.scene.control.Button;
import javafx.scene.control.FocusModel;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.com.sun.javafx.scene.control.behavior.ListViewAnchorRetriever;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

//@Ignore("Disabling tests as they fail with OOM in continuous builds")
public class ListViewMouseInputTest {
    private ListView<String> listView;
    private MultipleSelectionModel<String> sm;
    private FocusModel<String> fm;

    private StageLoader stageLoader;

    @Before public void setup() {
        listView = new ListView<>();
        sm = listView.getSelectionModel();
        fm = listView.getFocusModel();

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        listView.getItems().setAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        sm.clearAndSelect(0);
    }

    @After public void tearDown() {
        listView.getSkin().dispose();

        if (stageLoader != null) {
            stageLoader.dispose();
        }
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
        return ListViewAnchorRetriever.getAnchor(listView);
    }

    private boolean isAnchor(int index) {
        return getAnchor() == index;
    }


    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/

    @Test public void test_rt29833_mouse_select_upwards() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(9);

        // select all from 9 - 7
        VirtualFlowTestUtils.clickOnRow(listView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(7,8,9));

        // select all from 9 - 7 - 5
        VirtualFlowTestUtils.clickOnRow(listView, 5, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }

    @Test public void test_rt29833_mouse_select_downwards() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(5);

        // select all from 5 - 7
        VirtualFlowTestUtils.clickOnRow(listView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(5,6,7));

        // select all from 5 - 7 - 9
        VirtualFlowTestUtils.clickOnRow(listView, 9, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }

    private int rt30394_count = 0;
    @Test public void test_rt30394() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        final FocusModel fm = listView.getFocusModel();
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
        VirtualFlowTestUtils.clickOnRow(listView, 0, KeyModifier.SHIFT);
        assertEquals(1, rt30394_count);
        assertTrue(fm.isFocused(0));
    }

    @Test public void test_rt32119() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        // select rows 2, 3, and 4
        VirtualFlowTestUtils.clickOnRow(listView, 2);
        VirtualFlowTestUtils.clickOnRow(listView, 4, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertTrue(sm.isSelected(3));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));

        // now shift click on the 2nd row - this should make only row 2 be
        // selected. The bug is that row 4 remains selected also.
        VirtualFlowTestUtils.clickOnRow(listView, 2, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
        assertFalse(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt21444_up() {
        final int items = 8;
        listView.getItems().clear();
        for (int i = 1; i <= items; i++) {
            listView.getItems().add("Row " + i);
        }

        final int selectRow = 3;

        final MultipleSelectionModel sm = listView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 4", sm.getSelectedItem());

        VirtualFlowTestUtils.clickOnRow(listView, selectRow - 1, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 3", sm.getSelectedItem());
        assertEquals("Row 3", sm.getSelectedItems().get(0));
    }

    @Test public void test_rt21444_down() {
        final int items = 8;
        listView.getItems().clear();
        for (int i = 1; i <= items; i++) {
            listView.getItems().add("Row " + i);
        }

        final int selectRow = 3;

        final MultipleSelectionModel sm = listView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 4", sm.getSelectedItem());

        VirtualFlowTestUtils.clickOnRow(listView, selectRow + 1, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 5", sm.getSelectedItem());
        assertEquals("Row 5", sm.getSelectedItems().get(1));
    }

    @Test public void test_rt32560_cell() {
        final int items = 8;
        listView.getItems().clear();
        for (int i = 0; i < items; i++) {
            listView.getItems().add("Row " + i);
        }

        final MultipleSelectionModel sm = listView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());

        VirtualFlowTestUtils.clickOnRow(listView, 5, KeyModifier.SHIFT);
        assertEquals(5, sm.getSelectedIndex());
        assertEquals(5, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(listView, 0, KeyModifier.SHIFT);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedItems().size());
    }

    private int rt_30626_count = 0;
    @Test public void test_rt_30626() {
        final int items = 8;
        listView.getItems().clear();
        for (int i = 0; i < items; i++) {
            listView.getItems().add("Row " + i);
        }

        final MultipleSelectionModel sm = listView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        listView.getSelectionModel().getSelectedItems().addListener((ListChangeListener) c -> {
            while (c.next()) {
                rt_30626_count++;
            }
        });

        assertEquals(0, rt_30626_count);
        VirtualFlowTestUtils.clickOnRow(listView, 1);
        assertEquals(1, rt_30626_count);

        VirtualFlowTestUtils.clickOnRow(listView, 1);
        assertEquals(1, rt_30626_count);
    }

    @Test public void test_rt_34649() {
        final int items = 8;
        listView.getItems().clear();
        for (int i = 0; i < items; i++) {
            listView.getItems().add("Row " + i);
        }

        final MultipleSelectionModel sm = listView.getSelectionModel();
        final FocusModel fm = listView.getFocusModel();
        sm.setSelectionMode(SelectionMode.SINGLE);

        assertFalse(sm.isSelected(4));
        assertFalse(fm.isFocused(4));
        VirtualFlowTestUtils.clickOnRow(listView, 4, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(4));
        assertTrue(fm.isFocused(4));

        VirtualFlowTestUtils.clickOnRow(listView, 4, KeyModifier.getShortcutKey());
        assertFalse(sm.isSelected(4));
        assertTrue(fm.isFocused(4));
    }

    @Test public void test_rt_37069() {
        final int items = 8;
        listView.getItems().clear();
        for (int i = 0; i < items; i++) {
            listView.getItems().add("Row " + i);
        }
        listView.setFocusTraversable(false);

        Button btn = new Button("Button");
        VBox vbox = new VBox(btn, listView);

        stageLoader = new StageLoader(vbox);
        stageLoader.getStage().requestFocus();
        btn.requestFocus();
        Toolkit.getToolkit().firePulse();

        assertTrue(btn.isFocused());
        assertFalse(listView.isFocused());

        ScrollBar vbar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(listView);
        MouseEventFirer mouse = new MouseEventFirer(vbar);
        mouse.fireMousePressAndRelease();

        assertTrue(btn.isFocused());
        assertFalse(listView.isFocused());
    }

    @Test public void test_jdk_8147823() {
        final int items = 3;
        listView.getItems().clear();
        for (int i = 0; i < items; i++) {
            listView.getItems().add("Row " + i);
        }
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        AtomicInteger hitCount = new AtomicInteger();
        sm.getSelectedItems().addListener((ListChangeListener<String>) c -> {
            hitCount.incrementAndGet();

            // The overarching issue this test ensures is that we do not end up with a null in the items list.
            // Because of the nature of the bug, we copy the items into a temporary list, and analyse that for nulls
            List<String> copy = new ArrayList<>(sm.getSelectedItems());
            assertFalse(copy.contains(null));
        });

        // select all
        VirtualFlowTestUtils.clickOnRow(listView, 0, KeyModifier.getShortcutKey());
        assertEquals(1, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(listView, 1, KeyModifier.getShortcutKey());
        assertEquals(2, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(listView, 2, KeyModifier.getShortcutKey());
        assertEquals(3, hitCount.get());
        assertEquals(3, sm.getSelectedIndices().size());
        isSelected(0,1,2);

        // start deselecting, row 1, and then row 0
        VirtualFlowTestUtils.clickOnRow(listView, 1, KeyModifier.getShortcutKey());
        assertEquals(4, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(listView, 0, KeyModifier.getShortcutKey());
        assertEquals(5, hitCount.get());

        // we should not have null / -1 selected, we should have row 2 selected
        assertEquals(1, sm.getSelectedIndices().size());
        isSelected(2);
        isNotSelected(-1, 0, 1);

        assertNotNull(sm.getSelectedItems().get(0));
        assertNotNull(sm.getSelectedItem());
    }

    @Test public void testClickWithNullSelectionModelDoesNotThrowNPE() {
        listView.setSelectionModel(null);

        stageLoader = new StageLoader(listView);

        assertDoesNotThrow(()  -> VirtualFlowTestUtils.clickOnRow(listView, 2));
    }

}
