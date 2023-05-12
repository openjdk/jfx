/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableHeaderRowShim;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link TableHeaderRow} of a {@link TableView}.
 */
public class TableViewTableHeaderRowTest {

    private TableView<String> tableView;
    private TableHeaderRow tableHeaderRow;
    private Pane cornerRegion;

    private StageLoader stageLoader;

    @BeforeEach
    void setup() {
        tableView = new TableView<>();
        tableView.setTableMenuButtonVisible(true);

        for (int index = 0; index < 10; index++) {
            tableView.getColumns().addAll(new TableColumn<String, String>("Column" + index));
        }

        stageLoader = new StageLoader(tableView);

        tableHeaderRow = VirtualFlowTestUtils.getTableHeaderRow(tableView);
        cornerRegion = TableHeaderRowShim.getCornerRegion(tableHeaderRow);
    }

    @AfterEach
    void cleanup() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    @Test
    void testTableMenuButtonVisibility() {
        assertTrue(cornerRegion.isVisible());

        tableView.setTableMenuButtonVisible(false);

        assertFalse(cornerRegion.isVisible());
    }

    @Test
    void testColumnPopupMenuInitializing() {
        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);
        assertNull(columnPopupMenu);

        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);
        assertNotNull(columnPopupMenu);

        assertEquals(tableView.getColumns().size(), columnPopupMenu.getItems().size());

        for (int index = 0; index < 10; index++) {
            String columnText = tableView.getColumns().get(index).getText();
            String columnPopupItemText = columnPopupMenu.getItems().get(index).getText();
            assertEquals(columnText, columnPopupItemText);
        }
    }

    @Test
    void testColumnPopupMenuColumnTextChanged() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        TableColumn<String, ?> firstColumn = tableView.getColumns().get(0);
        MenuItem firstMenuItem = columnPopupMenu.getItems().get(0);
        assertEquals(firstColumn.getText(), firstMenuItem.getText());

        String newColumnText = "MyNewColumnText";
        assertNotEquals(newColumnText, firstMenuItem.getText());

        firstColumn.setText(newColumnText);
        assertEquals(newColumnText, firstMenuItem.getText());
    }

    @Test
    void testColumnPopupMenuColumnVisibility() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        TableColumn<String, ?> firstColumn = tableView.getColumns().get(0);
        CheckMenuItem firstMenuItem = (CheckMenuItem) columnPopupMenu.getItems().get(0);
        assertEquals(firstColumn.isVisible(), firstMenuItem.isSelected());
        assertTrue(firstMenuItem.isSelected());

        firstColumn.setVisible(false);

        assertEquals(firstColumn.isVisible(), firstMenuItem.isSelected());
        assertFalse(firstMenuItem.isSelected());

        firstMenuItem.setSelected(true);

        assertEquals(firstColumn.isVisible(), firstMenuItem.isSelected());
        assertTrue(firstColumn.isVisible());
    }

    @Test
    void testColumnPopupMenuColumnVisibilityBound() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        TableColumn<String, ?> firstColumn = tableView.getColumns().get(0);
        MenuItem firstMenuItem = columnPopupMenu.getItems().get(0);
        assertFalse(firstMenuItem.isDisable());

        SimpleBooleanProperty visibilityBinding = new SimpleBooleanProperty(false);
        firstColumn.visibleProperty().bind(visibilityBinding);

        // Add a column to trigger the column popup menu rebuild.
        tableView.getColumns().add(new TableColumn<>("new"));

        firstMenuItem = columnPopupMenu.getItems().get(0);
        assertTrue(firstMenuItem.isDisable());
    }

    @Test
    void testColumnPopupMenuColumnsAdded() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        int itemSize = columnPopupMenu.getItems().size();
        assertEquals(tableView.getColumns().size(), columnPopupMenu.getItems().size());

        tableView.getColumns().addAll(new TableColumn<>("new1"), new TableColumn<>("new2"), new TableColumn<>("new3"));

        assertEquals(tableView.getColumns().size(), columnPopupMenu.getItems().size());
        assertTrue(columnPopupMenu.getItems().size() > itemSize);
    }

    @Test
    void testColumnPopupMenuColumnsRemoved() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        int itemSize = columnPopupMenu.getItems().size();
        assertEquals(tableView.getColumns().size(), columnPopupMenu.getItems().size());

        tableView.getColumns().remove(tableView.getColumns().size() - 3, tableView.getColumns().size());

        assertEquals(tableView.getColumns().size(), columnPopupMenu.getItems().size());
        assertTrue(columnPopupMenu.getItems().size() < itemSize);
    }

}
