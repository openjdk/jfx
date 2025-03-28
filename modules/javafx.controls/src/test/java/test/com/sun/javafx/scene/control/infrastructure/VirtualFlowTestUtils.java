/*
 * Copyright (c) 2013, 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.javafx.scene.control.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableRow;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.NestedTableColumnHeader;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkinBase;
import javafx.scene.control.skin.VirtualFlow;
import javafx.util.Callback;
import com.sun.javafx.scene.control.LabeledText;
import com.sun.javafx.scene.control.VirtualScrollBar;

public class VirtualFlowTestUtils {

    public static void assertListContainsItemsInOrder(final List items, final Object... expected) {
        assertEquals(expected.length, items.size());
        for (int i = 0; i < expected.length; i++) {
            Object item = items.get(i);
            assertEquals(expected[i], item);
        }
    }

    public static void clickOnRow(final Control control, int row, KeyModifier... modifiers) {
        clickOnRow(control, row, 1, false, modifiers);
    }

    public static void clickOnRow(final Control control, int row, int clickCount, KeyModifier... modifiers) {
        clickOnRow(control, row, clickCount, false, modifiers);
    }

    public static void clickOnRow(final Control control, int row, boolean ignoreChildren, KeyModifier... modifiers) {
        clickOnRow(control, row, 1, ignoreChildren, modifiers);
    }

    // ignore children allows clicking on the row, rather than a child in that row.
    // This is good for testing, for example, TableRowBehavior
    public static void clickOnRow(final Control control, int row, int clickCount, boolean ignoreChildren, KeyModifier... modifiers) {
        IndexedCell cell = VirtualFlowTestUtils.getCell(control, row);

        if (! ignoreChildren && ((cell instanceof TableRow) || (cell instanceof TreeTableRow))) {
            for (Node n : cell.getChildrenUnmodifiable()) {
                if (! (n instanceof IndexedCell)) {
                    continue;
                }
                IndexedCell<?> childCell = (IndexedCell<?>)n;
                MouseEventFirer mouse = new MouseEventFirer(childCell);
                mouse.fireMousePressAndRelease(clickCount, modifiers);
                mouse.dispose();
                break;
            }
        } else {
            if (ignoreChildren) {
                // special case when we want to click on the row rather than its
                // children (e.g. TableRow rather than its TableCell)
                MouseEventFirer mouse = new MouseEventFirer(cell);
                mouse.fireMousePressed(cell.getWidth(), cell.getHeight() / 2.0, modifiers);
                mouse.fireMouseReleased(cell.getWidth(), cell.getHeight() / 2.0, modifiers);
                mouse.dispose();
            } else {
                MouseEventFirer mouse = new MouseEventFirer(cell);
                mouse.fireMousePressAndRelease(clickCount, modifiers);
                mouse.dispose();
            }
        }
    }

    public static void assertRowsEmpty(final Control control, final int startRow, final int endRow) {
        assertRows(control, startRow, endRow, true);
    }

    public static void assertRowsNotEmpty(final Control control, final int startRow, final int endRow) {
        assertRows(control, startRow, endRow, false);
    }

    public static void assertCellEmpty(IndexedCell cell) {
        if (cell instanceof TableRow || cell instanceof TreeTableRow) {
            for (Node n : cell.getChildrenUnmodifiable()) {
                if (! (n instanceof IndexedCell)) {
                    continue;
                }
                IndexedCell<?> childCell = (IndexedCell<?>)n;
                assertCellEmpty(childCell);
            }
        } else {
            final String text = cell.getText();
            assertTrue(text == null || text.isEmpty(), "Expected null, found '" + text + "'");

            final Node graphic = cell.getGraphic();
            assertTrue(graphic == null, "Expected null graphic, found " + graphic);
        }
    }

    public static void assertCellNotEmpty(IndexedCell cell) {
        if (cell instanceof TableRow || cell instanceof TreeTableRow) {
            for (Node n : cell.getChildrenUnmodifiable()) {
                if (! (n instanceof IndexedCell)) {
                    continue;
                }
                IndexedCell<?> childCell = (IndexedCell<?>)n;
                assertCellNotEmpty(childCell);
            }
        } else {
            final String text = cell.getText();
            final Node graphic = cell.getGraphic();
            assertTrue((text != null && ! text.isEmpty()) || graphic != null, "Expected a non-null text or graphic property");
        }
    }

    private static void assertRows(final Control control, final int startRow, final int endRow, final boolean expectEmpty) {
        Callback<IndexedCell<?>, Void> callback = indexedCell -> {
            boolean hasChildrenCell = false;
            for (Node n : indexedCell.getChildrenUnmodifiable()) {
                if (! (n instanceof IndexedCell)) {
                    continue;
                }
                hasChildrenCell = true;
                IndexedCell<?> childCell = (IndexedCell<?>)n;

                if (expectEmpty) {
                    assertCellEmpty(childCell);
                } else {
                    assertCellNotEmpty(childCell);
                }
            }

            if (! hasChildrenCell) {
                if (expectEmpty) {
                    assertCellEmpty(indexedCell);
                } else {
                    assertCellNotEmpty(indexedCell);
                }
            }
            return null;
        };

        assertCallback(control, startRow, endRow, callback);
    }

    public static void assertCellTextEquals(final Control control, final int index, final String... expected) {
        if (expected == null || expected.length == 0) return;

        Callback<IndexedCell<?>, Void> callback = indexedCell -> {
            if (indexedCell.getIndex() != index) return null;

            if (expected.length == 1) {
                assertEquals(expected[0], indexedCell.getText());
            } else {
                int jump = 0;
                for (int i = 0; i < expected.length; i++) {
                    Node childNode = indexedCell.getChildrenUnmodifiable().get(i + jump);
                    String text = null;
                    if (! (childNode instanceof IndexedCell)) {
                        jump++;
                        continue;
                    }

                    text = ((IndexedCell) childNode).getText();
                    assertEquals(expected[i], text);
                }
            }
            return null;
        };

        assertCallback(control, index, index + 1, callback);
    }

    public static void assertTableCellTextEquals(final Control control, final int row, final int column, final String expected) {
        Callback<IndexedCell<?>, Void> callback = indexedCell -> {
            if (indexedCell.getIndex() != row) return null;

            int _column = column;

            IndexedCell cell = (IndexedCell) indexedCell.getChildrenUnmodifiable().get(_column);
            assertEquals(expected, cell.getText());
            return null;
        };

        assertCallback(control, row, row + 1, callback);
    }

    // used by TreeView / TreeTableView to ensure the correct indentation
    // (although note that it has only been developed so far for TreeView)
    public static void assertLayoutX(final Control control, final int startRow, final int endRow, final double expectedLayoutX) {
        Callback<IndexedCell<?>, Void> callback = indexedCell -> {
            List<Node> childrenOfCell = indexedCell.getChildrenUnmodifiable();
            LabeledText labeledText = null;
            for (int j = 0; j < childrenOfCell.size(); j++) {
                Node child = childrenOfCell.get(j);
                if (child instanceof LabeledText) {
                    labeledText = (LabeledText) child;
                }
            }

            String error = "Element in row " + indexedCell.getIndex() + " has incorrect indentation. "
                    + "Expected " + expectedLayoutX + ", but found " + labeledText.getLayoutX();
            assertEquals(expectedLayoutX, labeledText.getLayoutX(), 0.0, error);
            return null;
        };

        assertCallback(control, startRow, endRow, callback);
    }

    public static int getCellCount(final Control control) {
        return getVirtualFlow(control).getCellCount();
    }

    public static IndexedCell getCell(final Control control, final int index) {
        return getVirtualFlow(control).getCell(index);
    }

    public static IndexedCell getCell(final Control control, final int row, final int column) {
        IndexedCell rowCell = getVirtualFlow(control).getCell(row);
        if (column == -1) {
            return rowCell;
        }

        int count = -1;
        for (Node n : rowCell.getChildrenUnmodifiable()) {
            if (! (n instanceof IndexedCell)) {
                continue;
            }
            count++;
            if (count == column) {
                return (IndexedCell) n;
            }
        }
        return null;
    }

    public static void assertCallback(final Control control, final int startRow, final int endRow, final Callback<IndexedCell<?>, Void> callback) {
        final VirtualFlow<?> flow = getVirtualFlow(control);
        final int sheetSize = flow.getCellCount();

        // NOTE: we used to only go to the end of the sheet, but now we go past that if
        // endRow desires. This is to allow for us to test cells that are visually
        // shown, but empty.
        final int end = endRow == -1 ? sheetSize : endRow; //Math.min(endRow, sheetSize);

        for (int row = startRow; row < end; row++) {
            // old approach:
            // callback.call((IndexedCell<?>)sheet.getChildren().get(row));

            // new approach:
            callback.call(flow.getCell(row));
        }
    }

    public static void assertGraphicIsVisible(final Control control, int row) {
        assertGraphicIsVisible(control, row, -1);
    }

    public static void assertGraphicIsVisible(final Control control, int row, int column) {
        Cell cell = getCell(control, row, column);
        Node graphic = cell.getGraphic();
        assertNotNull(graphic);
        assertTrue(graphic.isVisible() && graphic.getOpacity() == 1.0);
    }

    public static void assertGraphicIsNotVisible(final Control control, int row) {
        assertGraphicIsNotVisible(control, row, -1);
    }

    public static void assertGraphicIsNotVisible(final Control control, int row, int column) {
        Cell cell = getCell(control, row, column);
        Node graphic = cell.getGraphic();
        if (graphic == null) {
            return;
        }

        assertNotNull(graphic);
        assertTrue(!graphic.isVisible() || graphic.getOpacity() == 0.0);
    }

    public static void assertCellCount(final Control control, final int expected) {
        assertEquals(getVirtualFlow(control).getCellCount(), expected);
    }

    public static void assertTableHeaderColumnExists(final Control control, final TableColumnBase column, boolean expected) {
        TableHeaderRow headerRow = getTableHeaderRow(control);

        NestedTableColumnHeader rootHeader = getNestedTableColumnHeader(headerRow);
        boolean match = false;
        for (TableColumnHeader header : rootHeader.getColumnHeaders()) {
            match = column.equals(header.getTableColumn());
            if (match) break;
        }

        if (expected) {
            assertTrue(match);
        } else {
            assertFalse(match);
        }
    }

    public static VirtualFlow<?> getVirtualFlow(Control control) {
        StageLoader sl = null;
        if (control.getScene() == null) {
            sl = new StageLoader(control);
        }

        VirtualFlow<?> flow;
        if (control instanceof ComboBox) {
            final ComboBox cb = (ComboBox) control;
            final ComboBoxListViewSkin skin = (ComboBoxListViewSkin) cb.getSkin();
            control = (ListView) skin.getPopupContent();
        }

        flow = (VirtualFlow<?>)control.lookup("#virtual-flow");

        if (sl != null) {
            sl.dispose();
        }

        return flow;
    }

    public static VirtualScrollBar getVirtualFlowVerticalScrollbar(final Control control) {
        return getVirtualFlowScrollbar(control, Orientation.VERTICAL);
    }

    public static VirtualScrollBar getVirtualFlowHorizontalScrollbar(final Control control) {
        return getVirtualFlowScrollbar(control, Orientation.HORIZONTAL);
    }

    // this method must be called with the control having been shown in a
    // stage (e.g. via StageLoader). Be sure to dispose too!
    public static TableHeaderRow getTableHeaderRow(final Control control) {
        if (control.getSkin() == null) {
            throw new IllegalStateException("getTableHeaderRow requires the control to be visible in a stage");
        }

        TableViewSkinBase<?,?,?,?,?> skin = (TableViewSkinBase) control.getSkin();
        TableHeaderRow headerRow = null;
        for (Node n : skin.getChildren()) {
            if (n instanceof TableHeaderRow) {
                headerRow = (TableHeaderRow) n;
                break;
            }
        }

        return headerRow;
    }

    private static VirtualScrollBar getVirtualFlowScrollbar(final Control control, Orientation orientation) {
        VirtualFlow<?> flow = getVirtualFlow(control);
        VirtualScrollBar scrollBar = null;
        for (Node child : flow.getChildrenUnmodifiable()) {
            if (child instanceof VirtualScrollBar) {
                if (((VirtualScrollBar)child).getOrientation() == orientation) {
                    scrollBar = (VirtualScrollBar) child;
                }
            }
        }

        return scrollBar;
    }

    public static TableColumnHeader getTableColumnHeader(Control table, TableColumnBase<?,?> column) {
        TableHeaderRow headerRow = VirtualFlowTestUtils.getTableHeaderRow(table);
        return findColumnHeader(getNestedTableColumnHeader(headerRow), column);
    }

    private static TableColumnHeader findColumnHeader(NestedTableColumnHeader nestedHeader, TableColumnBase<?,?> column) {
        for (TableColumnHeader header : nestedHeader.getColumnHeaders()) {
            if (header instanceof NestedTableColumnHeader) {
                if (column.equals(header.getTableColumn())) {
                    return header;
                }
                TableColumnHeader result = findColumnHeader((NestedTableColumnHeader)header, column);
                if (result != null) {
                    return result;
                }
            } else {
                if (column.equals(header.getTableColumn())) {
                    return header;
                }
            }
        }
        return null;
    }

    public static NestedTableColumnHeader getNestedTableColumnHeader(TableHeaderRow headerRow) {
        NestedTableColumnHeader rootHeader = null;

        for (Node n : headerRow.getChildren()) {
            if (n instanceof NestedTableColumnHeader) {
                rootHeader = (NestedTableColumnHeader) n;
            }
        }
        return rootHeader;
    }
}
