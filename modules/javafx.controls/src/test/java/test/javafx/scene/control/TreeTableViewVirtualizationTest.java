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
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
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

class TreeTableViewVirtualizationTest {

    private StageLoader stageLoader;
    private TreeTableView<String> treeTableView;

    @BeforeEach
    void setUp() {
        treeTableView = new TreeTableView<>();
        treeTableView.setFixedCellSize(24);
        treeTableView.setPrefWidth(300);
        treeTableView.setShowRoot(false);
        treeTableView.setRoot(new TreeItem<>());
        treeTableView.getRoot().getChildren()
                .addAll(new TreeItem<>("1"), new TreeItem<>("2"), new TreeItem<>("3"), new TreeItem<>("4"));

        for (int index = 0; index < 5; index++) {
            TreeTableColumn<String, String> tableColumn = new TreeTableColumn<>(String.valueOf(index));
            tableColumn.setPrefWidth(100);
            treeTableView.getColumns().add(tableColumn);
        }

        stageLoader = new StageLoader(treeTableView);

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
        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationClearColumns() {
        treeTableView.getColumns().clear();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 0);
        }
    }

    @Test
    void testHorizontalVirtualizationAddColumnsOneByOne() {
        treeTableView.getColumns().clear();
        Toolkit.getToolkit().firePulse();

        TreeTableColumn<String, String> tableColumn = new TreeTableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        treeTableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 1);
        }

        tableColumn = new TreeTableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        treeTableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 2);
        }

        tableColumn = new TreeTableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        treeTableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        tableColumn = new TreeTableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        treeTableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        tableColumn = new TreeTableColumn<>("Column");
        tableColumn.setPrefWidth(100);
        treeTableView.getColumns().add(tableColumn);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveFirstColumn() {
        treeTableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveLastColumn() {
        treeTableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationAddFirstColumn() {
        TreeTableColumn<String, String> tableColumn = new TreeTableColumn<>("NEW");
        tableColumn.setPrefWidth(50);
        treeTableView.getColumns().addFirst(tableColumn);

        // Needs a double pulse so that the viewport breadth is correctly calculated.
        Toolkit.getToolkit().firePulse();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 4);
        }

        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(treeTableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationAddLastColumn() {
        TreeTableColumn<String, String> tableColumn = new TreeTableColumn<>("NEW");
        tableColumn.setPrefWidth(50);
        treeTableView.getColumns().addLast(tableColumn);

        // Needs a double pulse so that the viewport breadth is correctly calculated.
        Toolkit.getToolkit().firePulse();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(treeTableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 4);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveAllColumnsFromLast() {
        treeTableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        treeTableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        treeTableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 2);
        }

        treeTableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 1);
        }

        treeTableView.getColumns().removeLast();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 0);
        }
    }

    @Test
    void testHorizontalVirtualizationRemoveAllColumnsFromFirst() {
        treeTableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        treeTableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        treeTableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 2);
        }

        treeTableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 1);
        }

        treeTableView.getColumns().removeFirst();
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 0);
        }
    }

    /**
     * This test does the same as JavaFX is doing when reordering a column (via dragging).
     * We expect that the cells match with the table columns.
     */
    @Test
    void testHorizontalVirtualizationMoveColumn() {
        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);

            IndexedCell<?> row = getRow(index);
            for (int cellIndex = 0; cellIndex < row.getChildrenUnmodifiable().size(); cellIndex++) {
                Node cell = row.getChildrenUnmodifiable().get(cellIndex);
                if (cell instanceof TreeTableCell<?, ?> tableCell) {
                    assertSame(treeTableView.getColumns().get(cellIndex), tableCell.getTableColumn());
                }
            }
        }

        TreeTableColumn<String, ?> columnToMove = treeTableView.getColumns().getLast();

        List<TreeTableColumn<String, ?>> allColumns = new ArrayList<>(treeTableView.getColumns());
        allColumns.remove(columnToMove);
        allColumns.addFirst(columnToMove);

        treeTableView.getColumns().setAll(allColumns);

        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);

            IndexedCell<?> row = getRow(index);
            for (int cellIndex = 0; cellIndex < row.getChildrenUnmodifiable().size(); cellIndex++) {
                Node cell = row.getChildrenUnmodifiable().get(cellIndex);
                if (cell instanceof TreeTableCell<?, ?> tableCell) {
                    assertSame(treeTableView.getColumns().get(cellIndex), tableCell.getTableColumn());
                }
            }
        }
    }

    @Test
    void testHorizontalVirtualizationInitialChangeFixedCellSize() {
        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }

        treeTableView.setFixedCellSize(-1);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 5);
        }

        treeTableView.setFixedCellSize(24);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationScrolledToEnd() {
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(treeTableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationScrolledToEndAndNearStart() {
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(treeTableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        scrollBar.setValue(10);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 4);
        }
    }

    @Test
    void testHorizontalVirtualizationScrolledToEndAndNearStartAndStart() {
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(treeTableView);

        scrollBar.setValue(scrollBar.getMax());
        Toolkit.getToolkit().firePulse();

        scrollBar.setValue(10);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 4);
        }

        scrollBar.setValue(0);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 3);
        }
    }

    @Test
    void testHorizontalVirtualizationIncreaseTableWidth() {
        treeTableView.setPrefWidth(400);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 4);
        }
    }

    @Test
    void testHorizontalVirtualizationDecreaseTableWidth() {
        treeTableView.setPrefWidth(200);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 2);
        }
    }

    @Test
    void testHorizontalVirtualizationIncreaseColumnSize() {
        treeTableView.getColumns().getFirst().setPrefWidth(200);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 2);
        }
    }

    @Test
    void testHorizontalVirtualizationDecreaseColumnSize() {
        treeTableView.getColumns().getFirst().setPrefWidth(50);
        Toolkit.getToolkit().firePulse();

        for (int index = 0; index < treeTableView.getRoot().getChildren().size(); index++) {
            assertCellCountInRow(index, 4);
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
        treeTableView.setPrefHeight(500);
        treeTableView.setPrefWidth(500);

        final String cellValue = "NOT VISIBLE";

        for (int index = 0; index < 15; index++) {
            TreeTableColumn<String, String> tableColumn = new TreeTableColumn<>(String.valueOf(index));
            tableColumn.setPrefWidth(100);
            tableColumn.setCellValueFactory(_ -> new SimpleStringProperty(cellValue));
            treeTableView.getColumns().add(tableColumn);
        }

        for (int index = 0; index < 100; index++) {
            treeTableView.getRoot().getChildren().add(new TreeItem<>(String.valueOf(index)));
        }

        Toolkit.getToolkit().firePulse();

        VirtualScrollBar vbar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(treeTableView);
        VirtualScrollBar hbar = VirtualFlowTestUtils.getVirtualFlowHorizontalScrollbar(treeTableView);

        vbar.setValue(vbar.getMax());
        Toolkit.getToolkit().firePulse();

        hbar.setValue(hbar.getMax());
        Toolkit.getToolkit().firePulse();

        treeTableView.setPrefHeight(300);
        Toolkit.getToolkit().firePulse();

        hbar.setValue(0);
        Toolkit.getToolkit().firePulse();

        vbar.setValue(0.85);
        Toolkit.getToolkit().firePulse();

        VirtualFlow<IndexedCell<?>> virtualFlow = VirtualFlowShim.getVirtualFlow(treeTableView.getSkin());
        List<IndexedCell<?>> rows = VirtualFlowShim.getCells(virtualFlow);
        for (IndexedCell<?> row : rows) {
            for (Node cell : row.getChildrenUnmodifiable()) {
                if (cell instanceof TreeTableCell<?, ?> tableCell) {
                    assertNotEquals(cellValue, tableCell.getItem());
                }
            }
        }
    }

    private IndexedCell<?> getRow(int index) {
        return VirtualFlowTestUtils.getVirtualFlow(treeTableView).getVisibleCell(index);
    }

    private int getCellCount(IndexedCell<?> row) {
        return row.getChildrenUnmodifiable().size();
    }

    private void assertCellCountInRow(int index, int count) {
        IndexedCell<?> row = getRow(index);
        assertEquals(count, getCellCount(row));
    }

}
