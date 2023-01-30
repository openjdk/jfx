/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static javafx.scene.control.TreeTableColumn.editCommitEvent;
import static javafx.scene.control.TreeTableColumn.*;
import static org.junit.Assert.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellEditEvent;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;

/**
 * Test cell edit event for TableColumn: must not throw NPE in accessors (JDK-8269871).
 */
public class CellEditEventOfTreeTableColumnTest {

    private TreeTableView<String> table;
    private TreeTableColumn<String, String> editingColumn;

//------------ default commit handler

    @Test
    public void testDefaultOnCommitHandlerTablePositionWithNullTable() {
        String edited = "edited";
        TreeTablePosition<String, String> pos = new TreeTablePosition<>(null, 1, editingColumn);
        CellEditEvent<String, String> event = new CellEditEvent<>(table, pos, editCommitEvent(), edited);
        Event.fireEvent(editingColumn, event);
    }

    @Test
    public void testDefaultOnCommitHandlerNullTablePosition() {
        String edited = "edited";
        CellEditEvent<String, String> event = new CellEditEvent<>(table, null, editCommitEvent(), edited);
        Event.fireEvent(editingColumn, event);
    }

  //---------------- accessors in CellEditEvent

    @Test
    public void testNullTablePositionGetTableView() {
        CellEditEvent<String, String> ev = new CellEditEvent<>(table, null, editAnyEvent(), null);
        assertNull("treeTable must be null if pos is null", ev.getTreeTableView());
    }

    @Test
    public void testNullTablePositionGetTableColumn() {
        CellEditEvent<String, String> ev = new CellEditEvent<>(table, null, editAnyEvent(), null);
        assertNull("column must be null for null pos", ev.getTableColumn());
    }

    @Test
    public void testNullTablePositionGetOldValue() {
        CellEditEvent<String, String> ev = new CellEditEvent<>(table, null, editAnyEvent(), null);
        assertNull("oldValue must be null for null pos", ev.getOldValue());
    }

    @Test
    public void testNullTablePositionGetRowValue() {
        CellEditEvent<String, String> ev = new CellEditEvent<>(table, null, editAnyEvent(), null);
        assertNull("rowValue must be null for null pos", ev.getRowValue());
    }

    @Test
    public void testNullTablePositionGetNewValue() {
        String editedValue = "edited";
        CellEditEvent<String, String> ev = new CellEditEvent<>(table, null, editAnyEvent(), editedValue);
        assertEquals("editedValue must be available for null pos", editedValue, ev.getNewValue());
    }

    @Test
    public void testTablePositionWithNullTable() {
        String editedValue = "edited";
        TreeTablePosition<String, String> pos = new TreeTablePosition<>(null, 1, editingColumn);
        CellEditEvent<String, String> ev = new CellEditEvent<>(table, pos, editAnyEvent(), editedValue);
        assertNull("rowValue must be null for null pos", ev.getRowValue());
    }

// ------------- event source

    @Test
    public void testNullTable() {
        new CellEditEvent<>(null, // null table must not throw NPE
                new TreeTablePosition<>(null, -1, null), editAnyEvent(), null);
    }

    @Test
    public void testCellEditEventDifferentSource() {
        assertCellEditEvent(new TreeTableView<>());
    }

    @Test
    public void testCellEditEventSameSource() {
        assertCellEditEvent(table);
    }

    @Test
    public void testCellEditEventNullSource() {
        assertCellEditEvent(null);
    }

    /**
     * Creates a CellEditEvent with the given source, not-null position and asserts
     * all properties of the event.
     *
     * @param source the source of the event
     */
    private void assertCellEditEvent(TreeTableView<String> source) {
        int editingRow = 1;
        String editedValue = "edited";
        TreeItem<String> rowValue = table.getTreeItem(editingRow);
        String oldValue = rowValue.getValue();
        TreeTablePosition<String, String> pos = new TreeTablePosition<>(table, editingRow, editingColumn);
        CellEditEvent<String,String> ev = new CellEditEvent<>(source, pos, editAnyEvent(), editedValue);
        if (source != null) {
            assertEquals(source, ev.getSource());
        }
        assertCellEditEventState(ev, table, editingColumn, pos, editedValue, oldValue, rowValue);
    }

    /**
     * Asserts all properties of the event against the given expected values.
     */
    private <S, T> void assertCellEditEventState(CellEditEvent<S, T> event,
            TreeTableView<S> table, TreeTableColumn<S, T> tableColumn, TreeTablePosition<S, T> pos,
            T newValue, T oldValue, TreeItem<S> rowValue) {
        assertEquals(newValue, event.getNewValue());
        assertEquals(oldValue, event.getOldValue());
        assertEquals(rowValue, event.getRowValue());
        assertEquals(tableColumn, event.getTableColumn());
        assertEquals(pos, event.getTreeTablePosition());
        assertEquals(table, event.getTreeTableView());
    }

//------------ init

    @Before public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });

        TreeItem<String> root = new TreeItem<>("root");
        root.setExpanded(true);
        ObservableList<String> model = FXCollections.observableArrayList("Four", "Five", "Fear");
        root.getChildren().addAll(model.stream().map(TreeItem::new).collect(Collectors.toList()));
        table = new TreeTableView<>(root);
        editingColumn = new TreeTableColumn<>("TEST");
        table.getColumns().addAll(editingColumn);
        editingColumn.setCellValueFactory(e -> e.getValue().valueProperty());
    }

    @After
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

}
