/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.VirtualScrollBar;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.skin.VirtualFlowShim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TableViewVirtualizationTest {

    private StageLoader stageLoader;
    private TableView<String> tableView;

    @BeforeEach
    void setUp() {
        tableView = new TableView<>();
        tableView.setFixedCellSize(24);
        tableView.setPrefWidth(300);
        tableView.setPrefHeight(300);
        tableView.setItems(FXCollections.observableArrayList("1", "2", "3", "4"));

        for (int index = 0; index < 5; index++) {
            TableColumn<String, String> tableColumn = new TableColumn<>(String.valueOf(index));
            tableColumn.setPrefWidth(100);
            tableView.getColumns().add(tableColumn);
        }

        stageLoader = new StageLoader(tableView);

        Toolkit.getToolkit().firePulse();
    }

    @AfterEach
    void cleanUp() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    @Test
    void testHorizontalVirtualizationInitial() {
        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationClearColumns() {
        tableView.getColumns().clear();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 0);
        }
    }

    @Test
    void testHorizontalVirtualizationAddColumnsOneByOne() {
        tableView.getColumns().clear();
        Toolkit.getToolkit().firePulse();

        TableColumn<String, String> tableColumn = new TableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        tableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 1);
        }

        tableColumn = new TableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        tableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 2);
        }

        tableColumn = new TableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        tableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        tableColumn = new TableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        tableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        tableColumn = new TableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        tableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveFirstColumn() {
        tableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveLastColumn() {
        tableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationAddFirstColumn() {
        TableColumn<String, String> tableColumn = new TableColumn<>("NEW");
        tableColumn.setPrefWidth(50);
        tableView.getColumns().addFirst(tableColumn);

        // Needs a double pulse so that the viewport breadth is correctly calculated.
        Toolkit.getToolkit().firePulse();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 4);
        }

        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(tableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationAddLastColumn() {
        TableColumn<String, String> tableColumn = new TableColumn<>("NEW");
        tableColumn.setPrefWidth(50);
        tableView.getColumns().addLast(tableColumn);

        // Needs a double pulse so that the viewport breadth is correctly calculated.
        Toolkit.getToolkit().firePulse();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(tableView);
        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 4);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveAllColumnsFromLast() {
        tableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        tableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        tableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 2);
        }

        tableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 1);
        }

        tableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            assertCellCountInRow(index, 0);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveAllColumnsFromFirst() {
        tableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, getCellCount(row));
        }

        tableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, getCellCount(row));
        }

        tableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(2, getCellCount(row));
        }

        tableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(1, getCellCount(row));
        }

        tableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(0, getCellCount(row));
        }
    }

    /**
     * This test does the same as JavaFX is doing when reordering a column (via dragging).
     * We expect that the cells match with the table columns.
     */
    @Test
    void testHorizontalVirtualizationMoveColumn() {
        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, row.getChildrenUnmodifiable().size());
            for (int cellIndex = 0; cellIndex < row.getChildrenUnmodifiable().size(); cellIndex++) {
                Node cell = row.getChildrenUnmodifiable().get(cellIndex);
                if (cell instanceof TableCell<?, ?> tableCell) {
                    assertSame(tableView.getColumns().get(cellIndex), tableCell.getTableColumn());
                }
            }
        }

        TableColumn<String, ?> columnToMove = tableView.getColumns().getLast();

        List<TableColumn<String, ?>> allColumns = new ArrayList<>(tableView.getColumns());
        allColumns.remove(columnToMove);
        allColumns.addFirst(columnToMove);

        tableView.getColumns().setAll(allColumns);

        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, row.getChildrenUnmodifiable().size());
            for (int cellIndex = 0; cellIndex < row.getChildrenUnmodifiable().size(); cellIndex++) {
                Node cell = row.getChildrenUnmodifiable().get(cellIndex);
                if (cell instanceof TableCell<?, ?> tableCell) {
                    assertSame(tableView.getColumns().get(cellIndex), tableCell.getTableColumn());
                }
            }
        }
    }

    @Test
    void testHorizontalVirtualizationInitialChangeFixedCellSize() {
        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, row.getChildrenUnmodifiable().size());
        }

        tableView.setFixedCellSize(-1);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(5, row.getChildrenUnmodifiable().size());
        }

        tableView.setFixedCellSize(24);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, row.getChildrenUnmodifiable().size());
        }
    }

    @Test
    void testHorizontalVirtualizationScrolledToEnd() {
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(tableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, row.getChildrenUnmodifiable().size());
        }
    }

    @Test
    void testHorizontalVirtualizationScrolledToEndAndStart() {
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(tableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        scrollBar.setValue(0);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, row.getChildrenUnmodifiable().size());
        }
    }

    @Test
    void testHorizontalVirtualizationScrolledToEndAndNearStartAndStart() {
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(tableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        scrollBar.setValue(10);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(4, getCellCount(row));
        }

        scrollBar.setValue(0);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(3, getCellCount(row));
        }
    }

    @Test
    void testHorizontalVirtualizationIncreaseTableWidth() {
        tableView.setPrefWidth(400);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(4, row.getChildrenUnmodifiable().size());
        }
    }

    @Test
    void testHorizontalVirtualizationDecreaseTableWidth() {
        tableView.setPrefWidth(200);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(2, row.getChildrenUnmodifiable().size());
        }
    }

    @Test
    void testHorizontalVirtualizationIncreaseColumnSize() {
        tableView.getColumns().getFirst().setPrefWidth(200);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(2, row.getChildrenUnmodifiable().size());
        }
    }

    @Test
    void testHorizontalVirtualizationDecreaseColumnSize() {
        tableView.getColumns().getFirst().setPrefWidth(50);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < tableView.getItems().size(); index++) {
            IndexedCell<?> row = getRow(index);

            assertEquals(4, row.getChildrenUnmodifiable().size());
        }
    }

    /**
     * More complex test scenario where we:
     * <ul>
     *     <li>1. Scroll to the bottom right first</li>
     *     <li>2. Resize the table</li>
     *     <li>3. Scroll to the left and a little bit up</li>
     *     <li>4. Check, that the cells of the rendered rows do not contain outdated items</li>
     * </ul>
     */
    @Test
    void testHorizontalVirtualizationDoesNotBreakTableCells() {
        tableView.setPrefHeight(500);
        tableView.setPrefWidth(500);

        final String cellValue = "NOT VISIBLE";

        for (int index = 0; index < 15; index++) {
            TableColumn<String, String> tableColumn = new TableColumn<>(String.valueOf(index));
            tableColumn.setPrefWidth(100);
            tableColumn.setCellValueFactory(_ -> new SimpleStringProperty(cellValue));
            tableView.getColumns().add(tableColumn);
        }

        for (int index = 0; index < 100; index++) {
            tableView.getItems().add(String.valueOf(index));
        }

        Toolkit.getToolkit().firePulse();

        VirtualScrollBar vbar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(tableView);
        VirtualScrollBar hbar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(tableView);

        vbar.setValue(vbar.getMax());
        Toolkit.getToolkit().firePulse();

        hbar.setValue(hbar.getMax());
        Toolkit.getToolkit().firePulse();

        tableView.setPrefHeight(300);
        Toolkit.getToolkit().firePulse();

        hbar.setValue(0);
        Toolkit.getToolkit().firePulse();

        vbar.setValue(0.85);
        Toolkit.getToolkit().firePulse();

        VirtualFlow<IndexedCell<?>> virtualFlow = VirtualFlowShim.getVirtualFlow(tableView.getSkin());

        List<IndexedCell<?>> rows = VirtualFlowShim.getCells(virtualFlow);
        for (IndexedCell<?> row : rows) {
            for (Node cell : row.getChildrenUnmodifiable()) {
                if (cell instanceof TableCell<?, ?> tableCell) {
                    assertNotEquals(cellValue, tableCell.getItem());
                }
            }
        }
    }

    private IndexedCell<?> getRow(int index) {
        return VirtualFlowTestUtils.getVirtualFlow(tableView).getVisibleCell(index);
    }

    private int getCellCount(IndexedCell<?> row) {
        return row.getChildrenUnmodifiable().size();
    }

    private void assertCellCountInRow(int index, int count) {
        IndexedCell<?> row = getRow(index);
        assertEquals(count, getCellCount(row));
    }

}
