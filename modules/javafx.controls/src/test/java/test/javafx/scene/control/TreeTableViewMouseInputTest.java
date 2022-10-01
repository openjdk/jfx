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

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import test.com.sun.javafx.scene.control.behavior.TreeTableViewAnchorRetriever;
import test.com.sun.javafx.scene.control.test.Person;

import static org.junit.Assert.*;

import com.sun.javafx.tk.Toolkit;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;

import javafx.scene.layout.VBox;
import org.junit.After;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//@Ignore("Disabling tests as they fail with OOM in continuous builds")
public class TreeTableViewMouseInputTest {
    private TreeTableView<String> tableView;
    private TreeTableView.TreeTableViewSelectionModel<?> sm;
    private TreeTableView.TreeTableViewFocusModel<?> fm;

    private final TreeTableColumn<String, String> col0 = new TreeTableColumn<String, String>("col0");
    private final TreeTableColumn<String, String> col1 = new TreeTableColumn<String, String>("col1");
    private final TreeTableColumn<String, String> col2 = new TreeTableColumn<String, String>("col2");
    private final TreeTableColumn<String, String> col3 = new TreeTableColumn<String, String>("col3");
    private final TreeTableColumn<String, String> col4 = new TreeTableColumn<String, String>("col4");

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
        root = new TreeItem<String>("Root");             // 0
        child1 = new TreeItem<String>("Child 1");        // 1
        child2 = new TreeItem<String>("Child 2");        // 2
        child3 = new TreeItem<String>("Child 3");        // 3
        subchild1 = new TreeItem<String>("Subchild 1");  // 4
        subchild2 = new TreeItem<String>("Subchild 2");  // 5
        subchild3 = new TreeItem<String>("Subchild 3");  // 6
        child4 = new TreeItem<String>("Child 4");        // 7
        child5 = new TreeItem<String>("Child 5");        // 8
        child6 = new TreeItem<String>("Child 6");        // 9
        child7 = new TreeItem<String>("Child 7");        // 10
        child8 = new TreeItem<String>("Child 8");        // 11
        child9 = new TreeItem<String>("Child 9");        // 12
        child10 = new TreeItem<String>("Child 10");      // 13

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

