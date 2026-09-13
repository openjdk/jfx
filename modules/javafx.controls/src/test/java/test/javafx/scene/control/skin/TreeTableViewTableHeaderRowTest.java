/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableHeaderRowShim;
import javafx.scene.control.skin.TableViewSkinBase;
import javafx.scene.control.skin.TreeTableViewSkin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.binding.ExpressionHelperUtility;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.util.memory.JMemoryBuddy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link TableHeaderRow} of a {@link TreeTableView}.
 */
public class TreeTableViewTableHeaderRowTest {

    private TreeTableView<String> treeTableView;
    private TableHeaderRow tableHeaderRow;
    private Pane cornerRegion;

    private StageLoader stageLoader;

    @BeforeEach
    void setup() {
        treeTableView = new TreeTableView<>();
        treeTableView.setTableMenuButtonVisible(true);

        for (int index = 0; index < 10; index++) {
            treeTableView.getColumns().addAll(new TreeTableColumn<String, String>("Column" + index));
        }

        stageLoader = new StageLoader(treeTableView);

        tableHeaderRow = VirtualFlowTestUtils.getTableHeaderRow(treeTableView);
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

        treeTableView.setTableMenuButtonVisible(false);

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

        assertEquals(treeTableView.getColumns().size(), columnPopupMenu.getItems().size());

        for (int index = 0; index < 10; index++) {
            String columnText = treeTableView.getColumns().get(index).getText();
            String columnPopupItemText = columnPopupMenu.getItems().get(index).getText();
            assertEquals(columnText, columnPopupItemText);
        }
    }

    @Test
    void testColumnPopupMenuColumnTextChanged() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        TreeTableColumn<String, ?> firstColumn = treeTableView.getColumns().get(0);
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

        TreeTableColumn<String, ?> firstColumn = treeTableView.getColumns().get(0);
        CheckMenuItem firstMenuItem = (CheckMenuItem) columnPopupMenu.getItems().get(0);
        assertEquals(firstColumn.isVisible(), firstMenuItem.isSelected());
        assertTrue(firstMenuItem.isSelected());

        firstColumn.setVisible(false);

        firstMenuItem = (CheckMenuItem) columnPopupMenu.getItems().get(0);
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

        TreeTableColumn<String, ?> firstColumn = treeTableView.getColumns().get(0);
        MenuItem firstMenuItem = columnPopupMenu.getItems().get(0);
        assertFalse(firstMenuItem.isDisable());

        SimpleBooleanProperty visibilityBinding = new SimpleBooleanProperty(false);
        firstColumn.visibleProperty().bind(visibilityBinding);

        // Add a column to trigger the column popup menu rebuild.
        treeTableView.getColumns().add(new TreeTableColumn<>("new"));

