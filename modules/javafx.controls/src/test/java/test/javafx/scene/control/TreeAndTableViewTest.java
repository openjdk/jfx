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
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

/**
 * Tests for:
 * - cell selection logic JDK-8292353.
 */
public class TreeAndTableViewTest {
    /** TreeTableView with cell selection enabled should not select TreeTableRows */
    @Test
    public void test_TableView_jdk_8292353() {
        TableView<String> table = new TableView<>();
        table.requestFocus();
        table.getColumns().addAll(
            createTableColumn("C0"),
            createTableColumn("C1"),
            createTableColumn("C2")
            );
        table.getItems().addAll(
            "",
            "",
            ""
            );

        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        ObservableList<TablePosition> selectedCells = sm.getSelectedCells();

        // important: actually creates cells
        TableRow row = (TableRow)VirtualFlowTestUtils.getCell(table, 0);
        assertTrue(row == getTableRow(table, 0));

        TableCell c0 = getTableCell(table, 0, 0);
        TableCell c1 = getTableCell(table, 0, 1);
        TableCell c2 = getTableCell(table, 0, 2);

        assertEquals(0, selectedCells.size());
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:0
        mouseClick(c0, KeyModifier.getShortcutKey());

        assertEquals(1, selectedCells.size());
        assertTrue(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(2, selectedCells.size());
        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:2
        mouseClick(c2, KeyModifier.getShortcutKey());

        assertEquals(3, selectedCells.size());
        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());

        // deselect 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(2, selectedCells.size());
        assertTrue(c0.isSelected());
        assertFalse(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());

        // and now with the cell selection off
        sm.setCellSelectionEnabled(false);
        sm.clearSelection();

        // select 0:0
        mouseClick(c0, KeyModifier.getShortcutKey());

        assertEquals(1, selectedCells.size()); // counts selected rows instead of cells
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertTrue(row.isSelected());

        // select 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(0, selectedCells.size());
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:2
        mouseClick(c2, KeyModifier.getShortcutKey());

        assertEquals(1, selectedCells.size());
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertTrue(row.isSelected());

        // deselect 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(0, selectedCells.size());
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());
    }

    /** TreeTableView with cell selection enabled should not select TreeTableRows */
    @Test
    public void test_TreeTableView_jdk_8292353() {
        TreeItem<String> root = new TreeItem<String>("");
        root.setExpanded(true);
        root.getChildren().setAll(
            new TreeItem<>(""),
            new TreeItem<>(""),
            new TreeItem<>("")
            );

        TreeTableView<String> tree = new TreeTableView<>();
        tree.setRoot(root);
        tree.setShowRoot(false);
        tree.requestFocus();
        tree.getColumns().addAll(
            createTreeTableColumn("C0"),
            createTreeTableColumn("C1"),
            createTreeTableColumn("C2")
            );

        TreeTableView.TreeTableViewSelectionModel<String> sm = tree.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        ObservableList<TreeTablePosition<String,?>> selectedCells = sm.getSelectedCells();

        // important: actually creates cells
        TreeTableRow row = (TreeTableRow)VirtualFlowTestUtils.getCell(tree, 0);
        assertTrue(row == getTreeTableRow(tree, 0));

        TreeTableCell c0 = getTreeTableCell(tree, 0, 0);
        TreeTableCell c1 = getTreeTableCell(tree, 0, 1);
        TreeTableCell c2 = getTreeTableCell(tree, 0, 2);

        assertTrue(selectedCells.size() == 0);
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:0
        mouseClick(c0, KeyModifier.getShortcutKey());

        assertEquals(1, selectedCells.size());
        assertTrue(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(2, selectedCells.size());
        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:2
        mouseClick(c2, KeyModifier.getShortcutKey());

        assertEquals(3, selectedCells.size());
        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected()); // JDK-8292353 failure

        // deselect 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(2, selectedCells.size());
        assertTrue(c0.isSelected());
        assertFalse(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected()); // JDK-8292353 failure

        // and now with the cell selection off
        sm.setCellSelectionEnabled(false);
        sm.clearSelection();

        // select 0:0
        mouseClick(c0, KeyModifier.getShortcutKey());

        assertEquals(1, selectedCells.size()); // counts selected rows instead of cells
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertTrue(row.isSelected());

        // select 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(0, selectedCells.size());
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:2
        mouseClick(c2, KeyModifier.getShortcutKey());

        assertEquals(1, selectedCells.size());
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertTrue(row.isSelected());

        // deselect 0:1
        mouseClick(c1, KeyModifier.getShortcutKey());

        assertEquals(0, selectedCells.size());
        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());
    }

    protected static TreeTableCell getTreeTableCell(TreeTableView t, int row, int column) {
        TreeTableColumn col = (TreeTableColumn)t.getColumns().get(column);

        for (Node n: t.lookupAll(".tree-table-cell")) {
            if (n instanceof TreeTableCell c) {
                if (row == c.getTableRow().getIndex()) {
                    if (col == c.getTableColumn()) {
                        return c;
                    }
                }
            }
        }
        throw new Error("TreeTableCell not found at " + row + ":" + column);
    }

    protected static TreeTableRow getTreeTableRow(TreeTableView t, int row) {
        for (Node n: t.lookupAll(".tree-table-row-cell")) {
            if (n instanceof TreeTableRow c) {
                if (row == c.getIndex()) {
                    return c;
                }
            }
        }
        throw new Error("TreeTableRow not found at " + row);
    }

    protected static TreeTableColumn createTreeTableColumn(String name) {
        TreeTableColumn c = new TreeTableColumn(name);
        c.setCellValueFactory((f) -> new SimpleStringProperty("..."));
        return c;
    }

    protected static void mouseClick(EventTarget t, KeyModifier... modifiers) {
        MouseEventFirer m = new MouseEventFirer(t);
        m.fireMousePressAndRelease(modifiers);
        m.fireMouseEvent(MouseEvent.MOUSE_RELEASED, modifiers);
        m.dispose();

        Toolkit.getToolkit().firePulse();
    }

    protected static TableCell getTableCell(TableView t, int row, int column) {
        TableColumn col = (TableColumn)t.getColumns().get(column);

        for (Node n: t.lookupAll(".table-cell")) {
            if (n instanceof TableCell c) {
                if (row == c.getTableRow().getIndex()) {
                    if (col == c.getTableColumn()) {
                        return c;
                    }
                }
            }
        }
        throw new Error("TableCell not found at " + row + ":" + column);
    }

    protected static TableRow getTableRow(TableView t, int row) {
        for (Node n: t.lookupAll(".table-row-cell")) {
            if (n instanceof TableRow c) {
                if (row == c.getIndex()) {
                    return c;
                }
            }
        }
        throw new Error("TableRow not found at " + row);
    }

    protected static TableColumn createTableColumn(String name) {
        TableColumn c = new TableColumn(name);
        c.setCellValueFactory((f) -> new SimpleStringProperty("..."));
        return c;
    }
}