        tableView = new TreeTableView<String>();
        sm = tableView.getSelectionModel();
        fm = tableView.getFocusModel();

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);

        tableView.setRoot(root);
        tableView.getColumns().setAll(col0, col1, col2, col3, col4);
    }

    @After public void tearDown() {
        if (tableView.getSkin() != null) {
            tableView.getSkin().dispose();
        }
        sm = null;
    }

    /***************************************************************************
     * Util methods
     **************************************************************************/

    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Cells: [");

        ObservableList<? extends TreeTablePosition<?, ?>> cells = sm.getSelectedCells();
        for (TreeTablePosition<?,?> tp : cells) {
            sb.append("(");
            sb.append(tp.getRow());
            sb.append(",");
            sb.append(tp.getColumn());
            sb.append("), ");
        }

        sb.append("] \nFocus: (" + fm.getFocusedCell().getRow() + ", " + fm.getFocusedCell().getColumn() + ")");

        TreeTablePosition anchor = getAnchor();
        sb.append(" \nAnchor: (" + (anchor == null ? "null" : anchor.getRow()) +
                ", " + (anchor == null ? "null" : anchor.getColumn()) + ")");
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

    private TreeTablePosition getAnchor() {
        return TreeTableViewAnchorRetriever.getAnchor(tableView);
    }

    private boolean isAnchor(int row) {
        TreeTablePosition tp = new TreeTablePosition(tableView, row, null);
        return getAnchor() != null && getAnchor().equals(tp);
    }

    private boolean isAnchor(int row, int col) {
        TreeTablePosition tp = new TreeTablePosition(tableView, row, tableView.getColumns().get(col));
        return getAnchor() != null && getAnchor().equals(tp);
    }

    private int getItemCount() {
        return root.getChildren().size() + child3.getChildren().size();
    }



    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/

    @Test public void test_rt29833_mouse_select_upwards() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(9);

        // select all from 9 - 7
        VirtualFlowTestUtils.clickOnRow(tableView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(7,8,9));

        // select all from 9 - 7 - 5
        VirtualFlowTestUtils.clickOnRow(tableView, 5, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }

    @Test public void test_rt29833_mouse_select_downwards() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(5);

        // select all from 5 - 7
        VirtualFlowTestUtils.clickOnRow(tableView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(5,6,7));

        // select all from 5 - 7 - 9
        VirtualFlowTestUtils.clickOnRow(tableView, 9, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }

    private int rt30394_count = 0;
//    @Ignore("Ignoring due to RT-37166")
    @Test public void test_rt30394() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        final TableFocusModel fm = tableView.getFocusModel();
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
        VirtualFlowTestUtils.clickOnRow(tableView, 0, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(1, rt30394_count);
        assertTrue(fm.isFocused(0));
    }

    @Test public void test_rt32119() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        // select rows 2, 3, and 4
        VirtualFlowTestUtils.clickOnRow(tableView, 2);
        VirtualFlowTestUtils.clickOnRow(tableView, 4, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertTrue(sm.isSelected(3));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));

        // now shift click on the 2nd row - this should make only row 2 be
        // selected. The bug is that row 4 remains selected also.
        VirtualFlowTestUtils.clickOnRow(tableView, 2, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
        assertFalse(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt31020() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        // set all the columns to be very narrow (so the mouse click happens
        // to the right of them all, out in no-mans land
        tableView.setMinWidth(200);
        tableView.setPrefWidth(200);
        tableView.getColumns().clear();
        col0.setMaxWidth(10);
        tableView.getColumns().add(col0);

        // select rows 1, 2, 3, 4, and 5
        VirtualFlowTestUtils.clickOnRow(tableView, 1, true);
        VirtualFlowTestUtils.clickOnRow(tableView, 5, true, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertTrue(sm.isSelected(3));
        assertTrue(sm.isSelected(4));
        assertTrue(sm.isSelected(5));
    }

    @Test public void test_rt21444_up_cell() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int selectRow = 3;

        tableView.setShowRoot(false);
        final MultipleSelectionModel<TreeItem<String>> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem().getValue());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow - 1, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 2", sm.getSelectedItem().getValue());
        assertEquals("Row 2", sm.getSelectedItems().get(0).getValue());
    }

    @Test public void test_rt21444_down_cell() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int selectRow = 3;

        tableView.setShowRoot(false);
        final MultipleSelectionModel<TreeItem<String>> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem().getValue());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow + 1, KeyModifier.SHIFT);
        assertEquals("Row 4", sm.getSelectedItem().getValue());
    }

    @Test public void test_rt21444_up_row() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int selectRow = 3;

        tableView.setShowRoot(false);
        final MultipleSelectionModel<TreeItem<String>> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem().getValue());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow - 1, true, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 2", sm.getSelectedItem().getValue());
        assertEquals("Row 2", sm.getSelectedItems().get(0).getValue());
    }

    @Test public void test_rt21444_down_row() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int selectRow = 3;

        tableView.setShowRoot(false);
        final MultipleSelectionModel<TreeItem<String>> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem().getValue());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow + 1, true, KeyModifier.SHIFT);
        assertEquals("Row 4", sm.getSelectedItem().getValue());
    }

    @Test public void test_rt32560_cell() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(root, sm.getSelectedItem());
        assertEquals(0, fm.getFocusedIndex());

        VirtualFlowTestUtils.clickOnRow(tableView, 5, KeyModifier.SHIFT);
        assertEquals(5, sm.getSelectedIndex());
        assertEquals(5, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(tableView, 0, KeyModifier.SHIFT);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedItems().size());
    }

    @Test public void test_rt32560_row() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