        firstMenuItem = columnPopupMenu.getItems().get(0);
        assertTrue(firstMenuItem.isDisable());
    }

    @Test
    void testColumnPopupMenuColumnsAdded() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        int itemSize = columnPopupMenu.getItems().size();
        assertEquals(treeTableView.getColumns().size(), columnPopupMenu.getItems().size());

        treeTableView.getColumns().addAll(new TreeTableColumn<>("new1"), new TreeTableColumn<>("new2"),
                new TreeTableColumn<>("new3"));

        assertEquals(treeTableView.getColumns().size(), columnPopupMenu.getItems().size());
        assertTrue(columnPopupMenu.getItems().size() > itemSize);
    }

    @Test
    void testColumnPopupMenuColumnsRemoved() {
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);

        int itemSize = columnPopupMenu.getItems().size();
        assertEquals(treeTableView.getColumns().size(), columnPopupMenu.getItems().size());

        treeTableView.getColumns().remove(treeTableView.getColumns().size() - 3, treeTableView.getColumns().size());

        assertEquals(treeTableView.getColumns().size(), columnPopupMenu.getItems().size());
        assertTrue(columnPopupMenu.getItems().size() < itemSize);
    }

    @Test
    void testOverriddenColumnPopupMenu() {
        treeTableView.setSkin(new CustomTreeTableViewSkin<>(treeTableView));

        tableHeaderRow = VirtualFlowTestUtils.getTableHeaderRow(treeTableView);
        cornerRegion = TableHeaderRowShim.getCornerRegion(tableHeaderRow);

        ContextMenu columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);
        assertNull(columnPopupMenu);

        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        // Since the showColumnMenu() is overridden, the column popup menu should not be created.
        columnPopupMenu = TableHeaderRowShim.getColumnPopupMenu(tableHeaderRow);
        assertNull(columnPopupMenu);
    }

    /**
     * Tests that re-setting the same columns does not cause excessive listener registrations.
     * See also: <a href="https://bugs.openjdk.org/browse/JDK-8341687">JDK-8341687</a>.
     */
    @Test
    void testReSettingColumnsDoesNotCauseExcessiveListeners() {
        for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
            assertEquals(2, getVisibilityListenerCount(column));
        }

        // Trigger the menu once so that it will start listening to column changes.
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        // Now the table menu is listening for changes as well.
        for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
            assertEquals(3, getVisibilityListenerCount(column));
        }

        treeTableView.getColumns().setAll(FXCollections.observableArrayList(treeTableView.getColumns()));
        treeTableView.getColumns().setAll(FXCollections.observableArrayList(treeTableView.getColumns()));

        // The count should be the same still.
        for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
            assertEquals(3, getVisibilityListenerCount(column));
        }
    }

    /**
     * Tests that toggling the column visibility does not cause excessive listener registrations.
     * See also: <a href="https://bugs.openjdk.org/browse/JDK-8341687">JDK-8341687</a>.
     */
    @Test
    void testTogglingColumnVisibilityDoesNotCauseExcessiveListeners() {
        for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
            assertEquals(2, getVisibilityListenerCount(column));
        }

        // Trigger the menu once so that it will start listening to column changes.
        MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
        mouseEventFirer.fireMousePressed();

        // Now the table menu logic is listening for changes as well.
        for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
            assertEquals(3, getVisibilityListenerCount(column));
        }

        for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
            column.setVisible(false);
            column.setVisible(true);
            column.setVisible(false);
            column.setVisible(true);
        }

        // The count should be the same still.
        for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
            assertEquals(3, getVisibilityListenerCount(column));
        }
    }

    /**
     * Tests that re-setting the same columns does not cause memory leaks.
     * See also: <a href="https://bugs.openjdk.org/browse/JDK-8341687">JDK-8341687</a>.
     */
    @Test
    void testReSettingColumnsDoesNotCauseMemoryLeaks() {
        JMemoryBuddy.memoryTest((mem) -> {
            mem.assertCollectable(tableHeaderRow);

            // Trigger the menu once so that it will start listening to column changes.
            MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
            mouseEventFirer.fireMousePressed();

            cornerRegion = null;
            tableHeaderRow = null;

            treeTableView.getColumns().setAll(FXCollections.observableArrayList(treeTableView.getColumns()));
            treeTableView.getColumns().setAll(FXCollections.observableArrayList(treeTableView.getColumns()));

            treeTableView.setSkin(new TreeTableViewSkin<>(treeTableView));
            Toolkit.getToolkit().firePulse();
        });
    }

    /**
     * Tests that toggling the column visibility does not cause memory leaks.
     * See also: <a href="https://bugs.openjdk.org/browse/JDK-8341687">JDK-8341687</a>.
     */
    @Test
    void testTogglingColumnVisibilityDoesNotCauseMemoryLeaks() {
        JMemoryBuddy.memoryTest((mem) -> {
            mem.assertCollectable(tableHeaderRow);

            // Trigger the menu once so that it will start listening to column changes.
            MouseEventFirer mouseEventFirer = new MouseEventFirer(cornerRegion);
            mouseEventFirer.fireMousePressed();

            cornerRegion = null;
            tableHeaderRow = null;

            for (TreeTableColumn<String, ?> column : treeTableView.getColumns()) {
                column.setVisible(false);
                column.setVisible(true);
                column.setVisible(false);
                column.setVisible(true);
            }

            treeTableView.setSkin(new TreeTableViewSkin<>(treeTableView));
            Toolkit.getToolkit().firePulse();
        });
    }

    private int getVisibilityListenerCount(TreeTableColumn<String, ?> column) {
        return ExpressionHelperUtility.getInvalidationListeners(column.visibleProperty()).size();
    }

    private static class CustomTreeTableViewSkin<S> extends TreeTableViewSkin<S> {

        public CustomTreeTableViewSkin(TreeTableView<S> control) {
            super(control);
        }

        @Override
        protected TableHeaderRow createTableHeaderRow() {
            return new CustomTableHeaderRow(this);
        }

        private static class CustomTableHeaderRow extends TableHeaderRow {

            public CustomTableHeaderRow(TableViewSkinBase skin) {
                super(skin);
            }

            @Override
            protected void showColumnMenu(MouseEvent mouseEvent) {
                // noop - overridden for testing
            }
        }
    }

}
