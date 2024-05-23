/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.junit.After;
import org.junit.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Contains TableViewRow tests.
 */
public class TableViewRowTest {

    StageLoader stageLoader;

    @After
    public void after() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    /** TableView with cell selection enabled should not select TableRows, see JDK-8292353 */
    @Test
    public void test_TableView_select_all() {
        TableView<String> table = ControlUtils.createTableView();

        stageLoader = new StageLoader(table);
        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        TableColumn<String,?> col0 = table.getColumns().get(0);
        TableColumn<String,?> col1 = table.getColumns().get(1);
        TableColumn<String,?> col2 = table.getColumns().get(2);
        TableRow row = ControlUtils.getTableRow(table, 0);
        TableCell c0 = ControlUtils.getTableCell(table, 0, 0);
        TableCell c1 = ControlUtils.getTableCell(table, 0, 1);
        TableCell c2 = ControlUtils.getTableCell(table, 0, 2);

        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select all cells in the first row
        sm.select(0, col0);
        sm.select(0, col1);
        sm.select(0, col2);

        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());
    }

    /**
     * TableView with cell selection enabled should not select TableRows,
     * even when selected as a group, see JDK-8292353
     */
    @Test
    public void test_TableView_select_all_as_group() {
        TableView<String> table = ControlUtils.createTableView();

        stageLoader = new StageLoader(table);
        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        TableColumn<String,?> col0 = table.getColumns().get(0);
        TableColumn<String,?> col1 = table.getColumns().get(1);
        TableColumn<String,?> col2 = table.getColumns().get(2);
        TableRow row = ControlUtils.getTableRow(table, 0);
        TableCell c0 = ControlUtils.getTableCell(table, 0, 0);
        TableCell c1 = ControlUtils.getTableCell(table, 0, 1);
        TableCell c2 = ControlUtils.getTableCell(table, 0, 2);

        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select all cells in the first row as a group
        sm.select(0, null);

        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());
    }

    /** TableView with cell selection enabled should not select TableRows, see JDK-8292353 */
    @Test
    public void test_TableView_select_all_but_one() {
        TableView<String> table = ControlUtils.createTableView();

        stageLoader = new StageLoader(table);
        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        TableColumn<String,?> col1 = table.getColumns().get(1);
        TableRow row = ControlUtils.getTableRow(table, 0);
        TableCell c0 = ControlUtils.getTableCell(table, 0, 0);
        TableCell c1 = ControlUtils.getTableCell(table, 0, 1);
        TableCell c2 = ControlUtils.getTableCell(table, 0, 2);

        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:0 and 0:2
        sm.select(0, null);
        sm.clearSelection(0, col1);

        assertTrue(c0.isSelected());
        assertFalse(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());
    }

    /**
     * Same index and underlying item should not cause the updateItem(..) method to be called.
     */
    @Test
    public void testSameIndexAndItemShouldNotUpdateItem() {
        AtomicInteger counter = new AtomicInteger();

        TableView<String> table = ControlUtils.createTableView();
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                counter.incrementAndGet();
                super.updateItem(item, empty);
            }
        });

        stageLoader = new StageLoader(table);

        counter.set(0);
        TableRow<String> row = ControlUtils.getTableRow(table, 0);
        row.updateIndex(0);

        assertEquals(0, counter.get());
    }

    /**
     * The contract of a {@link TableRow} is that isItemChanged(..)
     * is called when the index is 'changed' to the same number as the old one, to evaluate if we need to call
     * updateItem(..).
     */
    @Test
    public void testSameIndexIsItemsChangedShouldBeCalled() {
        AtomicBoolean isItemChangedCalled = new AtomicBoolean();

        TableView<String> table = ControlUtils.createTableView();
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected boolean isItemChanged(String oldItem, String newItem) {
                isItemChangedCalled.set(true);
                return super.isItemChanged(oldItem, newItem);
            }
        });

        stageLoader = new StageLoader(table);

        TableRow<String> row = ControlUtils.getTableRow(table, 0);
        row.updateIndex(0);

        assertTrue(isItemChangedCalled.get());
    }
}