//        StageLoader sl = new StageLoader(tableView);

        tableView.setShowRoot(true);
        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(root, sm.getSelectedItem());
        assertEquals(0, fm.getFocusedIndex());

        VirtualFlowTestUtils.clickOnRow(tableView, 5, true, KeyModifier.SHIFT);
        assertEquals(5, sm.getSelectedIndex());
        assertEquals(5, fm.getFocusedIndex());
        assertEquals(sm.getSelectedItems().toString(), 6, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(tableView, 0, true, KeyModifier.SHIFT);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(debug(), 1, sm.getSelectedItems().size());

//        sl.dispose();
    }

    @Test public void test_rt_32963() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);

        tableView.setShowRoot(true);
        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(9, tableView.getExpandedItemCount());

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(root, sm.getSelectedItem());
        assertEquals(1, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(tableView, 5, KeyModifier.SHIFT);
        assertEquals("Actual selected index: " + sm.getSelectedIndex(), 5, sm.getSelectedIndex());
        assertEquals("Actual focused index: " + fm.getFocusedIndex(), 5, fm.getFocusedIndex());
        assertTrue("Selected indices: " + sm.getSelectedIndices(), sm.getSelectedIndices().contains(0));
        assertTrue("Selected items: " + sm.getSelectedItems(), sm.getSelectedItems().contains(root));
        assertEquals(6, sm.getSelectedItems().size());
    }

    @Ignore("Test doesn't work - mouse event not firing as expected")
    @Test public void test_rt_33101() {
        final int items = 8;
        root.setValue("New Root");
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);

        StageLoader sl = new StageLoader(tableView);

        tableView.setShowRoot(true);
        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        TreeTableRow rootRow = (TreeTableRow) VirtualFlowTestUtils.getCell(tableView, 0);
        Node disclosureNode = rootRow.getDisclosureNode();

        assertTrue(root.isExpanded());
        assertEquals("New Root", rootRow.getTreeItem().getValue());
        assertNotNull(disclosureNode);
        assertTrue(disclosureNode.isVisible());
        assertTrue(disclosureNode.getScene() != null);
        assertEquals(9, tableView.getExpandedItemCount());

        MouseEventFirer mouse = new MouseEventFirer(disclosureNode);
        mouse.fireMousePressAndRelease();
        assertFalse(root.isExpanded());
        assertEquals(1, tableView.getExpandedItemCount());

        mouse.fireMousePressAndRelease();
        assertTrue(root.isExpanded());
        assertEquals(9, tableView.getExpandedItemCount());

        mouse.dispose();
        sl.dispose();
    }

    private int rt_30626_count = 0;
    @Test public void test_rt_30626() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 1; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);

        tableView.setShowRoot(true);
        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener) c -> {
            while (c.next()) {
                rt_30626_count++;
            }
        });

        Cell cell = VirtualFlowTestUtils.getCell(tableView, 1, 0);
        MouseEventFirer mouse = new MouseEventFirer(cell);

        assertEquals(0, rt_30626_count);
        mouse.fireMousePressAndRelease();
        assertEquals(1, rt_30626_count);

        mouse.fireMousePressAndRelease();
        assertEquals(1, rt_30626_count);
    }

    @Test public void test_rt_33897_rowSelection() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);

        final TreeTableView.TreeTableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);

        VirtualFlowTestUtils.clickOnRow(tableView, 1);
        assertEquals(1, sm.getSelectedCells().size());

        TreeTablePosition pos = sm.getSelectedCells().get(0);
        assertEquals(1, pos.getRow());
        assertNotNull(pos.getTableColumn());
    }

    @Test public void test_rt_33897_cellSelection() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);

        final TreeTableView.TreeTableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        VirtualFlowTestUtils.clickOnRow(tableView, 1);
        assertEquals(1, sm.getSelectedCells().size());

        TreeTablePosition pos = sm.getSelectedCells().get(0);
        assertEquals(1, pos.getRow());
        assertNotNull(pos.getTableColumn());
    }

    @Test public void test_rt_34649() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        final FocusModel fm = tableView.getFocusModel();
        sm.setSelectionMode(SelectionMode.SINGLE);

        assertFalse(sm.isSelected(4));
        assertFalse(fm.isFocused(4));
        VirtualFlowTestUtils.clickOnRow(tableView, 4, true, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(4));
        assertTrue(fm.isFocused(4));

        VirtualFlowTestUtils.clickOnRow(tableView, 4, true, KeyModifier.getShortcutKey());
        assertFalse(sm.isSelected(4));
        assertTrue(fm.isFocused(4));
    }

    @Test public void test_rt_35338() {
        root.getChildren().clear();
        tableView.setRoot(root);
        tableView.getColumns().setAll(col0);

        TableColumnBaseShim.setWidth(col0, 20);
        tableView.setMinWidth(1000);
        tableView.setMinWidth(1000);

        TreeTableRow row = (TreeTableRow) VirtualFlowTestUtils.getCell(tableView, 4);
        assertNotNull(row);
        assertNull(row.getItem());
        assertEquals(4, row.getIndex());

        MouseEventFirer mouse = new MouseEventFirer(row);
        mouse.fireMousePressAndRelease(1, 100, 10);
        mouse.dispose();
    }

    @Test public void test_rt_36509() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(false);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);

        // expand
        assertFalse(root.isExpanded());
        VirtualFlowTestUtils.clickOnRow(tableView, 0, 2);
        assertTrue(root.isExpanded());

        // collapse
        VirtualFlowTestUtils.clickOnRow(tableView, 0, 2);
        assertFalse(root.isExpanded());

        // expand again
        VirtualFlowTestUtils.clickOnRow(tableView, 0, 2);
        assertTrue(root.isExpanded());
    }

    @Test public void test_rt_37069() {
        final int items = 8;
        root.getChildren().clear();
        root.setExpanded(false);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setRoot(root);
        tableView.setFocusTraversable(false);

        Button btn = new Button("Button");
        VBox vbox = new VBox(btn, tableView);

        StageLoader sl = new StageLoader(vbox);
        sl.getStage().requestFocus();
        btn.requestFocus();
        Toolkit.getToolkit().firePulse();
        Scene scene = sl.getStage().getScene();

        assertTrue(btn.isFocused());
        assertFalse(tableView.isFocused());

        ScrollBar vbar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(tableView);
        MouseEventFirer mouse = new MouseEventFirer(vbar);
        mouse.fireMousePressAndRelease();

        assertTrue(btn.isFocused());
        assertFalse(tableView.isFocused());

        sl.dispose();
    }

    @Test public void test_rt_38306_selectFirstRow() {
        test_rt_38306(false);
    }

    @Test public void test_rt_38306_selectFirstTwoRows() {
        test_rt_38306(true);
    }

    private void test_rt_38306(boolean selectTwoRows) {
        TreeItem<Person> root = new TreeItem<>();
        root.getChildren().addAll(
                new TreeItem<>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<>(new Person("Michael", "Brown", "michael.brown@example.com"))
        );

        TreeTableView<Person> table = new TreeTableView<>();
        table.setRoot(root);
        root.setExpanded(true);
        table.setShowRoot(false);

        TreeTableView.TreeTableViewSelectionModel sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));

        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        sm.select(0, firstNameCol);

        assertTrue(sm.isSelected(0, firstNameCol));
        assertEquals(1, sm.getSelectedCells().size());

        TreeTableCell cell_0_0 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 0, 0);
        TreeTableCell cell_0_1 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 0, 1);
        TreeTableCell cell_0_2 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 0, 2);

        TreeTableCell cell_1_0 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 1, 0);
        TreeTableCell cell_1_1 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 1, 1);
        TreeTableCell cell_1_2 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 1, 2);

        MouseEventFirer mouse = selectTwoRows ?
                new MouseEventFirer(cell_1_2) : new MouseEventFirer(cell_0_2);

        mouse.fireMousePressAndRelease(KeyModifier.SHIFT);

        assertTrue(sm.isSelected(0, firstNameCol));
        assertTrue(sm.isSelected(0, lastNameCol));
        assertTrue(sm.isSelected(0, emailCol));

        if (selectTwoRows) {
            assertTrue(sm.isSelected(1, firstNameCol));
            assertTrue(sm.isSelected(1, lastNameCol));
            assertTrue(sm.isSelected(1, emailCol));
        }

        assertEquals(selectTwoRows ? 6 : 3, sm.getSelectedCells().size());

        assertTrue(cell_0_0.isSelected());
        assertTrue(cell_0_1.isSelected());
        assertTrue(cell_0_2.isSelected());

        if (selectTwoRows) {
            assertTrue(cell_1_0.isSelected());
            assertTrue(cell_1_1.isSelected());
            assertTrue(cell_1_2.isSelected());
        }
    }

    @Test public void test_rt_38464_rowSelection() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<>(new Person("Michael", "Brown", "michael.brown@example.com")));

        TreeTableView<Person> table = new TreeTableView<>();

        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);

        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));

        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearSelection();
        sm.select(0, lastNameCol);

        assertTrue(sm.isSelected(0, lastNameCol));
        assertEquals(1, sm.getSelectedCells().size());

        TreeTableCell cell_4_2 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 4, 1);

        MouseEventFirer mouse = new MouseEventFirer(cell_4_2);
        mouse.fireMousePressAndRelease(KeyModifier.SHIFT);

        // we are in row selection mode, so all cells in the selected rows should
        // be selected. We test this per-cell, but also per-row.
        for (int row = 0; row < 5; row++) {
            // test that the selection model is accurate
            assertTrue(sm.isSelected(row, firstNameCol));
            assertTrue(sm.isSelected(row, lastNameCol));
            assertTrue(sm.isSelected(row, emailCol));
            assertTrue(sm.isSelected(row));

            // and assert that the visuals are accurate
            // (TableCells should not be selected, but TableRows should be)
            for (int column = 0; column < 3; column++) {
                if (row == 4 && column == 2) {
                    // bizarrely cell (4,2), i.e. the bottom-right cell consisting
                    // of Michael Brown's email address, doesn't exist.
                    continue;
                }
                TreeTableCell cell = (TreeTableCell) VirtualFlowTestUtils.getCell(table, row, column);
                assertFalse("cell[row: " + row + ", column: " + column + "] is selected, but shouldn't be", cell.isSelected());
            }
            TreeTableRow cell = (TreeTableRow) VirtualFlowTestUtils.getCell(table, row);
            assertTrue(cell.isSelected());
        }
    }

    @Test public void test_rt_38464_cellSelection() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<>(new Person("Michael", "Brown", "michael.brown@example.com")));

        TreeTableView<Person> table = new TreeTableView<>();

        TreeItem<Person> root = new TreeItem<>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);

        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));

        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearSelection();
        sm.select(0, emailCol);
        table.getFocusModel().focus(0, emailCol);

        assertTrue(sm.isSelected(0, emailCol));
        assertEquals(1, sm.getSelectedCells().size());

        TreeTableCell cell_4_2 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 4, 2);
        assertEquals(emailCol, cell_4_2.getTableColumn());

        new MouseEventFirer(cell_4_2).fireMousePressAndRelease(KeyModifier.SHIFT);

        for (int row = 0; row < 5; row++) {
            // test that the selection model is accurate
            assertFalse(sm.isSelected(row, firstNameCol));
            assertFalse(sm.isSelected(row, lastNameCol));
            assertTrue(sm.isSelected(row, emailCol));
            assertTrue(sm.isSelected(row));

            // and assert that the visuals are accurate
            // (some TableCells should be selected, but TableRows should not be)
            for (int column = 0; column < 3; column++) {
                if (row == 4 && column == 2) {
                    // bizarrely cell (4,2), i.e. the bottom-right cell consisting
                    // of Michael Brown's email address, doesn't exist.
                    continue;
                }
                TreeTableCell cell = (TreeTableCell) VirtualFlowTestUtils.getCell(table, row, column);
                assertEquals(column == 2 ? true : false, cell.isSelected());
            }
            TreeTableRow cell = (TreeTableRow) VirtualFlowTestUtils.getCell(table, row);
            assertFalse(cell.isSelected());
        }
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_cellSelection_singleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(true, true);
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_cellSelection_multipleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(true, false);
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_rowSelection_singleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(false, true);
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_rowSelection_multipleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(false, false);
    }

    private void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(boolean cellSelection, boolean singleSelection) {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<>(new Person("Michael", "Brown", "michael.brown@example.com")));

        TreeTableView<Person> table = new TreeTableView<>();

        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);

        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));

        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        TreeTableView.TreeTableViewSelectionModel<Person> sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(cellSelection);
        sm.setSelectionMode(singleSelection ? SelectionMode.SINGLE : SelectionMode.MULTIPLE);

        TreeTableCell cell_0_0 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 0, 0);
        TreeTableCell cell_0_1 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 0, 1);
        TreeTableCell cell_0_2 = (TreeTableCell) VirtualFlowTestUtils.getCell(table, 0, 2);

        sm.clearSelection();

        // click on cell (0,0).
        new MouseEventFirer(cell_0_0).fireMousePressAndRelease();

        if (cellSelection) {
            // Because we are in cell selection mode, this has the effect of
            // selecting just the one cell.
            assertTrue(sm.isSelected(0));
            assertTrue(sm.isSelected(0, firstNameCol));
            assertFalse(sm.isSelected(0, lastNameCol));
            assertFalse(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            assertEquals(0, sm.getSelectedCells().get(0).getRow());
            assertEquals(firstNameCol, sm.getSelectedCells().get(0).getTableColumn());
        } else {
            // Because we are in row selection mode, this has
            // the effect of selecting all cells and the backing row. However, the
            // selected cell will be (0, firstNameCol) only
            assertTrue(sm.isSelected(0));
            assertTrue(sm.isSelected(0, firstNameCol));
            assertTrue(sm.isSelected(0, lastNameCol));
            assertTrue(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            assertEquals(0, sm.getSelectedCells().get(0).getRow());
            assertEquals(firstNameCol, sm.getSelectedCells().get(0).getTableColumn());
        }

        // click on cell (0,1).
        new MouseEventFirer(cell_0_1).fireMousePressAndRelease();

        if (cellSelection) {
            // Everything should remain the same, except the
            // column of the single selected cell should change to lastNameCol.
            assertTrue(sm.isSelected(0));
            assertFalse(sm.isSelected(0, firstNameCol));
            assertTrue(sm.isSelected(0, lastNameCol));
            assertFalse(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            TreeTablePosition<?,?> cell = sm.getSelectedCells().get(0);
            assertEquals(0, cell.getRow());
            assertEquals(lastNameCol, cell.getTableColumn());
        } else {
            // Everything should remain the same, except the
            // column of the single selected cell should change to lastNameCol.
            assertTrue(sm.isSelected(0));
            assertTrue(sm.isSelected(0, firstNameCol));
            assertTrue(sm.isSelected(0, lastNameCol));
            assertTrue(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            TreeTablePosition<?,?> cell = sm.getSelectedCells().get(0);
            assertEquals(0, cell.getRow());
            assertEquals(lastNameCol, cell.getTableColumn());
        }
    }

    @Test public void test_jdk_8147823() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<>(new Person("Michael", "Brown", "michael.brown@example.com")));

        TreeTableView<Person> table = new TreeTableView<>();

        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);

        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        table.getColumns().addAll(firstNameCol);

        TreeTableView.TreeTableViewSelectionModel<Person> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        AtomicInteger hitCount = new AtomicInteger();
        sm.getSelectedItems().addListener((ListChangeListener)c -> {
            hitCount.incrementAndGet();

            // The overarching issue this test ensures is that we do not end up with a null in the items list.
            // Because of the nature of the bug, we copy the items into a temporary list, and analyse that for nulls
            List<?> copy = new ArrayList<>(sm.getSelectedItems());
            assertFalse(copy.contains(null));
        });

        // select all
        VirtualFlowTestUtils.clickOnRow(table, 0, true, KeyModifier.getShortcutKey());
        assertEquals(1, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(table, 1, true, KeyModifier.getShortcutKey());
        assertEquals(2, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(table, 2, true, KeyModifier.getShortcutKey());
        assertEquals(3, hitCount.get());
        assertEquals(3, sm.getSelectedIndices().size());
        isSelected(0,1,2);

        // start deselecting, row 1, and then row 0
        VirtualFlowTestUtils.clickOnRow(table, 1, true, KeyModifier.getShortcutKey());
        assertEquals(4, hitCount.get());
        VirtualFlowTestUtils.clickOnRow(table, 0, true, KeyModifier.getShortcutKey());
        assertEquals(5, hitCount.get());

        // we should not have null / -1 selected, we should have row 2 selected
        assertEquals(1, sm.getSelectedIndices().size());
        isSelected(2);
        isNotSelected(-1, 0, 1);

        assertNotNull(sm.getSelectedItems().get(0));
        assertNotNull(sm.getSelectedItem());
    }
}
