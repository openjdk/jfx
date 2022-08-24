/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.Test;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseEvent;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

/**
 * Tests for: - cell selection logic JDK-8292353.
 */
public class TreeAndTableViewTest {
    /** TreeTableView with cell selection enabled should not select TreeTableRows */
    @Test
    public void test_TableView_jdk_8292353() {
        TableView<String> table = ControlUtils.createTableView();

        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        ObservableList<TablePosition> selectedCells = sm.getSelectedCells();

        StageLoader stageLoader = new StageLoader(table);
        try {
            TableRow row = ControlUtils.getTableRow(table, 0);
            TableCell c0 = ControlUtils.getTableCell(table, 0, 0);
            TableCell c1 = ControlUtils.getTableCell(table, 0, 1);
            TableCell c2 = ControlUtils.getTableCell(table, 0, 2);

            assertEquals(0, selectedCells.size());
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:0
            ControlUtils.mouseClick(c0, KeyModifier.getShortcutKey());

            assertEquals(1, selectedCells.size());
            assertTrue(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(2, selectedCells.size());
            assertTrue(c0.isSelected());
            assertTrue(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:2
            ControlUtils.mouseClick(c2, KeyModifier.getShortcutKey());

            assertEquals(3, selectedCells.size());
            assertTrue(c0.isSelected());
            assertTrue(c1.isSelected());
            assertTrue(c2.isSelected());
            assertFalse(row.isSelected());

            // deselect 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(2, selectedCells.size());
            assertTrue(c0.isSelected());
            assertFalse(c1.isSelected());
            assertTrue(c2.isSelected());
            assertFalse(row.isSelected());

            // and now with the cell selection off
            sm.setCellSelectionEnabled(false);
            sm.clearSelection();

            // select 0:0
            ControlUtils.mouseClick(c0, KeyModifier.getShortcutKey());

            assertEquals(1, selectedCells.size()); // counts selected rows instead of cells
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertTrue(row.isSelected());

            // select 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(0, selectedCells.size());
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:2
            ControlUtils.mouseClick(c2, KeyModifier.getShortcutKey());

            assertEquals(1, selectedCells.size());
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertTrue(row.isSelected());

            // deselect 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(0, selectedCells.size());
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());
        } finally {
            stageLoader.dispose();
        }
    }

    /** TreeTableView with cell selection enabled should not select TreeTableRows */
    @Test
    public void test_TreeTableView_jdk_8292353() {
        TreeTableView<String> tree = ControlUtils.createTreeTableView();

        StageLoader stageLoader = new StageLoader(tree);
        try {
            TreeTableView.TreeTableViewSelectionModel<String> sm = tree.getSelectionModel();
            sm.setSelectionMode(SelectionMode.MULTIPLE);
            sm.setCellSelectionEnabled(true);
            sm.clearSelection();

            ObservableList<TreeTablePosition<String,?>> selectedCells = sm.getSelectedCells();

            TreeTableRow row = ControlUtils.getTreeTableRow(tree, 0);

            TreeTableCell c0 = ControlUtils.getTreeTableCell(tree, 0, 0);
            TreeTableCell c1 = ControlUtils.getTreeTableCell(tree, 0, 1);
            TreeTableCell c2 = ControlUtils.getTreeTableCell(tree, 0, 2);

            assertTrue(selectedCells.size() == 0);
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:0
            ControlUtils.mouseClick(c0, KeyModifier.getShortcutKey());

            assertEquals(1, selectedCells.size());
            assertTrue(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(2, selectedCells.size());
            assertTrue(c0.isSelected());
            assertTrue(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:2
            ControlUtils.mouseClick(c2, KeyModifier.getShortcutKey());

            assertEquals(3, selectedCells.size());
            assertTrue(c0.isSelected());
            assertTrue(c1.isSelected());
            assertTrue(c2.isSelected());
            assertFalse(row.isSelected()); // JDK-8292353 failure

            // deselect 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(2, selectedCells.size());
            assertTrue(c0.isSelected());
            assertFalse(c1.isSelected());
            assertTrue(c2.isSelected());
            assertFalse(row.isSelected()); // JDK-8292353 failure

            // and now with the cell selection off
            sm.setCellSelectionEnabled(false);
            sm.clearSelection();

            // select 0:0
            ControlUtils.mouseClick(c0, KeyModifier.getShortcutKey());

            assertEquals(1, selectedCells.size()); // counts selected rows instead of cells
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertTrue(row.isSelected());

            // select 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(0, selectedCells.size());
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());

            // select 0:2
            ControlUtils.mouseClick(c2, KeyModifier.getShortcutKey());

            assertEquals(1, selectedCells.size());
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertTrue(row.isSelected());

            // deselect 0:1
            ControlUtils.mouseClick(c1, KeyModifier.getShortcutKey());

            assertEquals(0, selectedCells.size());
            assertFalse(c0.isSelected());
            assertFalse(c1.isSelected());
            assertFalse(c2.isSelected());
            assertFalse(row.isSelected());
        } finally {
            stageLoader.dispose();
        }
    }
}
