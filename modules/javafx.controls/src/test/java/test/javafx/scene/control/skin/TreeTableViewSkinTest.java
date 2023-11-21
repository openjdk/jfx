/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.NestedTableColumnHeader;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableColumnHeaderShim;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableHeaderRowShim;
import javafx.scene.control.skin.TreeTableViewSkin;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeTableViewSkinTest {

    private StageLoader stageLoader;

    @AfterEach
    void cleanup() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    @Test
    void testInitialColumnResizeNodePositions() {
        TreeTableView<String> treeTableView = new TreeTableView<>();
        for (int i = 0; i < 5; i++) {
            TreeTableColumn<String, String> column = new TreeTableColumn<>("Col " + i);
            column.setMinWidth(100);
            column.setMaxWidth(100);
            treeTableView.getColumns().add(column);
        }

        stageLoader = new StageLoader(treeTableView);

        Node columnResizeLine = treeTableView.lookup(".column-resize-line");
        Node columnReorderOverlay = treeTableView.lookup(".column-overlay");

        TableHeaderRow header = (TableHeaderRow) treeTableView.lookup("TableHeaderRow");
        header.setReordering(true);
        TableHeaderRowShim.setReorderingColumn(header, treeTableView.getColumns().get(0));
        TableHeaderRowShim.setReorderingRegion(header, header.getRootHeader().getColumnHeaders().get(0));

        Toolkit.getToolkit().firePulse();

        assertEquals(1, columnResizeLine.getLayoutX());
        assertEquals(1, columnResizeLine.getLayoutY());

        assertEquals(1, columnReorderOverlay.getLayoutX());
        assertEquals(1 + header.prefHeight(Region.USE_COMPUTED_SIZE), columnReorderOverlay.getLayoutY());
    }

    @Test
    void testColumnResizeNodePositionsWithPadding() {
        TreeTableView<String> treeTableView = new TreeTableView<>();
        treeTableView.setPadding(new Insets(5, 5, 5, 5));
        for (int i = 0; i < 5; i++) {
            TreeTableColumn<String, String> column = new TreeTableColumn<>("Col " + i);
            column.setMinWidth(100);
            column.setMaxWidth(100);
            treeTableView.getColumns().add(column);
        }

        stageLoader = new StageLoader(treeTableView);

        Node columnResizeLine = treeTableView.lookup(".column-resize-line");
        Node columnReorderOverlay = treeTableView.lookup(".column-overlay");

        TableHeaderRow header = (TableHeaderRow) treeTableView.lookup("TableHeaderRow");
        header.setReordering(true);
        TableHeaderRowShim.setReorderingColumn(header, treeTableView.getColumns().get(0));
        TableHeaderRowShim.setReorderingRegion(header, header.getRootHeader().getColumnHeaders().get(0));

        Toolkit.getToolkit().firePulse();

        assertEquals(5, columnResizeLine.getLayoutX());
        assertEquals(5, columnResizeLine.getLayoutY());

        assertEquals(5, columnReorderOverlay.getLayoutX());
        assertEquals(5 + header.prefHeight(Region.USE_COMPUTED_SIZE), columnReorderOverlay.getLayoutY());
    }

    @Test
    void testColumnResizeNodePositionsWithCustomSkin() {
        TreeTableView<String> treeTableView = new TreeTableView<>();
        treeTableView.setSkin(new CustomTreeTableViewSkin<>(treeTableView));
        for (int i = 0; i < 5; i++) {
            TreeTableColumn<String, String> column = new TreeTableColumn<>("Col " + i);
            column.setMinWidth(100);
            column.setMaxWidth(100);
            treeTableView.getColumns().add(column);
        }

        stageLoader = new StageLoader(treeTableView);

        Node columnResizeLine = treeTableView.lookup(".column-resize-line");
        Node columnReorderOverlay = treeTableView.lookup(".column-overlay");

        TableHeaderRow header = (TableHeaderRow) treeTableView.lookup("TableHeaderRow");
        header.setReordering(true);
        TableHeaderRowShim.setReorderingColumn(header, treeTableView.getColumns().get(0));
        TableHeaderRowShim.setReorderingRegion(header, header.getRootHeader().getColumnHeaders().get(0));

        Toolkit.getToolkit().firePulse();

        assertEquals(11, columnResizeLine.getLayoutX());
        assertEquals(11, columnResizeLine.getLayoutY());

        assertEquals(11, columnReorderOverlay.getLayoutX());
        assertEquals(11 + header.prefHeight(Region.USE_COMPUTED_SIZE), columnReorderOverlay.getLayoutY());
    }

    @Test
    void testColumnHeaderReorderCorrectTranslateX() {
        int dragAmount = 20;

        TreeTableView<String> treeTableView = new TreeTableView<>();
        treeTableView.setPadding(new Insets(0, 10, 0, 30));
        for (int i = 0; i < 5; i++) {
            TreeTableColumn<String, String> column = new TreeTableColumn<>("Col " + i);
            column.setMinWidth(100);
            column.setMaxWidth(100);
            treeTableView.getColumns().add(column);
        }

        stageLoader = new StageLoader(treeTableView);

        TableHeaderRow header = (TableHeaderRow) treeTableView.lookup("TableHeaderRow");
        Node columnDragHeader = header.lookup(".column-drag-header");

        assertEquals(0, columnDragHeader.getTranslateX());

        TableColumnHeader tableColumnHeader = header.getRootHeader().getColumnHeaders().get(0);
        Bounds bounds = tableColumnHeader.localToScene(tableColumnHeader.getLayoutBounds());
        TableColumnHeaderShim.columnReordering(tableColumnHeader, bounds.getMinX() + dragAmount, bounds.getMinY());

        assertEquals(dragAmount, columnDragHeader.getTranslateX());
    }

    @Test
    void testHeaderReorderWithinNestedColumns() {
        int width = 100;
        int dragAmount = 20;

        TreeTableView<String> treeTableView = new TreeTableView<>();
        for (int i = 0; i < 2; i++) {
            TreeTableColumn<String, String> column = new TreeTableColumn<>("Col " + i);
            column.setMinWidth(100);
            column.setMaxWidth(100);
            treeTableView.getColumns().add(column);
        }

        TreeTableColumn<String, String> column = new TreeTableColumn<>("Column with nested");
        for (int i = 0; i < 2; i++) {
            TreeTableColumn<String, String> nestedCol = new TreeTableColumn<>("NestedCol " + i);
            nestedCol.setMinWidth(100);
            nestedCol.setMaxWidth(100);
            column.getColumns().add(nestedCol);
        }
        treeTableView.getColumns().add(column);

        stageLoader = new StageLoader(treeTableView);

        TableHeaderRow header = (TableHeaderRow) treeTableView.lookup("TableHeaderRow");
        Node columnDragHeader = header.lookup(".column-drag-header");

        assertEquals(0, columnDragHeader.getTranslateX());

        NestedTableColumnHeader nestedTableColumnHeader =
                (NestedTableColumnHeader) header.getRootHeader().getColumnHeaders().get(2);
        TableColumnHeader tableColumnHeader = nestedTableColumnHeader.getColumnHeaders().get(0);

        Bounds bounds = tableColumnHeader.localToScene(tableColumnHeader.getLayoutBounds());
        TableColumnHeaderShim.columnReordering(tableColumnHeader, bounds.getMinX() + dragAmount, bounds.getMinY());

        // 220, since we have 2 columns to the left with a size of 100 and a dragged this column by 20.
        assertEquals(width * 2 + dragAmount, columnDragHeader.getTranslateX());
    }

    private static class CustomTreeTableViewSkin<S> extends TreeTableViewSkin<S> {

        CustomTreeTableViewSkin(TreeTableView<S> control) {
            super(control);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x + 10, y + 10, w, h);
        }
    }

}
